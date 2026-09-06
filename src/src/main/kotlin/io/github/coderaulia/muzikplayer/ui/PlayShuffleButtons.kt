package io.github.coderaulia.muzikplayer.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.coderaulia.muzikplayer.data.Song
import io.github.coderaulia.muzikplayer.data.SongQueueController
import io.github.coderaulia.muzikplayer.generated.resources.Res
import io.github.coderaulia.muzikplayer.generated.resources.action_play
import io.github.coderaulia.muzikplayer.generated.resources.action_shuffle_from_here
import org.jetbrains.compose.resources.stringResource

@Composable
fun PlayShuffleButtons(
    modifier: Modifier = Modifier,
    controller: SongQueueController,
    songs: List<Song>,
) {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Row(modifier) {
            IconButtonWithBG({
                controller.playSongs(songs, null)
            }) {
                Icon(Icons.Default.PlayArrow, stringResource(Res.string.action_play))
            }
            IconButtonWithBG({
                controller.playSongs(songs, null, shuffle = true)
            }) {
                Icon(Icons.Default.Shuffle, stringResource(Res.string.action_shuffle_from_here))
            }
        }
    }
}