package com.example.core.swf

import com.example.core.text.TextObject
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater

/**
 * Result of building an SWF file.
 */
data class SwfBuildResult(
    val outputBytes: ByteArray,
    val modifiedTagsCount: Int,
    val totalTagsCount: Int,
    val originalSize: Long,
    val outputSize: Long,
    val isCompressed: Boolean
)

/**
 * High-performance, non-destructive SWF Builder.
 * Reconstructs the SWF file applying only user text edits, preserving all graphics,
 * audio, sprites, frames, and binary metadata with 100% integrity.
 */
class SwfBuilder {

    fun build(
        originalResult: SwfParseResult,
        textObjects: List<TextObject>,
        onProgress: ((Float, String) -> Unit)? = null
    ): SwfBuildResult {
        onProgress?.invoke(0.1f, "Preparing text modifications...")

        // Index modifications by tag index
        val modifiedByTagIndex = textObjects
            .filter { it.isModified }
            .groupBy { it.tagIndex }

        val tagsToBuild = mutableListOf<SwfTag>()
        var modifiedTagsCount = 0
        val totalTags = originalResult.tags.size

        for ((idx, tag) in originalResult.tags.withIndex()) {
            val progress = 0.1f + 0.5f * (idx.toFloat() / totalTags.coerceAtLeast(1))
            val modTexts = modifiedByTagIndex[idx]

            if (modTexts != null && modTexts.isNotEmpty()) {
                onProgress?.invoke(progress, "Rebuilding text tag #${idx + 1} (${tag.name})...")
                val rebuiltTag = rebuildTagWithModifications(tag, modTexts, originalResult.fonts)
                tagsToBuild.add(rebuiltTag)
                modifiedTagsCount++
            } else {
                tagsToBuild.add(tag)
            }
        }

        onProgress?.invoke(0.65f, "Rebuilding SWF body...")
        // Write uncompressed body
        val bodyWriter = SwfBinaryWriter()
        bodyWriter.writeRect(originalResult.header.frameSize)
        bodyWriter.writeFixed8(originalResult.header.frameRate)
        bodyWriter.writeUI16(originalResult.header.frameCount)

        for (tag in tagsToBuild) {
            bodyWriter.writeTag(tag)
        }

        val uncompressedBody = bodyWriter.toByteArray()
        val totalUncompressedLength = 8L + uncompressedBody.size.toLong()

        onProgress?.invoke(0.80f, "Applying compression & packaging...")
        val finalBytes: ByteArray
        val isCompressed = originalResult.header.signature == "CWS"

        if (isCompressed) {
            val headerWriter = SwfBinaryWriter()
            headerWriter.writeString("CWS", Charsets.US_ASCII)
            headerWriter.writeUI8(originalResult.header.version)
            headerWriter.writeUI32(totalUncompressedLength)

            val compressedBody = compressZlib(uncompressedBody)
            val fullOut = ByteArrayOutputStream(headerWriter.size + compressedBody.size)
            fullOut.write(headerWriter.toByteArray())
            fullOut.write(compressedBody)
            finalBytes = fullOut.toByteArray()
        } else {
            val headerWriter = SwfBinaryWriter()
            headerWriter.writeString("FWS", Charsets.US_ASCII)
            headerWriter.writeUI8(originalResult.header.version)
            headerWriter.writeUI32(totalUncompressedLength)

            val fullOut = ByteArrayOutputStream(headerWriter.size + uncompressedBody.size)
            fullOut.write(headerWriter.toByteArray())
            fullOut.write(uncompressedBody)
            finalBytes = fullOut.toByteArray()
        }

        onProgress?.invoke(1.0f, "SWF Built successfully.")

        return SwfBuildResult(
            outputBytes = finalBytes,
            modifiedTagsCount = modifiedTagsCount,
            totalTagsCount = totalTags,
            originalSize = originalResult.originalFileSize,
            outputSize = finalBytes.size.toLong(),
            isCompressed = isCompressed
        )
    }

    private fun rebuildTagWithModifications(
        tag: SwfTag,
        modifications: List<TextObject>,
        fonts: Map<Int, SwfFontInfo>
    ): SwfTag {
        return when (tag.code) {
            SwfTagCode.DEFINE_EDIT_TEXT -> rebuildDefineEditText(tag, modifications.first())
            SwfTagCode.DEFINE_TEXT, SwfTagCode.DEFINE_TEXT2 -> rebuildDefineText(tag, modifications.first(), fonts)
            SwfTagCode.DO_ACTION, SwfTagCode.DO_INIT_ACTION -> rebuildDoAction(tag, modifications)
            SwfTagCode.DO_ABC -> rebuildDoAbc(tag, modifications)
            else -> tag
        }
    }

    private fun rebuildDefineEditText(tag: SwfTag, textObj: TextObject): SwfTag {
        val reader = SwfBinaryReader(tag.data)
        val characterId = reader.readUI16()
        val bounds = reader.readRect()
        var flags = reader.readUI16()

        val hasFont = (flags and 0x0001) != 0
        val hasMaxLength = (flags and 0x0002) != 0
        val hasTextColor = (flags and 0x0004) != 0
        val hasLayout = (flags and 0x2000) != 0
        val hasFontClass = (flags and 0x8000) != 0

        // Ensure hasText flag is set if new text is non-empty
        if (textObj.editedText.isNotEmpty()) {
            flags = flags or 0x0080
        }

        var fontId = 0
        var fontHeight = 0
        if (hasFont) {
            fontId = reader.readUI16()
            fontHeight = reader.readUI16()
        }

        var fontClass = ""
        if (hasFontClass) {
            fontClass = reader.readNullTerminatedString()
        }

        var textColor = 0L
        if (hasTextColor) {
            textColor = reader.readUI32()
        }

        var maxLength = 0
        if (hasMaxLength) {
            maxLength = reader.readUI16()
        }

        var align = 0
        var leftMargin = 0
        var rightMargin = 0
        var indent = 0
        var leading = 0
        if (hasLayout) {
            align = reader.readUI8()
            leftMargin = reader.readUI16()
            rightMargin = reader.readUI16()
            indent = reader.readUI16()
            leading = reader.readSI16()
        }

        val variableName = reader.readNullTerminatedString()
        // Skip old text
        if ((flags and 0x0080) != 0 && reader.hasRemaining) {
            reader.readNullTerminatedString()
        }

        // Write back
        val writer = SwfBinaryWriter()
        writer.writeUI16(characterId)
        writer.writeRect(bounds)
        writer.writeUI16(flags)

        if (hasFont) {
            writer.writeUI16(fontId)
            writer.writeUI16(fontHeight)
        }
        if (hasFontClass) {
            writer.writeNullTerminatedString(fontClass)
        }
        if (hasTextColor) {
            writer.writeUI32(textColor)
        }
        if (hasMaxLength) {
            writer.writeUI16(maxLength)
        }
        if (hasLayout) {
            writer.writeUI8(align)
            writer.writeUI16(leftMargin)
            writer.writeUI16(rightMargin)
            writer.writeUI16(indent)
            writer.writeSI16(leading)
        }
        writer.writeNullTerminatedString(variableName)
        if ((flags and 0x0080) != 0) {
            writer.writeNullTerminatedString(textObj.editedText, Charsets.UTF_8)
        }

        val newData = writer.toByteArray()
        return tag.copy(data = newData)
    }

    private fun rebuildDefineText(
        tag: SwfTag,
        textObj: TextObject,
        fonts: Map<Int, SwfFontInfo>
    ): SwfTag {
        // DefineText / DefineText2 glyph rewrite
        val reader = SwfBinaryReader(tag.data)
        val characterId = reader.readUI16()
        val bounds = reader.readRect()

        // Copy matrix
        val matrixStart = reader.position
        skipMatrix(reader)
        val matrixBytes = ByteArray(reader.position - matrixStart)
        System.arraycopy(tag.data, matrixStart, matrixBytes, 0, matrixBytes.size)

        val glyphBits = reader.readUI8()
        val advanceBits = reader.readUI8()

        val isText2 = tag.code == SwfTagCode.DEFINE_TEXT2

        val writer = SwfBinaryWriter()
        writer.writeUI16(characterId)
        writer.writeRect(bounds)
        writer.writeBytes(matrixBytes)
        writer.writeUI8(glyphBits)
        writer.writeUI8(advanceBits)

        var currentFontId: Int? = null
        var textInserted = false

        while (reader.hasRemaining) {
            val recordHeader = reader.readUI8()
            if (recordHeader == 0) {
                break
            }

            val hasFont = (recordHeader and 0x08) != 0
            val hasColor = (recordHeader and 0x04) != 0
            val hasYOffset = (recordHeader and 0x02) != 0
            val hasXOffset = (recordHeader and 0x01) != 0

            writer.writeUI8(recordHeader)

            if (hasFont) {
                currentFontId = reader.readUI16()
                writer.writeUI16(currentFontId)
            }
            if (hasColor) {
                val r = reader.readUI8()
                val g = reader.readUI8()
                val b = reader.readUI8()
                writer.writeUI8(r)
                writer.writeUI8(g)
                writer.writeUI8(b)
                if (isText2) {
                    val a = reader.readUI8()
                    writer.writeUI8(a)
                }
            }
            if (hasXOffset) {
                val x = reader.readSI16()
                writer.writeSI16(x)
            }
            if (hasYOffset) {
                val y = reader.readSI16()
                writer.writeSI16(y)
            }
            if (hasFont) {
                val height = reader.readUI16()
                writer.writeUI16(height)
            }

            val origGlyphCount = reader.readUI8()
            val font = currentFontId?.let { fonts[it] }

            if (!textInserted) {
                textInserted = true
                val newChars = textObj.editedText
                writer.writeUI8(newChars.length.coerceAtMost(255))
                for (ch in newChars) {
                    val glyphIndex = font?.findGlyphIndex(ch) ?: if (ch.code in 0..255) ch.code else 32
                    writer.writeBits(glyphIndex.toLong(), glyphBits)
                    writer.writeSignedBits(200, advanceBits) // standard 10px approx advance
                }
                writer.flushBits()
            } else {
                writer.writeUI8(0) // empty record
            }

            // Skip original glyphs in reader
            for (g in 0 until origGlyphCount) {
                reader.readBits(glyphBits)
                reader.readSignedBits(advanceBits)
            }
            reader.resetBitBuffer()
        }

        writer.writeUI8(0) // End record
        return tag.copy(data = writer.toByteArray())
    }

    private fun rebuildDoAction(tag: SwfTag, modifications: List<TextObject>): SwfTag {
        var data = tag.data
        for (mod in modifications) {
            val origBytes = mod.originalText.toByteArray(Charsets.UTF_8)
            val newBytes = mod.editedText.toByteArray(Charsets.UTF_8)
            data = replaceSubarray(data, origBytes, newBytes)
        }
        return tag.copy(data = data)
    }

    private fun rebuildDoAbc(tag: SwfTag, modifications: List<TextObject>): SwfTag {
        var data = tag.data
        for (mod in modifications) {
            val origBytes = mod.originalText.toByteArray(Charsets.UTF_8)
            val newBytes = mod.editedText.toByteArray(Charsets.UTF_8)
            data = replaceSubarray(data, origBytes, newBytes)
        }
        return tag.copy(data = data)
    }

    private fun replaceSubarray(source: ByteArray, target: ByteArray, replacement: ByteArray): ByteArray {
        if (target.isEmpty() || source.size < target.size) return source
        val out = ByteArrayOutputStream()
        var i = 0
        while (i < source.size) {
            if (i <= source.size - target.size && matchSubarray(source, i, target)) {
                out.write(replacement)
                i += target.size
            } else {
                out.write(source[i].toInt())
                i++
            }
        }
        return out.toByteArray()
    }

    private fun matchSubarray(source: ByteArray, offset: Int, target: ByteArray): Boolean {
        for (j in target.indices) {
            if (source[offset + j] != target[j]) return false
        }
        return true
    }

    private fun compressZlib(data: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        deflater.setInput(data)
        deflater.finish()
        val out = ByteArrayOutputStream(data.size)
        val buffer = ByteArray(4096)
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            out.write(buffer, 0, count)
        }
        deflater.end()
        return out.toByteArray()
    }

    private fun skipMatrix(reader: SwfBinaryReader) {
        reader.resetBitBuffer()
        val hasScale = reader.readBits(1) != 0L
        if (hasScale) {
            val nScaleBits = reader.readBits(5).toInt()
            reader.readSignedBits(nScaleBits)
            reader.readSignedBits(nScaleBits)
        }
        val hasRotate = reader.readBits(1) != 0L
        if (hasRotate) {
            val nRotateBits = reader.readBits(5).toInt()
            reader.readSignedBits(nRotateBits)
            reader.readSignedBits(nRotateBits)
        }
        val nTranslateBits = reader.readBits(5).toInt()
        if (nTranslateBits > 0) {
            reader.readSignedBits(nTranslateBits)
            reader.readSignedBits(nTranslateBits)
        }
        reader.resetBitBuffer()
    }
}
