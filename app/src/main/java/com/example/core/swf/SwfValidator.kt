package com.example.core.swf

import com.example.core.text.TextExtractor
import com.example.core.text.TextObject

data class ValidationResult(
    val isValid: Boolean,
    val error: String? = null,
    val warnings: List<String> = emptyList(),
    val verifiedModificationsCount: Int = 0,
    val totalOutputTags: Int = 0,
    val outputUncompressedSize: Long = 0
)

/**
 * Validates generated SWF binary against specification rules, structural integrity,
 * and text modification consistency before user export.
 */
class SwfValidator {

    fun validate(
        builtBytes: ByteArray,
        expectedModifications: List<TextObject>
    ): ValidationResult {
        if (builtBytes.size < 8) {
            return ValidationResult(isValid = false, error = "Built file is too small (${builtBytes.size} bytes).")
        }

        val signature = String(builtBytes, 0, 3, Charsets.US_ASCII)
        if (signature != "FWS" && signature != "CWS") {
            return ValidationResult(isValid = false, error = "Invalid SWF header signature '$signature'.")
        }

        val parser = SwfParser()
        val parseResult: SwfParseResult
        try {
            parseResult = parser.parse(builtBytes)
        } catch (e: Exception) {
            return ValidationResult(
                isValid = false,
                error = "Built SWF structure validation failed during parsing: ${e.message}"
            )
        }

        val warnings = mutableListOf<String>()
        warnings.addAll(parseResult.notes)

        // Check End tag
        val hasEndTag = parseResult.tags.any { it.code == SwfTagCode.END }
        if (!hasEndTag) {
            warnings.add("Warning: EndTag (Tag 0) not explicitly found at stream end.")
        }

        // Re-extract texts from the newly built SWF to verify edits persisted
        val extractor = TextExtractor()
        val newTexts = extractor.extract(parseResult.tags, parseResult.fonts)

        var verifiedCount = 0
        val modifiedExpected = expectedModifications.filter { it.isModified }

        for (expected in modifiedExpected) {
            val matching = newTexts.find {
                it.characterId == expected.characterId &&
                        it.tagType == expected.tagType &&
                        it.index == expected.index
            } ?: newTexts.find { it.originalText == expected.originalText || it.editedText == expected.editedText }

            if (matching != null && matching.editedText == expected.editedText) {
                verifiedCount++
            }
        }

        return ValidationResult(
            isValid = true,
            warnings = warnings,
            verifiedModificationsCount = verifiedCount,
            totalOutputTags = parseResult.tags.size,
            outputUncompressedSize = parseResult.uncompressedFileSize
        )
    }
}
