package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.swf.SwfBinaryWriter
import com.example.core.swf.SwfBuildResult
import com.example.core.swf.SwfBuilder
import com.example.core.swf.SwfParseResult
import com.example.core.swf.SwfParser
import com.example.core.swf.SwfRect
import com.example.core.swf.SwfTag
import com.example.core.swf.SwfTagCode
import com.example.core.swf.SwfValidator
import com.example.core.swf.ValidationResult
import com.example.core.text.ImportResult
import com.example.core.text.TextExtractor
import com.example.core.text.TextObject
import com.example.core.text.TranslationIO
import com.example.data.AppDatabase
import com.example.data.ProjectEntity
import com.example.data.ProjectRepository
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppStrings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

enum class TextFilter { ALL, MODIFIED, UNMODIFIED, EMPTY, STATIC, EDITABLE }
enum class TextSort { ID, FRAME, MODIFIED, LENGTH }

sealed class BuildUiState {
    object Idle : BuildUiState()
    data class Building(val progress: Float, val status: String) : BuildUiState()
    data class Success(val result: SwfBuildResult, val validation: ValidationResult) : BuildUiState()
    data class Error(val message: String, val details: String? = null) : BuildUiState()
}

data class LoadedSwfData(
    val fileName: String,
    val fileUri: String?,
    val cachedFile: File,
    val parseResult: SwfParseResult,
    val originalBytes: ByteArray
)

class SwfEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProjectRepository = ProjectRepository(
        AppDatabase.getDatabase(application).projectDao()
    )

    val recentProjects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _language = MutableStateFlow(AppLanguage.ENGLISH)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _strings = MutableStateFlow(AppStrings(AppLanguage.ENGLISH))
    val strings: StateFlow<AppStrings> = _strings.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _isAutoSaveEnabled = MutableStateFlow(true)
    val isAutoSaveEnabled: StateFlow<Boolean> = _isAutoSaveEnabled.asStateFlow()

    private val _loadedSwf = MutableStateFlow<LoadedSwfData?>(null)
    val loadedSwf: StateFlow<LoadedSwfData?> = _loadedSwf.asStateFlow()

    private val _textObjects = MutableStateFlow<List<TextObject>>(emptyList())
    val textObjects: StateFlow<List<TextObject>> = _textObjects.asStateFlow()

    private val _selectedText = MutableStateFlow<TextObject?>(null)
    val selectedText: StateFlow<TextObject?> = _selectedText.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _replaceQuery = MutableStateFlow("")
    val replaceQuery: StateFlow<String> = _replaceQuery.asStateFlow()

    private val _activeFilter = MutableStateFlow(TextFilter.ALL)
    val activeFilter: StateFlow<TextFilter> = _activeFilter.asStateFlow()

    private val _activeSort = MutableStateFlow(TextSort.ID)
    val activeSort: StateFlow<TextSort> = _activeSort.asStateFlow()

    private val _buildState = MutableStateFlow<BuildUiState>(BuildUiState.Idle)
    val buildState: StateFlow<BuildUiState> = _buildState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadingStatus = MutableStateFlow("")
    val loadingStatus: StateFlow<String> = _loadingStatus.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _lastBuiltBytes = MutableStateFlow<ByteArray?>(null)
    val lastBuiltBytes: StateFlow<ByteArray?> = _lastBuiltBytes.asStateFlow()

    private val _activeProjectId = MutableStateFlow<Long?>(null)
    val activeProjectId: StateFlow<Long?> = _activeProjectId.asStateFlow()

    // Undo / Redo stacks
    private val undoStack = mutableListOf<List<TextObject>>()
    private val redoStack = mutableListOf<List<TextObject>>()

    private var autosaveJob: Job? = null

    init {
        // Load default sample SWF if nothing opened yet
        viewModelScope.launch {
            loadSampleSwf()
        }
    }

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
        _strings.value = AppStrings(lang)
    }

    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun toggleAutoSave() {
        _isAutoSaveEnabled.value = !_isAutoSaveEnabled.value
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setReplaceQuery(query: String) {
        _replaceQuery.value = query
    }

    fun setFilter(filter: TextFilter) {
        _activeFilter.value = filter
    }

    fun setSort(sort: TextSort) {
        _activeSort.value = sort
    }

    fun selectText(textObject: TextObject?) {
        _selectedText.value = textObject
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    /**
     * Loads an SWF file from an Android SAF URI.
     */
    fun openSwfFromUri(uri: Uri, context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingStatus.value = "Reading SWF file from storage..."
            try {
                var fileName = "document.swf"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex) ?: "document.swf"
                    }
                }

                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _errorMessage.value = "Unable to open input stream for selected file."
                    _isLoading.value = false
                    return@launch
                }

                val rawBytes = inputStream.use { it.readBytes() }
                _loadingStatus.value = "Analyzing SWF structure..."
                processSwfBytes(rawBytes, fileName, uri.toString())
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load SWF: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Process parsed SWF bytes and populate extracted text objects.
     */
    fun processSwfBytes(bytes: ByteArray, fileName: String, uriString: String? = null) {
        try {
            val parser = SwfParser()
            val parseResult = parser.parse(bytes)

            // Cache file locally
            val cacheDir = getApplication<Application>().cacheDir
            val cachedFile = File(cacheDir, "current_${System.currentTimeMillis()}_$fileName")
            FileOutputStream(cachedFile).use { it.write(bytes) }

            val extractor = TextExtractor()
            val extractedTexts = extractor.extract(parseResult.tags, parseResult.fonts)

            _loadedSwf.value = LoadedSwfData(
                fileName = fileName,
                fileUri = uriString,
                cachedFile = cachedFile,
                parseResult = parseResult,
                originalBytes = bytes
            )
            _textObjects.value = extractedTexts
            _buildState.value = BuildUiState.Idle
            _lastBuiltBytes.value = null
            undoStack.clear()
            redoStack.clear()

            // Save or update recent project
            viewModelScope.launch {
                saveCurrentProjectInternal(fileName, uriString ?: "", cachedFile.absolutePath, extractedTexts, parseResult)
            }
        } catch (e: Exception) {
            _errorMessage.value = "Error parsing SWF: ${e.message}"
        }
    }

    /**
     * Generates a sample authentic Flash game / dialogue SWF file for quick exploration.
     */
    fun loadSampleSwf() {
        viewModelScope.launch {
            try {
                val sampleBytes = createSampleSwfBytes()
                processSwfBytes(sampleBytes, "FlashQuest_Dialogue.swf", null)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create sample SWF: ${e.message}"
            }
        }
    }

    /**
     * Updates text of a specific text object.
     */
    fun updateText(index: Int, newText: String) {
        val currentList = _textObjects.value
        if (index !in currentList.indices) return

        // Push to undo stack
        pushUndoState(currentList)

        val updated = currentList.toMutableList()
        val old = updated[index]
        val newObj = old.copy(editedText = newText)
        updated[index] = newObj
        _textObjects.value = updated

        if (_selectedText.value?.index == index) {
            _selectedText.value = newObj
        }

        triggerAutosave()
    }

    fun resetText(index: Int) {
        val currentList = _textObjects.value
        if (index !in currentList.indices) return

        pushUndoState(currentList)
        val updated = currentList.toMutableList()
        val old = updated[index]
        val newObj = old.copy(editedText = old.originalText)
        updated[index] = newObj
        _textObjects.value = updated

        if (_selectedText.value?.index == index) {
            _selectedText.value = newObj
        }
        triggerAutosave()
    }

    fun resetAllTexts() {
        val currentList = _textObjects.value
        if (currentList.isEmpty()) return

        pushUndoState(currentList)
        val updated = currentList.map { it.copy(editedText = it.originalText) }
        _textObjects.value = updated

        val selected = _selectedText.value
        if (selected != null && selected.index in updated.indices) {
            _selectedText.value = updated[selected.index]
        }
        triggerAutosave()
    }

    /**
     * Batch replaces text across all occurrences.
     */
    fun batchReplace(target: String, replacement: String, caseSensitive: Boolean = false) {
        if (target.isEmpty()) return
        val currentList = _textObjects.value
        pushUndoState(currentList)

        val updated = currentList.map { item ->
            if (caseSensitive) {
                if (item.editedText.contains(target)) {
                    item.copy(editedText = item.editedText.replace(target, replacement))
                } else item
            } else {
                if (item.editedText.contains(target, ignoreCase = true)) {
                    item.copy(editedText = item.editedText.replace(target, replacement, ignoreCase = true))
                } else item
            }
        }
        _textObjects.value = updated
        triggerAutosave()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(_textObjects.value)
            _textObjects.value = prev
            triggerAutosave()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(_textObjects.value)
            _textObjects.value = next
            triggerAutosave()
        }
    }

    private fun pushUndoState(list: List<TextObject>) {
        undoStack.add(list.map { it.copy() })
        if (undoStack.size > 30) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
    }

    /**
     * Builds the modified SWF file.
     */
    fun buildSwf() {
        val currentLoaded = _loadedSwf.value ?: return
        viewModelScope.launch {
            _buildState.value = BuildUiState.Building(0.05f, "Validating project state...")
            try {
                val builder = SwfBuilder()
                val result = builder.build(
                    originalResult = currentLoaded.parseResult,
                    textObjects = _textObjects.value,
                    onProgress = { progress, status ->
                        _buildState.value = BuildUiState.Building(progress, status)
                    }
                )

                _buildState.value = BuildUiState.Building(0.90f, "Verifying SWF integrity...")
                val validator = SwfValidator()
                val validationResult = validator.validate(result.outputBytes, _textObjects.value)

                if (validationResult.isValid) {
                    _lastBuiltBytes.value = result.outputBytes
                    _buildState.value = BuildUiState.Success(result, validationResult)
                } else {
                    _buildState.value = BuildUiState.Error(
                        message = validationResult.error ?: "Validation failed.",
                        details = validationResult.warnings.joinToString("\n")
                    )
                }
            } catch (e: Exception) {
                _buildState.value = BuildUiState.Error(
                    message = "Build failed: ${e.message}",
                    details = e.stackTraceToString()
                )
            }
        }
    }

    /**
     * Exports the built SWF file to a user-selected SAF URI.
     */
    fun exportBuiltSwfToUri(uri: Uri, context: Context): Boolean {
        val bytes = _lastBuiltBytes.value ?: return false
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(bytes)
                out.flush()
            }
            true
        } catch (e: Exception) {
            _errorMessage.value = "Failed to export SWF: ${e.message}"
            false
        }
    }

    /**
     * Imports translation file (JSON or CSV).
     */
    fun importTranslations(content: String, isJson: Boolean): ImportResult {
        val currentTexts = _textObjects.value
        val result = if (isJson) {
            TranslationIO.importFromJson(content, currentTexts)
        } else {
            TranslationIO.importFromCsv(content, currentTexts)
        }
        if (result.success) {
            pushUndoState(currentTexts)
            _textObjects.value = result.updatedTexts
            triggerAutosave()
        }
        return result
    }

    /**
     * Exports translation in requested format.
     */
    fun exportTranslations(format: String): String {
        val texts = _textObjects.value
        return when (format.uppercase()) {
            "JSON" -> TranslationIO.exportToJson(texts)
            "CSV" -> TranslationIO.exportToCsv(texts)
            "TXT" -> TranslationIO.exportToTxt(texts)
            else -> TranslationIO.exportToJson(texts)
        }
    }

    fun openRecentProject(project: ProjectEntity) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingStatus.value = "Loading project ${project.projectName}..."
            try {
                val file = File(project.cachedSwfPath)
                if (file.exists()) {
                    val bytes = file.readBytes()
                    processSwfBytes(bytes, project.originalFileName, project.originalFileUri)
                    _activeProjectId.value = project.id

                    // Apply stored modifications
                    if (project.modificationsJson.isNotEmpty() && project.modificationsJson != "{}") {
                        val json = JSONObject(project.modificationsJson)
                        val updated = _textObjects.value.map { item ->
                            val key = (item.index + 1).toString()
                            if (json.has(key)) {
                                item.copy(editedText = json.getString(key))
                            } else item
                        }
                        _textObjects.value = updated
                    }
                } else {
                    _errorMessage.value = "Cached SWF file for project not found on disk."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to open project: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            repository.deleteProjectById(projectId)
        }
    }

    private fun triggerAutosave() {
        if (!_isAutoSaveEnabled.value) return
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(5000) // 5 seconds inactivity debounce
            saveCurrentProjectInternal()
        }
    }

    fun saveCurrentProject() {
        viewModelScope.launch {
            saveCurrentProjectInternal()
        }
    }

    private suspend fun saveCurrentProjectInternal(
        fileNameOverride: String? = null,
        uriOverride: String? = null,
        cachedPathOverride: String? = null,
        textsOverride: List<TextObject>? = null,
        parseResultOverride: SwfParseResult? = null
    ) {
        val loaded = _loadedSwf.value ?: return
        val texts = textsOverride ?: _textObjects.value
        val parse = parseResultOverride ?: loaded.parseResult

        val modMap = JSONObject()
        for (item in texts) {
            if (item.isModified) {
                modMap.put((item.index + 1).toString(), item.editedText)
            }
        }

        val entity = ProjectEntity(
            id = _activeProjectId.value ?: 0,
            projectName = (fileNameOverride ?: loaded.fileName).removeSuffix(".swf"),
            originalFileName = fileNameOverride ?: loaded.fileName,
            originalFileUri = uriOverride ?: (loaded.fileUri ?: ""),
            cachedSwfPath = cachedPathOverride ?: loaded.cachedFile.absolutePath,
            totalTexts = texts.size,
            modifiedTextsCount = texts.count { it.isModified },
            lastModifiedTime = System.currentTimeMillis(),
            modificationsJson = modMap.toString(),
            swfVersion = parse.header.version,
            fileSize = loaded.originalBytes.size.toLong()
        )

        val id = repository.saveProject(entity)
        _activeProjectId.value = id
    }

    /**
     * Creates a synthetic, fully valid SWF binary containing multiple DefineEditText and DefineFont2 tags.
     */
    private fun createSampleSwfBytes(): ByteArray {
        val bodyWriter = SwfBinaryWriter()
        // RECT: 800x600 px (16000x12000 twips)
        bodyWriter.writeRect(SwfRect(15, 0, 16000, 0, 12000))
        bodyWriter.writeFixed8(30.0f) // 30 FPS
        bodyWriter.writeUI16(60) // 60 frames

        // SetBackgroundColor (Tag 9) - Dark Navy #0D1117
        val bgWriter = SwfBinaryWriter()
        bgWriter.writeUI8(0x0D)
        bgWriter.writeUI8(0x11)
        bgWriter.writeUI8(0x17)
        bodyWriter.writeTag(SwfTag(SwfTagCode.SET_BACKGROUND_COLOR, bgWriter.toByteArray()))

        // DefineFont2 (Tag 48) - Arial with character code table
        val fontWriter = SwfBinaryWriter()
        fontWriter.writeUI16(1) // FontID 1
        fontWriter.writeUI8(0x20 or 0x04) // Flags: Unicode, WideCodes
        fontWriter.writeUI8(0) // LangCode
        val fontName = "Arial"
        fontWriter.writeUI8(fontName.length)
        fontWriter.writeString(fontName, Charsets.US_ASCII)
        val sampleChars = " ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,!?:;'-()_+=/@#%&*مرحباًبكمفياللعبةالخياراتبدء".toList()
        fontWriter.writeUI16(sampleChars.size)
        // Offset table (wide offsets = 4 bytes each)
        for (i in 0..sampleChars.size) {
            fontWriter.writeUI32((i * 10).toLong())
        }
        // Code table (wide codes = 2 bytes each)
        for (ch in sampleChars) {
            fontWriter.writeUI16(ch.code)
        }
        bodyWriter.writeTag(SwfTag(SwfTagCode.DEFINE_FONT2, fontWriter.toByteArray()))

        // DefineEditText #1 - Game Title
        bodyWriter.writeTag(createSampleEditTextTag(101, 1, 480, 240, 200, 10000, 2000, "Flash Quest: Chronicles of Light", "title_txt"))

        // DefineEditText #2 - Dialogue Line 1
        bodyWriter.writeTag(createSampleEditTextTag(102, 1, 320, 255, 255, 2000, 8000, "Welcome, brave adventurer! The kingdom of Eldoria is in grave danger.", "dialogue_01"))

        // DefineEditText #3 - Dialogue Line 2
        bodyWriter.writeTag(createSampleEditTextTag(103, 1, 320, 220, 220, 2000, 9000, "Seek the Crystal of Dawn in the Whispering Forest before twilight falls.", "dialogue_02"))

        // DefineEditText #4 - Button: Start Game
        bodyWriter.writeTag(createSampleEditTextTag(104, 1, 280, 255, 180, 4000, 5000, "Start Game", "btn_start"))

        // DefineEditText #5 - Button: Options
        bodyWriter.writeTag(createSampleEditTextTag(105, 1, 280, 200, 200, 4000, 6000, "Game Options & Settings", "btn_options"))

        // DefineEditText #6 - Button: Exit
        bodyWriter.writeTag(createSampleEditTextTag(106, 1, 280, 200, 200, 4000, 7000, "Quit to Main Menu", "btn_quit"))

        // DefineEditText #7 - Quest log text
        bodyWriter.writeTag(createSampleEditTextTag(107, 1, 260, 180, 240, 1000, 10000, "Quest Objective: Speak to Elder Matthew at the Sanctuary.", "quest_obj"))

        // ShowFrame (Tag 1)
        bodyWriter.writeTag(SwfTag(SwfTagCode.SHOW_FRAME, ByteArray(0)))

        // End tag (Tag 0)
        bodyWriter.writeTag(SwfTag(SwfTagCode.END, ByteArray(0)))

        val uncompressedBody = bodyWriter.toByteArray()
        val totalLen = 8L + uncompressedBody.size.toLong()

        val headerWriter = SwfBinaryWriter()
        headerWriter.writeString("FWS", Charsets.US_ASCII)
        headerWriter.writeUI8(10) // Flash Player 10
        headerWriter.writeUI32(totalLen)

        val fullStream = ByteArrayOutputStream()
        fullStream.write(headerWriter.toByteArray())
        fullStream.write(uncompressedBody)
        return fullStream.toByteArray()
    }

    private fun createSampleEditTextTag(
        charId: Int,
        fontId: Int,
        fontSizeTwips: Int,
        r: Int,
        g: Int,
        xTwips: Int,
        yTwips: Int,
        text: String,
        varName: String
    ): SwfTag {
        val writer = SwfBinaryWriter()
        writer.writeUI16(charId)
        writer.writeRect(SwfRect(15, xTwips, xTwips + 8000, yTwips, yTwips + 1200))
        // Flags: HasFont (0x01), HasTextColor (0x04), HasText (0x80), HasLayout (0x2000)
        val flags = 0x0001 or 0x0004 or 0x0080 or 0x2000
        writer.writeUI16(flags)
        writer.writeUI16(fontId)
        writer.writeUI16(fontSizeTwips)
        // Text Color RGBA
        writer.writeUI8(r)
        writer.writeUI8(g)
        writer.writeUI8(255)
        writer.writeUI8(255)
        // Layout: Align (0 = left), LeftMargin, RightMargin, Indent, Leading
        writer.writeUI8(0)
        writer.writeUI16(0)
        writer.writeUI16(0)
        writer.writeUI16(0)
        writer.writeSI16(0)
        // VariableName
        writer.writeNullTerminatedString(varName)
        // InitialText
        writer.writeNullTerminatedString(text, Charsets.UTF_8)
        return SwfTag(SwfTagCode.DEFINE_EDIT_TEXT, writer.toByteArray())
    }
}
