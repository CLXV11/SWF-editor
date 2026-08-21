package com.example.core.text

/**
 * Represents an extracted text item from an SWF file.
 */
data class TextObject(
    val id: String, // e.g. "Text #001"
    val index: Int,
    val tagIndex: Int,
    val characterId: Int = 0,
    val tagType: String, // "DefineEditText", "DefineText", "DefineText2", "DoABC", "DoAction"
    val tagCode: Int,
    val frame: Int = 1,
    val originalText: String,
    var editedText: String = originalText,
    val fontId: Int? = null,
    val fontName: String? = null,
    val fontSize: Float? = null,
    val colorHex: String? = null,
    val alignment: TextAlignment = TextAlignment.LEFT,
    val isStatic: Boolean = false,
    val isMultiline: Boolean = false,
    val isPassword: Boolean = false,
    val isReadOnly: Boolean = false,
    val isHtml: Boolean = false,
    val variableName: String? = null,
    val abcStringIndex: Int? = null,
    val bounds: String? = null
) {
    val isModified: Boolean
        get() = originalText != editedText

    val characterCount: Int
        get() = editedText.length

    val originalCharacterCount: Int
        get() = originalText.length

    fun hasArabicCharacters(): Boolean {
        return editedText.any { it in '\u0600'..'\u06FF' || it in '\u0750'..'\u077F' || it in '\u08A0'..'\u08FF' || it in '\uFB50'..'\uFDFF' || it in '\uFE70'..'\uFEFF' }
    }

    fun hasCyrillicCharacters(): Boolean {
        return editedText.any { it in '\u0400'..'\u04FF' || it in '\u0500'..'\u052F' }
    }
}

enum class TextAlignment {
    LEFT, RIGHT, CENTER, JUSTIFY;

    companion object {
        fun fromSwfAlign(align: Int): TextAlignment {
            return when (align) {
                0 -> LEFT
                1 -> RIGHT
                2 -> CENTER
                3 -> JUSTIFY
                else -> LEFT
            }
        }
    }
}
