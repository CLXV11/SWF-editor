package com.example.core.swf

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

/**
 * Result of parsing an SWF file.
 */
data class SwfParseResult(
    val header: SwfHeader,
    val tags: List<SwfTag>,
    val fonts: Map<Int, SwfFontInfo>,
    val totalFrames: Int,
    val originalFileSize: Long,
    val uncompressedFileSize: Long,
    val isPartialSupport: Boolean = false,
    val notes: List<String> = emptyList()
)

/**
 * Robust, production-grade parser for Macromedia / Adobe Flash SWF files.
 */
class SwfParser {

    /**
     * Parses raw SWF bytes into structured header, tags, and font definitions.
     */
    fun parse(rawBytes: ByteArray): SwfParseResult {
        require(rawBytes.size >= 8) { "Invalid SWF file: size too small (${rawBytes.size} bytes)" }

        val signature = String(rawBytes, 0, 3, Charsets.US_ASCII)
        require(signature == "FWS" || signature == "CWS" || signature == "ZWS") {
            "Invalid SWF signature '$signature'. Expected 'FWS', 'CWS', or 'ZWS'."
        }

        val version = rawBytes[3].toInt() and 0xFF
        val uncompressedLength = (rawBytes[4].toLong() and 0xFF) or
                ((rawBytes[5].toLong() and 0xFF) shl 8) or
                ((rawBytes[6].toLong() and 0xFF) shl 16) or
                ((rawBytes[7].toLong() and 0xFF) shl 24)

        // Decompress body if needed
        val uncompressedBody: ByteArray = when (signature) {
            "FWS" -> {
                val body = ByteArray(rawBytes.size - 8)
                System.arraycopy(rawBytes, 8, body, 0, body.size)
                body
            }
            "CWS" -> {
                decompressZlib(rawBytes, 8, uncompressedLength - 8)
            }
            "ZWS" -> {
                throw UnsupportedOperationException("LZMA (ZWS) compression is not supported in this version.")
            }
            else -> throw IllegalArgumentException("Unknown signature $signature")
        }

        val reader = SwfBinaryReader(uncompressedBody)
        val frameSize = reader.readRect()
        val frameRate = reader.readFixed8()
        val frameCount = reader.readUI16()

        val header = SwfHeader(
            signature = signature,
            version = version,
            uncompressedLength = uncompressedLength,
            frameSize = frameSize,
            frameRate = frameRate,
            frameCount = frameCount
        )

        val tags = mutableListOf<SwfTag>()
        val fonts = mutableMapOf<Int, SwfFontInfo>()
        val notes = mutableListOf<String>()
        var tagIndex = 0

        while (reader.hasRemaining) {
            val tagHeaderPos = reader.position
            val tagCodeAndLength = reader.readUI16()
            val tagCode = tagCodeAndLength ushr 6
            var tagLength = tagCodeAndLength and 0x3F
            var isLongHeader = false

            if (tagLength == 0x3F) {
                isLongHeader = true
                tagLength = reader.readUI32().toInt()
            }

            if (tagLength < 0 || tagLength > reader.remaining) {
                notes.add("Tag $tagCode at index $tagIndex had invalid length $tagLength. Reading remaining ${reader.remaining} bytes.")
                val data = reader.readBytes(reader.remaining)
                tags.add(SwfTag(tagCode, data, isLongHeader, tagIndex))
                break
            }

            val data = reader.readBytes(tagLength)
            val tag = SwfTag(tagCode, data, isLongHeader, tagIndex)
            tags.add(tag)

            // Extract font information for character compatibility checks
            try {
                when (tagCode) {
                    SwfTagCode.DEFINE_FONT2, SwfTagCode.DEFINE_FONT3 -> {
                        val fontInfo = parseDefineFont2Or3(data, tagCode)
                        if (fontInfo != null) {
                            fonts[fontInfo.fontId] = fontInfo
                        }
                    }
                    SwfTagCode.DEFINE_FONT_NAME -> {
                        val fontNameInfo = parseDefineFontName(data)
                        if (fontNameInfo != null) {
                            val existing = fonts[fontNameInfo.first]
                            if (existing != null) {
                                fonts[fontNameInfo.first] = existing.copy(fontName = fontNameInfo.second)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                notes.add("Failed to parse font tag $tagCode: ${e.message}")
            }

            tagIndex++

            if (tagCode == SwfTagCode.END) {
                break
            }
        }

        return SwfParseResult(
            header = header,
            tags = tags,
            fonts = fonts,
            totalFrames = frameCount,
            originalFileSize = rawBytes.size.toLong(),
            uncompressedFileSize = 8L + uncompressedBody.size.toLong(),
            isPartialSupport = notes.isNotEmpty(),
            notes = notes
        )
    }

    private fun decompressZlib(rawBytes: ByteArray, offset: Int, expectedSize: Long): ByteArray {
        val inflater = Inflater()
        val inputStream = ByteArrayInputStream(rawBytes, offset, rawBytes.size - offset)
        val inflaterStream = InflaterInputStream(inputStream, inflater)
        val out = ByteArrayOutputStream(expectedSize.toInt().coerceAtLeast(1024))
        val buffer = ByteArray(4096)
        var bytesRead: Int
        while (inflaterStream.read(buffer).also { bytesRead = it } != -1) {
            out.write(buffer, 0, bytesRead)
        }
        inflater.end()
        return out.toByteArray()
    }

    private fun parseDefineFont2Or3(data: ByteArray, tagCode: Int): SwfFontInfo? {
        if (data.size < 4) return null
        val reader = SwfBinaryReader(data)
        val fontId = reader.readUI16()
        val flags = reader.readUI8()
        val isHasLayout = (flags and 0x80) != 0
        val isShiftJIS = (flags and 0x40) != 0
        val isUnicode = (flags and 0x20) != 0
        val isAnsi = (flags and 0x10) != 0
        val isWideOffsets = (flags and 0x08) != 0
        val isWideCodes = (flags and 0x04) != 0
        val isItalic = (flags and 0x02) != 0
        val isBold = (flags and 0x01) != 0

        val langCode = reader.readUI8()
        val fontNameLength = reader.readUI8()
        val fontName = reader.readStringOfLength(fontNameLength, Charsets.US_ASCII)
        val numGlyphs = reader.readUI16()

        // Skip offset table
        val offsetEntrySize = if (isWideOffsets) 4 else 2
        val offsetTableBytes = (numGlyphs + 1) * offsetEntrySize
        if (reader.remaining < offsetTableBytes) {
            return SwfFontInfo(fontId, fontName, isBold, isItalic, isUnicode, numGlyphs = numGlyphs)
        }
        reader.readBytes(offsetTableBytes)

        // CodeTable
        val codeTable = mutableListOf<Char>()
        if (numGlyphs > 0 && reader.hasRemaining) {
            val codeEntrySize = if (isWideCodes || tagCode == SwfTagCode.DEFINE_FONT3) 2 else 1
            for (i in 0 until numGlyphs) {
                if (!reader.hasRemaining) break
                val charCode = if (codeEntrySize == 2) reader.readUI16() else reader.readUI8()
                codeTable.add(charCode.toChar())
            }
        }

        return SwfFontInfo(
            fontId = fontId,
            fontName = fontName,
            isBold = isBold,
            isItalic = isItalic,
            isUnicode = isUnicode,
            isAnsi = isAnsi,
            isShiftJIS = isShiftJIS,
            numGlyphs = numGlyphs,
            codeTable = codeTable
        )
    }

    private fun parseDefineFontName(data: ByteArray): Pair<Int, String>? {
        if (data.size < 4) return null
        val reader = SwfBinaryReader(data)
        val fontId = reader.readUI16()
        val fontName = reader.readNullTerminatedString()
        return Pair(fontId, fontName)
    }
}
