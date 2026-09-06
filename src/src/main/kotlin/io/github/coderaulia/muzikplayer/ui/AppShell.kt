package io.github.coderaulia.muzikplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.coderaulia.muzikplayer.audio.Position
import io.github.coderaulia.muzikplayer.data.Song
import io.github.coderaulia.muzikplayer.generated.resources.*
import io.github.coderaulia.muzikplayer.playerController
import io.github.coderaulia.muzikplayer.utils.format
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO

private val shellSurface = Color(0xFF2A2A2A)
private val sidebarSurface = Color(0xFF1B1C1C)
private val shellCanvas = Color(0xFF131313)

@Composable
fun MuzikPlayerShell(
    selectedPanel: Panel,
    selectPanel: (Panel) -> Unit,
    openSettings: () -> Unit,
    closeApp: () -> Unit,
    openPlaylists: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(shellCanvas)) {
        MuzikHeader(closeApp = closeApp)
        Row(Modifier.weight(1f).fillMaxWidth()) {
            MuzikSidebar(
                selectedPanel = selectedPanel,
                selectPanel = selectPanel,
                openSettings = openSettings,
                openPlaylists = openPlaylists,
            )
            Box(Modifier.weight(1f).fillMaxHeight()) { content() }
        }
        MuzikTransportBar()
    }
}

@Composable
private fun MuzikHeader(closeApp: () -> Unit) {
    WindowDraggableArea {
        Row(
            Modifier.fillMaxWidth().height(48.dp).background(shellSurface).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(30.dp),
                shape = RoundedCornerShape(7.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("M", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                "MuzikPlayer",
                modifier = Modifier.padding(start = 9.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(20.dp))
            HeaderTab("Albums")
            HeaderTab("Artists")
            HeaderTab("Tracks")
            HeaderTab("Genres")
            Spacer(Modifier.weight(1f))
            Surface(
                Modifier.widthIn(min = 180.dp, max = 360.dp).height(32.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0E0E0E),
            ) {
                Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Search local library...",
                        Modifier.padding(start = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            IconButton(onClick = closeApp, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Close, stringResource(Res.string.action_close_app), Modifier.size(17.dp))
            }
        }
    }
}

@Composable
private fun HeaderTab(label: String) {
    Text(
        label,
        Modifier.clip(RoundedCornerShape(6.dp)).clickable {}.padding(horizontal = 10.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun MuzikSidebar(
    selectedPanel: Panel,
    selectPanel: (Panel) -> Unit,
    openSettings: () -> Unit,
    openPlaylists: () -> Unit,
) {
    Column(
        Modifier.width(240.dp).fillMaxHeight().background(sidebarSurface).padding(horizontal = 10.dp, vertical = 16.dp),
    ) {
        Text(
            "LIBRARY",
            Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        SidebarItem("Home", Icons.Default.Home, selectedPanel == Panel.PLAYER) { selectPanel(Panel.PLAYER) }
        SidebarItem("Library", Icons.Default.LibraryMusic, selectedPanel == Panel.LIBRARY) { selectPanel(Panel.LIBRARY) }
        SidebarItem("Queue", Icons.AutoMirrored.Filled.QueueMusic, selectedPanel == Panel.QUEUE) { selectPanel(Panel.QUEUE) }
        SidebarItem("Playlists", Icons.Default.PlaylistPlay, false, openPlaylists)
        Spacer(Modifier.weight(1f))
        SidebarItem("Settings", Icons.Default.Settings, false, openSettings)
        Surface(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color(0x990E0E0E),
        ) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GraphicEq, null, Modifier.size(15.dp), tint = Color(0xFF48E087))
                Text("Local audio engine", Modifier.padding(start = 7.dp), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF48E087)))
            }
        }
    }
}

@Composable
private fun SidebarItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val foreground = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(background).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = foreground)
        Text(label, Modifier.padding(start = 12.dp), color = foreground, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun MuzikTransportBar() {
    val player = playerController.current
    val queue = player.queue
    val song = queue?.currentSong
    val scope = rememberCoroutineScope()
    var position by remember { mutableStateOf(ZERO) }
    if (song != null) {
        player.ObservePosition { position = it }
    }
    Surface(Modifier.fillMaxWidth().height(72.dp), color = shellSurface, tonalElevation = 2.dp) {
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(44.dp), shape = RoundedCornerShape(5.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Album, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Column(Modifier.widthIn(min = 150.dp, max = 240.dp).padding(start = 12.dp)) {
                Text(song?.title ?: "No Track Playing", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song?.album?.artist?.name ?: "MuzikPlayer Desktop Engine", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { scope.launch { player.transformQueue { it?.previous() to Position.Beginning }; player.play() } }) {
                Icon(Icons.Default.SkipPrevious, stringResource(Res.string.action_go_to_previous_song))
            }
            FilledIconButton(
                onClick = { scope.launch { if (player.pause) player.play() else player.pause() } },
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) { Icon(if (player.pause) Icons.Default.PlayArrow else Icons.Default.Pause, stringResource(Res.string.action_play)) }
            IconButton(onClick = { scope.launch { player.transformQueue { it?.next() to Position.Beginning }; player.play() } }) {
                Icon(Icons.Default.SkipNext, stringResource(Res.string.action_go_to_next_song))
            }
            Spacer(Modifier.width(18.dp))
            if (song != null) {
                Text(position.format(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.widthIn(min = 120.dp, max = 360.dp).padding(horizontal = 8.dp)) {
                    LinearProgressIndicator(
                        progress = { (position / song.length).toFloat().coerceIn(0f, 1f) },
                        Modifier.fillMaxWidth().height(4.dp),
                    )
                }
                Text(song.length.format(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Spacer(Modifier.width(240.dp))
            }
            Spacer(Modifier.width(18.dp))
            VolumeSlider(player)
        }
    }
}
