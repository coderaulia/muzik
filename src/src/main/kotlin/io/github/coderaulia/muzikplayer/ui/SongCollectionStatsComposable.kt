package io.github.coderaulia.muzikplayer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import io.github.coderaulia.muzikplayer.data.SongCollectionStats
import io.github.coderaulia.muzikplayer.generated.resources.Res
import io.github.coderaulia.muzikplayer.generated.resources.n_songs
import io.github.coderaulia.muzikplayer.utils.format
import org.jetbrains.compose.resources.pluralStringResource

@Composable
fun SongCollectionStatsComposable(stats: SongCollectionStats, dateOnly: Boolean = false) {
    val style = MaterialTheme.typography.labelMedium.merge(
        TextStyle(
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center,
        )
    )
    if (!dateOnly) {
        SingleLineText(pluralStringResource(Res.plurals.n_songs, stats.songsCount, stats.songsCount), style = style)
        SingleLineText(stats.totalLength.format(), style = style)
    }
    if (stats.dateRange != null) {
        SingleLineText(stats.dateRange.toString(), style = style)
    }
}
