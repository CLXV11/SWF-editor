package com.example.core.swf

/**
 * Represents the parsed header of an SWF file.
 */
data class SwfHeader(
    val signature: String, // "FWS" (uncompressed), "CWS" (zlib), or "ZWS" (lzma)
    val version: Int,
    val uncompressedLength: Long,
    val frameSize: SwfRect,
    val frameRate: Float,
    val frameCount: Int
) {
    val isCompressed: Boolean
        get() = signature == "CWS" || signature == "ZWS"

    val widthPx: Int
        get() = ((frameSize.xMax - frameSize.xMin) / 20.0f).toInt()

    val heightPx: Int
        get() = ((frameSize.yMax - frameSize.yMin) / 20.0f).toInt()
}

/**
 * Flash RECT structure (coordinates in twips, 20 twips = 1 pixel).
 */
data class SwfRect(
    val nBits: Int,
    val xMin: Int,
    val xMax: Int,
    val yMin: Int,
    val yMax: Int
) {
    companion object {
        fun default(): SwfRect = SwfRect(15, 0, 11000, 0, 8000)
    }
}
