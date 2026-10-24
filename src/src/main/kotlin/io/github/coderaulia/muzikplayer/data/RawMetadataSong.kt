package io.github.coderaulia.muzikplayer.data

import io.github.coderaulia.muzikplayer.utils.formatSuspend
import io.github.coderaulia.muzikplayer.utils.trimToNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jetbrains.compose.resources.ResourceEnvironment
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class RawMetadataSong(
    val file: Path,
    override val disk: Int?,
    override val track: Int?,
    override val length: Duration,
    val formattedLength: String,
    val title: String?,
    val album: String?,
    val artist: String?,
    val albumArtist: String?,
    override val date: PartialDate?,
    val cover: Deferred<AlbumCover?>,
    val lyrics: Lyrics?,
    val rawDateStr: String? = null,
    val rawArtworkBytes: ByteArray? = null,
) : BaseSong {
    val nnTitle get() = title ?: file.nameWithoutExtension
    val nnAlbum get() = album ?: "Unknown"
    val nnArtist get() = artist ?: "Unknown"
    val nnAlbumArtist get() = albumArtist ?: nnArtist

    companion object {

        suspend fun fromMusicFile(
            file: Path,
            decoder: CoversDecoder,
            env: Deferred<ResourceEnvironment>,
            partialDateParserCache: PartialDateParserCache,
        ): RawMetadataSong {
            val f = AudioFileIO.read(file.toFile())
            val tag = f.tag
            val header = f.audioHeader

            val dateStr = tag?.getFirst(FieldKey.YEAR)
            val date = dateStr?.let { PartialDate.parse(it, partialDateParserCache) }?.let { date ->
                // Unfortunately, a lot of songs say 1st of January to mean the year.
                if (date is PartialDate.Date && date.date.dayOfYear == 1) {
                    date.year
                } else {
                    date
                }
            }

            val length = header.preciseTrackLength.seconds
            val artworkBytes = tag?.firstArtwork?.binaryData
            val cover = artworkBytes?.let { decoder.decode(it) } ?: CompletableDeferred(value = null)
            val formattedLength = length.formatSuspend(env.await())
            return RawMetadataSong(
                file = file,
                disk = tag?.getFirst(FieldKey.DISC_NO)?.toIntOrNull(),
                track = tag?.getFirst(FieldKey.TRACK)?.toIntOrNull(),
                length = length,
                formattedLength = formattedLength,
                title = tag?.getFirst(FieldKey.TITLE)?.trimToNull(),
                album = tag?.getFirst(FieldKey.ALBUM)?.trimToNull(),
                artist = tag?.getFirst(FieldKey.ARTIST)?.trimToNull(),
                albumArtist = tag?.getFirst(FieldKey.ALBUM_ARTIST)?.trimToNull(),
                date = date,
                cover = cover,
                lyrics = Lyrics.of(tag?.getFirst(FieldKey.LYRICS)),
                rawDateStr = dateStr,
                rawArtworkBytes = artworkBytes,
            )
        }

        suspend fun fromCachedEntry(
            file: Path,
            entry: CachedSongEntry,
            decoder: CoversDecoder,
            coverBytes: ByteArray?,
            partialDateParserCache: PartialDateParserCache,
        ): RawMetadataSong {
            val date = entry.dateStr?.let { str ->
                PartialDate.parse(str, partialDateParserCache)?.let { date ->
                    if (date is PartialDate.Date && date.date.dayOfYear == 1) {
                        date.year
                    } else {
                        date
                    }
                }
            }
            val cover: Deferred<AlbumCover?> = if (entry.hasArtwork && coverBytes != null) {
                decoder.decode(coverBytes)
            } else {
                CompletableDeferred(null)
            }
            return RawMetadataSong(
                file = file,
                disk = entry.disk,
                track = entry.track,
                length = entry.lengthSeconds.seconds,
                formattedLength = entry.formattedLength,
                title = entry.title,
                album = entry.album,
                artist = entry.artist,
                albumArtist = entry.albumArtist,
                date = date,
                cover = cover,
                lyrics = Lyrics.of(entry.lyricsRaw),
                rawDateStr = entry.dateStr,
                rawArtworkBytes = coverBytes,
            )
        }
    }
}
