package com.example.core.text

import com.example.core.swf.SwfBinaryReader
import com.example.core.swf.SwfFontInfo
import com.example.core.swf.SwfTag
import com.example.core.swf.SwfTagCode
import java.util.Locale

/**
 * Extracts all editable and static text objects from an SWF tag list.
 */
class TextExtractor {

    fun extract(tags: List<SwfTag>, fonts: Map<Int, SwfFontInfo>): List<TextObject> {
        val result = mutableListOf<TextObject>()
        var textIndex = 1
        var currentFrame = 1

        for ((tagIdx, tag) in tags.withIndex()) {
            if (tag.code == SwfTagCode.SHOW_FRAME) {
                currentFrame++
                continue
            }

            when (tag.code) {
                SwfTagCode.DEFINE_EDIT_TEXT -> {
                    val textObj = extractDefineEditText(tag, tagIdx, textIndex, currentFrame, fonts)
                    if (textObj != null) {
                        result.add(textObj)
                        textIndex++
                    }
                }
                SwfTagCode.DEFINE_TEXT, SwfTagCode.DEFINE_TEXT2 -> {
                    val textObjects = extractDefineText(tag, tagIdx, textIndex, currentFrame, fonts)
                    for (obj in textObjects) {
                        result.add(obj)
                        textIndex++
                    }
                }
                SwfTagCode.DO_ABC -> {
                    val abcTexts = extractDoAbcStrings(tag, tagIdx, textIndex, currentFrame)
                    for (obj in abcTexts) {
                        result.add(obj)
                        textIndex++
                    }
                }
                SwfTagCode.DO_ACTION, SwfTagCode.DO_INIT_ACTION -> {
                    val actionTexts = extractDoActionStrings(tag, tagIdx, textIndex, currentFrame)
                    for (obj in actionTexts) {
                        result.add(obj)
                        textIndex++
                    }
                }
            }
        }

        return result
    }

    private fun extractDefineEditText(
        tag: SwfTag,
        tagIdx: Int,
        textIndex: Int,
        frame: Int,
        fonts: Map<Int, SwfFontInfo>
    ): TextObject? {
        if (tag.data.size < 6) return null
        val reader = SwfBinaryReader(tag.data)
        val characterId = reader.readUI16()
        val bounds = reader.readRect()
        val flags = reader.readUI16()

        val hasFont = (flags and 0x0001) != 0
        val hasMaxLength = (flags and 0x0002) != 0
        val hasTextColor = (flags and 0x0004) != 0
        val isReadOnly = (flags and 0x0008) != 0
        val isPassword = (flags and 0x0010) != 0
        val isMultiline = (flags and 0x0020) != 0
        val isWordWrap = (flags and 0x0040) != 0
        val hasText = (flags and 0x0080) != 0
        val isHtml = (flags and 0x0200) != 0
        val wasStatic = (flags and 0x0400) != 0
        val hasLayout = (flags and 0x2000) != 0
        val hasFontClass = (flags and 0x8000) != 0

        var fontId: Int? = null
        var fontHeight: Float? = null
        if (hasFont) {
            fontId = reader.readUI16()
            fontHeight = reader.readUI16() / 20.0f
        }

        if (hasFontClass) {
            reader.readNullTerminatedString()
        }

        var colorHex: String? = null
        if (hasTextColor) {
            val r = reader.readUI8()
            val g = reader.readUI8()
            val b = reader.readUI8()
            val a = reader.readUI8()
            colorHex = String.format(Locale.US, "#%02X%02X%02X%02X", a, r, g, b)
        }

        if (hasMaxLength) {
            reader.readUI16()
        }

        var alignment = TextAlignment.LEFT
        if (hasLayout) {
            val align = reader.readUI8()
            alignment = TextAlignment.fromSwfAlign(align)
            reader.readUI16() // left margin
            reader.readUI16() // right margin
            reader.readUI16() // indent
            reader.readSI16() // leading
        }

        val variableName = reader.readNullTerminatedString()
        val initialText = if (hasText && reader.hasRemaining) {
            reader.readNullTerminatedString()
        } else {
            ""
        }

        val font = fontId?.let { fonts[it] }
        val fontName = font?.fontName ?: if (fontId != null) "Font #$fontId" else null

        val idStr = String.format(Locale.US, "Text #%03d", textIndex)
        val boundsStr = "${bounds.xMin / 20}, ${bounds.yMin / 20} - ${bounds.xMax / 20}x${bounds.yMax / 20}"

        return TextObject(
            id = idStr,
            index = textIndex - 1,
            tagIndex = tagIdx,
            characterId = characterId,
            tagType = "DefineEditText",
            tagCode = tag.code,
            frame = frame,
            originalText = initialText,
            editedText = initialText,
            fontId = fontId,
            fontName = fontName,
            fontSize = fontHeight,
            colorHex = colorHex,
            alignment = alignment,
            isStatic = wasStatic,
            isMultiline = isMultiline,
            isPassword = isPassword,
            isReadOnly = isReadOnly,
            isHtml = isHtml,
            variableName = variableName.ifEmpty { null },
            bounds = boundsStr
        )
    }

    private fun extractDefineText(
        tag: SwfTag,
        tagIdx: Int,
        startIndex: Int,
        frame: Int,
        fonts: Map<Int, SwfFontInfo>
    ): List<TextObject> {
        if (tag.data.size < 6) return emptyList()
        val reader = SwfBinaryReader(tag.data)
        val characterId = reader.readUI16()
        val bounds = reader.readRect()
        skipMatrix(reader)
        val glyphBits = reader.readUI8()
        val advanceBits = reader.readUI8()

        val textParts = mutableListOf<String>()
        var currentFontId: Int? = null
        var currentFontSize: Float? = null
        var currentColorHex: String? = null

        val isText2 = tag.code == SwfTagCode.DEFINE_TEXT2

        while (reader.hasRemaining) {
            val recordHeader = reader.readUI8()
            if (recordHeader == 0) break // End of records

            val hasFont = (recordHeader and 0x08) != 0
            val hasColor = (recordHeader and 0x04) != 0
            val hasYOffset = (recordHeader and 0x02) != 0
            val hasXOffset = (recordHeader and 0x01) != 0

            if (hasFont) {
                currentFontId = reader.readUI16()
            }
            if (hasColor) {
                val r = reader.readUI8()
                val g = reader.readUI8()
                val b = reader.readUI8()
                val a = if (isText2) reader.readUI8() else 255
                currentColorHex = String.format(Locale.US, "#%02X%02X%02X%02X", a, r, g, b)
            }
            if (hasXOffset) {
                reader.readSI16()
            }
            if (hasYOffset) {
                reader.readSI16()
            }
            if (hasFont) {
                currentFontSize = reader.readUI16() / 20.0f
            }

            val glyphCount = reader.readUI8()
            val font = currentFontId?.let { fonts[it] }
            val sb = java.lang.StringBuilder()

            for (g in 0 until glyphCount) {
                val glyphIndex = reader.readBits(glyphBits).toInt()
                reader.readSignedBits(advanceBits) // advance

                if (font != null && font.codeTable.isNotEmpty() && glyphIndex < font.codeTable.size) {
                    sb.append(font.codeTable[glyphIndex])
                } else if (glyphIndex in 32..126) {
                    sb.append(glyphIndex.toChar())
                } else {
                    sb.append(' ')
                }
            }
            reader.resetBitBuffer()

            val str = sb.toString().trim()
            if (str.isNotEmpty()) {
                textParts.add(str)
            }
        }

        if (textParts.isEmpty()) return emptyList()

        val fullText = textParts.joinToString(" ")
        val idStr = String.format(Locale.US, "Text #%03d", startIndex)
        val font = currentFontId?.let { fonts[it] }
        val fontName = font?.fontName ?: if (currentFontId != null) "Font #$currentFontId" else null
        val boundsStr = "${bounds.xMin / 20}, ${bounds.yMin / 20} - ${bounds.xMax / 20}x${bounds.yMax / 20}"

        return listOf(
            TextObject(
                id = idStr,
                index = startIndex - 1,
                tagIndex = tagIdx,
                characterId = characterId,
                tagType = if (isText2) "DefineText2" else "DefineText",
                tagCode = tag.code,
                frame = frame,
                originalText = fullText,
                editedText = fullText,
                fontId = currentFontId,
                fontName = fontName,
                fontSize = currentFontSize,
                colorHex = currentColorHex,
                alignment = TextAlignment.LEFT,
                isStatic = true,
                bounds = boundsStr
            )
        )
    }

    private fun extractDoAbcStrings(
        tag: SwfTag,
        tagIdx: Int,
        startIndex: Int,
        frame: Int
    ): List<TextObject> {
        val result = mutableListOf<TextObject>()
        val reader = SwfBinaryReader(tag.data)
        if (tag.data.size < 12) return emptyList()

        try {
            val flags = reader.readUI32()
            val name = reader.readNullTerminatedString()
            val minorVersion = reader.readUI16()
            val majorVersion = reader.readUI16()

            // Integer count
            val intCount = reader.readEncodedU32().toInt()
            for (i in 1 until intCount) reader.readEncodedU32()

            // UInt count
            val uintCount = reader.readEncodedU32().toInt()
            for (i in 1 until uintCount) reader.readEncodedU32()

            // Double count
            val doubleCount = reader.readEncodedU32().toInt()
            for (i in 1 until doubleCount) reader.readBytes(8)

            // String constant pool
            val stringCount = reader.readEncodedU32().toInt()
            var currentIdx = startIndex

            for (s in 1 until stringCount) {
                if (!reader.hasRemaining) break
                val strLen = reader.readEncodedU32().toInt()
                if (strLen in 2..500 && strLen <= reader.remaining) {
                    val str = reader.readStringOfLength(strLen, Charsets.UTF_8)
                    // Keep meaningful dialogue and UI strings
                    if (isMeaningfulDialogueString(str)) {
                        val idStr = String.format(Locale.US, "Text #%03d", currentIdx)
                        result.add(
                            TextObject(
                                id = idStr,
                                index = currentIdx - 1,
                                tagIndex = tagIdx,
                                characterId = 0,
                                tagType = "DoABC",
                                tagCode = tag.code,
                                frame = frame,
                                originalText = str,
                                editedText = str,
                                abcStringIndex = s,
                                isStatic = false
                            )
                        )
                        currentIdx++
                    }
                } else if (strLen > 0) {
                    reader.readBytes(strLen.coerceAtMost(reader.remaining))
                }
            }
        } catch (_: Exception) {
            // Ignore format deviations in complex ABC pools
        }

        return result
    }

    private fun extractDoActionStrings(
        tag: SwfTag,
        tagIdx: Int,
        startIndex: Int,
        frame: Int
    ): List<TextObject> {
        val result = mutableListOf<TextObject>()
        val data = tag.data
        var i = 0
        var currentIdx = startIndex

        while (i < data.size) {
            val actionCode = data[i].toInt() and 0xFF
            if (actionCode == 0) break // ActionEnd
            if (actionCode >= 0x80 && i + 2 < data.size) {
                val length = (data[i + 1].toInt() and 0xFF) or ((data[i + 2].toInt() and 0xFF) shl 8)
                val actionDataStart = i + 3
                if (actionCode == 0x96) { // ActionPush
                    // Parse strings pushed onto stack
                    var p = actionDataStart
                    val endP = (actionDataStart + length).coerceAtMost(data.size)
                    while (p < endP) {
                        val pushType = data[p].toInt() and 0xFF
                        p++
                        if (pushType == 0) { // String
                            val strStart = p
                            while (p < endP && data[p] != 0.toByte()) p++
                            val str = String(data, strStart, p - strStart, Charsets.UTF_8)
                            if (p < endP && data[p] == 0.toByte()) p++
                            if (isMeaningfulDialogueString(str)) {
                                val idStr = String.format(Locale.US, "Text #%03d", currentIdx)
                                result.add(
                                    TextObject(
                                        id = idStr,
                                        index = currentIdx - 1,
                                        tagIndex = tagIdx,
                                        characterId = 0,
                                        tagType = "DoAction",
                                        tagCode = tag.code,
                                        frame = frame,
                                        originalText = str,
                                        editedText = str,
                                        isStatic = false
                                    )
                                )
                                currentIdx++
                            }
                        } else if (pushType == 1) { // Float
                            p += 4
                        } else if (pushType == 4 || pushType == 5 || pushType == 9) { // Register, Boolean, Short
                            p += if (pushType == 9) 2 else 1
                        } else if (pushType == 6 || pushType == 7) { // Double, Integer
                            p += if (pushType == 6) 8 else 4
                        } else if (pushType == 8) { // Constant8
                            p += 1
                        }
                    }
                }
                i += 3 + length
            } else {
                i++
            }
        }

        return result
    }

    private fun isMeaningfulDialogueString(str: String): Boolean {
        if (str.length < 2) return false
        // Filter out typical bytecode / internal symbols
        if (str.startsWith("flash.") || str.startsWith("http://") || str.startsWith("https://")) return false
        if (str.contains("xmlns:") || str.contains("::")) return false
        if (str.matches(Regex("^[a-zA-Z0-9_\\$]+\\(\\)$"))) return false
        // Contains at least some letter or arabic or cyrillic
        val hasLetter = str.any { it.isLetter() || it in '\u0600'..'\u06FF' || it in '\u0400'..'\u04FF' }
        return hasLetter
    }

    private fun skipMatrix(reader: SwfBinaryReader) {
        reader.resetBitBuffer()
        val hasScale = reader.readBits(1) != 0L
        if (hasScale) {
            val nScaleBits = reader.readBits(5).toInt()
            reader.readSignedBits(nScaleBits) // scaleX
            reader.readSignedBits(nScaleBits) // scaleY
        }
        val hasRotate = reader.readBits(1) != 0L
        if (hasRotate) {
            val nRotateBits = reader.readBits(5).toInt()
            reader.readSignedBits(nRotateBits) // rotateSkew0
            reader.readSignedBits(nRotateBits) // rotateSkew1
        }
        val nTranslateBits = reader.readBits(5).toInt()
        if (nTranslateBits > 0) {
            reader.readSignedBits(nTranslateBits) // translateX
            reader.readSignedBits(nTranslateBits) // translateY
        }
        reader.resetBitBuffer()
    }
}
