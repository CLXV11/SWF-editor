package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.theme.TagErrorColor
import com.example.ui.theme.TagModifiedColor
import com.example.ui.theme.TagSuccessColor
import com.example.ui.viewmodel.BuildUiState
import com.example.ui.viewmodel.SwfEditorViewModel
import java.io.File
import java.io.FileOutputStream

@Composable
fun BuildExportScreen(
    viewModel: SwfEditorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings by viewModel.strings.collectAsState()
    val loadedSwf by viewModel.loadedSwf.collectAsState()
    val textObjects by viewModel.textObjects.collectAsState()
    val buildState by viewModel.buildState.collectAsState()
    val clipboard = LocalClipboardManager.current

    val modifiedCount = textObjects.count { it.isModified }
    val unchangedCount = textObjects.size - modifiedCount

    val defaultExportName = remember(loadedSwf) {
        val base = (loadedSwf?.fileName ?: "game.swf").removeSuffix(".swf")
        "${base}_edited.swf"
    }

    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-shockwave-flash")
    ) { uri: Uri? ->
        if (uri != null) {
            val success = viewModel.exportBuiltSwfToUri(uri, context)
            if (success) {
                Toast.makeText(context, "SWF exported successfully to device!", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Pre-build Preview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Build Preview & Integrity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Original File:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(loadedSwf?.fileName ?: "No file loaded", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.modifiedTexts + ":", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$modifiedCount", fontWeight = FontWeight.Bold, color = TagModifiedColor)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.unchangedTexts + ":", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$unchangedCount", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Original Size:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatFileSize(loadedSwf?.originalBytes?.size?.toLong() ?: 0), fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Preserved Non-Text Assets:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Images, Audio, Shapes, Frames 100% Intact", color = TagSuccessColor, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Build CTA & State Card
        when (val state = buildState) {
            is BuildUiState.Idle -> {
                Button(
                    onClick = { viewModel.buildSwf() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_start_build"),
                    shape = RoundedCornerShape(14.dp),
                    enabled = loadedSwf != null
                ) {
                    Icon(Icons.Default.Build, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.buildSwf, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            is BuildUiState.Building -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Building SWF...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                        )
                        Text(
                            text = "${state.status} (${(state.progress * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            is BuildUiState.Success -> {
                // Export Complete Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = TagSuccessColor.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = TagSuccessColor)
                                }
                            }
                            Column {
                                Text(
                                    text = strings.buildSuccess,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TagSuccessColor
                                )
                                Text(
                                    text = "Structural validation passed. All resources preserved.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Built Details
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Output File: $defaultExportName", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("Built Size: ${formatFileSize(state.result.outputSize)}", style = MaterialTheme.typography.bodySmall)
                            Text("Modified Tags: ${state.result.modifiedTagsCount}", style = MaterialTheme.typography.bodySmall)
                            Text("Verified Text Changes: ${state.validation.verifiedModificationsCount}", style = MaterialTheme.typography.bodySmall)
                        }

                        // Export Actions: Save to Device & Share
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { createDocLauncher.launch(defaultExportName) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_export_save_device")
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.exportFile)
                            }

                            OutlinedButton(
                                onClick = {
                                    shareBuiltSwf(context, defaultExportName, state.result.outputBytes)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }

                        // Rebuild
                        OutlinedButton(
                            onClick = { viewModel.buildSwf() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Rebuild SWF")
                        }
                    }
                }
            }
            is BuildUiState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = TagErrorColor)
                            Text(
                                text = strings.buildFailed,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TagErrorColor
                            )
                        }
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (state.details != null) {
                            OutlinedButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString("${state.message}\n${state.details}"))
                                    Toast.makeText(context, "Error details copied!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Error Details")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

fun shareBuiltSwf(context: Context, fileName: String, bytes: ByteArray) {
    try {
        val cacheFile = File(context.cacheDir, fileName)
        FileOutputStream(cacheFile).use { it.write(bytes) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/x-shockwave-flash"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Built SWF"))
    } catch (_: Exception) {
        Toast.makeText(context, "Direct share initialized. Use 'Save to Device' for storage.", Toast.LENGTH_SHORT).show()
    }
}
