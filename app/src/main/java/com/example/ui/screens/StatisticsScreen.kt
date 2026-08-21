package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.swf.SwfTagCode
import com.example.ui.theme.TagModifiedColor
import com.example.ui.theme.TagSuccessColor
import com.example.ui.viewmodel.SwfEditorViewModel

@Composable
fun StatisticsScreen(
    viewModel: SwfEditorViewModel,
    modifier: Modifier = Modifier
) {
    val strings by viewModel.strings.collectAsState()
    val loadedSwf by viewModel.loadedSwf.collectAsState()
    val textObjects by viewModel.textObjects.collectAsState()

    if (loadedSwf == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = strings.noFileLoaded,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val parse = loadedSwf!!.parseResult
    val tagBreakdown = remember(parse) {
        parse.tags.groupBy { it.code }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
    }

    val totalChars = remember(textObjects) { textObjects.sumOf { it.editedText.length } }
    val avgChars = if (textObjects.isNotEmpty()) totalChars / textObjects.size else 0
    val modifiedChars = remember(textObjects) {
        textObjects.filter { it.isModified }.sumOf { it.editedText.length }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Text & Character Analytics Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Text & Character Metrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Text Objects:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${textObjects.size}", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Characters in File:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$totalChars chars", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Average String Length:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$avgChars chars", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Modified Characters:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$modifiedChars chars", fontWeight = FontWeight.Bold, color = TagModifiedColor)
                }
            }
        }

        // Embedded Fonts & Glyph Table
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.FontDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Embedded Fonts (${parse.fonts.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                if (parse.fonts.isEmpty()) {
                    Text(
                        text = "No embedded font definitions found (uses system device fonts).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    parse.fonts.values.forEach { font ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Font ID ${font.fontId}: ${font.fontName}", fontWeight = FontWeight.Bold)
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (font.hasArabicSupport) TagSuccessColor.copy(alpha = 0.15f) else TagModifiedColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (font.hasArabicSupport) "Arabic / Unicode OK" else "Latin Only",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (font.hasArabicSupport) TagSuccessColor else TagModifiedColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text("Glyphs Count: ${font.glyphCount} • Unicode: ${font.isUnicode}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // SWF Tag Container Breakdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Tag Breakdown (${parse.tags.size} Total Tags)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                tagBreakdown.take(8).forEach { (code, count) ->
                    val tagName = getTagName(code)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(tagName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$count tag(s)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

fun getTagName(code: Int): String {
    return when (code) {
        SwfTagCode.END -> "End (Tag 0)"
        SwfTagCode.SHOW_FRAME -> "ShowFrame (Tag 1)"
        SwfTagCode.DEFINE_SHAPE -> "DefineShape (Tag 2)"
        SwfTagCode.SET_BACKGROUND_COLOR -> "SetBackgroundColor (Tag 9)"
        SwfTagCode.DEFINE_FONT -> "DefineFont (Tag 10)"
        SwfTagCode.DEFINE_TEXT -> "DefineText (Tag 11)"
        SwfTagCode.DO_ACTION -> "DoAction (Tag 12)"
        SwfTagCode.DEFINE_SOUND -> "DefineSound (Tag 14)"
        SwfTagCode.DEFINE_BITS_JPEG2 -> "DefineBitsJPEG2 (Tag 21)"
        SwfTagCode.DEFINE_SHAPE2 -> "DefineShape2 (Tag 22)"
        SwfTagCode.PLACE_OBJECT2 -> "PlaceObject2 (Tag 26)"
        SwfTagCode.REMOVE_OBJECT2 -> "RemoveObject2 (Tag 28)"
        SwfTagCode.DEFINE_TEXT2 -> "DefineText2 (Tag 33)"
        SwfTagCode.DEFINE_EDIT_TEXT -> "DefineEditText (Tag 37)"
        SwfTagCode.DEFINE_SPRITE -> "DefineSprite (Tag 39)"
        SwfTagCode.DEFINE_FONT2 -> "DefineFont2 (Tag 48)"
        SwfTagCode.DEFINE_FONT3 -> "DefineFont3 (Tag 75)"
        SwfTagCode.DO_ABC -> "DoABC (Tag 82)"
        else -> "Tag ID $code"
    }
}
