package io.github.coderaulia.muzikplayer.data

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class LyricsTest : FunSpec({
    context("toSynchronizedLine") {
        withData(
            LrcFile.Entry("", "", null) to null,
            LrcFile.Entry("", "", "asd") to null,
            LrcFile.Entry("12", "34", null) to null,
            LrcFile.Entry("12", "34", "asd") to 12.minutes + 34.seconds,
            LrcFile.Entry("", "56", "asd") to 56.seconds,
            LrcFile.Entry("1", "2.34", "asd") to 1.minutes + 2.seconds + 340.milliseconds,
            LrcFile.Entry("1", "a.34", "asd") to null,
            LrcFile.Entry("1", "3.a", "asd") to 1.minutes + 3.seconds,
            LrcFile.Entry("1", "3.", "asd") to 1.minutes + 3.seconds,
            LrcFile.Entry("1", "3", "asd") to 1.minutes + 3.seconds,
        ) { (entry, expected) ->
            entry.toSynchronizedLine(0)?.start shouldBe expected
            if (expected != null) {
                entry.toSynchronizedLine(100)?.start shouldBe expected - 100.milliseconds
                entry.toSynchronizedLine(-300)?.start shouldBe expected + 300.milliseconds
            }
        }
    }

    context("multi-timestamp lines") {
        test("parses multi-timestamp lines and sorts chronologically") {
            val lrc = """
                [00:10.00][00:30.00]Refrain
                [00:20.00]Verse
            """.trimIndent()
            val lyrics = Lyrics.of(lrc)
            lyrics.shouldNotBeNull()
            (lyrics is Lyrics.Synchronized) shouldBe true
            val sync = lyrics as Lyrics.Synchronized
            sync.lines.size shouldBe 3
            sync.lines[0].start shouldBe 10.seconds
            sync.lines[0].content shouldBe "Refrain"
            sync.lines[1].start shouldBe 20.seconds
            sync.lines[1].content shouldBe "Verse"
            sync.lines[2].start shouldBe 30.seconds
            sync.lines[2].content shouldBe "Refrain"
        }

        test("parses multiple timestamps with brackets back-to-back") {
            val lrc = "[00:10.00][00:20.00][00:30.00]Repeated Chorus"
            val lyrics = Lyrics.of(lrc)
            lyrics.shouldNotBeNull()
            (lyrics is Lyrics.Synchronized) shouldBe true
            val sync = lyrics as Lyrics.Synchronized
            sync.lines.size shouldBe 3
            sync.lines.map { it.start } shouldBe listOf(10.seconds, 20.seconds, 30.seconds)
            sync.lines.map { it.content } shouldBe listOf("Repeated Chorus", "Repeated Chorus", "Repeated Chorus")
        }
    }

    context("fractional seconds precision") {
        test("3-digit millisecond precision") {
            val lrc = "[01:02.345]Line"
            val lyrics = Lyrics.of(lrc)
            lyrics.shouldNotBeNull()
            (lyrics is Lyrics.Synchronized) shouldBe true
            val sync = lyrics as Lyrics.Synchronized
            sync.lines.single().start shouldBe 62_345.milliseconds
            sync.lines.single().content shouldBe "Line"
        }

        test("handles 1, 2, and 3 digit fractional seconds") {
            val e1 = LrcFile.Entry("0", "1.5", "one digit")
            e1.toSynchronizedLine(0)?.start shouldBe 1500.milliseconds

            val e2 = LrcFile.Entry("0", "1.50", "two digits")
            e2.toSynchronizedLine(0)?.start shouldBe 1500.milliseconds

            val e3 = LrcFile.Entry("0", "1.500", "three digits")
            e3.toSynchronizedLine(0)?.start shouldBe 1500.milliseconds

            val colonFraction = LrcFile.Entry("1", "2:345", "colon fraction")
            colonFraction.toSynchronizedLine(0)?.start shouldBe 62_345.milliseconds
        }
    }

    context("inline word-sync tags") {
        test("strips inline timestamps like <00:10.50>") {
            val lrc = "[00:10.00]Hello <00:10.50>world"
            val lyrics = Lyrics.of(lrc)
            lyrics.shouldNotBeNull()
            (lyrics is Lyrics.Synchronized) shouldBe true
            val sync = lyrics as Lyrics.Synchronized
            sync.lines.single().content shouldBe "Hello world"
        }

        test("strips leading and multiple inline timestamps") {
            val lrc = "[00:10.00]<00:10.00>Hello <00:10.50>beautiful <00:11.00>world"
            val lyrics = Lyrics.of(lrc)
            lyrics.shouldNotBeNull()
            (lyrics is Lyrics.Synchronized) shouldBe true
            val sync = lyrics as Lyrics.Synchronized
            sync.lines.single().content shouldBe "Hello beautiful world"
        }
    }

    context("LyricsRepository.parseLrclibJson") {
        test("extracts syncedLyrics") {
            val json = """
                {
                  "id": 100,
                  "trackName": "Track",
                  "artistName": "Artist",
                  "instrumental": false,
                  "plainLyrics": "Plain fallback",
                  "syncedLyrics": "[00:10.00]Synced lyric\n[00:20.00]Second line"
                }
            """.trimIndent()
            val lyrics = LyricsRepository.parseLrclibJson(json)
            lyrics.shouldNotBeNull()
            (lyrics is Lyrics.Synchronized) shouldBe true
            val sync = lyrics as Lyrics.Synchronized
            sync.lines.size shouldBe 2
            sync.lines[0].start shouldBe 10.seconds
            sync.lines[0].content shouldBe "Synced lyric"
            sync.lines[1].start shouldBe 20.seconds
            sync.lines[1].content shouldBe "Second line"
        }

        test("falls back to plainLyrics when syncedLyrics is null") {
            val json = """
                {
                  "id": 101,
                  "trackName": "Track",
                  "artistName": "Artist",
                  "instrumental": false,
                  "plainLyrics": "Plain line 1\nPlain line 2",
                  "syncedLyrics": null
                }
            """.trimIndent()
            val lyrics = LyricsRepository.parseLrclibJson(json)
            lyrics.shouldNotBeNull()
            (lyrics is Lyrics.Plain) shouldBe true
            val plain = lyrics as Lyrics.Plain
            plain.lyrics shouldBe "Plain line 1\nPlain line 2"
        }

        test("returns null for instrumental tracks") {
            val json = """
                {
                  "id": 102,
                  "trackName": "Track",
                  "artistName": "Artist",
                  "instrumental": true,
                  "plainLyrics": null,
                  "syncedLyrics": null
                }
            """.trimIndent()
            LyricsRepository.parseLrclibJson(json).shouldBeNull()
        }

        test("handles JSON string escape sequences correctly") {
            val json = """
                {
                  "instrumental": false,
                  "syncedLyrics": "[00:05.00]Quote: \"Hello\" and slash: \\ and unicode: \u0041"
                }
            """.trimIndent()
            val lyrics = LyricsRepository.parseLrclibJson(json)
            lyrics.shouldNotBeNull()
            (lyrics is Lyrics.Synchronized) shouldBe true
            val sync = lyrics as Lyrics.Synchronized
            sync.lines.single().content shouldBe "Quote: \"Hello\" and slash: \\ and unicode: A"
        }
    }

    context("LyricsRepository.resolveLyrics") {
        test("returns embedded song lyrics when present") {
            LyricsRepository.clearMemoryCache()
            val embedded = Lyrics.Plain("Embedded lyrics")
            val s = testSong(1, lyrics = embedded)
            val resolved = LyricsRepository.resolveLyrics(s, allowNetworkFetch = false)
            resolved shouldBe embedded
            LyricsRepository.memoryCache[s.uniqueKey] shouldBe embedded
        }

        test("reads sidecar .lrc file if present") {
            LyricsRepository.clearMemoryCache()
            val tempDir = Files.createTempDirectory("lyrics_sidecar_test")
            try {
                val songPath = tempDir.resolve("track.mp3")
                Files.createFile(songPath)
                val lrcPath = tempDir.resolve("track.lrc")
                Files.writeString(lrcPath, "[00:05.00]Sidecar line")

                val s = testSong(2, file = songPath)
                val resolved = LyricsRepository.resolveLyrics(s, allowNetworkFetch = false)
                resolved.shouldNotBeNull()
                (resolved is Lyrics.Synchronized) shouldBe true
                (resolved as Lyrics.Synchronized).lines.single().content shouldBe "Sidecar line"
            } finally {
                tempDir.toFile().deleteRecursively()
            }
        }

        test("reads disk cache if present") {
            LyricsRepository.clearMemoryCache()
            val tempCacheDir = Files.createTempDirectory("lyrics_disk_cache_test")
            val originalCacheDir = LyricsRepository.cacheDir
            try {
                LyricsRepository.cacheDir = tempCacheDir
                val s = testSong(3, title = "My Track", artistName = "My Artist")
                val cacheFile = tempCacheDir.resolve("My Artist - My Track.lrc")
                Files.writeString(cacheFile, "[00:15.00]Cached lyric")

                val resolved = LyricsRepository.resolveLyrics(s, allowNetworkFetch = false)
                resolved.shouldNotBeNull()
                (resolved is Lyrics.Synchronized) shouldBe true
                (resolved as Lyrics.Synchronized).lines.single().content shouldBe "Cached lyric"
            } finally {
                LyricsRepository.cacheDir = originalCacheDir
                tempCacheDir.toFile().deleteRecursively()
            }
        }
    }

    context("fuzzy") {
        checkAll(iterations = 10_000, Arb.string(1..20, "[]:\n1.a ")) {
            val lyrics = Lyrics.of(it)
            if (it.isBlank()) {
                lyrics.shouldBeNull()
            } else {
                lyrics.shouldNotBeNull()
            }
        }
    }
})

private fun testSong(
    number: Int,
    lyrics: Lyrics? = null,
    file: java.nio.file.Path = Path("song-$number.mp3"),
    title: String = "Song $number",
    artistName: String = "Artist",
): Song {
    val artist = Artist(artistName, SongCollectionStats(0, kotlin.time.Duration.ZERO, null, null))
    val album = Album("Album", artist, null, SongCollectionStats(0, kotlin.time.Duration.ZERO, null, null))
    return Song(file, null, number, title, album, null, lyrics, number.seconds, null, "$number:00")
}
