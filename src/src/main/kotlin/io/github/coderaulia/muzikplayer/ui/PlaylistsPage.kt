package io.github.coderaulia.muzikplayer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.coderaulia.muzikplayer.audio.Position
import io.github.coderaulia.muzikplayer.data.*
import io.github.coderaulia.muzikplayer.playerController
import io.github.coderaulia.muzikplayer.utils.format
import io.github.coderaulia.muzikplayer.utils.Preferences
import kotlinx.coroutines.launch
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.extension
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO

private val slateCanvas = Color(0xFF131313)
private val surfaceContainerLowest = Color(0xFF0E0E0E)
private val surfaceContainerLow = Color(0xFF1B1C1C)
private val surfaceContainer = Color(0xFF1F2020)
private val surfaceContainerHigh = Color(0xFF2A2A2A)
private val surfaceContainerHighest = Color(0xFF353535)
private val surfaceBright = Color(0xFF393939)
private val textOnSurface = Color(0xFFE4E2E1)
private val textOnSurfaceVariant = Color(0xFFC1C6D4)
private val outlineVariant = Color(0xFF414752)
private val tertiaryGreen = Color(0xFF48E087)
private val primaryBlue = Color(0xFF4691F2)
private val secondaryPurple = Color(0xFFCABEFF)
private val secondaryContainer = Color(0xFF4A16D1)

@Composable
fun PlaylistsPage(
    library: Library?,
    modifier: Modifier = Modifier,
) {
    val player = playerController.current
    val scope = rememberCoroutineScope()

    val playlists = library?.playlists.orEmpty()
    var selectedPlaylistKey by remember(playlists) {
        mutableStateOf(playlists.firstOrNull()?.uniqueKey)
    }

    val activePlaylist = remember(playlists, selectedPlaylistKey) {
        playlists.firstOrNull { it.uniqueKey == selectedPlaylistKey } ?: playlists.firstOrNull()
    }

    var isDrawerOpen by remember { mutableStateOf(false) }
    var selectedSongsToAdd by remember { mutableStateOf(setOf<SongKey>()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("All") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var playlistError by remember { mutableStateOf<String?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameValue by remember { mutableStateOf("") }
    var exportMessage by remember { mutableStateOf<String?>(null) }

    // Collect songs in the active playlist
    val activePlaylistSongs = remember(activePlaylist, library) {
        if (activePlaylist == null || library == null) emptyList()
        else {
            activePlaylist.songs.mapNotNull { library.songsByKey[it] }
        }
    }

    val totalDuration = remember(activePlaylistSongs) {
        activePlaylistSongs.fold(ZERO) { acc, s -> acc + s.length }
    }

    val currentPlayingSongKey = player.queue?.currentSongKey

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(slateCanvas)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Top Bar & Breadcrumbs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = surfaceContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, null, Modifier.size(15.dp), tint = primaryBlue)
                        Text(
                            "Playlists Manager",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = textOnSurfaceVariant,
                        )
                    }
                }
                Text("/", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = outlineVariant)
                Text(
                    activePlaylist?.name ?: "All Playlists",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = textOnSurface,
                )
            }

            // Right controls: Split View / Grid toggles + Add Songs Button
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = surfaceContainerLow,
                ) {
                    Row(Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Split View",
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(surfaceBright)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = textOnSurface,
                        )
                        Text(
                            "Grid",
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {}
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = textOnSurfaceVariant,
                        )
                    }
                }

                Button(
                    onClick = { isDrawerOpen = !isDrawerOpen },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDrawerOpen) primaryBlue else surfaceContainerHigh,
                        contentColor = Color.White,
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Icon(if (isDrawerOpen) Icons.Default.Close else Icons.Default.AddCircle, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isDrawerOpen) "Close Drawer" else "Add Songs", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }

        // Main 3-Column / 2-Column Area
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Column 1: Playlist Directory Panel (width 260dp)
            Column(
                modifier = Modifier.width(260.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Header & New Playlist action
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = surfaceContainerLow,
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "PLAYLISTS (${playlists.size})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = textOnSurfaceVariant,
                            )
                            Icon(Icons.Default.CreateNewFolder, null, Modifier.size(17.dp), tint = textOnSurfaceVariant)
                        }

                        Button(
                            onClick = { showCreateDialog = true; playlistError = null },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = surfaceContainerHigh,
                                contentColor = textOnSurface,
                            ),
                            contentPadding = PaddingValues(vertical = 8.dp),
                        ) {
                            Icon(Icons.Default.Add, null, Modifier.size(16.dp), tint = primaryBlue)
                            Spacer(Modifier.width(6.dp))
                            Text("New Playlist", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                }

                // Playlist items list
                Surface(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = surfaceContainerLow,
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        itemsIndexed(playlists) { _, pl ->
                            val isSelected = pl.uniqueKey == activePlaylist?.uniqueKey
                            val plSongs = pl.songs.mapNotNull { library?.songsByKey?.get(it) }
                            val plDuration = plSongs.fold(ZERO) { acc, s -> acc + s.length }
                            val firstCover = plSongs.firstOrNull { it.cover != null }?.cover

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedPlaylistKey = pl.uniqueKey },
                                color = if (isSelected) surfaceContainer else Color.Transparent,
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(28.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(primaryBlue)
                                        )
                                    }

                                    Surface(
                                        modifier = Modifier.size(44.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        color = surfaceContainerHighest,
                                    ) {
                                        if (firstCover != null) {
                                            AlbumCoverContent(firstCover, modifier = Modifier.fillMaxSize())
                                        } else {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.AutoMirrored.Filled.QueueMusic, null, Modifier.size(22.dp), tint = textOnSurfaceVariant)
                                            }
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                pl.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = if (isSelected) primaryBlue else textOnSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f),
                                            )
                                            if (isSelected) {
                                                Icon(Icons.Default.Equalizer, null, Modifier.size(15.dp), tint = primaryBlue)
                                            }
                                        }
                                        Text(
                                            "${pl.songs.size} tracks • ${plDuration.format()}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                            color = textOnSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Telemetry & Local Cache footprint
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = surfaceContainerLowest.copy(alpha = 0.6f),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val plSongsCount = activePlaylistSongs.size
                        val libTotalSongs = (library?.songs?.size ?: 0).coerceAtLeast(1)
                        val cacheRatio = if (activePlaylist != null) {
                            (plSongsCount.toFloat() / libTotalSongs.toFloat()).coerceIn(0.05f, 1f)
                        } else 1f
                        val plSizeBytes = remember(activePlaylistSongs) {
                            activePlaylistSongs.sumOf { runCatching { it.file.toFile().length() }.getOrDefault(0L) }
                        }
                        val plSizeStr = remember(plSizeBytes) {
                            if (plSizeBytes > 1_000_000_000L) {
                                String.format(Locale.US, "%.1f GB", plSizeBytes / 1_000_000_000.0)
                            } else {
                                String.format(Locale.US, "%.1f MB", plSizeBytes / 1_000_000.0)
                            }
                        }
                        val formatsStr = remember(activePlaylistSongs) {
                            val distinctFmts = activePlaylistSongs.map { it.file.extension.uppercase() }.filter { it.isNotEmpty() }.distinct().sorted()
                            if (distinctFmts.isNotEmpty()) distinctFmts.joinToString(" • ") else "AUDIO"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (activePlaylist != null) "PLAYLIST CACHE" else "LIBRARY CACHE",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = textOnSurfaceVariant,
                            )
                            Text(
                                if (activePlaylist != null) "$plSongsCount / $libTotalSongs Tracks" else "$libTotalSongs Tracks",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = tertiaryGreen,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(surfaceContainerHigh)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(cacheRatio)
                                    .fillMaxHeight()
                                    .background(primaryBlue)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                if (activePlaylist != null) "$plSizeStr • ${totalDuration.format()}" else plSizeStr,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                color = textOnSurfaceVariant,
                            )
                            Text(
                                formatsStr,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                color = textOnSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Column 2: Active Playlist Center Panel
            Column(
                modifier = Modifier
                    .weight(if (isDrawerOpen) 0.6f else 1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (activePlaylist != null) {
                    // Playlist Header Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = surfaceContainerLow,
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Cover art
                            val playlistCover = activePlaylistSongs.firstOrNull { it.cover != null }?.cover
                            Surface(
                                modifier = Modifier.size(130.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = surfaceContainerHighest,
                                shadowElevation = 6.dp,
                            ) {
                                if (playlistCover != null) {
                                    AlbumCoverContent(playlistCover, modifier = Modifier.fillMaxSize())
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.AutoMirrored.Filled.QueueMusic, null, Modifier.size(54.dp), tint = textOnSurfaceVariant)
                                    }
                                }
                            }

                            // Details & Buttons
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = surfaceContainerHighest,
                                    ) {
                                        Text(
                                            "PLAYLIST",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                                            color = textOnSurface,
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = secondaryContainer.copy(alpha = 0.3f),
                                    ) {
                                        Text(
                                            "24-bit Lossless",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                            color = secondaryPurple,
                                        )
                                    }
                                }

                                Text(
                                    activePlaylist.name,
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = textOnSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "${activePlaylistSongs.size} songs, ${totalDuration.format()}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = textOnSurfaceVariant,
                                )

                                // Action Buttons
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Button(
                                        onClick = {
                                            if (activePlaylistSongs.isNotEmpty()) {
                                                scope.launch {
                                                    player.transformQueue { _ ->
                                                        SongQueue(
                                                            originalSongs = activePlaylist.songs,
                                                            songs = activePlaylist.songs,
                                                            position = 0,
                                                            songsByKey = activePlaylistSongs.associateBy { it.uniqueKey },
                                                        ) to Position.Beginning
                                                    }
                                                    player.play()
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = primaryBlue, contentColor = Color.White),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    ) {
                                        Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Play All", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                                    }

                                    Button(
                                        onClick = {
                                            if (activePlaylistSongs.isNotEmpty()) {
                                                scope.launch {
                                                    player.transformQueue { _ ->
                                                        SongQueue(
                                                            originalSongs = activePlaylist.songs,
                                                            songs = activePlaylist.songs,
                                                            position = 0,
                                                            songsByKey = activePlaylistSongs.associateBy { it.uniqueKey },
                                                        ).shuffled() to Position.Beginning
                                                    }
                                                    player.play()
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = surfaceContainerHigh, contentColor = textOnSurface),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    ) {
                                        Icon(Icons.Default.Shuffle, null, Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Shuffle", style = MaterialTheme.typography.labelMedium)
                                    }

                                    IconButton(
                                        onClick = {
                                            renameValue = activePlaylist.name
                                            playlistError = null
                                            showRenameDialog = true
                                        },
                                        modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(surfaceContainerHigh),
                                    ) {
                                        Icon(Icons.Default.Edit, "Edit", Modifier.size(16.dp), tint = textOnSurfaceVariant)
                                    }

                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                runCatching {
                                                    val downloads = java.nio.file.Path.of(System.getProperty("user.home"), "Downloads")
                                                    PlaylistStore.export(activePlaylist, downloads)
                                                }.onSuccess {
                                                    exportMessage = "Exported to $it"
                                                }.onFailure {
                                                    exportMessage = it.message ?: "Could not export playlist"
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(surfaceContainerHigh),
                                    ) {
                                        Icon(Icons.Default.FileDownload, "Export", Modifier.size(16.dp), tint = textOnSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    // Tracklist Content Card
                    Surface(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = surfaceContainerLow,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // Table Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("#", modifier = Modifier.width(30.dp), style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = textOnSurfaceVariant)
                                Text("Title", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = textOnSurfaceVariant)
                                Text("Album", modifier = Modifier.weight(0.7f), style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = textOnSurfaceVariant)
                                Text("Time", modifier = Modifier.width(50.dp), style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = textOnSurfaceVariant)
                            }

                            Divider(color = surfaceContainerHighest.copy(alpha = 0.5f))

                            // Rows
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                itemsIndexed(activePlaylistSongs) { index, song ->
                                    val isPlaying = song.uniqueKey == currentPlayingSongKey
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                scope.launch {
                                                    player.transformQueue { _ ->
                                                        SongQueue(
                                                            originalSongs = activePlaylist.songs,
                                                            songs = activePlaylist.songs,
                                                            position = index,
                                                            songsByKey = activePlaylistSongs.associateBy { it.uniqueKey },
                                                        ) to Position.Beginning
                                                    }
                                                    player.play()
                                                }
                                            },
                                        color = if (isPlaying) surfaceContainer else Color.Transparent,
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            if (isPlaying) {
                                                Icon(Icons.Default.VolumeUp, null, Modifier.size(16.dp), tint = primaryBlue)
                                                Spacer(Modifier.width(14.dp))
                                            } else {
                                                Text(
                                                    (index + 1).toString().padStart(2, '0'),
                                                    modifier = Modifier.width(30.dp),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                                    color = textOnSurfaceVariant,
                                                )
                                            }

                                            // Title & Artist
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    song.title,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Normal
                                                    ),
                                                    color = if (isPlaying) primaryBlue else textOnSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Text(
                                                    song.artist.name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = textOnSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }

                                            // Album + format
                                            Row(
                                                modifier = Modifier.weight(0.7f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                Text(
                                                    song.album.title,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = textOnSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false),
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(3.dp),
                                                    color = secondaryContainer.copy(alpha = 0.25f),
                                                ) {
                                                    Text(
                                                        song.file.extension.uppercase(),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.sp),
                                                        color = secondaryPurple,
                                                    )
                                                }
                                            }

                                            // Duration & remove
                                            Text(
                                                song.length.format(),
                                                modifier = Modifier.width(50.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                                color = if (isPlaying) primaryBlue else outlineVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Column 3: Add Songs from Library Drawer (width 320dp, collapsible)
            AnimatedVisibility(visible = isDrawerOpen) {
                Surface(
                    modifier = Modifier.width(320.dp).fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp),
                    color = surfaceContainerLow,
                    shadowElevation = 8.dp,
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(Icons.Default.LibraryAdd, null, Modifier.size(18.dp), tint = primaryBlue)
                                    Text("Add from Library", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = textOnSurface)
                                }
                                Text("Scan ${library?.songs?.size ?: 0} local master files", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = textOnSurfaceVariant)
                            }
                            IconButton(
                                onClick = { isDrawerOpen = false },
                                modifier = Modifier.size(28.dp).clip(CircleShape).background(surfaceContainer),
                            ) {
                                Icon(Icons.Default.Close, "Close", Modifier.size(16.dp), tint = textOnSurfaceVariant)
                            }
                        }

                        // Search Input
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = surfaceContainer,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Search, null, Modifier.size(16.dp), tint = textOnSurfaceVariant)
                                Spacer(Modifier.width(8.dp))
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(color = textOnSurface),
                                    decorationBox = { innerTextField ->
                                        if (searchQuery.isEmpty()) {
                                            Text("Search track, artist, or album...", style = MaterialTheme.typography.bodySmall, color = outlineVariant)
                                        }
                                        innerTextField()
                                    },
                                )
                            }
                        }

                        // Filter format pills
                        val availableFormats = remember(library) {
                            val fmts = library?.songs?.map { it.file.extension.uppercase() }?.filter { it.isNotEmpty() }?.distinct()?.sorted().orEmpty()
                            listOf("All") + fmts
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            availableFormats.take(6).forEach { genre ->
                                val isGenreSelected = selectedGenre == genre
                                Text(
                                    genre,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isGenreSelected) primaryBlue else surfaceContainer)
                                        .clickable { selectedGenre = genre }
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = if (isGenreSelected) Color.White else textOnSurfaceVariant,
                                )
                            }
                        }

                        // Available Songs List
                        val availableSongs = remember(library, searchQuery, selectedGenre) {
                            val allSongs = library?.songs.orEmpty()
                            allSongs.filter { s ->
                                val matchesQuery = searchQuery.isBlank() ||
                                    s.title.contains(searchQuery, ignoreCase = true) ||
                                    s.artist.name.contains(searchQuery, ignoreCase = true) ||
                                    s.album.title.contains(searchQuery, ignoreCase = true)
                                val matchesFormat = selectedGenre == "All" || s.file.extension.equals(selectedGenre, ignoreCase = true)
                                matchesQuery && matchesFormat
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            itemsIndexed(availableSongs.take(40)) { _, song ->
                                val isInPlaylist = activePlaylist?.songSet?.contains(song.uniqueKey) == true
                                val isSelected = song.uniqueKey in selectedSongsToAdd

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (!isInPlaylist) {
                                                selectedSongsToAdd = if (isSelected) {
                                                    selectedSongsToAdd - song.uniqueKey
                                                } else {
                                                    selectedSongsToAdd + song.uniqueKey
                                                }
                                            }
                                        },
                                    color = surfaceContainer,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(36.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            color = surfaceContainerHighest,
                                        ) {
                                            if (song.cover != null) {
                                                AlbumCoverContent(song.cover, modifier = Modifier.fillMaxSize())
                                            } else {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.Album, null, Modifier.size(18.dp), tint = textOnSurfaceVariant)
                                                }
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(song.title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = textOnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${song.artist.name} • ${song.length.format()}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = textOnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }

                                        if (isInPlaylist) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = surfaceContainerHighest,
                                            ) {
                                                Text("Added", modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp), color = textOnSurfaceVariant)
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    selectedSongsToAdd = if (isSelected) selectedSongsToAdd - song.uniqueKey else selectedSongsToAdd + song.uniqueKey
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isSelected) primaryBlue else surfaceContainerHigh,
                                                    contentColor = if (isSelected) Color.White else textOnSurface,
                                                ),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(26.dp),
                                            ) {
                                                Icon(if (isSelected) Icons.Default.Check else Icons.Default.Add, null, Modifier.size(12.dp))
                                                Spacer(Modifier.width(3.dp))
                                                Text(if (isSelected) "Selected" else "Add", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Drawer Confirmation
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = surfaceContainer,
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text("Selection", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = textOnSurfaceVariant)
                                    Text("${selectedSongsToAdd.size} selected", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = primaryBlue)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            selectedSongsToAdd = emptySet()
                                            isDrawerOpen = false
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = surfaceContainerHigh, contentColor = textOnSurface),
                                        contentPadding = PaddingValues(vertical = 6.dp),
                                    ) {
                                        Text("Cancel", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Button(
                                        onClick = {
                                            val playlist = activePlaylist
                                            val selected = library?.songs.orEmpty().filter { it.uniqueKey in selectedSongsToAdd }
                                            if (playlist == null || selected.isEmpty()) {
                                                playlistError = "Choose a playlist and at least one track."
                                            } else {
                                                scope.launch {
                                                    runCatching { PlaylistStore.append(playlist, selected) }
                                                        .onSuccess {
                                                            selectedSongsToAdd = emptySet()
                                                            isDrawerOpen = false
                                                            playlistError = null
                                                        }
                                                        .onFailure { playlistError = it.message ?: "Could not update playlist" }
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1.5f),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = primaryBlue, contentColor = Color.White),
                                        contentPadding = PaddingValues(vertical = 6.dp),
                                    ) {
                                        Icon(Icons.Default.Done, null, Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Confirm (${selectedSongsToAdd.size})", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
                                    }
                                }
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
            title = { Text("New Playlist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it; playlistError = null },
                        label = { Text("Playlist name") },
                        singleLine = true,
                    )
                    playlistError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        runCatching { PlaylistStore.create(Preferences.libraryFolder.get(), newPlaylistName) }
                            .onSuccess {
                                newPlaylistName = ""
                                showCreateDialog = false
                                playlistError = null
                            }
                            .onFailure { playlistError = it.message ?: "Could not create playlist" }
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } },
        )
    }
    if (showRenameDialog && activePlaylist != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Playlist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = renameValue,
                        onValueChange = { renameValue = it; playlistError = null },
                        label = { Text("Playlist name") },
                        singleLine = true,
                    )
                    playlistError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        runCatching { PlaylistStore.rename(activePlaylist, renameValue) }
                            .onSuccess {
                                showRenameDialog = false
                                playlistError = null
                            }
                            .onFailure { playlistError = it.message ?: "Could not rename playlist" }
                    }
                }) { Text("Rename") }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") } },
        )
    }
    exportMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { exportMessage = null },
            title = { Text("Playlist Export") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { exportMessage = null }) { Text("Close") } },
        )
    }
}
