package com.example.core.swf

import java.nio.charset.Charset

/**
 * A fast, robust low-level binary reader for SWF data streams.
 * Supports little-endian integers, bit-packed fields (RECT, MATRIX), and null-terminated strings.
 */
class SwfBinaryReader(private val bytes: ByteArray, var position: Int = 0) {

    private var bitBuffer: Long = 0L
    private var bitCount: Int = 0

    val remaining: Int
        get() = (bytes.size - position).coerceAtLeast(0)

    val hasRemaining: Boolean
        get() = position < bytes.size

    val size: Int
        get() = bytes.size

    fun resetBitBuffer() {
        bitBuffer = 0L
        bitCount = 0
    }

    fun readUI8(): Int {
        resetBitBuffer()
        if (position >= bytes.size) return 0
        return bytes[position++].toInt() and 0xFF
    }

    fun readSI8(): Int {
        resetBitBuffer()
        if (position >= bytes.size) return 0
        return bytes[position++].toInt()
    }

    fun readUI16(): Int {
        resetBitBuffer()
        if (position + 1 >= bytes.size) return 0
        val b0 = bytes[position++].toInt() and 0xFF
        val b1 = bytes[position++].toInt() and 0xFF
        return b0 or (b1 shl 8)
    }

    fun readSI16(): Int {
        val ui16 = readUI16()
        return if (ui16 >= 0x8000) ui16 - 0x10000 else ui16
    }

    fun readUI32(): Long {
        resetBitBuffer()
        if (position + 3 >= bytes.size) return 0L
        val b0 = (bytes[position++].toLong() and 0xFF)
        val b1 = (bytes[position++].toLong() and 0xFF)
        val b2 = (bytes[position++].toLong() and 0xFF)
        val b3 = (bytes[position++].toLong() and 0xFF)
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    fun readSI32(): Int {
        return readUI32().toInt()
    }

    fun readFixed8(): Float {
        val frac = readUI8()
        val intPart = readUI8()
        return intPart + (frac / 256.0f)
    }

    fun readBytes(count: Int): ByteArray {
        resetBitBuffer()
        val safeCount = count.coerceAtMost(remaining)
        val result = ByteArray(safeCount)
        System.arraycopy(bytes, position, result, 0, safeCount)
        position += safeCount
        return result
    }

    fun readBits(nbits: Int): Long {
        if (nbits <= 0) return 0L
        while (bitCount < nbits) {
            if (position >= bytes.size) {
                bitBuffer = (bitBuffer shl 8)
                bitCount += 8
            } else {
                val nextByte = bytes[position++].toInt() and 0xFF
                bitBuffer = (bitBuffer shl 8) or nextByte.toLong()
                bitCount += 8
            }
        }
        val shift = bitCount - nbits
        val mask = (1L shl nbits) - 1L
        val result = (bitBuffer ushr shift) and mask
        bitCount -= nbits
        return result
    }

    fun readSignedBits(nbits: Int): Int {
        if (nbits <= 0) return 0
        val raw = readBits(nbits)
        val signBit = 1L shl (nbits - 1)
        return if ((raw and signBit) != 0L) {
            (raw - (1L shl nbits)).toInt()
        } else {
            raw.toInt()
        }
    }

    fun readRect(): SwfRect {
        resetBitBuffer()
        val nBits = readBits(5).toInt()
        if (nBits <= 0) {
            return SwfRect(0, 0, 0, 0, 0)
        }
        val xMin = readSignedBits(nBits)
        val xMax = readSignedBits(nBits)
        val yMin = readSignedBits(nBits)
        val yMax = readSignedBits(nBits)
        resetBitBuffer()
        return SwfRect(nBits, xMin, xMax, yMin, yMax)
    }

    fun readNullTerminatedString(charset: Charset = Charsets.UTF_8): String {
        resetBitBuffer()
        val start = position
        while (position < bytes.size && bytes[position] != 0.toByte()) {
            position++
        }
        val length = position - start
        val str = String(bytes, start, length, charset)
        if (position < bytes.size && bytes[position] == 0.toByte()) {
            position++ // skip null terminator
        }
        return str
    }

    fun readStringOfLength(length: Int, charset: Charset = Charsets.UTF_8): String {
        resetBitBuffer()
        val safeLen = length.coerceAtMost(remaining)
        val str = String(bytes, position, safeLen, charset)
        position += safeLen
        return str
    }

    fun readEncodedU32(): Long {
        var result = 0L
        for (i in 0 until 5) {
            val byte = readUI8()
            result = result or ((byte and 0x7F).toLong() shl (i * 7))
            if ((byte and 0x80) == 0) break
        }
        return result
    }

    fun getRemainingBytes(): ByteArray {
        return readBytes(remaining)
    }
}
