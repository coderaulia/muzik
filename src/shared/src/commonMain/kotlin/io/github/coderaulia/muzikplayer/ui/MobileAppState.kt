package io.github.coderaulia.muzikplayer.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.coderaulia.muzikplayer.audio.DEFAULT_EQ_PRESETS
import io.github.coderaulia.muzikplayer.audio.SHARED_EQ_BANDS
import io.github.coderaulia.muzikplayer.platform.MediaUri
import io.github.coderaulia.muzikplayer.platform.SharedAlbum
import io.github.coderaulia.muzikplayer.platform.SharedArtist
import io.github.coderaulia.muzikplayer.platform.SharedLyricsLine
import io.github.coderaulia.muzikplayer.platform.SharedLyricsParser
import io.github.coderaulia.muzikplayer.platform.SharedPlaylist
import io.github.coderaulia.muzikplayer.platform.SharedRepeatMode
import io.github.coderaulia.muzikplayer.platform.SharedTrack
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO

enum class MobileTab(val label: String) {
    HOME("Now Playing"),
    LIBRARY("Library"),
    PLAYLISTS("Playlists"),
    EQUALIZER("Equalizer"),
    SETTINGS("Settings"),
}

enum class MobileLibrarySection(val label: String) {
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    FAVORITES("Favorites"),
}

enum class MobileSortField(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album"),
    DURATION("Duration"),
}

enum class MobileInspectorSubTab(val label: String) {
    LYRICS("Lyrics"),
    QUEUE("Up Next"),
    DETAILS("Audio Info"),
}

/**
 * Shared reactive controller for the Android Mobile & Multiplatform Compact UI.
 * Exposes 1:1 functionality with Desktop MuzikPlayer:
 * - Library browsing (Songs, Albums, Artists, Favorites, Search, Sort)
 * - Queue management (Play, Pause, Seek, Next, Previous, Shuffle, Repeat, Reorder/Remove)
 * - M3U Playlists (Create, Append Song, Remove Song, Rename, Delete)
 * - 10-band Parametric Equalizer & Presets
 * - Synchronized LRC & Plain Lyrics with live offset adjustment
 * - Settings (SAF Library Folders, Theme Dark/Light/System, ReplayGain, Peak Protection, MediaSession)
 */
@Stable
class MobileMuzikController(
    initialTracks: List<SharedTrack> = emptyList(),
    initialPlaylists: List<SharedPlaylist> = emptyList(),
    initialFolders: List<String> = listOf("Internal Storage / Music"),
    private val onPlayTrack: (SharedTrack, Duration) -> Unit = { _, _ -> },
    private val onSetPlaying: (Boolean) -> Unit = {},
    private val onSeekTo: (Duration) -> Unit = {},
    private val onSetVolume: (Float) -> Unit = {},
    private val onUpdateEqualizer: (Boolean, List<Float>) -> Unit = { _, _ -> },
    private val onRequestAddFolder: () -> Unit = {},
    private val onRequestRescan: () -> Unit = {},
    private val onPersistPlaylist: (SharedPlaylist) -> Unit = {},
    private val onDeletePlaylist: (SharedPlaylist) -> Unit = {},
) {
    var activeTab by mutableStateOf(MobileTab.LIBRARY)
    var librarySection by mutableStateOf(MobileLibrarySection.SONGS)
    var sortField by mutableStateOf(MobileSortField.TITLE)
    var inspectorSubTab by mutableStateOf(MobileInspectorSubTab.LYRICS)
    var searchQuery by mutableStateOf("")

    val allTracks = mutableStateListOf<SharedTrack>().apply { addAll(initialTracks) }
    val favoriteTrackIds = mutableStateListOf<String>()
    val playlists = mutableStateListOf<SharedPlaylist>().apply { addAll(initialPlaylists) }
    val libraryFolders = mutableStateListOf<String>().apply { addAll(initialFolders) }

    // Queue & transport state
    val queue = mutableStateListOf<SharedTrack>()
    private val unshuffledQueue = mutableStateListOf<SharedTrack>()
    var currentQueueIndex by mutableIntStateOf(-1)
    var isPlaying by mutableStateOf(false)
    var shuffleEnabled by mutableStateOf(false)
    var repeatMode by mutableStateOf(SharedRepeatMode.OFF)
    var currentPosition by mutableStateOf(ZERO)
    var volume by mutableFloatStateOf(1f)

    // Equalizer & DSP state
    var equalizerEnabled by mutableStateOf(false)
    val equalizerGains = mutableStateListOf<Float>().apply {
        repeat(SHARED_EQ_BANDS.size) { add(0f) }
    }
    var selectedPresetName by mutableStateOf("Flat")
    var peakProtection by mutableStateOf(true)
    var replayGainEnabled by mutableStateOf(true)
    var gaplessPlayback by mutableStateOf(true)
    var lyricsOffsetMs by mutableIntStateOf(0)

    // Appearance & system integration state
    var darkThemeMode by mutableStateOf(true)
    var dynamicCoverAccent by mutableStateOf(true)
    var showTrackThumbnails by mutableStateOf(true)
    var mediaSessionEnabled by mutableStateOf(true)
    var hasAudioPermission by mutableStateOf(true)

    val currentTrack: SharedTrack?
        get() = queue.getOrNull(currentQueueIndex)

    val filteredTracks: List<SharedTrack>
        get() {
            val base = when (librarySection) {
                MobileLibrarySection.FAVORITES -> allTracks.filter { it.id in favoriteTrackIds }
                else -> allTracks.toList()
            }
            val searched = if (searchQuery.isBlank()) {
                base
            } else {
                val q = searchQuery.trim().lowercase()
                base.filter {
                    it.title.lowercase().contains(q) ||
                        it.artist.lowercase().contains(q) ||
                        it.album.lowercase().contains(q)
                }
            }
            return when (sortField) {
                MobileSortField.TITLE -> searched.sortedBy { it.title.lowercase() }
                MobileSortField.ARTIST -> searched.sortedWith(compareBy({ it.artist.lowercase() }, { it.album.lowercase() }, { it.trackNumber ?: 0 }))
                MobileSortField.ALBUM -> searched.sortedWith(compareBy({ it.album.lowercase() }, { it.discNumber ?: 1 }, { it.trackNumber ?: 0 }))
                MobileSortField.DURATION -> searched.sortedByDescending { it.duration }
            }
        }

    val albums: List<SharedAlbum>
        get() = allTracks
            .groupBy { it.album to it.artist }
            .map { (key, tracks) ->
                val sorted = tracks.sortedWith(compareBy({ it.discNumber ?: 1 }, { it.trackNumber ?: 0 }, { it.title }))
                SharedAlbum(
                    name = key.first,
                    artist = key.second,
                    year = sorted.firstNotNullOfOrNull { it.year },
                    tracks = sorted,
                )
            }
            .sortedBy { it.name.lowercase() }

    val artists: List<SharedArtist>
        get() = allTracks
            .groupBy { it.artist }
            .map { (artistName, tracks) ->
                val artistAlbums = tracks.groupBy { it.album }.map { (albumName, albumTracks) ->
                    SharedAlbum(
                        name = albumName,
                        artist = artistName,
                        year = albumTracks.firstNotNullOfOrNull { it.year },
                        tracks = albumTracks.sortedBy { it.trackNumber ?: 0 },
                    )
                }
                SharedArtist(
                    name = artistName,
                    albums = artistAlbums,
                    tracks = tracks,
                )
            }
            .sortedBy { it.name.lowercase() }

    val currentSynchronizedLyrics: List<SharedLyricsLine>
        get() = SharedLyricsParser.parseSynchronized(currentTrack?.lyricsRaw, lyricsOffsetMs)

    fun updateLibraryTracks(newTracks: List<SharedTrack>) {
        allTracks.clear()
        allTracks.addAll(newTracks)
        if (queue.isEmpty() && newTracks.isNotEmpty()) {
            queue.addAll(newTracks)
            unshuffledQueue.clear()
            unshuffledQueue.addAll(newTracks)
            currentQueueIndex = 0
        }
    }

    fun playTrackFromList(track: SharedTrack, sourceList: List<SharedTrack> = filteredTracks) {
        val list = if (sourceList.isEmpty()) listOf(track) else sourceList
        unshuffledQueue.clear()
        unshuffledQueue.addAll(list)
        queue.clear()
        if (shuffleEnabled) {
            val rest = list.filter { it.id != track.id }.shuffled()
            queue.add(track)
            queue.addAll(rest)
            currentQueueIndex = 0
        } else {
            queue.addAll(list)
            currentQueueIndex = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        }
        currentPosition = ZERO
        isPlaying = true
        onPlayTrack(track, ZERO)
        onSetPlaying(true)
    }

    fun shuffleAndPlay(sourceList: List<SharedTrack> = filteredTracks) {
        if (sourceList.isEmpty()) return
        shuffleEnabled = true
        val shuffled = sourceList.shuffled()
        unshuffledQueue.clear()
        unshuffledQueue.addAll(sourceList)
        queue.clear()
        queue.addAll(shuffled)
        currentQueueIndex = 0
        currentPosition = ZERO
        isPlaying = true
        onPlayTrack(shuffled.first(), ZERO)
        onSetPlaying(true)
    }

    fun togglePlayPause() {
        if (currentTrack == null && allTracks.isNotEmpty()) {
            playTrackFromList(allTracks.first(), allTracks)
            return
        }
        if (currentTrack == null) return
        isPlaying = !isPlaying
        onSetPlaying(isPlaying)
    }

    fun skipNext(fromAutoAdvance: Boolean = false) {
        if (queue.isEmpty()) return
        if (fromAutoAdvance && repeatMode == SharedRepeatMode.ONE) {
            seekTo(ZERO)
            isPlaying = true
            currentTrack?.let { onPlayTrack(it, ZERO) }
            return
        }
        val nextIndex = currentQueueIndex + 1
        if (nextIndex < queue.size) {
            currentQueueIndex = nextIndex
            currentPosition = ZERO
            isPlaying = true
            onPlayTrack(queue[currentQueueIndex], ZERO)
            onSetPlaying(true)
        } else if (repeatMode == SharedRepeatMode.ALL) {
            currentQueueIndex = 0
            currentPosition = ZERO
            isPlaying = true
            onPlayTrack(queue[0], ZERO)
            onSetPlaying(true)
        } else if (fromAutoAdvance) {
            isPlaying = false
            currentPosition = ZERO
            onSetPlaying(false)
        }
    }

    fun skipPrevious() {
        if (queue.isEmpty()) return
        if (currentPosition.inWholeSeconds > 3L) {
            seekTo(ZERO)
            return
        }
        val prevIndex = currentQueueIndex - 1
        if (prevIndex >= 0) {
            currentQueueIndex = prevIndex
            currentPosition = ZERO
            isPlaying = true
            onPlayTrack(queue[currentQueueIndex], ZERO)
            onSetPlaying(true)
        } else if (repeatMode == SharedRepeatMode.ALL) {
            currentQueueIndex = queue.lastIndex
            currentPosition = ZERO
            isPlaying = true
            onPlayTrack(queue[currentQueueIndex], ZERO)
            onSetPlaying(true)
        } else {
            seekTo(ZERO)
        }
    }

    fun toggleShuffle() {
        val current = currentTrack
        shuffleEnabled = !shuffleEnabled
        if (queue.isEmpty()) return
        if (shuffleEnabled) {
            val rest = queue.filter { it.id != current?.id }.shuffled()
            queue.clear()
            if (current != null) queue.add(current)
            queue.addAll(rest)
            currentQueueIndex = 0
        } else if (unshuffledQueue.isNotEmpty()) {
            queue.clear()
            queue.addAll(unshuffledQueue)
            currentQueueIndex = if (current != null) {
                queue.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
            } else {
                0
            }
        }
    }

    fun cycleRepeatMode() {
        repeatMode = when (repeatMode) {
            SharedRepeatMode.OFF -> SharedRepeatMode.ALL
            SharedRepeatMode.ALL -> SharedRepeatMode.ONE
            SharedRepeatMode.ONE -> SharedRepeatMode.OFF
        }
    }

    fun seekTo(target: Duration) {
        val duration = currentTrack?.duration ?: ZERO
        val clamped = target.coerceIn(ZERO, if (duration > ZERO) duration else target.coerceAtLeast(ZERO))
        currentPosition = clamped
        onSeekTo(clamped)
    }

    fun updateVolume(newVolume: Float) {
        volume = newVolume.coerceIn(0f, 1f)
        onSetVolume(volume)
    }

    fun toggleFavorite(trackId: String) {
        if (trackId in favoriteTrackIds) {
            favoriteTrackIds.remove(trackId)
        } else {
            favoriteTrackIds.add(trackId)
        }
    }

    fun playNextInQueue(track: SharedTrack) {
        if (queue.isEmpty()) {
            playTrackFromList(track, listOf(track))
            return
        }
        val insertIndex = (currentQueueIndex + 1).coerceIn(0, queue.size)
        queue.add(insertIndex, track)
    }

    fun removeFromQueue(index: Int) {
        if (index !in queue.indices) return
        queue.removeAt(index)
        if (queue.isEmpty()) {
            currentQueueIndex = -1
            isPlaying = false
            onSetPlaying(false)
        } else if (index < currentQueueIndex) {
            currentQueueIndex--
        } else if (index == currentQueueIndex) {
            currentQueueIndex = currentQueueIndex.coerceAtMost(queue.lastIndex)
            currentPosition = ZERO
            currentTrack?.let { onPlayTrack(it, ZERO) }
        }
    }

    fun createPlaylist(name: String): Result<SharedPlaylist> {
        val trimmed = name.trim().removeSuffix(".m3u").removeSuffix(".M3U").trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Playlist name cannot be empty"))
        }
        if (trimmed.any { it == '/' || it == '\\' }) {
            return Result.failure(IllegalArgumentException("Playlist name cannot contain path separators"))
        }
        if (playlists.any { it.name.equals(trimmed, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("A playlist with that name already exists"))
        }
        val created = SharedPlaylist(
            id = "pl_${trimmed.lowercase().replace(' ', '_')}_${playlists.size + 1}",
            name = trimmed,
            uri = MediaUri("playlist://$trimmed.m3u"),
            trackIds = emptyList(),
        )
        playlists.add(created)
        onPersistPlaylist(created)
        return Result.success(created)
    }

    fun addTrackToPlaylist(playlistId: String, trackId: String) {
        val idx = playlists.indexOfFirst { it.id == playlistId }
        if (idx < 0) return
        val existing = playlists[idx]
        if (trackId !in existing.trackIds) {
            val updated = existing.copy(trackIds = existing.trackIds + trackId)
            playlists[idx] = updated
            onPersistPlaylist(updated)
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        val idx = playlists.indexOfFirst { it.id == playlistId }
        if (idx < 0) return
        val existing = playlists[idx]
        val updated = existing.copy(trackIds = existing.trackIds - trackId)
        playlists[idx] = updated
        onPersistPlaylist(updated)
    }

    fun deletePlaylist(playlistId: String) {
        val existing = playlists.firstOrNull { it.id == playlistId } ?: return
        playlists.remove(existing)
        onDeletePlaylist(existing)
    }

    fun setEqualizerBand(bandIndex: Int, gainDb: Float) {
        if (bandIndex !in equalizerGains.indices) return
        equalizerGains[bandIndex] = gainDb.coerceIn(-12f, 12f)
        selectedPresetName = "Custom"
        onUpdateEqualizer(equalizerEnabled, equalizerGains.toList())
    }

    fun applyEqualizerPreset(presetName: String) {
        val preset = DEFAULT_EQ_PRESETS.firstOrNull { it.name == presetName } ?: return
        selectedPresetName = preset.name
        for (i in equalizerGains.indices) {
            equalizerGains[i] = preset.gainsDb.getOrElse(i) { 0f }
        }
        onUpdateEqualizer(equalizerEnabled, equalizerGains.toList())
    }

    fun toggleEqualizer(enabled: Boolean) {
        equalizerEnabled = enabled
        onUpdateEqualizer(equalizerEnabled, equalizerGains.toList())
    }

    fun requestAddLibraryFolder() = onRequestAddFolder()

    fun requestLibraryRescan() = onRequestRescan()
}
