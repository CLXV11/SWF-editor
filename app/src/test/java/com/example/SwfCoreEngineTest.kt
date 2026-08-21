package com.example

import com.example.core.swf.SwfBinaryWriter
import com.example.core.swf.SwfBuilder
import com.example.core.swf.SwfParser
import com.example.core.swf.SwfRect
import com.example.core.swf.SwfTag
import com.example.core.swf.SwfTagCode
import com.example.core.swf.SwfValidator
import com.example.core.text.TextExtractor
import com.example.core.text.TranslationIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SwfCoreEngineTest {

    @Test
    fun testSwfParseAndExtractTexts() {
        val swfBytes = createTestSwf()
        val parser = SwfParser()
        val parseResult = parser.parse(swfBytes)

        assertEquals("FWS", parseResult.header.signature)
        assertEquals(10, parseResult.header.version)
        assertTrue(parseResult.tags.isNotEmpty())

        val extractor = TextExtractor()
        val texts = extractor.extract(parseResult.tags, parseResult.fonts)

        assertEquals(2, texts.size)
        assertEquals("Hello Flash World", texts[0].originalText)
        assertEquals("Start Game", texts[1].originalText)
    }

    @Test
    fun testSwfBuildAndRebuildValidation() {
        val swfBytes = createTestSwf()
        val parser = SwfParser()
        val parseResult = parser.parse(swfBytes)

        val extractor = TextExtractor()
        val texts = extractor.extract(parseResult.tags, parseResult.fonts)

        // Modify first text to Arabic / custom text
        val modifiedTexts = texts.toMutableList()
        modifiedTexts[0] = modifiedTexts[0].copy(editedText = "مرحبا بالعالم")

        val builder = SwfBuilder()
        val buildResult = builder.build(parseResult, modifiedTexts)

        assertTrue(buildResult.outputBytes.isNotEmpty())
        assertEquals(1, buildResult.modifiedTagsCount)

        // Validate the rebuilt SWF
        val validator = SwfValidator()
        val validation = validator.validate(buildResult.outputBytes, modifiedTexts)

        assertTrue(validation.isValid)
        assertEquals(1, validation.verifiedModificationsCount)

        // Parse rebuilt SWF directly
        val reParseResult = parser.parse(buildResult.outputBytes)
        val reExtractedTexts = extractor.extract(reParseResult.tags, reParseResult.fonts)

        assertEquals("مرحبا بالعالم", reExtractedTexts[0].editedText)
        assertEquals("Start Game", reExtractedTexts[1].editedText)
    }

    @Test
    fun testTranslationImportExport() {
        val swfBytes = createTestSwf()
        val parser = SwfParser()
        val parseResult = parser.parse(swfBytes)
        val extractor = TextExtractor()
        val texts = extractor.extract(parseResult.tags, parseResult.fonts)

        // Export to JSON
        val json = TranslationIO.exportToJson(texts)
        assertTrue(json.contains("Hello Flash World"))
        assertTrue(json.contains("Start Game"))

        // Modify via JSON import
        val customJson = """
            {
                "texts": [
                    {"id": "Text #001", "index": 1, "translation": "Bonjour le monde"},
                    {"id": "Text #002", "index": 2, "translation": "Commencer"}
                ]
            }
        """.trimIndent()

        val importRes = TranslationIO.importFromJson(customJson, texts)
        assertTrue(importRes.success)
        assertEquals(2, importRes.importedCount)
        assertEquals("Bonjour le monde", importRes.updatedTexts[0].editedText)
        assertEquals("Commencer", importRes.updatedTexts[1].editedText)
    }

    private fun createTestSwf(): ByteArray {
        val bodyWriter = SwfBinaryWriter()
        bodyWriter.writeRect(SwfRect(15, 0, 16000, 0, 12000))
        bodyWriter.writeFixed8(24.0f)
        bodyWriter.writeUI16(1) // 1 frame

        // Tag 1: DefineEditText with "Hello Flash World"
        val editWriter1 = SwfBinaryWriter()
        editWriter1.writeUI16(10)
        editWriter1.writeRect(SwfRect(15, 0, 4000, 0, 1000))
        editWriter1.writeUI16(0x0080) // HasText flag
        editWriter1.writeNullTerminatedString("var1")
        editWriter1.writeNullTerminatedString("Hello Flash World")
        bodyWriter.writeTag(SwfTag(SwfTagCode.DEFINE_EDIT_TEXT, editWriter1.toByteArray()))

        // Tag 2: DefineEditText with "Start Game"
        val editWriter2 = SwfBinaryWriter()
        editWriter2.writeUI16(11)
        editWriter2.writeRect(SwfRect(15, 0, 4000, 1000, 2000))
        editWriter2.writeUI16(0x0080)
        editWriter2.writeNullTerminatedString("var2")
        editWriter2.writeNullTerminatedString("Start Game")
        bodyWriter.writeTag(SwfTag(SwfTagCode.DEFINE_EDIT_TEXT, editWriter2.toByteArray()))

        // Tag 3: ShowFrame & End
        bodyWriter.writeTag(SwfTag(SwfTagCode.SHOW_FRAME, ByteArray(0)))
        bodyWriter.writeTag(SwfTag(SwfTagCode.END, ByteArray(0)))

        val body = bodyWriter.toByteArray()
        val headerWriter = SwfBinaryWriter()
        headerWriter.writeString("FWS", Charsets.US_ASCII)
        headerWriter.writeUI8(10)
        headerWriter.writeUI32(8L + body.size.toLong())

        val stream = ByteArrayOutputStream()
        stream.write(headerWriter.toByteArray())
        stream.write(body)
        return stream.toByteArray()
    }
}
