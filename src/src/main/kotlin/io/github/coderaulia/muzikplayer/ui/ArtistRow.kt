package io.github.coderaulia.muzikplayer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import io.github.coderaulia.muzikplayer.data.Artist
import io.github.coderaulia.muzikplayer.data.Song
import io.github.coderaulia.muzikplayer.data.SongQueueController

@Composable
fun ArtistRow(
    maxTrackNumber: Int?,
    artist: Artist, songs: List<Song>,
    sideOffset: Int,
    controller: SongQueueController,
) {
    val showArtistStats = songs.size >= 3

    BigSongRow(
        maxTrackNumber,
        showTrackNumber = false,
        showAlbumInfo = true,
        showArtistInfo = false,
        songs = songs,
        sideOffset = sideOffset,
        controller = controller,
    ) {
        Text(artist.name, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
        if (showArtistStats) {
            PlayShuffleButtons(Modifier, controller, songs)
            SongCollectionStatsComposable(artist.stats)
        }
    }
}