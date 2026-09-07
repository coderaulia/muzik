package io.github.coderaulia.muzikplayer.data

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.io.path.readLines
import kotlin.io.path.readText

class PlaylistStoreTest : FunSpec({
    test("create validates names and writes an M3U file") {
        val directory = Files.createTempDirectory("muzik-playlist-test")
        val file = runBlocking { PlaylistStore.create(directory, "  Favorites.m3u  ") }
        file.fileName.toString() shouldBe "Favorites.m3u"
        file.readText() shouldBe "#EXTM3U\n"
        shouldThrow<IllegalArgumentException> { runBlocking { PlaylistStore.create(directory, "Favorites") } }
        shouldThrow<IllegalArgumentException> { runBlocking { PlaylistStore.create(directory, "../escape") } }
    }

    test("append avoids duplicates within existing and new selections") {
        val directory = Files.createTempDirectory("muzik-playlist-test")
        val file = directory.resolve("Mix.m3u")
        Files.writeString(file, "#EXTM3U\n" + directory.resolve("one.mp3") + "\n")
        val existing = Playlist("Mix", file, listOf(SongKey(directory.resolve("one.mp3"))))
        val selected = listOf(songAt(directory, "one.mp3"), songAt(directory, "two.mp3"), songAt(directory, "two.mp3"))
        runBlocking { PlaylistStore.append(existing, selected) }
        file.readLines().drop(1) shouldContainExactly listOf(directory.resolve("one.mp3").toString(), directory.resolve("two.mp3").toString())
    }

    test("rename rejects collisions and export copies the playlist") {
        val directory = Files.createTempDirectory("muzik-playlist-test")
        val file = directory.resolve("Mix.m3u")
        Files.writeString(file, "#EXTM3U\n")
        Files.writeString(directory.resolve("Other.m3u"), "#EXTM3U\n")
        val playlist = Playlist("Mix", file, emptyList())
        shouldThrow<IllegalArgumentException> { runBlocking { PlaylistStore.rename(playlist, "Other") } }
        val exported = runBlocking { PlaylistStore.export(playlist, directory.resolve("exports")) }
        exported.readText() shouldBe "#EXTM3U\n"
    }
})

private fun songAt(directory: java.nio.file.Path, name: String): Song {
    val artist = Artist("Artist", SongCollectionStats(0, kotlin.time.Duration.ZERO, null, null))
    val album = Album("Album", artist, null, SongCollectionStats(0, kotlin.time.Duration.ZERO, null, null))
    return Song(directory.resolve(name), null, null, name, album, null, null, kotlin.time.Duration.ZERO, null, "0:00")
}
