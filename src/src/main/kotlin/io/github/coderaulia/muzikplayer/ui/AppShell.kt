package io.github.coderaulia.muzikplayer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.requestFocus
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.coderaulia.muzikplayer.audio.Position
import io.github.coderaulia.muzikplayer.data.RepeatMode
import io.github.coderaulia.muzikplayer.data.Song
import io.github.coderaulia.muzikplayer.generated.resources.*
import io.github.coderaulia.muzikplayer.playerController
import io.github.coderaulia.muzikplayer.utils.format
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.io.path.extension

private val shellSurface: Color @Composable get() = LocalMuzikColors.current.containerHigh
private val sidebarSurface: Color @Composable get() = LocalMuzikColors.current.containerLow
private val shellCanvas: Color @Composable get() = LocalMuzikColors.current.canvas
private val borderOutline: Color @Composable get() = LocalMuzikColors.current.border
private val tertiaryGreen: Color @Composable get() = LocalMuzikColors.current.accentTertiary
private val primaryBlue: Color @Composable get() = LocalMuzikColors.current.accentPrimary
private val secondaryPurple: Color @Composable get() = LocalMuzikColors.current.accentSecondary
private val secondaryContainer: Color @Composable get() = LocalMuzikColors.current.accentTertiaryContainer
private val surfaceContainerHighest: Color @Composable get() = LocalMuzikColors.current.containerHighest

private val textPrimary: Color @Composable get() = LocalMuzikColors.current.textPrimary
private val textSecondary: Color @Composable get() = LocalMuzikColors.current.textSecondary
private val textMuted: Color @Composable get() = LocalMuzikColors.current.textMuted
private val containerSurface: Color @Composable get() = LocalMuzikColors.current.container
private val containerLowest: Color @Composable get() = LocalMuzikColors.current.containerLowest
private val errorColor: Color @Composable get() = LocalMuzikColors.current.error

@Composable
fun MuzikPlayerShell(
    selectedPanel: Panel,
    selectPanel: (Panel) -> Unit,
    openSettings: () -> Unit,
    closeApp: () -> Unit,
    minimizeWindow: () -> Unit,
    toggleMaximizeWindow: () -> Unit,
    isWindowMaximized: Boolean,
    openPlaylists: () -> Unit,
    onSelectLibraryTab: ((LibraryHeaderTab?) -> Unit)? = null,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    searchFocusRequest: Int = 0,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(shellCanvas)) {
        val density = LocalDensity.current
        val compact = with(density) { maxWidth < 680.dp }
        Column(Modifier.fillMaxSize()) {
            MuzikHeader(
                closeApp = closeApp,
                minimizeWindow = minimizeWindow,
                toggleMaximizeWindow = toggleMaximizeWindow,
                isWindowMaximized = isWindowMaximized,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                searchFocusRequest = searchFocusRequest,
                onHeaderTabClick = { tab ->
                    selectPanel(Panel.LIBRARY)
                    onSelectLibraryTab?.invoke(tab)
                },
            )
            Row(Modifier.weight(1f).fillMaxWidth()) {
                if (!compact) {
                    MuzikSidebar(
                        selectedPanel = selectedPanel,
                        selectPanel = selectPanel,
                        openSettings = openSettings,
                        openPlaylists = openPlaylists,
                    )
                }
                Box(Modifier.weight(1f).fillMaxHeight()) { content() }
            }
            if (compact) {
                MuzikBottomBar(selectedPanel, selectPanel, openSettings)
            }
            MuzikTransportBar(
                onOpenQueue = { selectPanel(Panel.QUEUE) },
            )
        }
    }
}

@Composable
private fun MuzikBottomBar(
    selectedPanel: Panel,
    selectPanel: (Panel) -> Unit,
    openSettings: () -> Unit,
) {
    NavigationBar(containerColor = sidebarSurface) {
        listOf(
            "Home" to Panel.PLAYER,
            "Library" to Panel.LIBRARY,
            "Playlists" to Panel.PLAYLISTS,
            "Queue" to Panel.QUEUE,
        ).forEach { (label, panel) ->
            NavigationBarItem(
                selected = panel == selectedPanel,
                onClick = { selectPanel(panel) },
                icon = {
                    Icon(
                        when (panel) {
                            Panel.PLAYER -> Icons.Default.Home
                            Panel.LIBRARY -> Icons.Default.LibraryMusic
                            Panel.PLAYLISTS -> Icons.AutoMirrored.Filled.PlaylistPlay
                            Panel.QUEUE -> Icons.AutoMirrored.Filled.QueueMusic
                        },
                        null,
                    )
                },
                alwaysShowLabel = false,
                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
            )
        }
        NavigationBarItem(
            selected = false,
            onClick = openSettings,
            icon = { Icon(Icons.Default.Settings, "Settings") },
            alwaysShowLabel = false,
            label = { Text("Settings", style = MaterialTheme.typography.labelMedium) },
        )
    }
}

@Composable
private fun MuzikHeader(
    closeApp: () -> Unit,
    minimizeWindow: () -> Unit,
    toggleMaximizeWindow: () -> Unit,
    isWindowMaximized: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchFocusRequest: Int,
    onHeaderTabClick: ((LibraryHeaderTab) -> Unit)? = null,
) {
    val searchFocusRequester = remember { FocusRequester() }
    LaunchedEffect(searchFocusRequest) {
        if (searchFocusRequest > 0) searchFocusRequester.requestFocus()
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 760.dp
        WindowDraggableArea {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(shellSurface)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Logo & App Name
                Surface(
                    modifier = Modifier.size(30.dp),
                    shape = RoundedCornerShape(7.dp),
                    color = primaryBlue,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("M", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                Text(
                    "MuzikPlayer",
                    modifier = Modifier.padding(start = 10.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                )

                Spacer(Modifier.weight(1f))

                // Navigation Pill Tabs
                if (!compact) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = sidebarSurface,
                    ) {
                        Row(Modifier.padding(2.dp)) {
                            HeaderTab("Albums") { onHeaderTabClick?.invoke(LibraryHeaderTab.ALBUM) }
                            HeaderTab("Artists") { onHeaderTabClick?.invoke(LibraryHeaderTab.ARTIST) }
                            HeaderTab("Tracks") { onHeaderTabClick?.invoke(LibraryHeaderTab.SONG) }
                            HeaderTab("Playlists") { onHeaderTabClick?.invoke(LibraryHeaderTab.PLAYLIST) }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // Search input with Ctrl K badge
                    Surface(
                        Modifier.widthIn(min = 200.dp, max = 340.dp).height(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = containerLowest,
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Search, null, Modifier.size(17.dp), tint = textSecondary)
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                modifier = Modifier.padding(start = 8.dp).weight(1f).focusRequester(searchFocusRequester),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(color = textPrimary),
                                decorationBox = { field ->
                                    if (searchQuery.isEmpty()) Text("Search local library...", color = textMuted, style = MaterialTheme.typography.bodySmall)
                                    field()
                                },
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = shellSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderOutline),
                            ) {
                                Text(
                                    "Ctrl K",
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                    color = textSecondary,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.width(16.dp))

                // User profile avatar icon
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = primaryBlue,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, null, Modifier.size(17.dp), tint = Color.White)
                    }
                }

                Spacer(Modifier.width(10.dp))

                // Window control buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    HeaderWindowButton(Icons.Default.Remove, "Minimize", onClick = minimizeWindow)
                    HeaderWindowButton(
                        if (isWindowMaximized) Icons.Default.FilterNone else Icons.Default.CropSquare,
                        if (isWindowMaximized) "Restore" else "Maximize",
                        onClick = toggleMaximizeWindow,
                    )
                    HeaderWindowButton(Icons.Default.Close, "Close", isDestructive = true, onClick = closeApp)
                }
            }
        }
    }
}

@Composable
private fun HeaderWindowButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = containerSurface,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                description,
                Modifier.size(13.dp),
                tint = if (isDestructive) errorColor else textSecondary,
            )
        }
    }
}

@Composable
private fun HeaderTab(label: String, onClick: () -> Unit) {
    Text(
        label,
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
        color = textSecondary,
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
        Modifier
            .width(240.dp)
            .fillMaxHeight()
            .background(sidebarSurface)
            .padding(horizontal = 12.dp, vertical = 16.dp),
    ) {
        Text(
            "LIBRARY",
            Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
            ),
            color = textMuted,
        )
        Spacer(Modifier.height(10.dp))
        SidebarItem("Home", Icons.Default.Home, selectedPanel == Panel.PLAYER) { selectPanel(Panel.PLAYER) }
        SidebarItem("Library", Icons.Default.LibraryMusic, selectedPanel == Panel.LIBRARY) { selectPanel(Panel.LIBRARY) }
        SidebarItem("Playlists", Icons.AutoMirrored.Filled.PlaylistPlay, selectedPanel == Panel.PLAYLISTS) {
            selectPanel(Panel.PLAYLISTS)
            openPlaylists()
        }
        SidebarItem("Queue", Icons.AutoMirrored.Filled.QueueMusic, selectedPanel == Panel.QUEUE) { selectPanel(Panel.QUEUE) }

        Spacer(Modifier.weight(1f))

        SidebarItem("Settings", Icons.Default.Settings, false, openSettings)

        // System telemetry status pill matching design
        val player = playerController.current
        val isPlaying = player.queue?.currentSong != null && !player.pause
        val engineStatus = when {
            isPlaying -> "Playing"
            player.queue?.currentSong != null -> "Paused"
            else -> "Audio Ready"
        }
        val osName = System.getProperty("os.name") ?: "Linux"
        Surface(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            shape = RoundedCornerShape(8.dp),
            color = containerLowest.copy(alpha = 0.5f),
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.GraphicEq, null, Modifier.size(15.dp), tint = if (isPlaying) tertiaryGreen else textMuted)
                Text(
                    "$osName • $engineStatus",
                    Modifier.padding(start = 8.dp).weight(1f),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) tertiaryGreen else textMuted)
                )
            }
        }
    }
}

@Composable
private fun SidebarItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) primaryBlue else Color.Transparent
    val foreground = if (selected) Color.White else textSecondary
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = foreground)
        Text(
            label,
            Modifier.padding(start = 12.dp),
            color = foreground,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
        )
    }
}

@Composable
private fun MuzikTransportBar(
    onOpenQueue: () -> Unit,
) {
    val player = playerController.current
    val queue = player.queue
    val song = queue?.currentSong
    val scope = rememberCoroutineScope()
    var position by remember { mutableStateOf(ZERO) }
    if (song != null) {
        player.ObservePosition { position = it }
    }

    val isShuffled = queue?.isShuffled == true
    val repeatMode = queue?.repeatMode ?: RepeatMode.DO_NOT_REPEAT
    val formatTag = song?.file?.extension?.uppercase().orEmpty()

    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
        color = shellSurface,
        tonalElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderOutline),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val compactTransport = maxWidth < 820.dp
            Column(Modifier.fillMaxWidth().padding(horizontal = if (compactTransport) 10.dp else 20.dp, vertical = if (compactTransport) 6.dp else 0.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
            // Zone 1 (Left): Album thumb + Title + Artist + Lossless badge
            if (!compactTransport) Row(
                modifier = Modifier.widthIn(min = 200.dp, max = 280.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = containerSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderOutline),
                ) {
                    if (song?.cover != null) {
                        AlbumCoverContent(song.cover, modifier = Modifier.fillMaxSize())
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Album, null, Modifier.size(26.dp), tint = textSecondary)
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        song?.title ?: "No Track Playing",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        song?.artist?.name ?: "No track playing",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (formatTag.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = secondaryContainer.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, secondaryPurple.copy(alpha = 0.3f)),
                    ) {
                        Text(
                            formatTag,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                            color = secondaryPurple,
                        )
                    }
                }
            }

            if (!compactTransport) Spacer(Modifier.weight(1f))

            // Zone 2 (Center): Playback Controls + Interactive Scrubber
            Column(
                modifier = if (compactTransport) Modifier.fillMaxWidth() else Modifier.widthIn(min = 340.dp, max = 560.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Button bar
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Shuffle
                    IconButton(
                        onClick = {
                            scope.launch {
                                player.transformQueue { q -> q?.toggleShuffle() to Position.Current }
                            }
                        },
                        modifier = Modifier.size(30.dp),
                    ) {
                        Icon(
                            Icons.Default.Shuffle,
                            "Shuffle",
                            Modifier.size(18.dp),
                            tint = if (isShuffled) primaryBlue else textSecondary,
                        )
                    }

                    // Previous
                    IconButton(
                        onClick = {
                            scope.launch {
                                player.transformQueue { q -> q?.previous() to Position.Beginning }
                                player.play()
                            }
                        },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(Icons.Default.SkipPrevious, "Previous", Modifier.size(22.dp), tint = textPrimary)
                    }

                    // Play/Pause Circle Button
                    FilledIconButton(
                        onClick = {
                            scope.launch {
                                if (player.pause) player.play() else player.pause()
                            }
                        },
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = primaryBlue,
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(
                            if (player.pause) Icons.Default.PlayArrow else Icons.Default.Pause,
                            if (player.pause) "Play" else "Pause",
                            Modifier.size(22.dp),
                        )
                    }

                    // Next
                    IconButton(
                        onClick = {
                            scope.launch {
                                player.transformQueue { q -> q?.next() to Position.Beginning }
                                player.play()
                            }
                        },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(Icons.Default.SkipNext, "Next", Modifier.size(22.dp), tint = textPrimary)
                    }

                    // Repeat
                    IconButton(
                        onClick = {
                            scope.launch {
                                player.transformQueue { q -> q?.toggleRepeat() to Position.Current }
                            }
                        },
                        modifier = Modifier.size(30.dp),
                    ) {
                        Icon(
                            if (repeatMode == RepeatMode.REPEAT_SONG) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            "Repeat",
                            Modifier.size(18.dp),
                            tint = if (repeatMode != RepeatMode.DO_NOT_REPEAT) primaryBlue else textSecondary,
                        )
                    }
                }

                // Progress Bar with flanking timecodes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        position.format(),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = textSecondary,
                    )

                    // Continuous Interactive Scrubber
                    val totalDuration = song?.length ?: ZERO
                    TransportScrubber(
                        position = position,
                        duration = totalDuration,
                        onSeek = { target ->
                            scope.launch {
                                player.startSeek()
                                player.transformQueue { q ->
                                    if (q?.currentSongKey == song?.uniqueKey) {
                                        q to Position.Specific(target)
                                    } else q to Position.Current
                                }
                                player.endSeek()
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )

                    Text(
                        totalDuration.format(),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = textSecondary,
                    )
                }
            }

            if (!compactTransport) Spacer(Modifier.weight(1f))

            // Zone 3 (Right): Queue toggle + Volume Slider
            if (!compactTransport) Row(
                modifier = Modifier.widthIn(min = 180.dp, max = 240.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(
                    onClick = onOpenQueue,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, "Queue", Modifier.size(20.dp), tint = textSecondary)
                }

                Spacer(Modifier.width(8.dp))

                // Compact Volume Control
                CompactVolumeControl()
            }
                }
            }
        }
    }
}

@Composable
private fun TransportScrubber(
    position: Duration,
    duration: Duration,
    onSeek: (Duration) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isHovered by remember { mutableStateOf(false) }
    val progress = if (duration > ZERO) (position / duration).toFloat().coerceIn(0f, 1f) else 0f

    BoxWithConstraints(
        modifier = modifier
            .height(16.dp)
            .pointerInput(duration) {
                detectTapGestures { offset ->
                    val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek((duration.inWholeMilliseconds * ratio).toLong().milliseconds)
                }
            }
            .pointerInput(duration) {
                detectDragGestures { change, _ ->
                    val ratio = (change.position.x / size.width).coerceIn(0f, 1f)
                    onSeek((duration.inWholeMilliseconds * ratio).toLong().milliseconds)
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val barHeight = 4.dp
        // Background track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(2.dp))
                .background(surfaceContainerHighest)
        ) {
            // Filled portion
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(primaryBlue)
            )
        }
    }
}

@Composable
private fun CompactVolumeControl() {
    val player = playerController.current
    val scope = rememberCoroutineScope()
    val level = player.level
    var previousNonZeroLevel by remember { mutableStateOf(if (level > 0) level else 0.8f) }
    if (level > 0) previousNonZeroLevel = level

    val volIcon = when {
        level <= 0f -> Icons.AutoMirrored.Filled.VolumeOff
        level < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
        else -> Icons.AutoMirrored.Filled.VolumeUp
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = {
                scope.launch {
                    if (level > 0) player.setLevel(0f) else player.setLevel(previousNonZeroLevel)
                }
            },
            modifier = Modifier.size(30.dp),
        ) {
            Icon(volIcon, "Mute toggle", Modifier.size(18.dp), tint = textSecondary)
        }

        BoxWithConstraints(
            modifier = Modifier
                .width(84.dp)
                .height(16.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                        scope.launch { player.setLevel(ratio) }
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val ratio = (change.position.x / size.width).coerceIn(0f, 1f)
                        scope.launch { player.setLevel(ratio) }
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(surfaceContainerHighest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(level.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(primaryBlue)
                )
            }
        }
    }
}
