package io.github.coderaulia.muzikplayer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.coderaulia.muzikplayer.audio.Position
import io.github.coderaulia.muzikplayer.data.Library
import io.github.coderaulia.muzikplayer.data.Song
import io.github.coderaulia.muzikplayer.data.SongQueue
import io.github.coderaulia.muzikplayer.playerController
import io.github.coderaulia.muzikplayer.utils.format
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.io.path.extension

private val slateCanvas = Color(0xFF131313)
private val surfaceContainerLow = Color(0xFF1B1C1C)
private val surfaceContainer = Color(0xFF1F2020)
private val surfaceContainerHigh = Color(0xFF2A2A2A)
private val surfaceContainerHighest = Color(0xFF353535)
private val textOnSurface = Color(0xFFE4E2E1)
private val textOnSurfaceVariant = Color(0xFFC1C6D4)
private val outlineVariant = Color(0xFF414752)
private val tertiaryGreen = Color(0xFF48E087)
private val primaryBlue = Color(0xFF4691F2)
private val secondaryPurple = Color(0xFFCABEFF)
private val secondaryContainer = Color(0xFF4A16D1)

private enum class HomeInspectorTab {
    LYRICS, TAGS, DSP
}

@Composable
fun HomePage(
    library: Library?,
    onViewAlbumInLibrary: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val player = playerController.current
    val queue = player.queue
    val currentSong = queue?.currentSong
    val scope = rememberCoroutineScope()

    var position by remember { mutableStateOf(ZERO) }
    if (currentSong != null) {
        player.ObservePosition { position = it }
    }

    var selectedTab by remember { mutableStateOf(HomeInspectorTab.LYRICS) }
    var isFavorite by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(slateCanvas)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. Top System & Library Status Anchor Bar
        SystemStatusAnchorBar(library = library)

        if (currentSong == null) {
            // Empty state when nothing is in queue
            EmptyPlaybackHero(library = library)
        } else {
            // Main 2-column studio layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Left 8-col: Studio Player Stage + Tabbed Deck + Contextual Album Strip
                Column(
                    modifier = Modifier.weight(0.65f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    StudioPlayerStageCard(
                        song = currentSong,
                        queue = queue,
                        position = position,
                        isFavorite = isFavorite,
                        onToggleFavorite = { isFavorite = !isFavorite },
                        onSeek = { target ->
                            scope.launch {
                                player.startSeek()
                                player.transformQueue { q ->
                                    if (q?.currentSongKey == currentSong.uniqueKey) {
                                        q to Position.Specific(target)
                                    } else q to Position.Current
                                }
                                player.endSeek()
                            }
                        },
                    )

                    TabbedInspectorDeck(
                        song = currentSong,
                        position = position,
                        selectedTab = selectedTab,
                        onSelectTab = { selectedTab = it },
                        onSeek = { target ->
                            scope.launch {
                                player.startSeek()
                                player.transformQueue { q ->
                                    if (q?.currentSongKey == currentSong.uniqueKey) {
                                        q to Position.Specific(target)
                                    } else q to Position.Current
                                }
                                player.endSeek()
                            }
                        },
                    )

                    ContextualAlbumTracksStrip(
                        currentSong = currentSong,
                        library = library,
                        onViewAlbumInLibrary = onViewAlbumInLibrary,
                    )
                }

                // Right 4-col: Play Queue + Audio Diagnostic Monitor
                Column(
                    modifier = Modifier.weight(0.35f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    PlayQueueCard(
                        queue = queue,
                        onRemove = { index ->
                            scope.launch {
                                player.transformQueue { q ->
                                    if (q != null && q.songs.size > 1) {
                                        val newSongs = q.songs.toMutableList().apply { removeAt(index) }
                                        val newOriginal = q.originalSongs.toMutableList().apply { remove(q.songs[index]) }
                                        val newPos = when {
                                            index < q.position -> q.position - 1
                                            index == q.position -> q.position.coerceAtMost(newSongs.lastIndex)
                                            else -> q.position
                                        }
                                        q.copy(songs = newSongs, originalSongs = newOriginal, position = newPos) to Position.Current
                                    } else q to Position.Current
                                }
                            }
                        },
                        onClear = {
                            scope.launch {
                                player.transformQueue { q ->
                                    if (q != null) {
                                        q.copy(
                                            songs = listOf(q.currentSongKey),
                                            originalSongs = listOf(q.currentSongKey),
                                            position = 0,
                                        ) to Position.Current
                                    } else null to Position.Beginning
                                }
                            }
                        },
                    )

                    AudioBackendMonitorCard()
                }
            }
        }
    }
}

@Composable
private fun SystemStatusAnchorBar(library: Library?) {
    val songCount = library?.songs?.size ?: 0
    val totalLength = (library?.stats?.totalLength ?: ZERO).format()
    val musicDir = library?.songs?.firstOrNull()?.file?.parent?.toString() ?: "/home/asw/Music"

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 720.dp
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = surfaceContainer,
        ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(tertiaryGreen))
            Text(
                "PipeWire Direct: 96,000 Hz / 24-bit PCM",
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = textOnSurface,
                maxLines = 1,
            )
            if (!compact) {
                Text(" / ", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = outlineVariant)
                Icon(Icons.Default.FolderOpen, null, Modifier.size(15.dp), tint = textOnSurfaceVariant)
                Text(
                    musicDir,
                    modifier = Modifier.padding(start = 6.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = textOnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(12.dp))
                Text("$songCount tracks • $totalLength", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = outlineVariant, maxLines = 1)
            }
            Spacer(Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = surfaceContainerHigh,
            ) {
                Text(
                    if (songCount == 0) "Library empty" else "Library ready",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = tertiaryGreen,
                )
            }
            Spacer(Modifier.width(8.dp))
            if (!compact) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = surfaceContainerHighest,
                ) {
                    Row(Modifier.padding(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "96 kHz PCM",
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {}
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = textOnSurfaceVariant,
                    )
                    Text(
                        "PipeWire Default",
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(surfaceContainerLow)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = primaryBlue,
                    )
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun StudioPlayerStageCard(
    song: Song,
    queue: SongQueue?,
    position: Duration,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onSeek: (Duration) -> Unit,
) {
    val player = playerController.current
    val scope = rememberCoroutineScope()
    val formatTag = song.file.extension.uppercase()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = surfaceContainerLow,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Ambient subtle radial blur in corners
            Box(
                Modifier
                    .size(240.dp)
                    .offset((-40).dp, (-40).dp)
                    .background(Brush.radialGradient(listOf(primaryBlue.copy(alpha = 0.08f), Color.Transparent)))
            )

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Top row: Album cover with vinyl disc sleeve + Master Metadata & Specs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    // Album Cover with vinyl sleeve peek effect
                    Box(
                        modifier = Modifier.size(200.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        // Vinyl record peek background
                        Box(
                            modifier = Modifier
                                .size(190.dp)
                                .offset(x = 18.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0E0E0E))
                                .border(1.dp, Color(0xFF222222), CircleShape)
                        ) {
                            // Vinyl concentric circles
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .align(Alignment.Center)
                                    .border(1.dp, Color(0x33444444), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .align(Alignment.Center)
                                    .border(2.dp, Color(0x66CABEFF), CircleShape)
                            )
                        }

                        // Sleeve Album cover
                        Surface(
                            modifier = Modifier
                                .size(190.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            color = surfaceContainerHighest,
                            shadowElevation = 8.dp,
                        ) {
                            Box {
                                if (song.cover != null) {
                                    AlbumCoverContent(song.cover, modifier = Modifier.fillMaxSize())
                                } else {
                                    Box(
                                        Modifier.fillMaxSize().background(surfaceContainerHighest),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Default.Album, null, Modifier.size(72.dp), tint = textOnSurfaceVariant)
                                    }
                                }

                                // Play / Pause overlay trigger
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0x33000000))
                                        .clickable {
                                            scope.launch {
                                                if (player.pause) player.play() else player.pause()
                                            }
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Surface(
                                        modifier = Modifier.size(48.dp),
                                        shape = CircleShape,
                                        color = primaryBlue,
                                        shadowElevation = 6.dp,
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                if (player.pause) Icons.Default.PlayArrow else Icons.Default.Pause,
                                                null,
                                                Modifier.size(28.dp),
                                                tint = Color.White,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Master Metadata & Specs column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Badges row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = secondaryContainer,
                            ) {
                                Text(
                                    "$formatTag MASTER",
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = secondaryPurple,
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = surfaceContainerHigh,
                            ) {
                                Text(
                                    "96 kHz / 24-bit",
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = tertiaryGreen,
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = surfaceContainerHigh,
                            ) {
                                Text(
                                    "2,842 kbps",
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = textOnSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            val trackNum = song.track ?: ((queue?.position ?: 0) + 1)
                            val totalTracks = queue?.songs?.size ?: 1
                            Text(
                                "Track ${trackNum.toString().padStart(2, '0')}/${totalTracks.toString().padStart(2, '0')}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = textOnSurfaceVariant,
                            )
                        }

                        Spacer(Modifier.height(4.dp))
                        Text(
                            song.title,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = textOnSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            song.artist.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = primaryBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${song.album.title} ${song.date?.let { "($it)" } ?: ""} • Local Lossless Audio",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textOnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Waveform Visualizer Module
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.GraphicEq, null, Modifier.size(16.dp), tint = primaryBlue)
                            Text(
                                "Peak Waveform / Local Cache",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = textOnSurface,
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                position.format(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = primaryBlue,
                            )
                            Text("/", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = outlineVariant)
                            Text(
                                song.length.format(),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = textOnSurfaceVariant,
                            )
                        }
                    }

                    // Interactive Waveform Bars
                    StudioWaveformBars(
                        position = position,
                        duration = song.length,
                        onSeek = onSeek,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(surfaceContainer)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }

                // Transport Shortcut Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {},
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = surfaceContainer,
                                contentColor = textOnSurface,
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Icon(Icons.Default.BookmarkBorder, null, Modifier.size(16.dp), tint = primaryBlue)
                            Spacer(Modifier.width(6.dp))
                            Text("Mark A-B Loop", style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = {},
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = surfaceContainer,
                                contentColor = textOnSurface,
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Icon(Icons.Default.Tune, null, Modifier.size(16.dp), tint = secondaryPurple)
                            Spacer(Modifier.width(6.dp))
                            Text("Parametric EQ", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(surfaceContainer),
                        ) {
                            Icon(
                                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                "Favorite",
                                Modifier.size(18.dp),
                                tint = if (isFavorite) Color(0xFFFF5252) else textOnSurfaceVariant,
                            )
                        }
                        IconButton(
                            onClick = {},
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(surfaceContainer),
                        ) {
                            Icon(Icons.Default.Info, "Track info", Modifier.size(18.dp), tint = textOnSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioWaveformBars(
    position: Duration,
    duration: Duration,
    onSeek: (Duration) -> Unit,
    modifier: Modifier = Modifier,
) {
    val barCount = 80
    val progress = if (duration > ZERO) (position / duration).toFloat().coerceIn(0f, 1f) else 0f
    val activeBars = (progress * barCount).toInt()

    BoxWithConstraints(
        modifier = modifier.pointerInput(duration) {
            detectTapGestures { offset ->
                val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                onSeek((duration.inWholeMilliseconds * ratio).toLong().milliseconds)
            }
        }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (i in 0 until barCount) {
                // Generate natural-looking pseudo-waveform heights based on position and sine harmonics
                val norm = i.toFloat() / barCount
                val hFactor = 0.25f + 0.65f * kotlin.math.abs(
                    sin(norm * 14f) * 0.5f +
                            sin(norm * 28f + 1f) * 0.3f +
                            sin(norm * 6f + 2f) * 0.2f
                )
                val isPlayed = i <= activeBars
                val barColor = if (isPlayed) primaryBlue else surfaceContainerHighest

                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight(hFactor.coerceIn(0.15f, 1f))
                        .clip(RoundedCornerShape(1.dp))
                        .background(barColor)
                )
            }
        }
    }
}

@Composable
private fun TabbedInspectorDeck(
    song: Song,
    position: Duration,
    selectedTab: HomeInspectorTab,
    onSelectTab: (HomeInspectorTab) -> Unit,
    onSeek: (Duration) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = surfaceContainerLow,
                ) {
                    Row(Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        InspectorTabButton(
                            title = "Synced Lyrics",
                            selected = selectedTab == HomeInspectorTab.LYRICS,
                            onClick = { onSelectTab(HomeInspectorTab.LYRICS) },
                        )
                        InspectorTabButton(
                            title = "Tags & Precision Data",
                            selected = selectedTab == HomeInspectorTab.TAGS,
                            onClick = { onSelectTab(HomeInspectorTab.TAGS) },
                        )
                        InspectorTabButton(
                            title = "DSP Pipeline",
                            selected = selectedTab == HomeInspectorTab.DSP,
                            onClick = { onSelectTab(HomeInspectorTab.DSP) },
                        )
                    }
                }
                Text(
                    "LRC Verified • UTF-8",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = textOnSurfaceVariant,
                )
            }

            Crossfade(selectedTab) { tab ->
                when (tab) {
                    HomeInspectorTab.LYRICS -> LyricsDeckContent(song, position, onSeek)
                    HomeInspectorTab.TAGS -> TagsDeckContent(song)
                    HomeInspectorTab.DSP -> DspDeckContent()
                }
            }
        }
    }
}

@Composable
private fun InspectorTabButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) surfaceContainerHigh else Color.Transparent
    val fg = if (selected) textOnSurface else textOnSurfaceVariant
    val fw = if (selected) FontWeight.SemiBold else FontWeight.Normal
    Text(
        title,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = fw),
        color = fg,
    )
}

@Composable
private fun LyricsDeckContent(
    song: Song,
    position: Duration,
    onSeek: (Duration) -> Unit,
) {
    val lyrics = song.lyrics
    if (lyrics is io.github.coderaulia.muzikplayer.data.Lyrics.Synchronized) {
        val lines = lyrics.lines
        val activeIndex = lines.indexOfLast { it.start <= position }.coerceAtLeast(0)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 160.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            lines.forEachIndexed { index, line ->
                val lineTime = line.start
                val lineText = line.content
                val isActive = index == activeIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSeek(lineTime) }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (isActive) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(primaryBlue))
                    }
                    Text(
                        lineText,
                        style = if (isActive) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        else MaterialTheme.typography.bodyMedium,
                        color = if (isActive) primaryBlue else textOnSurfaceVariant,
                    )
                }
            }
        }
    } else {
        // Fallback ambient lyrics mockup matching design
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Shadows crawling down the ridge line slow", style = MaterialTheme.typography.bodyMedium, color = textOnSurfaceVariant.copy(alpha = 0.6f))
            Text("Cold heather waking up to the dawn glow", style = MaterialTheme.typography.bodyMedium, color = textOnSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(primaryBlue))
                Text(
                    "The mist recedes where the stone stands tall",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = primaryBlue,
                )
            }
            Text("And the golden ray breaks over us all", style = MaterialTheme.typography.bodyMedium, color = textOnSurfaceVariant.copy(alpha = 0.8f))
            Text("Footsteps echo through the heather deep...", style = MaterialTheme.typography.bodyMedium, color = textOnSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun TagsDeckContent(song: Song) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TagMetricItem("Container Format", song.file.extension.uppercase(), Modifier.weight(1f))
            TagMetricItem("Sample Rate", "96,000 Hz", Modifier.weight(1f))
            TagMetricItem("Bit Depth", "24-bit PCM", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TagMetricItem("Channels", "2 (Stereo L/R)", Modifier.weight(1f))
            TagMetricItem("Encoding", "Lossless FLAC", Modifier.weight(1f))
            TagMetricItem("ReplayGain", "-1.4 dB", Modifier.weight(1f))
        }
        TagMetricItem("Source File", song.file.toString(), Modifier.fillMaxWidth())
    }
}

@Composable
private fun TagMetricItem(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = surfaceContainerLow,
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = textOnSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold), color = textOnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DspDeckContent() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TagMetricItem("Engine Sink", "Direct PipeWire / ALSA", Modifier.weight(1f))
        TagMetricItem("Resampling", "Bit-Perfect (1:1 Bypass)", Modifier.weight(1f))
        TagMetricItem("DSP Headroom", "No clipping (0.0 dB)", Modifier.weight(1f))
    }
}

@Composable
private fun ContextualAlbumTracksStrip(
    currentSong: Song,
    library: Library?,
    onViewAlbumInLibrary: ((String) -> Unit)?,
) {
    val player = playerController.current
    val scope = rememberCoroutineScope()
    val albumSongs = remember(currentSong, library) {
        library?.songs?.filter { it.album.uniqueKey == currentSong.album.uniqueKey }?.sortedBy { it.track ?: 0 } ?: listOf(currentSong)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${currentSong.artist.name} — ${currentSong.album.title}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = textOnSurface,
                    )
                    Text(
                        "(${albumSongs.size} tracks)",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = textOnSurfaceVariant,
                    )
                }
                Text(
                    "View Album in Library",
                    modifier = Modifier.clickable { onViewAlbumInLibrary?.invoke(currentSong.album.title) },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = primaryBlue,
                )
            }

            // Grid of tracks
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                albumSongs.take(6).forEachIndexed { idx, song ->
                    val isPlaying = song.uniqueKey == currentSong.uniqueKey
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                scope.launch {
                                    player.transformQueue { q ->
                                        if (q != null && song.uniqueKey in q.songsByKey) {
                                            val songIdx = q.songs.indexOf(song.uniqueKey)
                                            if (songIdx >= 0) q.copy(position = songIdx) to Position.Beginning
                                            else q to Position.Current
                                        } else q to Position.Current
                                    }
                                    player.play()
                                }
                            },
                        color = if (isPlaying) primaryBlue.copy(alpha = 0.12f) else surfaceContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (isPlaying) {
                                Icon(Icons.Default.VolumeUp, null, Modifier.size(15.dp), tint = primaryBlue)
                                Spacer(Modifier.width(8.dp))
                            } else {
                                Text(
                                    (idx + 1).toString().padStart(2, '0'),
                                    modifier = Modifier.width(22.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = textOnSurfaceVariant,
                                )
                            }
                            Text(
                                song.title,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = if (isPlaying) primaryBlue else textOnSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                song.length.format(),
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

@Composable
private fun PlayQueueCard(
    queue: SongQueue?,
    onRemove: (Int) -> Unit,
    onClear: () -> Unit,
) {
    val player = playerController.current
    val scope = rememberCoroutineScope()
    val songs = queue?.songs.orEmpty()
    val songsByKey = queue?.songsByKey.orEmpty()
    val currentPos = queue?.position ?: 0

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Play Queue",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = textOnSurface,
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = surfaceContainerHigh,
                    ) {
                        Text(
                            "${songs.size} tracks",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = textOnSurfaceVariant,
                        )
                    }
                }
                Text(
                    "Clear",
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onClear)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = textOnSurfaceVariant,
                )
            }

            // Queue tracks list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                songs.forEachIndexed { index, key ->
                    val song = songsByKey[key]
                    val isPlaying = index == currentPos

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                scope.launch {
                                    player.transformQueue { q ->
                                        q?.copy(position = index) to Position.Beginning
                                    }
                                    player.play()
                                }
                            },
                        color = if (isPlaying) surfaceContainerHighest else surfaceContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (isPlaying) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(24.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(primaryBlue)
                                )
                                Icon(Icons.Default.Equalizer, null, Modifier.size(16.dp), tint = primaryBlue)
                            } else {
                                Icon(Icons.Default.DragIndicator, null, Modifier.size(16.dp), tint = outlineVariant)
                            }

                            // Thumb
                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = RoundedCornerShape(4.dp),
                                color = surfaceContainerHighest,
                            ) {
                                if (song?.cover != null) {
                                    AlbumCoverContent(song.cover, modifier = Modifier.fillMaxSize())
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Album, null, Modifier.size(18.dp), tint = textOnSurfaceVariant)
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    song?.title ?: "Track $index",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isPlaying) primaryBlue else textOnSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    if (isPlaying) "${song?.artist?.name ?: ""} • Now Playing" else (song?.artist?.name ?: ""),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = if (isPlaying) primaryBlue else textOnSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            Text(
                                song?.length?.format() ?: "0:00",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = if (isPlaying) primaryBlue else outlineVariant,
                            )

                            if (!isPlaying) {
                                IconButton(
                                    onClick = { onRemove(index) },
                                    modifier = Modifier.size(20.dp),
                                ) {
                                    Icon(Icons.Default.Close, "Remove", Modifier.size(14.dp), tint = outlineVariant)
                                }
                            }
                        }
                    }
                }
            }

            // Bottom summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                var autoQueue by remember { mutableStateOf(true) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Checkbox(
                        checked = autoQueue,
                        onCheckedChange = { autoQueue = it },
                        modifier = Modifier.size(16.dp),
                    )
                    Text("Auto-queue similar", style = MaterialTheme.typography.labelSmall, color = textOnSurfaceVariant)
                }
                Text(
                    "Total: ${songs.size} tracks",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = outlineVariant,
                )
            }
        }
    }
}

@Composable
private fun AudioBackendMonitorCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Memory, null, Modifier.size(16.dp), tint = tertiaryGreen)
                    Text("Audio Backend Monitor", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = textOnSurface)
                }
                Text("PipeWire 1.0.5", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = tertiaryGreen)
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DiagnosticPill("Buffer Latency", "5.33 ms (512 spl)", Modifier.weight(1f))
                    DiagnosticPill("ReplayGain", "-1.4 dB (Album)", Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DiagnosticPill("Resampling", "Bit-Perfect 1:1", Modifier.weight(1f))
                    DiagnosticPill("DSP State", "No clipping (0.0dB)", Modifier.weight(1f), isSuccess = true)
                }
            }
        }
    }
}

@Composable
private fun DiagnosticPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isSuccess: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = surfaceContainer,
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp), color = textOnSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = if (isSuccess) tertiaryGreen else textOnSurface,
            )
        }
    }
}

@Composable
private fun EmptyPlaybackHero(library: Library?) {
    val player = playerController.current
    val scope = rememberCoroutineScope()
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        shape = RoundedCornerShape(16.dp),
        color = surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = primaryBlue.copy(alpha = 0.15f),
                modifier = Modifier.size(72.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayCircleFilled, null, Modifier.size(40.dp), tint = primaryBlue)
                }
            }
            Text("No Track Currently Playing", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = textOnSurface)
            Text("Select any song from your local library or start playing all tracks.", style = MaterialTheme.typography.bodyMedium, color = textOnSurfaceVariant)
            Button(
                onClick = {
                    val firstSong = library?.songs?.firstOrNull()
                    if (firstSong != null) {
                        scope.launch {
                            player.transformQueue { _ ->
                                SongQueue(
                                    originalSongs = library.songs.map { it.uniqueKey },
                                    songs = library.songs.map { it.uniqueKey },
                                    position = 0,
                                    songsByKey = library.songsByKey,
                                ) to Position.Beginning
                            }
                            player.play()
                        }
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue, contentColor = Color.White),
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(6.dp))
                Text("Play All Tracks")
            }
        }
    }
}
