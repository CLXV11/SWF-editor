package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.text.TextObject
import com.example.ui.theme.TagAbcColor
import com.example.ui.theme.TagEditTextColor
import com.example.ui.theme.TagModifiedColor
import com.example.ui.theme.TagStaticColor
import com.example.ui.theme.TagSuccessColor
import com.example.ui.viewmodel.SwfEditorViewModel
import com.example.ui.viewmodel.TextFilter
import com.example.ui.viewmodel.TextSort

@Composable
fun TextsScreen(
    viewModel: SwfEditorViewModel,
    modifier: Modifier = Modifier
) {
    val strings by viewModel.strings.collectAsState()
    val textObjects by viewModel.textObjects.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val replaceQuery by viewModel.replaceQuery.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val activeSort by viewModel.activeSort.collectAsState()
    val selectedText by viewModel.selectedText.collectAsState()

    var showBatchReplaceDialog by remember { mutableStateOf(false) }
    var showDuplicatesDialog by remember { mutableStateOf(false) }

    // Filter & Sort
    val filteredTexts = remember(textObjects, searchQuery, activeFilter, activeSort) {
        var list = textObjects.filter { item ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                item.originalText.contains(searchQuery, ignoreCase = true) ||
                        item.editedText.contains(searchQuery, ignoreCase = true) ||
                        item.id.contains(searchQuery, ignoreCase = true) ||
                        (item.variableName?.contains(searchQuery, ignoreCase = true) == true)
            }
            val matchesFilter = when (activeFilter) {
                TextFilter.ALL -> true
                TextFilter.MODIFIED -> item.isModified
                TextFilter.UNMODIFIED -> !item.isModified
                TextFilter.EMPTY -> item.editedText.isEmpty()
                TextFilter.STATIC -> item.isStatic
                TextFilter.EDITABLE -> !item.isStatic
            }
            matchesSearch && matchesFilter
        }

        when (activeSort) {
            TextSort.ID -> list.sortedBy { it.index }
            TextSort.FRAME -> list.sortedBy { it.frame }
            TextSort.MODIFIED -> list.sortedByDescending { it.isModified }
            TextSort.LENGTH -> list.sortedByDescending { it.editedText.length }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text(strings.searchPlaceholder) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_text_input"),
            shape = RoundedCornerShape(12.dp)
        )

        // Filter and Sort Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = activeFilter == TextFilter.ALL,
                onClick = { viewModel.setFilter(TextFilter.ALL) },
                label = { Text("${strings.filterAll} (${textObjects.size})") }
            )
            FilterChip(
                selected = activeFilter == TextFilter.MODIFIED,
                onClick = { viewModel.setFilter(TextFilter.MODIFIED) },
                label = { Text("${strings.filterModified} (${textObjects.count { it.isModified }})") }
            )
            FilterChip(
                selected = activeFilter == TextFilter.UNMODIFIED,
                onClick = { viewModel.setFilter(TextFilter.UNMODIFIED) },
                label = { Text(strings.filterUnmodified) }
            )
            FilterChip(
                selected = activeFilter == TextFilter.STATIC,
                onClick = { viewModel.setFilter(TextFilter.STATIC) },
                label = { Text("Static Text") }
            )
            FilterChip(
                selected = activeFilter == TextFilter.EDITABLE,
                onClick = { viewModel.setFilter(TextFilter.EDITABLE) },
                label = { Text("Input / Dynamic") }
            )

            // Batch Actions
            OutlinedButton(
                onClick = { showBatchReplaceDialog = true },
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.FindReplace, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.replaceButton)
            }

            OutlinedButton(
                onClick = { showDuplicatesDialog = true },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Find Duplicates")
            }
        }

        // List of texts
        if (filteredTexts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotEmpty()) "No text matches \"$searchQuery\"" else "No texts available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTexts, key = { it.id + it.index }) { item ->
                    TextItemCard(
                        item = item,
                        strings = strings,
                        onClick = { viewModel.selectText(item) },
                        onReset = { viewModel.resetText(item.index) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Detail / Edit Sheet
    if (selectedText != null) {
        TextDetailSheet(
            textObject = selectedText!!,
            strings = strings,
            onDismiss = { viewModel.selectText(null) },
            onSave = { updatedText ->
                viewModel.updateText(selectedText!!.index, updatedText)
            },
            onReset = {
                viewModel.resetText(selectedText!!.index)
            }
        )
    }

    // Batch Replace Dialog
    if (showBatchReplaceDialog) {
        var targetText by remember { mutableStateOf(searchQuery) }
        var replaceText by remember { mutableStateOf(replaceQuery) }
        var caseSensitive by remember { mutableStateOf(false) }

        val matchCount = remember(targetText, textObjects, caseSensitive) {
            if (targetText.isEmpty()) 0 else {
                textObjects.count {
                    if (caseSensitive) it.editedText.contains(targetText)
                    else it.editedText.contains(targetText, ignoreCase = true)
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showBatchReplaceDialog = false },
            title = { Text(strings.replaceButton) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it },
                        label = { Text("Find text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = replaceText,
                        onValueChange = { replaceText = it },
                        label = { Text(strings.replacePlaceholder) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "$matchCount matching text object(s) found.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.batchReplace(targetText, replaceText, caseSensitive)
                        showBatchReplaceDialog = false
                    },
                    enabled = targetText.isNotEmpty()
                ) {
                    Text(strings.replaceAllButton)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchReplaceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Duplicate Texts Dialog
    if (showDuplicatesDialog) {
        val duplicates = remember(textObjects) {
            textObjects.groupBy { it.editedText }
                .filter { it.value.size > 1 && it.key.isNotBlank() }
                .toList()
                .sortedByDescending { it.second.size }
        }

        AlertDialog(
            onDismissRequest = { showDuplicatesDialog = false },
            title = { Text("Duplicate Texts (${duplicates.size})") },
            text = {
                if (duplicates.isEmpty()) {
                    Text("No duplicate texts detected in this SWF.")
                } else {
                    LazyColumn(
                        modifier = Modifier.height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(duplicates) { (text, occurrences) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setSearchQuery(text)
                                        showDuplicatesDialog = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "\"$text\"",
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Repeated ${occurrences.size} times (IDs: ${occurrences.take(4).joinToString { it.id }}${if (occurrences.size > 4) "..." else ""})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDuplicatesDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun TextItemCard(
    item: TextObject,
    strings: com.example.ui.localization.AppStrings,
    onClick: () -> Unit,
    onReset: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val tagColor = when (item.tagType) {
        "DefineEditText" -> TagEditTextColor
        "DoABC" -> TagAbcColor
        else -> TagStaticColor
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("text_card_${item.index}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isModified) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header row: ID, Tag type, Frame, Modified status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.id,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = tagColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = item.tagType,
                            style = MaterialTheme.typography.labelSmall,
                            color = tagColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (item.frame > 1) {
                        Text(
                            text = "Frame ${item.frame}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (item.isModified) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = TagModifiedColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = strings.filterModified,
                            style = MaterialTheme.typography.labelSmall,
                            color = TagModifiedColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Text Preview
            if (item.isModified) {
                // Show Original and Edited side-by-side or stacked
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Original: ${item.originalText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.editedText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = item.originalText.ifEmpty { "(Empty Text)" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.originalText.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Footer info: Font, Char count, Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.fontName ?: "Default font"} • ${item.editedText.length} chars",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { clipboard.setText(AnnotatedString(item.editedText)) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                    }
                    if (item.isModified) {
                        IconButton(
                            onClick = onReset,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(
                        onClick = onClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
