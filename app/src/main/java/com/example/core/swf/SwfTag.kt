package com.example.core.swf

/**
 * Tag types commonly found in SWF files.
 */
object SwfTagCode {
    const val END = 0
    const val SHOW_FRAME = 1
    const val DEFINE_SHAPE = 2
    const val PLACE_OBJECT = 4
    const val REMOVE_OBJECT = 5
    const val DEFINE_BITS = 6
    const val DEFINE_BUTTON = 7
    const val JPEG_TABLES = 8
    const val SET_BACKGROUND_COLOR = 9
    const val DEFINE_FONT = 10
    const val DEFINE_TEXT = 11
    const val DO_ACTION = 12
    const val DEFINE_FONT_INFO = 13
    const val DEFINE_SOUND = 14
    const val DEFINE_BUTTON_SOUND = 17
    const val DEFINE_BITS_LOSSLESS = 20
    const val DEFINE_BITS_JPEG2 = 21
    const val DEFINE_SHAPE2 = 22
    const val PLACE_OBJECT2 = 26
    const val REMOVE_OBJECT2 = 28
    const val DEFINE_SHAPE3 = 32
    const val DEFINE_TEXT2 = 33
    const val DEFINE_BUTTON2 = 34
    const val DEFINE_BITS_JPEG3 = 35
    const val DEFINE_BITS_LOSSLESS2 = 36
    const val DEFINE_EDIT_TEXT = 37
    const val DEFINE_SPRITE = 39
    const val FRAME_LABEL = 43
    const val DEFINE_MORPH_SHAPE = 46
    const val DEFINE_FONT2 = 48
    const val EXPORT_ASSETS = 56
    const val IMPORT_ASSETS = 57
    const val DO_INIT_ACTION = 59
    const val DEFINE_VIDEO_STREAM = 60
    const val VIDEO_FRAME = 61
    const val DEFINE_FONT_INFO2 = 62
    const val SCRIPT_LIMITS = 65
    const val SET_TAB_INDEX = 66
    const val FILE_ATTRIBUTES = 69
    const val PLACE_OBJECT3 = 70
    const val DEFINE_FONT_ALIGN_ZONES = 73
    const val CSM_TEXT_SETTINGS = 74
    const val DEFINE_FONT3 = 75
    const val SYMBOL_CLASS = 76
    const val METADATA = 77
    const val DEFINE_SCALING_GRID = 78
    const val DO_ABC = 82
    const val DEFINE_SHAPE4 = 83
    const val DEFINE_MORPH_SHAPE2 = 84
    const val DEFINE_SCENE_AND_FRAME_LABEL_DATA = 86
    const val DEFINE_BINARY_DATA = 87
    const val DEFINE_FONT_NAME = 88
    const val DEFINE_FONT4 = 91

    fun getTagName(code: Int): String {
        return when (code) {
            END -> "End"
            SHOW_FRAME -> "ShowFrame"
            DEFINE_SHAPE -> "DefineShape"
            PLACE_OBJECT -> "PlaceObject"
            REMOVE_OBJECT -> "RemoveObject"
            DEFINE_BITS -> "DefineBits"
            DEFINE_BUTTON -> "DefineButton"
            JPEG_TABLES -> "JPEGTables"
            SET_BACKGROUND_COLOR -> "SetBackgroundColor"
            DEFINE_FONT -> "DefineFont"
            DEFINE_TEXT -> "DefineText"
            DO_ACTION -> "DoAction"
            DEFINE_FONT_INFO -> "DefineFontInfo"
            DEFINE_SOUND -> "DefineSound"
            DEFINE_BUTTON_SOUND -> "DefineButtonSound"
            DEFINE_BITS_LOSSLESS -> "DefineBitsLossless"
            DEFINE_BITS_JPEG2 -> "DefineBitsJPEG2"
            DEFINE_SHAPE2 -> "DefineShape2"
            PLACE_OBJECT2 -> "PlaceObject2"
            REMOVE_OBJECT2 -> "RemoveObject2"
            DEFINE_SHAPE3 -> "DefineShape3"
            DEFINE_TEXT2 -> "DefineText2"
            DEFINE_BUTTON2 -> "DefineButton2"
            DEFINE_BITS_JPEG3 -> "DefineBitsJPEG3"
            DEFINE_BITS_LOSSLESS2 -> "DefineBitsLossless2"
            DEFINE_EDIT_TEXT -> "DefineEditText"
            DEFINE_SPRITE -> "DefineSprite"
            FRAME_LABEL -> "FrameLabel"
            DEFINE_MORPH_SHAPE -> "DefineMorphShape"
            DEFINE_FONT2 -> "DefineFont2"
            EXPORT_ASSETS -> "ExportAssets"
            IMPORT_ASSETS -> "ImportAssets"
            DO_INIT_ACTION -> "DoInitAction"
            FILE_ATTRIBUTES -> "FileAttributes"
            PLACE_OBJECT3 -> "PlaceObject3"
            DEFINE_FONT_ALIGN_ZONES -> "DefineFontAlignZones"
            CSM_TEXT_SETTINGS -> "CSMTextSettings"
            DEFINE_FONT3 -> "DefineFont3"
            SYMBOL_CLASS -> "SymbolClass"
            METADATA -> "Metadata"
            DO_ABC -> "DoABC"
            DEFINE_SHAPE4 -> "DefineShape4"
            DEFINE_BINARY_DATA -> "DefineBinaryData"
            DEFINE_FONT_NAME -> "DefineFontName"
            DEFINE_FONT4 -> "DefineFont4"
            else -> "Tag_$code"
        }
    }
}

/**
 * Represents a raw or parsed tag in the SWF structure.
 * Preserves the exact original payload unless explicitly rebuilt.
 */
data class SwfTag(
    val code: Int,
    val data: ByteArray,
    val isLongHeader: Boolean = false,
    val tagIndex: Int = 0
) {
    val name: String
        get() = SwfTagCode.getTagName(code)

    val isTextTag: Boolean
        get() = code == SwfTagCode.DEFINE_EDIT_TEXT ||
                code == SwfTagCode.DEFINE_TEXT ||
                code == SwfTagCode.DEFINE_TEXT2 ||
                code == SwfTagCode.DO_ABC ||
                code == SwfTagCode.DO_ACTION

    val isFontTag: Boolean
        get() = code == SwfTagCode.DEFINE_FONT ||
                code == SwfTagCode.DEFINE_FONT2 ||
                code == SwfTagCode.DEFINE_FONT3 ||
                code == SwfTagCode.DEFINE_FONT4 ||
                code == SwfTagCode.DEFINE_FONT_INFO ||
                code == SwfTagCode.DEFINE_FONT_INFO2 ||
                code == SwfTagCode.DEFINE_FONT_NAME

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SwfTag
        if (code != other.code) return false
        if (!data.contentEquals(other.data)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = code
        result = 31 * result + data.contentHashCode()
        return result
    }
}
