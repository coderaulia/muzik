package io.github.coderaulia.muzikplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import io.github.mmarco94.klibportal.portals.openFile
import io.github.coderaulia.muzikplayer.generated.resources.*
import io.github.coderaulia.muzikplayer.utils.GLOBAL_CONNECTION
import io.github.coderaulia.muzikplayer.utils.Preferences
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import java.text.NumberFormat
import kotlin.io.path.isDirectory
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

@Composable
fun AppSettingsWindow(close: () -> Unit) {
    val fontScale by Preferences.fontScale.state
    val useSystemDecorations by Preferences.useSystemDecorations.state
    var maintainOnTop by remember { mutableStateOf(0) }
    val cs = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf(SettingsCategory.LIBRARY) }

    Window(
        onCloseRequest = close,
        title = stringResource(Res.string.settings),
        state = rememberWindowState(
            size = DpSize(560.dp, 560.dp),
        ),
        alwaysOnTop = maintainOnTop > 0,
        onPreviewKeyEvent = { event ->
            if (event.type == KeyEventType.KeyDown) {
                when (event.key) {
                    Key.Escape -> {
                        close()
                        true
                    }

                    else -> false
                }
            } else false
        },
    ) {
        fun setUseSystemDecorations(value: Boolean) {
            // Changing this, to avoid the main window going on top of us
            val token = Random.nextInt(1..Int.MAX_VALUE)
            maintainOnTop = token
            cs.launch {
                delay(1.seconds)
                if (maintainOnTop == token) {
                    maintainOnTop = 0
                }
            }
            Preferences.useSystemDecorations.set(value)
        }

        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.width(156.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("SETTINGS", Modifier.padding(8.dp), style = MaterialTheme.typography.labelSmall)
                    SettingsCategory.entries.forEach { category ->
                        Text(category.label, Modifier.fillMaxWidth().clickable { selectedCategory = category }.padding(horizontal = 10.dp, vertical = 9.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    Text(selectedCategory.label, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(12.dp))
                    when (selectedCategory) {
                        SettingsCategory.LIBRARY -> LibraryDirectorySetting(onSelectingFolder = { maintainOnTop = 0 })
                        SettingsCategory.AUDIO -> StatusSetting(Icons.Default.GraphicEq, "Audio engine", "Java Sound / bundled decoder is active")
                        SettingsCategory.APPEARANCE -> {
                            ThemeSetting()
                            PreferenceItem(stringResource(Res.string.use_system_decorations), stringResource(if (useSystemDecorations) Res.string.yes else Res.string.no), Modifier.clickable { setUseSystemDecorations(!useSystemDecorations) }, { Icon(Icons.Default.Tab, null) }, { Checkbox(useSystemDecorations, ::setUseSystemDecorations) })
                            PreferenceItem(stringResource(Res.string.font_size), NumberFormat.getPercentInstance().format(fontScale), leadingContent = { Icon(Icons.Default.ZoomIn, null) }) {
                                Slider(fontScale, { Preferences.fontScale.set((it * 10).roundToInt() / 10f) }, steps = 14, valueRange = 0.5f..2f)
                            }
                        }
                        SettingsCategory.INTEGRATION -> StatusSetting(Icons.Default.Extension, "Desktop integration", "MPRIS controls are available when the session bus is running")
                        SettingsCategory.ABOUT -> StatusSetting(Icons.Default.Info, "MuzikPlayer", "Local music player for Linux\nVersion 1.5.3")
                    }
                }
            }
        }
    }
}

private enum class SettingsCategory(val label: String) { LIBRARY("Library"), AUDIO("Audio"), APPEARANCE("Appearance"), INTEGRATION("Integration"), ABOUT("About") }

@Composable
private fun StatusSetting(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    PreferenceItem(title, value, leadingContent = { Icon(icon, null) })
}

@Composable
private fun ThemeSetting() {
    val theme by Preferences.theme.state
    var open by remember { mutableStateOf(false) }

    PreferenceItem(
        title = stringResource(Res.string.theme),
        value = stringResource(theme.nameRes),
        modifier = Modifier.clickable { open = true },
        leadingContent = { Icon(Icons.Default.DarkMode, contentDescription = null) },
        tailingContent = {
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                content = {
                    for (theme in MuzikTheme.UserPreference.entries) {
                        DropdownMenuItem(
                            text = { Text(stringResource(theme.nameRes)) },
                            onClick = {
                                Preferences.theme.set(theme)
                                open = false
                            },
                        )
                    }
                }
            )
        }
    )
}

@Composable
private fun LibraryDirectorySetting(onSelectingFolder: () -> Unit = {}) {
    val cs = rememberCoroutineScope()
    val chooseLibraryStr = stringResource(Res.string.action_choose_library)
    fun openFolderSelector() {
        onSelectingFolder()
        cs.launch {
            try {
                val file = openFile(
                    checkNotNull(GLOBAL_CONNECTION.await()),
                    title = chooseLibraryStr,
                    directory = true,
                    multiple = false,
                ).singleOrNull()
                logger.info { "Selected file $file" }
                if (file != null && file.isDirectory()) {
                    Preferences.libraryFolder.set(file)
                }
            } catch (e: Exception) {
                logger.error(e) { "Error while picking file" }
            }
        }
    }

    val libraryFolder by Preferences.libraryFolder.state
    PreferenceItem(
        title = stringResource(Res.string.music_library_folder),
        value = libraryFolder.toString(),
        modifier = Modifier.clickable { openFolderSelector() },
        leadingContent = { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
    )
}

@Composable
private fun PreferenceItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    leadingContent: @Composable () -> Unit = {},
    tailingContent: @Composable () -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            leadingContent()
            Column(Modifier.weight(1f)) {
                Scaled { // Scaling individual components, rather than whole window, so the Slider is unaffected
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(value, style = MaterialTheme.typography.bodyMedium)
                }
            }
            tailingContent()
        }
        content()
    }
}
