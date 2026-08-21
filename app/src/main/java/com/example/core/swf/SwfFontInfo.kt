package com.example.core.swf

/**
 * Stores extracted metadata and glyph code tables for a font tag in the SWF.
 */
data class SwfFontInfo(
    val fontId: Int,
    val fontName: String,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnicode: Boolean = true,
    val isSmallText: Boolean = false,
    val isAnsi: Boolean = false,
    val isShiftJIS: Boolean = false,
    val numGlyphs: Int = 0,
    val codeTable: List<Char> = emptyList(),
    val glyphOffsets: List<Int> = emptyList()
) {
    val glyphCount: Int
        get() = if (numGlyphs > 0) numGlyphs else codeTable.size

    val hasArabicSupport: Boolean
        get() = codeTable.isEmpty() || codeTable.any { it in '\u0600'..'\u06FF' }

    /**
     * Checks whether this font includes glyphs for the characters in the given text.
     * Returns true if all characters are supported, or false if there are missing characters.
     */
    fun checkCharacterSupport(text: String): FontSupportResult {
        if (codeTable.isEmpty()) {
            // Dynamic system font without embedded glyph table
            return FontSupportResult(isFullySupported = true, missingChars = emptyList())
        }
        val supportedSet = codeTable.toSet()
        val missing = mutableListOf<Char>()
        for (ch in text) {
            if (ch > ' ' && !supportedSet.contains(ch)) {
                if (!missing.contains(ch)) {
                    missing.add(ch)
                }
            }
        }
        return FontSupportResult(
            isFullySupported = missing.isEmpty(),
            missingChars = missing
        )
    }

    /**
     * Finds the glyph index for a specific character.
     */
    fun findGlyphIndex(char: Char): Int? {
        val index = codeTable.indexOf(char)
        return if (index >= 0) index else null
    }
}

data class FontSupportResult(
    val isFullySupported: Boolean,
    val missingChars: List<Char>
)
