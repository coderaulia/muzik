package io.github.coderaulia.muzikplayer.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.ReadMore
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.coderaulia.muzikplayer.data.Song
import io.github.coderaulia.muzikplayer.data.SongListItem
import io.github.coderaulia.muzikplayer.data.SongQueueController
import io.github.coderaulia.muzikplayer.generated.resources.*
import io.github.coderaulia.muzikplayer.playerController
import io.github.coderaulia.muzikplayer.utils.digits
import kotlin.io.path.extension

@Composable
fun SongRow(
    modifier: Modifier,
    maxTrackNumber: Int?,
    item: SongListItem.SongListItem,
    showTrackNumber: Boolean = false,
    showAlbumInfo: Boolean = true,
    showArtistInfo: Boolean = true,
    showAlbumCover: Boolean = showAlbumInfo,
    controller: SongQueueController,
) {
    BaseSongRow(
        modifier = modifier,
        maxTrackNumber = maxTrackNumber,
        song = item.song,
        isCurrentSong = playerController.current.queue?.currentSongKey == item.song.uniqueKey,
        showTrackNumber = showTrackNumber,
        showAlbumInfo = showAlbumInfo,
        showArtistInfo = showArtistInfo,
        showAlbumCover = showAlbumCover,
        controller = controller,
        play = { shuffle ->
            controller.play(item.song, shuffle)
        },
    )
}

@Composable
fun SongRow(
    modifier: Modifier,
    maxTrackNumber: Int?,
    item: SongListItem.QueuedSongListItem,
    showTrackNumber: Boolean = false,
    showAlbumInfo: Boolean = true,
    showArtistInfo: Boolean = true,
    showAlbumCover: Boolean = showAlbumInfo,
    controller: SongQueueController,
) {
    BaseSongRow(
        modifier = modifier,
        maxTrackNumber = maxTrackNumber,
        song = item.song,
        isCurrentSong = playerController.current.queue?.position == item.indexInQueue,
        showTrackNumber = showTrackNumber,
        showAlbumInfo = showAlbumInfo,
        showArtistInfo = showArtistInfo,
        showAlbumCover = showAlbumCover,
        controller = controller,
        play = { shuffle ->
            controller.playQueued(item.song, item.indexInQueue, shuffle)
        },
    )
}

@Composable
fun songRowEstimateHeight(showInfo: Boolean = false, showAlbum: Boolean = false): Dp {
    val minHeight = 40.dp
    val padding = 16.dp
    val albumHeight = if (showAlbum) 48.dp + padding else 0.dp
    val textHeight = with(LocalDensity.current) {
        MaterialTheme.typography.titleSmall.lineHeight.toDp()
    }
    val contentHeight = padding + if (showInfo) textHeight * 2 + 4.dp else textHeight
    return maxOf(
        minHeight,
        albumHeight,
        contentHeight,
    )
}

@Composable
private fun BaseSongRow(
    modifier: Modifier,
    maxTrackNumber: Int?,
    song: Song,
    isCurrentSong: Boolean,
    showTrackNumber: Boolean,
    showAlbumInfo: Boolean,
    showArtistInfo: Boolean,
    showAlbumCover: Boolean,
    controller: SongQueueController,
    play: (shuffle: Boolean?) -> Unit,
) {
    val player = playerController.current
    ContextMenuArea(items = {
        listOf(
            ContextMenuItemWithIcon(Icons.Default.PlayCircle, Res.string.action_play) {
                play(false)
            },
            ContextMenuItemWithIcon(Icons.Default.Shuffle, Res.string.action_shuffle_from_here) {
                play(true)
            },
            ContextMenuItemWithIcon(Icons.AutoMirrored.Default.ReadMore, Res.string.action_play_next) {
                controller.playNext(song)
            },
            ContextMenuItemWithIcon(Icons.AutoMirrored.Default.PlaylistAdd, Res.string.action_add_to_queue) {
                controller.addToQueue(song)
            },
            ContextMenuItemWithIcon(Icons.Default.Album, Res.string.action_play_album) {
                controller.playAlbum(song)
            },
            ContextMenuItemWithIcon(Icons.Default.Groups, Res.string.action_play_artist) {
                controller.playArtist(song)
            },
        )
    }) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(6.dp),
            color = if (isCurrentSong) Color(0xFF2A2A2A) else Color.Transparent,
        ) {
            Row(
                Modifier
                    .clickable { play(null) }
                    .heightIn(min = 44.dp)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (isCurrentSong) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .height(26.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF4691F2))
                    )
                }

                if (showTrackNumber) {
                    val maxDigits = maxTrackNumber?.digits() ?: 0
                    if (isCurrentSong) {
                        SmallFakeSpectrometers(
                            Modifier.size(16.dp),
                            player,
                            color = Color(0xFF4691F2),
                        )
                    } else {
                        SingleLineText(
                            song.track?.toString().orEmpty().padStart(maxDigits, ' '),
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }

                if (showAlbumCover) {
                    AlbumCover(
                        song.cover,
                        Modifier.size(40.dp),
                        RoundedCornerShape(6.dp),
                        elevation = 2.dp,
                        overlay = {
                            if (isCurrentSong && !showTrackNumber) {
                                SmallFakeSpectrometers(
                                    Modifier.fillMaxSize()
                                        .background(Color(0xFF4691F2).copy(alpha = 0.8f))
                                        .padding(8.dp),
                                    player,
                                    color = Color.White,
                                )
                            }
                        }
                    )
                }

                if (showAlbumInfo || showArtistInfo) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            song.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isCurrentSong) Color(0xFF4691F2) else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val style = MaterialTheme.typography.labelSmall
                            if (showArtistInfo) {
                                SingleLineText(song.artist.name, style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (showAlbumInfo && showArtistInfo) {
                                SingleLineText(" • ", style = style, color = MaterialTheme.colorScheme.outline)
                            }
                            if (showAlbumInfo) {
                                SingleLineText(song.album.title, style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isCurrentSong) Color(0xFF4691F2) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Audio format badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF4A16D1).copy(alpha = 0.25f),
                ) {
                    Text(
                        song.file.extension.uppercase(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                        ),
                        color = Color(0xFFCABEFF),
                    )
                }

                SingleLineText(
                    song.formattedLength,
                    color = if (isCurrentSong) Color(0xFF4691F2) else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                )
            }
        }
    }
}
