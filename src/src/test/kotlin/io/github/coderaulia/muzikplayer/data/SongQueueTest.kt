package io.github.coderaulia.muzikplayer.data

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.seconds

class SongQueueTest : FunSpec({
    val songs = (1..4).map { song(it) }

    test("previous and next wrap at both queue boundaries") {
        val queue = queueOf(songs, 0)
        queue.previous().currentSongKey shouldBe songs.last().uniqueKey
        queue.next().next().next().next().currentSongKey shouldBe songs.first().uniqueKey
    }

    test("do not repeat stops after the final song") {
        val queue = queueOf(songs, songs.lastIndex, RepeatMode.DO_NOT_REPEAT)
        val (next, keepPlaying) = queue.nextInQueue()
        next.currentSongKey shouldBe songs.first().uniqueKey
        keepPlaying shouldBe false
    }

    test("repeat song keeps the current song") {
        val queue = queueOf(songs, 2, RepeatMode.REPEAT_SONG)
        val (next, keepPlaying) = queue.nextInQueue()
        next shouldBe queue
        keepPlaying shouldBe true
    }

    test("shuffle keeps the current song first and can be restored") {
        val queue = queueOf(songs, 2)
        val shuffled = queue.shuffled()
        shuffled.currentSongKey shouldBe songs[2].uniqueKey
        shuffled.songs.toSet() shouldBe queue.songs.toSet()
        shuffled.unshuffled().songs shouldBe queue.originalSongs
        shuffled.unshuffled().currentSongKey shouldBe songs[2].uniqueKey
    }
})

private fun song(number: Int): Song {
    val artist = Artist("Artist", SongCollectionStats(0, kotlin.time.Duration.ZERO, null, null))
    val album = Album("Album", artist, null, SongCollectionStats(0, kotlin.time.Duration.ZERO, null, null))
    return Song(Path("song-$number.mp3"), null, number, "Song $number", album, null, null, number.seconds, null, "$number:00")
}

private fun queueOf(songs: List<Song>, position: Int, repeatMode: RepeatMode = RepeatMode.DEFAULT) = SongQueue(
    originalSongs = songs.map { it.uniqueKey }, songs = songs.map { it.uniqueKey }, position = position,
    songsByKey = songs.associateBy { it.uniqueKey }, repeatMode = repeatMode,
)
