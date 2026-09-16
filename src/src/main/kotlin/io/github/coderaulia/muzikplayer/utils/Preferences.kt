package io.github.coderaulia.muzikplayer.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import io.github.coderaulia.muzikplayer.ui.MuzikTheme
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.awt.Insets
import java.nio.file.Path
import java.util.prefs.Preferences
import kotlin.io.path.pathString

private val logger = KotlinLogging.logger {}

object Preferences {

    private val prefs: Preferences = Preferences.userRoot().node("/io/github/music-player")

    val libraryFolders = PreferenceContainer(
        read = { prefs ->
            val raw = prefs.get("library_folders", null)
            if (raw.isNullOrBlank()) {
                val single = prefs.get("library_folder", null)
                val defaultPath = Path.of(single ?: (System.getProperty("user.home") + "/Music"))
                listOf(defaultPath)
            } else {
                raw.split(java.io.File.pathSeparator)
                    .filter { it.isNotBlank() }
                    .map { Path.of(it) }
                    .distinct()
                    .ifEmpty { listOf(Path.of(System.getProperty("user.home") + "/Music")) }
            }
        },
        write = { prefs, values ->
            val distinct = values.distinct()
            val serialized = distinct.joinToString(java.io.File.pathSeparator) { it.pathString }
            prefs.put("library_folders", serialized)
            distinct.firstOrNull()?.let { prefs.put("library_folder", it.pathString) }
        }
    )

    val libraryFolder = PreferenceContainer(
        read = { prefs ->
            libraryFolders.get().firstOrNull() ?: Path.of(prefs.get("library_folder", System.getProperty("user.home") + "/Music"))
        },
        write = { prefs, value ->
            prefs.put("library_folder", value.pathString)
            val current = libraryFolders.get().toMutableList()
            if (!current.contains(value)) {
                current.add(0, value)
            } else {
                current.remove(value)
                current.add(0, value)
            }
            libraryFolders.set(current)
        }
    )

    fun addLibraryFolders(newPaths: List<Path>) {
        val current = libraryFolders.get().toMutableList()
        var changed = false
        for (path in newPaths) {
            if (!current.contains(path)) {
                current.add(path)
                changed = true
            }
        }
        if (changed) {
            libraryFolders.set(current)
            libraryFolder.set(current.first())
            triggerLibraryRescan()
        }
    }

    fun removeLibraryFolder(path: Path) {
        val current = libraryFolders.get().toMutableList()
        if (current.remove(path)) {
            if (current.isEmpty()) {
                current.add(Path.of(System.getProperty("user.home") + "/Music"))
            }
            libraryFolders.set(current)
            libraryFolder.set(current.first())
            triggerLibraryRescan()
        }
    }

    val useSystemDecorations = PreferenceContainer(
        read = { prefs -> prefs.getBoolean("system_decorations", false) },
        write = { prefs, value ->
            prefs.putBoolean("system_decorations", value)
        }
    )
    val fontScale = PreferenceContainer(
        read = { prefs -> prefs.getFloat("font_scale", 1f).coerceAtLeast(0.1f) },
        write = { prefs, value ->
            prefs.putFloat("font_scale", value)
        }
    )
    val theme = PreferenceContainer(
        read = { prefs ->
            val preference = prefs.get("theme", "")
            MuzikTheme.UserPreference.entries.singleOrNull {
                it.name.equals(preference, ignoreCase = true)
            } ?: MuzikTheme.UserPreference.AUTO
        },
        write = { prefs, value ->
            prefs.put("theme", value.name)
        }
    )
    val readOnlyMode = PreferenceContainer(
        read = { prefs -> prefs.getBoolean("read_only_mode", true) },
        write = { prefs, value -> prefs.putBoolean("read_only_mode", value) }
    )
    val watchFilesystem = PreferenceContainer(
        read = { prefs -> prefs.getBoolean("watch_filesystem", true) },
        write = { prefs, value -> prefs.putBoolean("watch_filesystem", value) }
    )
    val bitPerfect = PreferenceContainer(
        read = { prefs -> prefs.getBoolean("bit_perfect", true) },
        write = { prefs, value -> prefs.putBoolean("bit_perfect", value) }
    )
    val bufferLatency = PreferenceContainer(
        read = { prefs -> prefs.get("buffer_latency", "512 (5.3ms)") },
        write = { prefs, value -> prefs.put("buffer_latency", value) }
    )
    val replayGain = PreferenceContainer(
        read = { prefs -> prefs.get("replay_gain", "Album Gain (-1.4 dB target)") },
        write = { prefs, value -> prefs.put("replay_gain", value) }
    )
    val peakProtection = PreferenceContainer(
        read = { prefs -> prefs.getBoolean("peak_protection", true) },
        write = { prefs, value -> prefs.putBoolean("peak_protection", value) }
    )
    val audioTelemetry = PreferenceContainer(
        read = { prefs -> prefs.getBoolean("audio_telemetry", true) },
        write = { prefs, value -> prefs.putBoolean("audio_telemetry", value) }
    )
    val showTableThumbnails = PreferenceContainer(
        read = { prefs -> prefs.getBoolean("show_table_thumbnails", true) },
        write = { prefs, value -> prefs.putBoolean("show_table_thumbnails", value) }
    )
    val mprisEnabled = PreferenceContainer(
        read = { prefs -> prefs.getBoolean("mpris_enabled", true) },
        write = { prefs, value -> prefs.putBoolean("mpris_enabled", value) }
    )
    val discordRpc = PreferenceContainer(
        read = { prefs -> prefs.getBoolean("discord_rpc", false) },
        write = { prefs, value -> prefs.putBoolean("discord_rpc", value) }
    )
    val listenBrainzToken = PreferenceContainer(
        read = { prefs -> prefs.get("listenbrainz_token", "") },
        write = { prefs, value -> prefs.put("listenbrainz_token", value) }
    )

    val libraryRescanToken = MutableStateFlow(0L)
    fun triggerLibraryRescan() {
        libraryRescanToken.value = System.currentTimeMillis()
    }

    @Composable
    fun mainWindowState(): WindowState {
        // 980x680 for screenshot
        val positionX = prefs.get("main_window_position_x", null)?.toFloatOrNull()
        val positionY = prefs.get("main_window_position_y", null)?.toFloatOrNull()
        val state = rememberWindowState(
            size = DpSize(
                prefs.getFloat("main_window_width", 980f).dp,
                prefs.getFloat("main_window_height", 680f).dp,
            ),
            placement = WindowPlacement.valueOf(prefs.get("main_window_placement", WindowPlacement.Floating.name)),
            position = if (positionX != null && positionY != null) {
                WindowPosition.Absolute(positionX.dp, positionY.dp)
            } else {
                WindowPosition.PlatformDefault
            }
        )
        return state
    }

    suspend fun save(state: WindowState, insets: Insets, density: Density) {
        val size = state.size
        val position = state.position
        val placement = state.placement
        logger.trace { "Saving window state: size = ${size}; position = $position; placement = $placement; insets = $insets" }
        withContext(Dispatchers.Default) {
            if (size.isSpecified && placement == WindowPlacement.Floating) {
                prefs.putFloat("main_window_width", size.width.value - (insets.left + insets.right) / density.density)
                prefs.putFloat("main_window_height", size.height.value - (insets.top + insets.bottom) / density.density)
            }
            prefs.put("main_window_placement", placement.name)
            if (position.isSpecified) {
                prefs.putFloat("main_window_position_x", position.x.value)
                prefs.putFloat("main_window_position_y", position.y.value)
            }
            prefs.flush()
        }
    }

    class PreferenceContainer<T>(
        val read: (Preferences) -> T,
        val write: (Preferences, T) -> Unit,
    ) {
        private class Box<T>(val wrapped: T)

        private val currentValueBoxed = MutableStateFlow(Box(read(prefs)))
        private val currentValue = currentValueBoxed.map { it.wrapped }

        @Suppress("OPT_IN_USAGE")
        val flow: Flow<T>
            get() = currentValue

        val state: State<T>
            @OptIn(ExperimentalCoroutinesApi::class)
            @Composable
            get() {
                return currentValue.collectAsState(initial = currentValueBoxed.value.wrapped)
            }

        fun sendSignal() {
            currentValueBoxed.value = Box(currentValueBoxed.value.wrapped)
        }

        fun get(): T {
            return currentValueBoxed.value.wrapped
        }

        fun set(value: T) {
            write(prefs, value)
            prefs.flush()
            currentValueBoxed.value = Box(value)
        }
    }
}
