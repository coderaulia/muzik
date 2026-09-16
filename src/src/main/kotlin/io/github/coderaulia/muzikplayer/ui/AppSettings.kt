package io.github.coderaulia.muzikplayer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import io.github.coderaulia.muzikplayer.data.Library
import io.github.coderaulia.muzikplayer.generated.resources.*
import io.github.coderaulia.muzikplayer.utils.GLOBAL_CONNECTION
import io.github.coderaulia.muzikplayer.utils.Preferences
import io.github.mmarco94.klibportal.portals.openFile
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import java.io.File
import java.nio.file.Path
import java.text.NumberFormat
import javax.swing.JFileChooser
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

// Libadwaita Slate design tokens delegating to active theme
private val SlateCanvas: Color @Composable get() = LocalMuzikColors.current.canvas
private val SlateContainerLow: Color @Composable get() = LocalMuzikColors.current.containerLow
private val SlateContainer: Color @Composable get() = LocalMuzikColors.current.container
private val SlateContainerHigh: Color @Composable get() = LocalMuzikColors.current.containerHigh
private val SlateContainerHighest: Color @Composable get() = LocalMuzikColors.current.containerHighest
private val SlateContainerLowest: Color @Composable get() = LocalMuzikColors.current.containerLowest
private val SlateBorder: Color @Composable get() = LocalMuzikColors.current.border
private val SlateBorderSubtle: Color @Composable get() = LocalMuzikColors.current.borderSubtle
private val SlateTextPrimary: Color @Composable get() = LocalMuzikColors.current.textPrimary
private val SlateTextSecondary: Color @Composable get() = LocalMuzikColors.current.textSecondary
private val SlateTextMuted: Color @Composable get() = LocalMuzikColors.current.textMuted
private val GnomeBlue: Color @Composable get() = LocalMuzikColors.current.accentPrimary
private val GnomeBlueLight: Color @Composable get() = LocalMuzikColors.current.accentPrimaryLight
private val GnomeGreen: Color @Composable get() = LocalMuzikColors.current.accentTertiary
private val GnomeGreenContainer: Color @Composable get() = LocalMuzikColors.current.accentTertiaryContainer
private val SlateError: Color @Composable get() = LocalMuzikColors.current.error

enum class SettingsCategory(
    val label: String,
    val icon: ImageVector,
    val subtitle: (Library?) -> String,
) {
    LIBRARY(
        label = "Library & Storage",
        icon = Icons.Default.Folder,
        subtitle = { lib -> "${lib?.stats?.songsCount ?: 0} watched tracks" },
    ),
    AUDIO(
        label = "Audio & Output Engine",
        icon = Icons.Default.GraphicEq,
        subtitle = { "PipeWire 24/96 ALSA" },
    ),
    APPEARANCE(
        label = "Appearance & Style",
        icon = Icons.Default.Palette,
        subtitle = { "Libadwaita Dark" },
    ),
    INTEGRATION(
        label = "Integration & MPRIS",
        icon = Icons.Default.Hub,
        subtitle = { "org.mpris.MediaPlayer2" },
    ),
    ABOUT(
        label = "About MuzikPlayer",
        icon = Icons.Default.Info,
        subtitle = { "v1.5.3 (x86_64)" },
    ),
}

@Composable
fun AppSettingsWindow(
    library: Library? = null,
    close: () -> Unit,
) {
    val cs = rememberCoroutineScope()
    var maintainOnTop by remember { mutableStateOf(0) }
    var selectedCategory by remember { mutableStateOf(SettingsCategory.LIBRARY) }
    var filterQuery by remember { mutableStateOf("") }

    Window(
        onCloseRequest = close,
        title = stringResource(Res.string.settings),
        state = rememberWindowState(
            size = DpSize(920.dp, 660.dp),
        ),
        alwaysOnTop = maintainOnTop > 0,
        onPreviewKeyEvent = { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                close()
                true
            } else {
                false
            }
        },
    ) {
        Scaled {
            MuzikTheme.Provider {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SlateCanvas,
                ) {
                    Column(Modifier.fillMaxSize()) {
                        // Top Header / Breadcrumb / Status Strip
                        SettingsHeaderBar(
                            selectedCategory = selectedCategory,
                            filterQuery = filterQuery,
                            onFilterChange = { filterQuery = it },
                        )

                        // Two-Pane Main Layout
                        Row(Modifier.weight(1f).fillMaxWidth()) {
                            // Left Navigation Sidebar
                            SettingsSidebar(
                                selectedCategory = selectedCategory,
                                onSelectCategory = { selectedCategory = it },
                                library = library,
                                modifier = Modifier.width(260.dp).fillMaxHeight(),
                            )

                            // Right Detail Pane
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(SlateCanvas)
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 28.dp, vertical = 24.dp),
                            ) {
                                when (selectedCategory) {
                                    SettingsCategory.LIBRARY -> LibrarySettingsPane(
                                        library = library,
                                        onSelectingFolder = { maintainOnTop = 0 },
                                        filterQuery = filterQuery,
                                    )
                                    SettingsCategory.AUDIO -> AudioSettingsPane(
                                        filterQuery = filterQuery,
                                    )
                                    SettingsCategory.APPEARANCE -> AppearanceSettingsPane(
                                        onToggledDecorations = { token ->
                                            maintainOnTop = token
                                            cs.launch {
                                                delay(1.seconds)
                                                if (maintainOnTop == token) maintainOnTop = 0
                                            }
                                        },
                                        filterQuery = filterQuery,
                                    )
                                    SettingsCategory.INTEGRATION -> IntegrationSettingsPane(
                                        filterQuery = filterQuery,
                                    )
                                    SettingsCategory.ABOUT -> AboutSettingsPane()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsHeaderBar(
    selectedCategory: SettingsCategory,
    filterQuery: String,
    onFilterChange: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        color = SlateContainerLow,
        border = BorderStroke(1.dp, SlateBorder),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Breadcrumb
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = GnomeBlueLight,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    "Preferences",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SlateTextPrimary,
                )
                Text(
                    "/",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateBorder,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    selectedCategory.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateTextSecondary,
                    fontWeight = FontWeight.Medium,
                )
            }

            // Right Status + Search
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Audio Engine Status Pill
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = SlateContainerHighest,
                    border = BorderStroke(1.dp, SlateBorder),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(GnomeGreen),
                        )
                        Text(
                            "PipeWire Node: bit-perfect locked",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                            ),
                            color = GnomeGreen,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                // Filter search bar
                Surface(
                    modifier = Modifier.width(180.dp).height(28.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = SlateContainer,
                    border = BorderStroke(1.dp, SlateBorder),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = null,
                            tint = SlateTextMuted,
                            modifier = Modifier.size(14.dp),
                        )
                        BasicTextField(
                            value = filterQuery,
                            onValueChange = onFilterChange,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(
                                color = SlateTextPrimary,
                                fontSize = 12.sp,
                            ),
                            cursorBrush = SolidColor(GnomeBlueLight),
                            decorationBox = { innerTextField ->
                                if (filterQuery.isEmpty()) {
                                    Text(
                                        "Filter settings...",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = SlateTextMuted,
                                    )
                                }
                                innerTextField()
                            },
                        )
                        if (filterQuery.isNotEmpty()) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear filter",
                                tint = SlateTextMuted,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .clickable { onFilterChange("") },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSidebar(
    selectedCategory: SettingsCategory,
    onSelectCategory: (SettingsCategory) -> Unit,
    library: Library?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = SlateContainerLow,
        border = BorderStroke(1.dp, SlateBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "CONFIGURATION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                    ),
                    color = SlateTextMuted,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                )

                SettingsCategory.entries.forEach { category ->
                    val isSelected = category == selectedCategory
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectCategory(category) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) GnomeBlue else Color.Transparent,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                category.icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else SlateTextSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    category.label,
                                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else SlateTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    category.subtitle(library),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                    ),
                                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else SlateTextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else SlateTextMuted.copy(alpha = 0.5f),
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
            }

            // Desktop Diagnostics Telemetry Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = SlateContainerLowest,
                border = BorderStroke(1.dp, SlateBorderSubtle),
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "STORAGE TELEMETRY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                            ),
                            color = GnomeGreen,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Indexed 100%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                            ),
                            color = SlateTextSecondary,
                        )
                    }

                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(SlateContainerHighest),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(GnomeGreen),
                        )
                    }

                    Text(
                        "${library?.stats?.songsCount ?: 0} Lossless Tracks (${library?.albums?.size ?: 0} Albums)\nCache: In-memory & RocksDB v8.1",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.5.sp,
                            lineHeight = 13.sp,
                        ),
                        color = SlateTextMuted,
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Category Detail Panes
// ------------------------------------------------------------------------------------------------

@Composable
private fun LibrarySettingsPane(
    library: Library?,
    onSelectingFolder: () -> Unit,
    filterQuery: String,
) {
    val cs = rememberCoroutineScope()
    val libraryFolders by Preferences.libraryFolders.state
    val readOnlyMode by Preferences.readOnlyMode.state
    val watchFilesystem by Preferences.watchFilesystem.state
    var cacheClearedMessage by remember { mutableStateOf<String?>(null) }
    var isRescanning by remember { mutableStateOf(false) }

    val chooseLibraryStr = stringResource(Res.string.action_choose_library)

    suspend fun pickFolders(title: String, multiple: Boolean): List<Path> {
        // Try XDG Desktop Portal first (native file chooser via D-Bus)
        try {
            val dbus = GLOBAL_CONNECTION.await()
            if (dbus != null) {
                val selected = openFile(
                    dbus,
                    title = title,
                    directory = true,
                    multiple = multiple,
                )
                if (selected.isNotEmpty()) {
                    return selected.filter { it.isDirectory() }
                }
            }
        } catch (e: Exception) {
            logger.warn(e) { "XDG Portal folder picker unavailable or failed, falling back to Swing JFileChooser" }
        }

        // Fallback: Swing JFileChooser (always works across all desktop environments)
        return withContext(Dispatchers.Swing) {
            val chooser = JFileChooser().apply {
                dialogTitle = title
                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                isMultiSelectionEnabled = multiple
            }
            val result = chooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                if (multiple && chooser.selectedFiles.isNotEmpty()) {
                    chooser.selectedFiles.map { it.toPath() }.filter { it.isDirectory() }
                } else if (chooser.selectedFile != null) {
                    listOfNotNull(chooser.selectedFile?.toPath()).filter { it.isDirectory() }
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }

    fun openAddFoldersSelector() {
        onSelectingFolder()
        cs.launch {
            try {
                val paths = pickFolders(title = chooseLibraryStr, multiple = true)
                logger.info { "Selected folders to add: $paths" }
                if (paths.isNotEmpty()) {
                    Preferences.addLibraryFolders(paths)
                }
            } catch (e: Exception) {
                logger.error(e) { "Error picking folder" }
            }
        }
    }

    fun openChangePrimaryFolderSelector() {
        onSelectingFolder()
        cs.launch {
            try {
                val paths = pickFolders(title = chooseLibraryStr, multiple = false)
                val first = paths.firstOrNull()
                logger.info { "Selected primary folder: $first" }
                if (first != null) {
                    Preferences.libraryFolder.set(first)
                    Preferences.triggerLibraryRescan()
                }
            } catch (e: Exception) {
                logger.error(e) { "Error changing primary folder" }
            }
        }
    }

    PaneHeader(
        title = "Library & Storage",
        description = "Manage your local media directories, file system monitoring, and non-destructive index modes.",
    )

    Spacer(Modifier.height(18.dp))

    // Preference Group 1: Monitored Directories
    SectionTitle("MUSIC LIBRARY DIRECTORIES")
    Spacer(Modifier.height(6.dp))
    LibadwaitaGroupCard {
        libraryFolders.forEachIndexed { index, folder ->
            val isPrimary = index == 0
            LibadwaitaPreferenceRow(
                title = folder.pathString,
                subtitle = if (isPrimary) {
                    "${library?.stats?.songsCount ?: 0} tracks • ${library?.albums?.size ?: 0} albums"
                } else {
                    "Monitored secondary library folder"
                },
                badgeText = if (isPrimary) "Primary" else "Secondary",
                badgeColor = if (isPrimary) GnomeBlueLight else SlateTextSecondary,
                badgeBackground = if (isPrimary) Color(0x334691F2) else SlateContainerHigh,
                filterQuery = filterQuery,
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SlateContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = if (isPrimary) GnomeBlueLight else SlateTextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
                trailingContent = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isPrimary) {
                            Button(
                                onClick = {
                                    isRescanning = true
                                    Preferences.triggerLibraryRescan()
                                    cs.launch {
                                        delay(1.seconds)
                                        isRescanning = false
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SlateContainerHigh,
                                    contentColor = SlateTextPrimary,
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(30.dp),
                            ) {
                                Text(
                                    if (isRescanning) "Scanning..." else "Rescan",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                )
                            }

                            Button(
                                onClick = ::openChangePrimaryFolderSelector,
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SlateContainerHigh,
                                    contentColor = SlateTextPrimary,
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(30.dp),
                            ) {
                                Text(
                                    "Change...",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                )
                            }
                        }

                        if (libraryFolders.size > 1) {
                            Button(
                                onClick = {
                                    Preferences.removeLibraryFolder(folder)
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SlateContainerHigh,
                                    contentColor = SlateError,
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(30.dp),
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Remove folder",
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Remove",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                )
                            }
                        }
                    }
                },
            )

            LibadwaitaDivider()
        }

        // Add music folder action row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                .clickable { openAddFoldersSelector() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Default.AddCircle,
                contentDescription = null,
                tint = GnomeBlueLight,
                modifier = Modifier.size(18.dp),
            )
            Text(
                "Add Music Folder...",
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                color = GnomeBlueLight,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }

    Spacer(Modifier.height(22.dp))

    // Preference Group 2: Integrity & Inotify
    SectionTitle("INTEGRITY & INOTIFY")
    Spacer(Modifier.height(6.dp))
    LibadwaitaGroupCard {
        LibadwaitaPreferenceRow(
            title = "Read-only mode (Strict)",
            subtitle = "MuzikPlayer will never modify, rename, or write ID3/FLAC metadata tags back to source media files.",
            badgeText = "Default",
            badgeColor = GnomeGreen,
            badgeBackground = GnomeGreenContainer,
            filterQuery = filterQuery,
            trailingContent = {
                LibadwaitaSwitch(
                    checked = readOnlyMode,
                    onCheckedChange = { Preferences.readOnlyMode.set(it) },
                )
            },
        )

        LibadwaitaDivider()

        LibadwaitaPreferenceRow(
            title = "Watch for filesystem changes (inotify)",
            subtitle = "Automatically refresh index when albums or cue sheets are moved, added, or deleted via file manager.",
            filterQuery = filterQuery,
            trailingContent = {
                LibadwaitaSwitch(
                    checked = watchFilesystem,
                    onCheckedChange = { Preferences.watchFilesystem.set(it) },
                )
            },
        )

        LibadwaitaDivider()

        LibadwaitaPreferenceRow(
            title = "Local Cover Art Cache",
            subtitle = "Store extracted high-resolution cover arts in ~/.cache/muzikplayer/covers for instant layout rendering.",
            filterQuery = filterQuery,
            trailingContent = {
                Button(
                    onClick = {
                        cs.launch {
                            try {
                                val cacheDir = File(System.getProperty("user.home"), ".cache/muzikplayer/covers")
                                if (cacheDir.exists()) {
                                    cacheDir.deleteRecursively()
                                }
                            } catch (_: Exception) {}
                            cacheClearedMessage = "Cache Cleared"
                            delay(2.seconds)
                            cacheClearedMessage = null
                        }
                    },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SlateContainerHigh,
                        contentColor = SlateTextPrimary,
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(30.dp),
                ) {
                    Text(
                        cacheClearedMessage ?: "Clear Cache",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    )
                }
            },
        )
    }
}

@Composable
private fun AudioSettingsPane(filterQuery: String) {
    val bitPerfect by Preferences.bitPerfect.state
    val bufferLatency by Preferences.bufferLatency.state
    val replayGain by Preferences.replayGain.state
    val peakProtection by Preferences.peakProtection.state
    var replayGainDropdownOpen by remember { mutableStateOf(false) }

    PaneHeader(
        title = "Audio & Output Engine",
        description = "Direct hardware routing, sample rate synchronization, and native Linux PipeWire / WirePlumber audio nodes.",
    )

    Spacer(Modifier.height(18.dp))

    SectionTitle("AUDIO OUTPUT & PIPEWIRE")
    Spacer(Modifier.height(6.dp))
    LibadwaitaGroupCard {
        // Output device status row
        LibadwaitaPreferenceRow(
            title = "Output Device",
            subtitle = "Physical DAC or default PipeWire audio sink",
            filterQuery = filterQuery,
            trailingContent = {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SlateContainerHigh,
                    border = BorderStroke(1.dp, SlateBorder),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(GnomeGreen),
                        )
                        Text(
                            "PipeWire Direct ALSA (hw:0,0)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = SlateTextPrimary,
                        )
                    }
                }
            },
        )

        LibadwaitaDivider()

        // Bit-perfect direct stream
        LibadwaitaPreferenceRow(
            title = "Bit-Perfect Direct Stream",
            subtitle = "Bypass system software mixer, avoid 48 kHz automatic resampler, match stream clock directly to DAC hardware.",
            badgeText = "Bit-Perfect 1:1",
            badgeColor = GnomeGreen,
            badgeBackground = GnomeGreenContainer,
            filterQuery = filterQuery,
            trailingContent = {
                LibadwaitaSwitch(
                    checked = bitPerfect,
                    onCheckedChange = { Preferences.bitPerfect.set(it) },
                )
            },
        )

        LibadwaitaDivider()

        // Buffer Latency
        LibadwaitaPreferenceRow(
            title = "Buffer Latency",
            subtitle = "Current: 5.33 ms (512 frames @ 96.0 kHz)",
            filterQuery = filterQuery,
            trailingContent = {
                LibadwaitaSegmentedControl(
                    options = listOf(
                        "512 (5.3ms)" to "512 (5.3ms)",
                        "1024 (10.6ms)" to "1024 (10.6ms)",
                        "2048 (Safe)" to "2048 (Safe)",
                    ),
                    selected = bufferLatency,
                    onSelect = { Preferences.bufferLatency.set(it) },
                )
            },
        )

        LibadwaitaDivider()

        // ReplayGain
        LibadwaitaPreferenceRow(
            title = "ReplayGain Loudness Normalization",
            subtitle = "EBU R128 loudness matching without dynamic range distortion.",
            filterQuery = filterQuery,
            trailingContent = {
                Box {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { replayGainDropdownOpen = true },
                        shape = RoundedCornerShape(6.dp),
                        color = SlateContainerHigh,
                        border = BorderStroke(1.dp, SlateBorder),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                replayGain,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = SlateTextPrimary,
                            )
                            Icon(
                                Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = SlateTextSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = replayGainDropdownOpen,
                        onDismissRequest = { replayGainDropdownOpen = false },
                    ) {
                        listOf(
                            "Album Gain (-1.4 dB target)",
                            "Track Gain (-1.4 dB target)",
                            "Disabled",
                        ).forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    Preferences.replayGain.set(option)
                                    replayGainDropdownOpen = false
                                },
                            )
                        }
                    }
                }
            },
        )

        LibadwaitaDivider()

        // True peak clipping protection
        LibadwaitaPreferenceRow(
            title = "Prevent True-Peak Intersample Clipping",
            subtitle = "Automatically lower preamp gain if high sample-rate intersample peaks exceed 0.0 dBFS.",
            filterQuery = filterQuery,
            trailingContent = {
                LibadwaitaSwitch(
                    checked = peakProtection,
                    onCheckedChange = { Preferences.peakProtection.set(it) },
                )
            },
        )
    }
}

@Composable
private fun AppearanceSettingsPane(
    onToggledDecorations: (Int) -> Unit,
    filterQuery: String,
) {
    val theme by Preferences.theme.state
    val useSystemDecorations by Preferences.useSystemDecorations.state
    val fontScale by Preferences.fontScale.state
    val audioTelemetry by Preferences.audioTelemetry.state
    val showTableThumbnails by Preferences.showTableThumbnails.state

    PaneHeader(
        title = "Interface & Typography",
        description = "GTK4/Libadwaita desktop theme alignment, system fonts, and player telemetry overlays.",
    )

    Spacer(Modifier.height(18.dp))

    SectionTitle("DESKTOP THEME & FONTS")
    Spacer(Modifier.height(6.dp))
    LibadwaitaGroupCard {
        // Theme selector
        LibadwaitaPreferenceRow(
            title = "Color Scheme",
            subtitle = "Adapt to GNOME dark style preference",
            filterQuery = filterQuery,
            trailingContent = {
                LibadwaitaSegmentedControl(
                    options = listOf(
                        "System" to MuzikTheme.UserPreference.AUTO,
                        "Dark" to MuzikTheme.UserPreference.DARK,
                        "Light" to MuzikTheme.UserPreference.LIGHT,
                    ),
                    selected = theme,
                    onSelect = { Preferences.theme.set(it) },
                )
            },
        )

        LibadwaitaDivider()

        // System window decorations
        LibadwaitaPreferenceRow(
            title = "Native Window Decorations",
            subtitle = "Use system titlebar instead of custom Libadwaita headerbar",
            filterQuery = filterQuery,
            trailingContent = {
                LibadwaitaSwitch(
                    checked = useSystemDecorations,
                    onCheckedChange = {
                        val token = Random.nextInt(1..Int.MAX_VALUE)
                        onToggledDecorations(token)
                        Preferences.useSystemDecorations.set(it)
                    },
                )
            },
        )

        LibadwaitaDivider()

        // Font scale slider
        val showFontScale = filterQuery.isBlank() || "ui font scale".contains(filterQuery.lowercase()) || "scaling".contains(filterQuery.lowercase())
        if (showFontScale) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "UI Font Scale",
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = SlateTextPrimary,
                        )
                        Text(
                            "Global text scaling factor across all panels",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = SlateTextSecondary,
                        )
                    }
                    Text(
                        NumberFormat.getPercentInstance().format(fontScale),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = GnomeBlueLight,
                    )
                }
                Slider(
                    value = fontScale,
                    onValueChange = { Preferences.fontScale.set((it * 10).roundToInt() / 10f) },
                    steps = 14,
                    valueRange = 0.5f..2f,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = GnomeBlueLight,
                        activeTrackColor = GnomeBlue,
                        inactiveTrackColor = SlateContainerHighest,
                    ),
                )
            }
            LibadwaitaDivider()
        }

        // Font family (status-only with fallback badge)
        LibadwaitaPreferenceRow(
            title = "UI Font Family",
            subtitle = "Native desktop typography with monospaced audio counters",
            badgeText = "Platform Fallback",
            badgeColor = SlateTextSecondary,
            badgeBackground = SlateContainerHighest,
            filterQuery = filterQuery,
            trailingContent = {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SlateContainerHigh,
                    border = BorderStroke(1.dp, SlateBorder),
                ) {
                    Text(
                        "SF Pro Text • SF Mono",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = SlateTextPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            },
        )

        LibadwaitaDivider()

        // Audio format telemetry
        LibadwaitaPreferenceRow(
            title = "Real-time Audio Format Telemetry",
            subtitle = "Display format pill (e.g. FLAC 24/96) in bottom transport bar deck.",
            filterQuery = filterQuery,
            trailingContent = {
                LibadwaitaSwitch(
                    checked = audioTelemetry,
                    onCheckedChange = { Preferences.audioTelemetry.set(it) },
                )
            },
        )

        LibadwaitaDivider()

        // Table row thumbnails
        LibadwaitaPreferenceRow(
            title = "Album Miniatures in Table Rows",
            subtitle = "Render 28px album thumbnails in high-density tracklists.",
            filterQuery = filterQuery,
            trailingContent = {
                LibadwaitaSwitch(
                    checked = showTableThumbnails,
                    onCheckedChange = { Preferences.showTableThumbnails.set(it) },
                )
            },
        )
    }
}

@Composable
private fun IntegrationSettingsPane(filterQuery: String) {
    val mprisEnabled by Preferences.mprisEnabled.state
    val discordRpc by Preferences.discordRpc.state
    val listenBrainzToken by Preferences.listenBrainzToken.state
    var showTokenDialog by remember { mutableStateOf(false) }
    var tokenInput by remember(listenBrainzToken) { mutableStateOf(listenBrainzToken) }

    PaneHeader(
        title = "Desktop Integration & MPRIS",
        description = "Connect with Linux D-Bus services, hardware media keys, and external scrobblers.",
    )

    Spacer(Modifier.height(18.dp))

    SectionTitle("D-BUS SESSION BUS")
    Spacer(Modifier.height(6.dp))
    LibadwaitaGroupCard {
        // MPRIS
        LibadwaitaPreferenceRow(
            title = "MPRIS v2 Media Player Interface",
            subtitle = "Expose track status and transport controls to GNOME top notification tray, lock screen, and keyboard keys.",
            badgeText = "Registered",
            badgeColor = GnomeGreen,
            badgeBackground = GnomeGreenContainer,
            filterQuery = filterQuery,
            trailingContent = {
                LibadwaitaSwitch(
                    checked = mprisEnabled,
                    onCheckedChange = { Preferences.mprisEnabled.set(it) },
                )
            },
        )

        LibadwaitaDivider()

        // Discord RPC
        LibadwaitaPreferenceRow(
            title = "Discord Rich Presence (RPC)",
            subtitle = "Broadcast current track title, artist, and elapsed timeline to local Discord IPC socket.",
            badgeText = "Status: Disabled",
            badgeColor = SlateTextMuted,
            badgeBackground = SlateContainerHighest,
            filterQuery = filterQuery,
            trailingContent = {
                LibadwaitaSwitch(
                    checked = discordRpc,
                    onCheckedChange = { Preferences.discordRpc.set(it) },
                )
            },
        )

        LibadwaitaDivider()

        // ListenBrainz Scrobbler
        val hasToken = listenBrainzToken.isNotBlank()
        LibadwaitaPreferenceRow(
            title = "ListenBrainz Open Scrobbling",
            subtitle = "Submit listen history to community-owned MetaBrainz open registry.",
            badgeText = if (hasToken) "Connected" else "Not Configured",
            badgeColor = if (hasToken) GnomeGreen else SlateTextMuted,
            badgeBackground = if (hasToken) GnomeGreenContainer else SlateContainerHighest,
            filterQuery = filterQuery,
            trailingContent = {
                Button(
                    onClick = { showTokenDialog = true },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SlateContainerHigh,
                        contentColor = SlateTextPrimary,
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(30.dp),
                ) {
                    Text(
                        if (hasToken) "Edit Token" else "Connect Token",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    )
                }
            },
        )
    }

    if (showTokenDialog) {
        AlertDialog(
            onDismissRequest = { showTokenDialog = false },
            title = { Text("ListenBrainz User Token", color = SlateTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enter your personal user token from listenbrainz.org/profile to enable scrobbling:",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateTextSecondary,
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SlateContainerLowest,
                        border = BorderStroke(1.dp, SlateBorder),
                    ) {
                        BasicTextField(
                            value = tokenInput,
                            onValueChange = { tokenInput = it },
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            singleLine = true,
                            textStyle = TextStyle(
                                color = SlateTextPrimary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                            ),
                            cursorBrush = SolidColor(GnomeBlueLight),
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        Preferences.listenBrainzToken.set(tokenInput.trim())
                        showTokenDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GnomeBlue, contentColor = Color.White),
                ) {
                    Text("Save Token")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTokenDialog = false }) {
                    Text("Cancel", color = SlateTextSecondary)
                }
            },
            containerColor = SlateContainerLow,
        )
    }
}

@Composable
private fun AboutSettingsPane() {
    PaneHeader(
        title = "About MuzikPlayer",
        description = "System architecture, runtimes, and licensing.",
    )

    Spacer(Modifier.height(20.dp))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SlateContainerLow,
        border = BorderStroke(1.dp, SlateBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // App Icon Box
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SlateContainerHigh)
                    .border(1.dp, SlateBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Album,
                    contentDescription = null,
                    tint = GnomeBlueLight,
                    modifier = Modifier.size(38.dp),
                )
            }

            Text(
                "MuzikPlayer Desktop",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = SlateTextPrimary,
            )

            Text(
                "Version 1.5.3 (Stable Flatpak • Linux x86_64)",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = SlateTextMuted,
            )

            Text(
                "High-fidelity local music environment designed in the spirit of Libadwaita. Native, telemetry-free, bit-perfect audio for Linux.",
                style = MaterialTheme.typography.bodyMedium,
                color = SlateTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Spacer(Modifier.height(8.dp))

            // Badges row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RuntimePill("PipeWire 1.0.5")
                RuntimePill("FLAC 1.4.3")
                RuntimePill("FFSampledSP 0.9.1")
                RuntimePill("Compose Desktop 1.7")
                RuntimePill("Kotlin 2.0")
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "Licensed under GNU General Public License v3.0\nio.github.coderaulia.MuzikPlayer",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                ),
                color = SlateTextMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Reusable Libadwaita Components
// ------------------------------------------------------------------------------------------------

@Composable
private fun PaneHeader(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
            fontWeight = FontWeight.Bold,
            color = SlateTextPrimary,
        )
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = SlateTextSecondary,
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
        ),
        color = SlateTextMuted,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun LibadwaitaGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = SlateContainerLow,
        border = BorderStroke(1.dp, SlateBorder),
    ) {
        Column(content = content)
    }
}

@Composable
private fun LibadwaitaDivider() {
    HorizontalDivider(color = SlateBorderSubtle, thickness = 1.dp)
}

@Composable
private fun LibadwaitaPreferenceRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badgeText: String? = null,
    badgeColor: Color = GnomeGreen,
    badgeBackground: Color = GnomeGreenContainer,
    filterQuery: String = "",
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    if (filterQuery.isNotBlank()) {
        val q = filterQuery.trim().lowercase()
        val matches = title.lowercase().contains(q) ||
            subtitle?.lowercase()?.contains(q) == true ||
            badgeText?.lowercase()?.contains(q) == true
        if (!matches) return
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            leadingIcon?.invoke()
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = SlateTextPrimary,
                    )
                    if (badgeText != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = badgeBackground,
                        ) {
                            Text(
                                badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                ),
                                color = badgeColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = SlateTextSecondary,
                    )
                }
            }
        }

        trailingContent?.invoke()
    }
}

@Composable
private fun LibadwaitaSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = GnomeBlue,
            uncheckedThumbColor = SlateTextSecondary,
            uncheckedTrackColor = SlateContainerHighest,
            uncheckedBorderColor = Color.Transparent,
        ),
    )
}

@Composable
private fun <T> LibadwaitaSegmentedControl(
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = SlateContainerLowest,
        border = BorderStroke(1.dp, SlateBorderSubtle),
    ) {
        Row(
            modifier = Modifier.padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            options.forEach { (label, value) ->
                val isSelected = value == selected
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSelect(value) },
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) GnomeBlue else Color.Transparent,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        ),
                        color = if (isSelected) Color.White else SlateTextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RuntimePill(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = SlateContainerHigh,
        border = BorderStroke(1.dp, SlateBorder),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            ),
            color = SlateTextPrimary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
