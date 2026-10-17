package io.github.coderaulia.muzikplayer.ui

import io.github.coderaulia.muzikplayer.platform.MediaUri
import io.github.coderaulia.muzikplayer.platform.SharedRepeatMode
import io.github.coderaulia.muzikplayer.platform.SharedTrack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class MobileMuzikControllerTest {

    private val sampleTracks = listOf(
        SharedTrack(
            id = "1",
            uri = MediaUri("content://media/external/audio/media/1"),
            title = "Aurora",
            artist = "Solaris",
            album = "Horizon",
            duration = 210.seconds,
            trackNumber = 1,
            year = 2025,
        ),
        SharedTrack(
            id = "2",
            uri = MediaUri("content://media/external/audio/media/2"),
            title = "Nebula",
            artist = "Solaris",
            album = "Horizon",
            duration = 195.seconds,
            trackNumber = 2,
            year = 2025,
        ),
        SharedTrack(
            id = "3",
            uri = MediaUri("content://media/external/audio/media/3"),
            title = "Zenith",
            artist = "Aether",
            album = "Pulse",
            duration = 240.seconds,
            trackNumber = 1,
            year = 2026,
        ),
    )

    @Test
    fun groupsAlbumsAndArtistsAndFiltersBySearch() {
        val controller = MobileMuzikController(initialTracks = sampleTracks)
        assertEquals(2, controller.albums.size)
        assertEquals(2, controller.artists.size)

        controller.searchQuery = "nebula"
        assertEquals(1, controller.filteredTracks.size)
        assertEquals("Nebula", controller.filteredTracks.single().title)
    }

    @Test
    fun handlesQueuePlaybackBoundariesAndRepeatModes() {
        val controller = MobileMuzikController(initialTracks = sampleTracks)
        controller.playTrackFromList(sampleTracks[0], sampleTracks)
        assertTrue(controller.isPlaying)
        assertEquals("1", controller.currentTrack?.id)

        controller.skipNext()
        assertEquals("2", controller.currentTrack?.id)

        controller.skipNext()
        assertEquals("3", controller.currentTrack?.id)

        // End of queue with Repeat OFF stops auto-advance
        controller.skipNext(fromAutoAdvance = true)
        assertFalse(controller.isPlaying)

        // Repeat ALL wraps to first track
        controller.repeatMode = SharedRepeatMode.ALL
        controller.skipNext()
        assertEquals("1", controller.currentTrack?.id)
        assertTrue(controller.isPlaying)
    }

    @Test
    fun validatesM3uPlaylistCreationAndPreventsDuplicates() {
        val controller = MobileMuzikController(initialTracks = sampleTracks)
        assertTrue(controller.createPlaylist("   ").isFailure)
        assertTrue(controller.createPlaylist("bad/name").isFailure)

        val created = controller.createPlaylist("Late Night.m3u").getOrThrow()
        assertEquals("Late Night", created.name)
        assertTrue(controller.createPlaylist("late night").isFailure)

        controller.addTrackToPlaylist(created.id, "1")
        controller.addTrackToPlaylist(created.id, "1") // Duplicate ignored
        assertEquals(listOf("1"), controller.playlists.single().trackIds)
    }

    @Test
    fun appliesEqualizerPresetsAndCustomBands() {
        var lastGains: List<Float> = emptyList()
        val controller = MobileMuzikController(
            initialTracks = sampleTracks,
            onUpdateEqualizer = { _, gains -> lastGains = gains },
        )
        controller.toggleEqualizer(true)
        controller.applyEqualizerPreset("Bass Boost")
        assertEquals("Bass Boost", controller.selectedPresetName)
        assertEquals(6f, lastGains.first())

        controller.setEqualizerBand(0, 9.5f)
        assertEquals("Custom", controller.selectedPresetName)
        assertEquals(9.5f, controller.equalizerGains[0])
    }
}
