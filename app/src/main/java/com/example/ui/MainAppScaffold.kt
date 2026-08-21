package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChangeCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.localization.AppLanguage
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.BuildExportScreen
import com.example.ui.screens.ChangesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.screens.TextsScreen
import com.example.ui.screens.TranslationScreen
import com.example.ui.theme.SwfEditorTheme
import com.example.ui.theme.TagErrorColor
import com.example.ui.theme.TagModifiedColor
import com.example.ui.viewmodel.SwfEditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(viewModel: SwfEditorViewModel) {
    val language by viewModel.language.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val strings by viewModel.strings.collectAsState()
    val loadedSwf by viewModel.loadedSwf.collectAsState()
    val textObjects by viewModel.textObjects.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadingStatus by viewModel.loadingStatus.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showLangMenu by remember { mutableStateOf(false) }

    val modifiedCount = textObjects.count { it.isModified }

    val layoutDirection = if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        SwfEditorTheme(darkTheme = isDarkMode) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = strings.appTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = loadedSwf?.fileName ?: "No file",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        actions = {
                            // Undo / Redo
                            IconButton(
                                onClick = { viewModel.undo() },
                                modifier = Modifier.testTag("action_undo")
                            ) {
                                Icon(Icons.Default.Undo, contentDescription = "Undo")
                            }
                            IconButton(
                                onClick = { viewModel.redo() },
                                modifier = Modifier.testTag("action_redo")
                            ) {
                                Icon(Icons.Default.Redo, contentDescription = "Redo")
                            }

                            // Language Switcher Dropdown
                            Box {
                                IconButton(
                                    onClick = { showLangMenu = true },
                                    modifier = Modifier.testTag("action_language")
                                ) {
                                    Icon(Icons.Default.Language, contentDescription = "Language")
                                }
                                DropdownMenu(
                                    expanded = showLangMenu,
                                    onDismissRequest = { showLangMenu = false }
                                ) {
                                    AppLanguage.entries.forEach { lang ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = lang.displayName,
                                                    fontWeight = if (language == lang) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                viewModel.setLanguage(lang)
                                                showLangMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        // 0: Overview
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Default.Home, contentDescription = null) },
                            label = { Text(strings.tabOverview, maxLines = 1) },
                            modifier = Modifier.testTag("nav_overview")
                        )

                        // 1: Texts
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (textObjects.isNotEmpty()) {
                                            Badge { Text("${textObjects.size}") }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.TextFields, contentDescription = null)
                                }
                            },
                            label = { Text(strings.tabTexts, maxLines = 1) },
                            modifier = Modifier.testTag("nav_texts")
                        )

                        // 2: Translation
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Default.Translate, contentDescription = null) },
                            label = { Text(strings.tabTranslation, maxLines = 1) },
                            modifier = Modifier.testTag("nav_translation")
                        )

                        // 3: Changes
                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (modifiedCount > 0) {
                                            Badge(containerColor = TagModifiedColor) {
                                                Text("$modifiedCount")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ChangeCircle, contentDescription = null)
                                }
                            },
                            label = { Text(strings.tabChanges, maxLines = 1) },
                            modifier = Modifier.testTag("nav_changes")
                        )

                        // 4: Build
                        NavigationBarItem(
                            selected = selectedTab == 4,
                            onClick = { selectedTab = 4 },
                            icon = { Icon(Icons.Default.Build, contentDescription = null) },
                            label = { Text(strings.tabBuild, maxLines = 1) },
                            modifier = Modifier.testTag("nav_build")
                        )

                        // 5: Analytics
                        NavigationBarItem(
                            selected = selectedTab == 5,
                            onClick = { selectedTab = 5 },
                            icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                            label = { Text("Stats", maxLines = 1) },
                            modifier = Modifier.testTag("nav_stats")
                        )

                        // 6: Settings
                        NavigationBarItem(
                            selected = selectedTab == 6,
                            onClick = { selectedTab = 6 },
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            label = { Text(strings.tabSettings, maxLines = 1) },
                            modifier = Modifier.testTag("nav_settings")
                        )

                        // 7: About
                        NavigationBarItem(
                            selected = selectedTab == 7,
                            onClick = { selectedTab = 7 },
                            icon = { Icon(Icons.Default.Info, contentDescription = null) },
                            label = { Text(strings.tabAbout, maxLines = 1) },
                            modifier = Modifier.testTag("nav_about")
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Error Banner
                        AnimatedVisibility(visible = errorMessage != null) {
                            Surface(
                                color = TagErrorColor.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = errorMessage ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TagErrorColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.dismissError() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TagErrorColor)
                                    }
                                }
                            }
                        }

                        // Main Screen Display
                        when (selectedTab) {
                            0 -> HomeScreen(
                                viewModel = viewModel,
                                onNavigateToTexts = { selectedTab = 1 },
                                onNavigateToBuild = { selectedTab = 4 }
                            )
                            1 -> TextsScreen(viewModel = viewModel)
                            2 -> TranslationScreen(viewModel = viewModel)
                            3 -> ChangesScreen(viewModel = viewModel, onNavigateToBuild = { selectedTab = 4 })
                            4 -> BuildExportScreen(viewModel = viewModel)
                            5 -> StatisticsScreen(viewModel = viewModel)
                            6 -> SettingsScreen(viewModel = viewModel)
                            7 -> AboutScreen(viewModel = viewModel)
                        }
                    }

                    // Loading Overlay
                    if (isLoading) {
                        Surface(
                            color = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    CircularProgressIndicator()
                                    Text(
                                        text = loadingStatus.ifEmpty { "Processing SWF..." },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
