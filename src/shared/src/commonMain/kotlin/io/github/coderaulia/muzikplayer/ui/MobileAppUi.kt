package io.github.coderaulia.muzikplayer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.coderaulia.muzikplayer.audio.DEFAULT_EQ_PRESETS
import io.github.coderaulia.muzikplayer.audio.SHARED_EQ_BANDS
import io.github.coderaulia.muzikplayer.platform.SharedAlbum
import io.github.coderaulia.muzikplayer.platform.SharedArtist
import io.github.coderaulia.muzikplayer.platform.SharedLyricsParser
import io.github.coderaulia.muzikplayer.platform.SharedPlaylist
import io.github.coderaulia.muzikplayer.platform.SharedRepeatMode
import io.github.coderaulia.muzikplayer.platform.SharedTrack
import kotlin.math.abs
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

/**
 * System-native Android & Compact Multiplatform UI for MuzikPlayer.
 * Combines Libadwaita Slate design tokens with Android Material 3 navigation,
 * edge-to-edge system bar insets, expandable mini-player, 10-band equalizer,
 * M3U playlist management, and synchronized LRC lyrics.
 */
@Composable
fun MuzikMobileApp(
    controller: MobileMuzikController,
    onRequestPermission: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val accentColor = remember(controller.currentTrack?.id, controller.dynamicCoverAccent) {
        if (!controller.dynamicCoverAccent || controller.currentTrack == null) {
            SharedSlatePalette.AccentPrimary
        } else {
            val hash = abs(controller.currentTrack!!.album.hashCode())
            when (hash % 4) {
                0 -> SharedSlatePalette.AccentPrimary
                1 -> SharedSlatePalette.AccentPurple
                2 -> SharedSlatePalette.AccentGreen
                else -> SharedSlatePalette.AccentAmber
            }
        }
    }

    val colorScheme = if (controller.darkThemeMode) {
        SharedSlatePalette.darkScheme(accentColor)
    } else {
        SharedSlatePalette.lightScheme(accentColor)
    }

    var trackMenuTarget by remember { mutableStateOf<SharedTrack?>(null) }

    MaterialTheme(colorScheme = colorScheme) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    MobileTopHeaderBar(
                        controller = controller,
                        accentColor = accentColor,
                    )
                },
                bottomBar = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        AnimatedVisibility(
                            visible = controller.currentTrack != null && controller.activeTab != MobileTab.HOME
                        ) {
                            controller.currentTrack?.let { current ->
                                MobileMiniPlayerBar(
                                    track = current,
                                    isPlaying = controller.isPlaying,
                                    isFavorite = current.id in controller.favoriteTrackIds,
                                    progress = if (current.duration.inWholeMilliseconds > 0) {
                                        (controller.currentPosition.inWholeMilliseconds.toFloat() /
                                            current.duration.inWholeMilliseconds.toFloat()).coerceIn(0f, 1f)
                                    } else {
                                        0f
                                    },
                                    accentColor = accentColor,
                                    onTapBar = { controller.activeTab = MobileTab.HOME },
                                    onToggleFavorite = { controller.toggleFavorite(current.id) },
                                    onTogglePlayPause = { controller.togglePlayPause() },
                                    onNext = { controller.skipNext() },
                                )
                            }
                        }
                        MobileBottomNavBar(
                            activeTab = controller.activeTab,
                            onSelectTab = { controller.activeTab = it },
                        )
                    }
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (controller.activeTab) {
                        MobileTab.HOME -> MobileNowPlayingScreen(
                            controller = controller,
                            accentColor = accentColor,
                        )
                        MobileTab.LIBRARY -> MobileLibraryScreen(
                            controller = controller,
                            accentColor = accentColor,
                            onRequestPermission = onRequestPermission,
                            onOpenTrackActions = { trackMenuTarget = it },
                        )
                        MobileTab.PLAYLISTS -> MobilePlaylistsScreen(
                            controller = controller,
                            accentColor = accentColor,
                        )
                        MobileTab.EQUALIZER -> MobileEqualizerScreen(
                            controller = controller,
                            accentColor = accentColor,
                        )
                        MobileTab.SETTINGS -> MobileSettingsScreen(
                            controller = controller,
                            onRequestPermission = onRequestPermission,
                        )
                    }
                }
            }

            trackMenuTarget?.let { target ->
                TrackActionsDialog(
                    track = target,
                    playlists = controller.playlists,
                    isFavorite = target.id in controller.favoriteTrackIds,
                    onDismiss = { trackMenuTarget = null },
                    onPlayNext = {
                        controller.playNextInQueue(target)
                        trackMenuTarget = null
                    },
                    onToggleFavorite = {
                        controller.toggleFavorite(target.id)
                        trackMenuTarget = null
                    },
                    onAddToPlaylist = { playlistId ->
                        controller.addTrackToPlaylist(playlistId, target.id)
                        trackMenuTarget = null
                    },
                )
            }
        }
    }
}

@Composable
private fun MobileTopHeaderBar(
    controller: MobileMuzikController,
    accentColor: Color,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column {
                    Text(
                        text = "MuzikPlayer",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    val track = controller.currentTrack
                    Text(
                        text = if (track != null) {
                            val sr = track.sampleRateHz?.let { "${it / 1000.0} kHz" } ?: "48.0 kHz"
                            val br = track.bitrateKbps?.let { "$it kbps" } ?: "Lossless"
                            "$sr • $br • ${if (controller.equalizerEnabled) "EQ Active" else "Bit-Perfect"}"
                        } else {
                            "${controller.allTracks.size} tracks in library"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { controller.requestLibraryRescan() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Rescan Library",
                    )
                }
                IconButton(onClick = { controller.activeTab = MobileTab.EQUALIZER }) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Equalizer",
                        tint = if (controller.equalizerEnabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MobileMiniPlayerBar(
    track: SharedTrack,
    isPlaying: Boolean,
    isFavorite: Boolean,
    progress: Float,
    accentColor: Color,
    onTapBar: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onTapBar),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(accentColor.copy(alpha = 0.85f), SharedSlatePalette.AccentPurple.copy(alpha = 0.65f))
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${track.artist} • ${track.album}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) SharedSlatePalette.AccentCoral else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onTogglePlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = accentColor,
                        modifier = Modifier.size(32.dp),
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.16f),
            )
        }
    }
}

@Composable
private fun MobileBottomNavBar(
    activeTab: MobileTab,
    onSelectTab: (MobileTab) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
    ) {
        NavigationBarItem(
            selected = activeTab == MobileTab.HOME,
            onClick = { onSelectTab(MobileTab.HOME) },
            icon = { Icon(Icons.Default.PlayCircleOutline, contentDescription = null) },
            label = { Text("Playing", maxLines = 1) },
        )
        NavigationBarItem(
            selected = activeTab == MobileTab.LIBRARY,
            onClick = { onSelectTab(MobileTab.LIBRARY) },
            icon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
            label = { Text("Library", maxLines = 1) },
        )
        NavigationBarItem(
            selected = activeTab == MobileTab.PLAYLISTS,
            onClick = { onSelectTab(MobileTab.PLAYLISTS) },
            icon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
            label = { Text("Playlists", maxLines = 1) },
        )
        NavigationBarItem(
            selected = activeTab == MobileTab.EQUALIZER,
            onClick = { onSelectTab(MobileTab.EQUALIZER) },
            icon = { Icon(Icons.Default.Equalizer, contentDescription = null) },
            label = { Text("EQ / DSP", maxLines = 1) },
        )
        NavigationBarItem(
            selected = activeTab == MobileTab.SETTINGS,
            onClick = { onSelectTab(MobileTab.SETTINGS) },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Settings", maxLines = 1) },
        )
    }
}

@Composable
private fun MobileNowPlayingScreen(
    controller: MobileMuzikController,
    accentColor: Color,
) {
    val track = controller.currentTrack
    if (track == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Album,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Nothing Playing",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "Select a song from your Library or start a shuffle session.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = {
                        if (controller.allTracks.isNotEmpty()) {
                            controller.shuffleAndPlay(controller.allTracks)
                        } else {
                            controller.activeTab = MobileTab.LIBRARY
                        }
                    },
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (controller.allTracks.isNotEmpty()) "Shuffle Library" else "Open Library")
                }
            }
        }
        return
    }

    val durationMs = track.duration.inWholeMilliseconds.coerceAtLeast(1L)
    val positionMs = controller.currentPosition.inWholeMilliseconds.coerceIn(0L, durationMs)
    val progress = (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Cover Art + Audio Telemetry Hero Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.45f),
                            SharedSlatePalette.BgElevated,
                            SharedSlatePalette.BgSurface,
                        )
                    )
                )
                .border(1.dp, SharedSlatePalette.BorderSubtle, RoundedCornerShape(24.dp))
                .padding(20.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            text = buildString {
                                append(track.mimeType?.substringAfterLast('/')?.uppercase() ?: "FLAC")
                                track.sampleRateHz?.let { append(" • ${it / 1000.0}kHz") }
                                track.bitrateKbps?.let { append(" • ${it}kbps") }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color.White,
                        )
                    }
                    IconButton(
                        onClick = { controller.toggleFavorite(track.id) },
                    ) {
                        val fav = track.id in controller.favoriteTrackIds
                        Icon(
                            imageVector = if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (fav) SharedSlatePalette.AccentCoral else Color.White,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(accentColor.copy(alpha = 0.28f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Album,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = buildString {
                                append(track.album)
                                track.year?.let { append(" ($it)") }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.68f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        // Interactive Waveform Visualizer + Seek Slider
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            WaveformSeekVisualizer(
                trackId = track.id,
                progress = progress,
                accentColor = accentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
            )
            Slider(
                value = progress,
                onValueChange = { fraction ->
                    val targetMs = (fraction * durationMs).toLong()
                    controller.seekTo(targetMs.milliseconds)
                },
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = SharedLyricsParser.formatDuration(controller.currentPosition),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "-${SharedLyricsParser.formatDuration((track.duration - controller.currentPosition).coerceAtLeast(0.milliseconds))}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Primary Transport Controls (Shuffle, Previous, Play/Pause, Next, Repeat)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { controller.toggleShuffle() }) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (controller.shuffleEnabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalIconButton(
                onClick = { controller.skipPrevious() },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
            }
            Button(
                onClick = { controller.togglePlayPause() },
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                contentPadding = PaddingValues(horizontal = 28.dp),
            ) {
                Icon(
                    imageVector = if (controller.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (controller.isPlaying) "Pause" else "Play",
                    tint = Color(0xFF0A192F),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (controller.isPlaying) "Pause" else "Play",
                    color = Color(0xFF0A192F),
                    fontWeight = FontWeight.Bold,
                )
            }
            FilledTonalIconButton(
                onClick = { controller.skipNext() },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next")
            }
            IconButton(onClick = { controller.cycleRepeatMode() }) {
                val icon = when (controller.repeatMode) {
                    SharedRepeatMode.ONE -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Repeat",
                    tint = if (controller.repeatMode != SharedRepeatMode.OFF) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Inspector Sub-Tabs: Synchronized Lyrics | Up Next Queue | Audio Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MobileInspectorSubTab.entries.forEach { tab ->
                val selected = controller.inspectorSubTab == tab
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { controller.inspectorSubTab = tab },
                    color = if (selected) accentColor.copy(alpha = 0.22f) else Color.Transparent,
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Inspector Content Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            when (controller.inspectorSubTab) {
                MobileInspectorSubTab.LYRICS -> {
                    val syncLines = controller.currentSynchronizedLyrics
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (syncLines.isNotEmpty()) "Synchronized Lyrics (LRC)" else "Track Lyrics",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { controller.lyricsOffsetMs -= 50 },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp),
                                ) {
                                    Text("-50ms", fontSize = 11.sp)
                                }
                                Text(
                                    text = "${controller.lyricsOffsetMs}ms",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                )
                                OutlinedButton(
                                    onClick = { controller.lyricsOffsetMs += 50 },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp),
                                ) {
                                    Text("+50ms", fontSize = 11.sp)
                                }
                            }
                        }

                        if (syncLines.isNotEmpty()) {
                            val activeIdx = syncLines.indexOfLast { it.start <= controller.currentPosition }.coerceAtLeast(0)
                            syncLines.forEachIndexed { idx, line ->
                                val isCurrent = idx == activeIdx
                                Text(
                                    text = line.text.ifEmpty { "♪" },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isCurrent) accentColor.copy(alpha = 0.16f) else Color.Transparent)
                                        .clickable { controller.seekTo(line.start) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = if (isCurrent) {
                                        MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                    } else {
                                        MaterialTheme.typography.bodyMedium
                                    },
                                    color = if (isCurrent) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else if (!track.lyricsRaw.isNullOrBlank()) {
                            Text(
                                text = track.lyricsRaw,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        } else {
                            Text(
                                text = "No embedded or .lrc sidecar lyrics found for \"${track.title}\".",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                MobileInspectorSubTab.QUEUE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Playback Queue (${controller.queue.size} tracks)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        )
                        controller.queue.forEachIndexed { index, qTrack ->
                            val isCurrent = index == controller.currentQueueIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isCurrent) accentColor.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable {
                                        controller.currentQueueIndex = index
                                        controller.seekTo(0.milliseconds)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    modifier = Modifier.width(26.dp),
                                    color = if (isCurrent) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = qTrack.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = qTrack.artist,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }
                                IconButton(onClick = { controller.removeFromQueue(index) }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove from queue",
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                MobileInspectorSubTab.DETAILS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Technical Audio Stream Telemetry",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        )
                        AudioInfoRow("Format / MIME", track.mimeType ?: "audio/flac")
                        AudioInfoRow("Sample Rate", "${track.sampleRateHz ?: 48000} Hz")
                        AudioInfoRow("Bitrate", "${track.bitrateKbps ?: 1411} kbps")
                        AudioInfoRow("Equalizer DSP", if (controller.equalizerEnabled) "Active (${controller.selectedPresetName})" else "Bypassed (Bit-Perfect)")
                        AudioInfoRow("Peak Protection", if (controller.peakProtection) "Enabled (-0.3 dBFS Ceiling)" else "Disabled")
                        AudioInfoRow("Source URI", track.uri.value)
                    }
                }
            }
        }
    }
}

@Composable
private fun WaveformSeekVisualizer(
    trackId: String,
    progress: Float,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val bars = remember(trackId) {
        val seed = trackId.hashCode()
        List(48) { idx ->
            val wave = abs(sin((idx + seed % 19) * 0.42f))
            (0.22f + wave * 0.78f).coerceIn(0.2f, 1f)
        }
    }
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
    Canvas(modifier = modifier) {
        val barCount = bars.size
        val spacing = 4.dp.toPx()
        val barWidth = ((size.width - spacing * (barCount - 1)) / barCount).coerceAtLeast(2f)
        for (i in 0 until barCount) {
            val fraction = i.toFloat() / barCount.toFloat()
            val barHeight = size.height * bars[i]
            val top = (size.height - barHeight) / 2f
            drawRoundRect(
                color = if (fraction <= progress) accentColor else inactiveColor,
                topLeft = Offset(i * (barWidth + spacing), top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}

@Composable
private fun AudioInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MobileLibraryScreen(
    controller: MobileMuzikController,
    accentColor: Color,
    onRequestPermission: () -> Unit,
    onOpenTrackActions: (SharedTrack) -> Unit,
) {
    var selectedAlbum by remember { mutableStateOf<SharedAlbum?>(null) }
    var selectedArtist by remember { mutableStateOf<SharedArtist?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (!controller.hasAudioPermission) {
            Surface(
                color = SharedSlatePalette.AccentAmber.copy(alpha = 0.16f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Storage Permission Required",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = "Grant Android MediaAudio permission to scan local music automatically.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Button(onClick = onRequestPermission) {
                        Text("Grant")
                    }
                }
            }
        }

        // Search input
        OutlinedTextField(
            value = controller.searchQuery,
            onValueChange = { controller.searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search songs, albums, artists…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (controller.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { controller.searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )

        // Section Chips (Songs, Albums, Artists, Favorites)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MobileLibrarySection.entries.forEach { section ->
                FilterChip(
                    selected = controller.librarySection == section,
                    onClick = {
                        selectedAlbum = null
                        selectedArtist = null
                        controller.librarySection = section
                    },
                    label = { Text(section.label) },
                )
            }
        }

        // Sort & Quick Play/Shuffle Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalButton(
                    onClick = {
                        val list = controller.filteredTracks
                        if (list.isNotEmpty()) controller.playTrackFromList(list.first(), list)
                    },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Play All")
                }
                OutlinedButton(
                    onClick = { controller.shuffleAndPlay(controller.filteredTracks) },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Shuffle")
                }
            }

            TextButton(
                onClick = {
                    val entries = MobileSortField.entries
                    val next = entries[(entries.indexOf(controller.sortField) + 1) % entries.size]
                    controller.sortField = next
                }
            ) {
                Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(controller.sortField.label)
            }
        }

        when {
            selectedAlbum != null -> {
                val album = selectedAlbum!!
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(onClick = { selectedAlbum = null }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to albums")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(album.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("${album.artist} • ${album.tracks.size} tracks", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(onClick = { controller.playTrackFromList(album.tracks.first(), album.tracks) }) {
                        Text("Play Album")
                    }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(album.tracks, key = { it.id }) { track ->
                        MobileTrackRow(
                            track = track,
                            isCurrent = controller.currentTrack?.id == track.id,
                            isFavorite = track.id in controller.favoriteTrackIds,
                            showThumbnail = controller.showTrackThumbnails,
                            accentColor = accentColor,
                            onClick = { controller.playTrackFromList(track, album.tracks) },
                            onOpenMenu = { onOpenTrackActions(track) },
                        )
                    }
                }
            }

            selectedArtist != null -> {
                val artist = selectedArtist!!
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(onClick = { selectedArtist = null }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to artists")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(artist.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("${artist.albums.size} albums • ${artist.tracks.size} songs", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(onClick = { controller.playTrackFromList(artist.tracks.first(), artist.tracks) }) {
                        Text("Play Artist")
                    }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(artist.tracks, key = { it.id }) { track ->
                        MobileTrackRow(
                            track = track,
                            isCurrent = controller.currentTrack?.id == track.id,
                            isFavorite = track.id in controller.favoriteTrackIds,
                            showThumbnail = controller.showTrackThumbnails,
                            accentColor = accentColor,
                            onClick = { controller.playTrackFromList(track, artist.tracks) },
                            onOpenMenu = { onOpenTrackActions(track) },
                        )
                    }
                }
            }

            controller.librarySection == MobileLibrarySection.ALBUMS -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(controller.albums, key = { "${it.name}_${it.artist}" }) { album ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { selectedAlbum = album },
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(accentColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Default.Album, contentDescription = null, tint = accentColor)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(album.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                                    Text(
                                        text = "${album.artist} • ${album.tracks.size} songs • ${SharedLyricsParser.formatDuration(album.totalDuration)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { controller.playTrackFromList(album.tracks.first(), album.tracks) }) {
                                    Icon(Icons.Default.PlayCircleOutline, contentDescription = "Play Album", tint = accentColor)
                                }
                            }
                        }
                    }
                }
            }

            controller.librarySection == MobileLibrarySection.ARTISTS -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(controller.artists, key = { it.name }) { artist ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { selectedArtist = artist },
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = accentColor)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(artist.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                                    Text(
                                        text = "${artist.albums.size} albums • ${artist.tracks.size} songs",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { controller.playTrackFromList(artist.tracks.first(), artist.tracks) }) {
                                    Icon(Icons.Default.PlayCircleOutline, contentDescription = "Play Artist", tint = accentColor)
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                val tracks = controller.filteredTracks
                if (tracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.LibraryMusic,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text("No matching tracks", style = MaterialTheme.typography.titleMedium)
                            OutlinedButton(onClick = { controller.requestAddLibraryFolder() }) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Add Music Folder")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(tracks, key = { it.id }) { track ->
                            MobileTrackRow(
                                track = track,
                                isCurrent = controller.currentTrack?.id == track.id,
                                isFavorite = track.id in controller.favoriteTrackIds,
                                showThumbnail = controller.showTrackThumbnails,
                                accentColor = accentColor,
                                onClick = { controller.playTrackFromList(track, tracks) },
                                onOpenMenu = { onOpenTrackActions(track) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MobileTrackRow(
    track: SharedTrack,
    isCurrent: Boolean,
    isFavorite: Boolean,
    showThumbnail: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (isCurrent) accentColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showThumbnail) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCurrent) accentColor else MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isCurrent) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = if (isCurrent) Color(0xFF0A192F) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isCurrent) accentColor else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${track.artist} • ${track.album}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (isFavorite) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = SharedSlatePalette.AccentCoral,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(16.dp),
                )
            }

            Text(
                text = SharedLyricsParser.formatDuration(track.duration),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            IconButton(onClick = onOpenMenu) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Track options",
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun MobilePlaylistsScreen(
    controller: MobileMuzikController,
    accentColor: Color,
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var createError by remember { mutableStateOf<String?>(null) }
    var activePlaylistId by remember { mutableStateOf<String?>(null) }
    var showAddTracksDialogFor by remember { mutableStateOf<SharedPlaylist?>(null) }

    val selectedPlaylist = controller.playlists.firstOrNull { it.id == activePlaylistId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "M3U Playlists",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "${controller.playlists.size} playlists synced with library",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = {
                newPlaylistName = ""
                createError = null
                showCreateDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("New .m3u")
            }
        }

        if (selectedPlaylist != null) {
            val playlistTracks = selectedPlaylist.trackIds.mapNotNull { id ->
                controller.allTracks.firstOrNull { it.id == id }
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { activePlaylistId = null }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                            Column {
                                Text(
                                    text = selectedPlaylist.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                )
                                Text(
                                    text = "${playlistTracks.size} tracks • ${selectedPlaylist.name}.m3u",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { showAddTracksDialogFor = selectedPlaylist }) {
                                Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Songs")
                            }
                            if (playlistTracks.isNotEmpty()) {
                                Button(onClick = { controller.playTrackFromList(playlistTracks.first(), playlistTracks) }) {
                                    Text("Play")
                                }
                            }
                        }
                    }

                    if (playlistTracks.isEmpty()) {
                        Text(
                            text = "This playlist is empty. Tap \"Add Songs\" to append tracks to ${selectedPlaylist.name}.m3u.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(playlistTracks, key = { it.id }) { track ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { controller.playTrackFromList(track, playlistTracks) }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(track.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                        Text("${track.artist} • ${track.album}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { controller.removeTrackFromPlaylist(selectedPlaylist.id, track.id) }) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove from playlist")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(controller.playlists, key = { it.id }) { playlist ->
                    val playlistTracks = playlist.trackIds.mapNotNull { id ->
                        controller.allTracks.firstOrNull { it.id == id }
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { activePlaylistId = playlist.id },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(accentColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = accentColor)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(playlist.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                                Text(
                                    text = "${playlistTracks.size} songs • ${playlist.name}.m3u",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { showAddTracksDialogFor = playlist }) {
                                Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add songs")
                            }
                            if (playlistTracks.isNotEmpty()) {
                                IconButton(onClick = { controller.playTrackFromList(playlistTracks.first(), playlistTracks) }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play playlist", tint = accentColor)
                                }
                            }
                            IconButton(onClick = { controller.deletePlaylist(playlist.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete playlist")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create M3U Playlist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = {
                            newPlaylistName = it
                            createError = null
                        },
                        label = { Text("Playlist Name") },
                        singleLine = true,
                    )
                    createError?.let { err ->
                        Text(err, color = SharedSlatePalette.AccentCoral, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    controller.createPlaylist(newPlaylistName)
                        .onSuccess {
                            showCreateDialog = false
                            activePlaylistId = it.id
                        }
                        .onFailure { createError = it.message }
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    showAddTracksDialogFor?.let { targetPlaylist ->
        AlertDialog(
            onDismissRequest = { showAddTracksDialogFor = null },
            title = { Text("Add Songs to ${targetPlaylist.name}") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(controller.allTracks, key = { it.id }) { track ->
                        val alreadyIn = track.id in targetPlaylist.trackIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !alreadyIn) {
                                    controller.addTrackToPlaylist(targetPlaylist.id, track.id)
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.title, style = MaterialTheme.typography.bodyMedium)
                                Text(track.artist, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (alreadyIn) {
                                Icon(Icons.Default.Check, contentDescription = "Added", tint = SharedSlatePalette.AccentGreen)
                            } else {
                                Icon(Icons.Default.Add, contentDescription = "Add")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddTracksDialogFor = null }) {
                    Text("Done")
                }
            },
        )
    }
}

@Composable
private fun MobileEqualizerScreen(
    controller: MobileMuzikController,
    accentColor: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "10-Band Parametric Equalizer",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = "Transposed Direct Form II Biquad DSP (-12 dB to +12 dB)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = controller.equalizerEnabled,
                    onCheckedChange = { controller.toggleEqualizer(it) },
                )
            }
        }

        // Preset selector chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DEFAULT_EQ_PRESETS.forEach { preset ->
                FilterChip(
                    selected = controller.selectedPresetName == preset.name,
                    onClick = { controller.applyEqualizerPreset(preset.name) },
                    label = { Text(preset.name) },
                )
            }
        }

        // 10-band sliders
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Frequency Bands",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                    TextButton(onClick = { controller.applyEqualizerPreset("Flat") }) {
                        Text("Reset (0 dB)")
                    }
                }

                SHARED_EQ_BANDS.forEachIndexed { index, freqHz ->
                    val gain = controller.equalizerGains.getOrElse(index) { 0f }
                    val freqLabel = if (freqHz >= 1000) "${freqHz / 1000}kHz" else "${freqHz}Hz"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = freqLabel,
                            modifier = Modifier.width(48.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        )
                        Slider(
                            value = gain,
                            onValueChange = { controller.setEqualizerBand(index, it) },
                            valueRange = -12f..12f,
                            enabled = controller.equalizerEnabled,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = accentColor,
                                activeTrackColor = accentColor,
                            ),
                        )
                        Text(
                            text = "${if (gain >= 0f) "+" else ""}${((gain * 10).toInt() / 10f)} dB",
                            modifier = Modifier.width(58.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = if (gain != 0f && controller.equalizerEnabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Preamp / Output Volume & Mastering Protection
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Output Gain & Mastering Protection",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                    Slider(
                        value = controller.volume,
                        onValueChange = { controller.updateVolume(it) },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${(controller.volume * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    )
                }
                SettingsToggleRow(
                    title = "Peak Limiter Protection",
                    subtitle = "Prevent digital clipping when boosting EQ bands",
                    checked = controller.peakProtection,
                    onCheckedChange = { controller.peakProtection = it },
                )
                SettingsToggleRow(
                    title = "ReplayGain Normalization",
                    subtitle = "Apply album/track loudness metadata (-14 LUFS target)",
                    checked = controller.replayGainEnabled,
                    onCheckedChange = { controller.replayGainEnabled = it },
                )
            }
        }
    }
}

@Composable
private fun MobileSettingsScreen(
    controller: MobileMuzikController,
    onRequestPermission: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        )

        // Category 1: Library & Storage
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Library & Storage (MediaStore + SAF)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )
                controller.libraryFolders.forEach { folder ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                text = folder,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { controller.requestAddLibraryFolder() }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Folder (SAF)")
                    }
                    OutlinedButton(onClick = { controller.requestLibraryRescan() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Rescan")
                    }
                }
                if (!controller.hasAudioPermission) {
                    Button(onClick = onRequestPermission) {
                        Text("Grant Android Audio Permission")
                    }
                }
            }
        }

        // Category 2: Appearance
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Appearance (Libadwaita Slate)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )
                SettingsToggleRow(
                    title = "Dark Slate Theme",
                    subtitle = "Use Libadwaita Slate dark surfaces (#1E1E24)",
                    checked = controller.darkThemeMode,
                    onCheckedChange = { controller.darkThemeMode = it },
                )
                SettingsToggleRow(
                    title = "Dynamic Cover Art Accent",
                    subtitle = "Adapt primary accent color to current album palette",
                    checked = controller.dynamicCoverAccent,
                    onCheckedChange = { controller.dynamicCoverAccent = it },
                )
                SettingsToggleRow(
                    title = "Show Track Thumbnails",
                    subtitle = "Display artwork badges in library song lists",
                    checked = controller.showTrackThumbnails,
                    onCheckedChange = { controller.showTrackThumbnails = it },
                )
            }
        }

        // Category 3: Audio & Android System Integration
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Playback & Android System Integration",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )
                SettingsToggleRow(
                    title = "Gapless Playback",
                    subtitle = "Pre-buffer next queue item for zero-gap transitions",
                    checked = controller.gaplessPlayback,
                    onCheckedChange = { controller.gaplessPlayback = it },
                )
                SettingsToggleRow(
                    title = "Android MediaSession & Lockscreen Controls",
                    subtitle = "Publish metadata and transport controls to Android system notification & Bluetooth",
                    checked = controller.mediaSessionEnabled,
                    onCheckedChange = { controller.mediaSessionEnabled = it },
                )
            }
        }

        // Category 4: About
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "About MuzikPlayer",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "Version 1.5.6 • Kotlin Multiplatform (Linux • Windows • Android)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Package: io.github.coderaulia.muzikplayer",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun TrackActionsDialog(
    track: SharedTrack,
    playlists: List<SharedPlaylist>,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "${track.artist} • ${track.album}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                TextButton(onClick = onPlayNext, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.QueuePlayNext, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Play Next in Queue", modifier = Modifier.weight(1f))
                }
                TextButton(onClick = onToggleFavorite, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (isFavorite) "Remove from Favorites" else "Add to Favorites", modifier = Modifier.weight(1f))
                }
                if (playlists.isNotEmpty()) {
                    Text(
                        text = "Add to M3U Playlist:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    playlists.forEach { pl ->
                        TextButton(
                            onClick = { onAddToPlaylist(pl.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(pl.name, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}
