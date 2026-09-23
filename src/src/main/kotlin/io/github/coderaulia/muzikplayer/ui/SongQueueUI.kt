package io.github.coderaulia.muzikplayer.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.coderaulia.muzikplayer.audio.Position
import io.github.coderaulia.muzikplayer.data.Library
import io.github.coderaulia.muzikplayer.data.RepeatMode
import io.github.coderaulia.muzikplayer.data.SongListItem
import io.github.coderaulia.muzikplayer.data.SongQueue
import io.github.coderaulia.muzikplayer.data.SongQueueController
import io.github.coderaulia.muzikplayer.generated.resources.*
import io.github.coderaulia.muzikplayer.playerController
import io.github.coderaulia.muzikplayer.utils.format
import io.github.coderaulia.muzikplayer.utils.sumOfDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SongQueueUI(
    sortedLibrary: Library,
    showToolbar: Boolean,
    openSettings: () -> Unit,
    closeApp: () -> Unit,
) {
    val player = playerController.current
    val queue = player.queue
    val cs = rememberCoroutineScope()
    if (queue == null) {
        WindowDraggableArea {
            Column {
                if (showToolbar) {
                    AppToolbar(openSettings = openSettings, closeApp = closeApp)
                }
                BigMessage(
                    Modifier.fillMaxSize(),
                    Icons.AutoMirrored.Default.QueueMusic,
                    stringResource(Res.string.empty_queue),
                    stringResource(Res.string.help_play_a_song_message),
                )
            }
        }
    } else {
        val controller = SongQueueController(cs, sortedLibrary, queue.originalSongs, player)
        Column(Modifier.fillMaxSize()) {
            WindowDraggableArea {
                Row(
                    Modifier.heightIn(min = 64.dp).padding(vertical = 8.dp).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.width(16.dp))
                    SmallFakeSpectrometers(
                        Modifier.size(32.dp),
                        player,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        val songsInQueueStr =
                            pluralStringResource(Res.plurals.n_songs_in_queue, queue.songs.size, queue.songs.size)
                        val totalLength = queue.songs.sumOfDuration { queue.songsByKey.getValue(it).length }.format()
                        SingleLineText("$songsInQueueStr • $totalLength", style = MaterialTheme.typography.bodyMedium)

                        val remaining = queue.remainingSongs
                        val songsRemainingStr =
                            pluralStringResource(Res.plurals.n_songs_remaining, remaining.size, remaining.size)
                        val remainingLength = remaining.sumOfDuration { queue.songsByKey.getValue(it).length }.format()
                        SingleLineText(
                            "$songsRemainingStr • $remainingLength", style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    RepeatIcon(cs, queue)
                    ShuffleIcon(cs, queue)
                    if (showToolbar) {
                        AppToolbar(
                            openSettings = openSettings,
                            closeApp = closeApp,
                            modifier = Modifier,
                            autoSize = false
                        )
                    }
                }
            }
            HorizontalDivider()
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val items = queue.songs.mapIndexed { index, it ->
                    SongListItem.QueuedSongListItem(index, queue.songsByKey.getValue(it))
                }
                val state = rememberLazySongListState(maxHeight, items, tryNotToScroll = false, sortOrder = null)
                SongListUI(
                    0,
                    items,
                    { state },
                    controller,
                )
            }
        }
    }
}

@Composable
fun ShuffleIcon(
    cs: CoroutineScope,
    queue: SongQueue
) {
    val player = playerController.current
    PlayerIcon(
        cs,
        Icons.Default.Shuffle,
        label = stringResource(if (queue.isShuffled) Res.string.action_disable_shuffle else Res.string.action_enable_shuffle),
        active = queue.isShuffled
    ) {
        player.transformQueue { queue ->
            queue?.toggleShuffle() to Position.Current
        }
    }
}

@Composable
fun RepeatIcon(
    cs: CoroutineScope,
    queue: SongQueue
) {
    val player = playerController.current
    val next = queue.repeatMode.next
    PlayerIcon(
        cs,
        if (queue.repeatMode == RepeatMode.REPEAT_SONG) Icons.Default.RepeatOne else Icons.Default.Repeat,
        label = stringResource(
            when (next) {
                RepeatMode.DO_NOT_REPEAT -> Res.string.action_repeat_none
                RepeatMode.REPEAT_QUEUE -> Res.string.action_repeat_queue
                RepeatMode.REPEAT_SONG -> Res.string.action_repeat_song
            }
        ),
        active = queue.repeatMode != RepeatMode.DO_NOT_REPEAT
    ) {
        player.transformQueue { queue ->
            queue?.setRepeatMode(next) to Position.Current
        }
    }
}

@Composable
private fun PlayerIcon(
    cs: CoroutineScope,
    icon: ImageVector,
    label: String,
    iconModifier: Modifier = Modifier,
    size: Dp = 40.dp,
    enabled: Boolean = true,
    active: Boolean = true,
    onClick: suspend CoroutineScope.() -> Unit
) {
    val alpha by animateFloatAsState(if (active) 1f else INACTIVE_ALPHA)
    BigIconButton(size = size, {
        cs.launch {
            onClick()
        }
    }, enabled = enabled) {
        Icon(icon, label, iconModifier.padding(4.dp).alpha(alpha))
    }
}