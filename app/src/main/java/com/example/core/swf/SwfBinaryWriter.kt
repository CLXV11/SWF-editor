package com.example.core.swf

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

/**
 * A fast, precise low-level binary writer for SWF files.
 */
class SwfBinaryWriter {
    private val buffer = ByteArrayOutputStream()
    private var bitBuffer: Long = 0L
    private var bitCount: Int = 0

    val size: Int
        get() = buffer.size()

    fun flushBits() {
        if (bitCount > 0) {
            val shift = 8 - bitCount
            val byte = ((bitBuffer shl shift) and 0xFF).toInt()
            buffer.write(byte)
            bitBuffer = 0L
            bitCount = 0
        }
    }

    fun writeUI8(value: Int) {
        flushBits()
        buffer.write(value and 0xFF)
    }

    fun writeSI8(value: Int) {
        flushBits()
        buffer.write(value and 0xFF)
    }

    fun writeUI16(value: Int) {
        flushBits()
        buffer.write(value and 0xFF)
        buffer.write((value ushr 8) and 0xFF)
    }

    fun writeSI16(value: Int) {
        writeUI16(value and 0xFFFF)
    }

    fun writeUI32(value: Long) {
        flushBits()
        buffer.write((value and 0xFF).toInt())
        buffer.write(((value ushr 8) and 0xFF).toInt())
        buffer.write(((value ushr 16) and 0xFF).toInt())
        buffer.write(((value ushr 24) and 0xFF).toInt())
    }

    fun writeSI32(value: Int) {
        writeUI32(value.toLong() and 0xFFFFFFFFL)
    }

    fun writeFixed8(value: Float) {
        val intPart = value.toInt() and 0xFF
        val fracPart = ((value - intPart) * 256.0f).toInt() and 0xFF
        writeUI8(fracPart)
        writeUI8(intPart)
    }

    fun writeBytes(bytes: ByteArray) {
        flushBits()
        buffer.write(bytes)
    }

    fun writeBits(value: Long, nbits: Int) {
        if (nbits <= 0) return
        var remainingBits = nbits
        val mask = if (nbits >= 64) -1L else (1L shl nbits) - 1L
        val cleanValue = value and mask

        while (remainingBits > 0) {
            val spaceInByte = 8 - bitCount
            if (remainingBits <= spaceInByte) {
                bitBuffer = (bitBuffer shl remainingBits) or (cleanValue and ((1L shl remainingBits) - 1L))
                bitCount += remainingBits
                remainingBits = 0
                if (bitCount == 8) {
                    buffer.write((bitBuffer and 0xFF).toInt())
                    bitBuffer = 0L
                    bitCount = 0
                }
            } else {
                val bitsToTake = spaceInByte
                val shift = remainingBits - bitsToTake
                val part = (cleanValue ushr shift) and ((1L shl bitsToTake) - 1L)
                bitBuffer = (bitBuffer shl bitsToTake) or part
                buffer.write((bitBuffer and 0xFF).toInt())
                bitBuffer = 0L
                bitCount = 0
                remainingBits -= bitsToTake
            }
        }
    }

    fun writeSignedBits(value: Int, nbits: Int) {
        val mask = (1L shl nbits) - 1L
        writeBits(value.toLong() and mask, nbits)
    }

    fun writeRect(rect: SwfRect) {
        flushBits()
        writeBits(rect.nBits.toLong(), 5)
        writeSignedBits(rect.xMin, rect.nBits)
        writeSignedBits(rect.xMax, rect.nBits)
        writeSignedBits(rect.yMin, rect.nBits)
        writeSignedBits(rect.yMax, rect.nBits)
        flushBits()
    }

    fun writeNullTerminatedString(text: String, charset: Charset = Charsets.UTF_8) {
        flushBits()
        val bytes = text.toByteArray(charset)
        buffer.write(bytes)
        buffer.write(0) // null terminator
    }

    fun writeString(text: String, charset: Charset = Charsets.UTF_8) {
        flushBits()
        val bytes = text.toByteArray(charset)
        buffer.write(bytes)
    }

    fun writeTag(tag: SwfTag) {
        flushBits()
        val length = tag.data.size
        // In SWF: if length < 63 and not forced long, write short header
        if (length < 63 && !tag.isLongHeader) {
            val tagCodeAndLength = (tag.code shl 6) or length
            writeUI16(tagCodeAndLength)
        } else {
            val tagCodeAndLength = (tag.code shl 6) or 0x3F
            writeUI16(tagCodeAndLength)
            writeUI32(length.toLong())
        }
        buffer.write(tag.data)
    }

    fun toByteArray(): ByteArray {
        flushBits()
        return buffer.toByteArray()
    }
}
