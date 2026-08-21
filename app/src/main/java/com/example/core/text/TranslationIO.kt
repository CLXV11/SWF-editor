package com.example.core.text

import org.json.JSONArray
import org.json.JSONObject

object TranslationIO {

    fun exportToJson(texts: List<TextObject>): String {
        val root = JSONObject()
        val textArray = JSONArray()
        for (text in texts) {
            val item = JSONObject()
            item.put("id", text.id)
            item.put("index", text.index + 1)
            item.put("original", text.originalText)
            item.put("translation", text.editedText)
            item.put("isModified", text.isModified)
            item.put("frame", text.frame)
            if (text.fontName != null) item.put("font", text.fontName)
            textArray.put(item)
        }
        root.put("swfEditorProject", "SWF-editor")
        root.put("version", "1.0")
        root.put("totalTexts", texts.size)
        root.put("texts", textArray)
        return root.toString(2)
    }

    fun exportToSimpleJsonMap(texts: List<TextObject>): String {
        val root = JSONObject()
        for (text in texts) {
            root.put((text.index + 1).toString(), text.editedText)
        }
        return root.toString(2)
    }

    fun exportToCsv(texts: List<TextObject>): String {
        val sb = StringBuilder()
        sb.append("ID,Index,Original,Translation,Frame,Font\n")
        for (text in texts) {
            val escapedOrig = text.originalText.replace("\"", "\"\"").replace("\n", "\\n")
            val escapedTrans = text.editedText.replace("\"", "\"\"").replace("\n", "\\n")
            val font = text.fontName ?: ""
            sb.append("\"${text.id}\",\"${text.index + 1}\",\"$escapedOrig\",\"$escapedTrans\",\"${text.frame}\",\"$font\"\n")
        }
        return sb.toString()
    }

    fun exportToTxt(texts: List<TextObject>): String {
        val sb = StringBuilder()
        sb.append("# SWF-editor Translation Export\n\n")
        for (text in texts) {
            sb.append("[${text.id}] (Frame ${text.frame})\n")
            sb.append("ORIGINAL: ${text.originalText}\n")
            sb.append("TRANSLATION: ${text.editedText}\n\n")
        }
        return sb.toString()
    }

    fun importFromJson(jsonString: String, currentTexts: List<TextObject>): ImportResult {
        var importedCount = 0
        val updatedTexts = currentTexts.map { it.copy() }.toMutableList()

        try {
            val root = JSONObject(jsonString)
            if (root.has("texts")) {
                val array = root.getJSONArray("texts")
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val index = item.optInt("index", -1) - 1
                    val id = item.optString("id", "")
                    val translation = item.optString("translation", "")
                    val original = item.optString("original", "")

                    val target = if (index in updatedTexts.indices) {
                        updatedTexts[index]
                    } else {
                        updatedTexts.find { it.id == id || it.originalText == original }
                    }

                    if (target != null && translation.isNotEmpty()) {
                        val targetIdx = updatedTexts.indexOf(target)
                        updatedTexts[targetIdx] = target.copy(editedText = translation)
                        importedCount++
                    }
                }
            } else {
                // Key-value format: {"1": "Text", "2": "Text"}
                val keys = root.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val idx = key.toIntOrNull()?.let { it - 1 }
                    val translation = root.getString(key)
                    if (idx != null && idx in updatedTexts.indices) {
                        updatedTexts[idx] = updatedTexts[idx].copy(editedText = translation)
                        importedCount++
                    }
                }
            }
        } catch (e: Exception) {
            return ImportResult(success = false, error = e.message, updatedTexts = currentTexts, importedCount = 0)
        }

        return ImportResult(success = true, updatedTexts = updatedTexts, importedCount = importedCount)
    }

    fun importFromCsv(csvString: String, currentTexts: List<TextObject>): ImportResult {
        var importedCount = 0
        val updatedTexts = currentTexts.map { it.copy() }.toMutableList()

        try {
            val lines = csvString.lines()
            for (line in lines) {
                if (line.isBlank() || line.startsWith("ID,")) continue
                val parts = parseCsvLine(line)
                if (parts.size >= 4) {
                    val index = parts[1].toIntOrNull()?.let { it - 1 }
                    val id = parts[0]
                    val translation = parts[3].replace("\\n", "\n")

                    val target = if (index != null && index in updatedTexts.indices) {
                        updatedTexts[index]
                    } else {
                        updatedTexts.find { it.id == id }
                    }

                    if (target != null && translation.isNotEmpty()) {
                        val targetIdx = updatedTexts.indexOf(target)
                        updatedTexts[targetIdx] = target.copy(editedText = translation)
                        importedCount++
                    }
                }
            }
        } catch (e: Exception) {
            return ImportResult(success = false, error = e.message, updatedTexts = currentTexts, importedCount = 0)
        }

        return ImportResult(success = true, updatedTexts = updatedTexts, importedCount = importedCount)
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var inQuotes = false
        val sb = StringBuilder()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    sb.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }
}

data class ImportResult(
    val success: Boolean,
    val error: String? = null,
    val updatedTexts: List<TextObject>,
    val importedCount: Int
)
