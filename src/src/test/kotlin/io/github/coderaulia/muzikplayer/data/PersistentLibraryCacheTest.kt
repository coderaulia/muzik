package io.github.coderaulia.muzikplayer.data

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.nio.file.Files

class PersistentLibraryCacheTest : FunSpec({
    test("saving and loading CachedSongEntry instances round-trips all fields accurately") {
        val tempCacheFile = Files.createTempFile("lib_cache_test_", ".bin")
        val tempCoversDir = Files.createTempDirectory("lib_covers_test_")
        try {
            val cache = PersistentLibraryCache(cacheFile = tempCacheFile, coversDir = tempCoversDir)

            val entry1 = CachedSongEntry(
                path = "/music/artist/album/01-track.flac",
                lastModifiedMs = 1700000000000L,
                fileSize = 45000000L,
                disk = 1,
                track = 5,
                lengthSeconds = 234.56,
                formattedLength = "03:54",
                title = "Test Track",
                album = "Test Album",
                artist = "Test Artist",
                albumArtist = "Test Album Artist",
                dateStr = "2024-05-12",
                lyricsRaw = "[00:01.00]Hello world\n[00:05.00]Line 2",
                hasArtwork = true,
            )

            val entry2 = CachedSongEntry(
                path = "/music/other/song.mp3",
                lastModifiedMs = 1600000000000L,
                fileSize = 8500000L,
                disk = null,
                track = null,
                lengthSeconds = 180.0,
                formattedLength = "03:00",
                title = null,
                album = null,
                artist = null,
                albumArtist = null,
                dateStr = null,
                lyricsRaw = null,
                hasArtwork = false,
            )

            cache.put(entry1)
            cache.put(entry2)
            cache.save()

            // Load into a new cache instance pointing to the same file
            val loadedCache = PersistentLibraryCache(cacheFile = tempCacheFile, coversDir = tempCoversDir)
            loadedCache.size shouldBe 2

            val loaded1 = loadedCache.get(entry1.path)
            loaded1 shouldNotBe null
            loaded1 shouldBe entry1

            val loaded2 = loadedCache.get(entry2.path)
            loaded2 shouldNotBe null
            loaded2 shouldBe entry2
        } finally {
            Files.deleteIfExists(tempCacheFile)
            tempCoversDir.toFile().deleteRecursively()
        }
    }

    test("mtime or fileSize mismatch invalidates cache lookup") {
        val tempCacheFile = Files.createTempFile("lib_cache_test_mismatch_", ".bin")
        val tempCoversDir = Files.createTempDirectory("lib_covers_test_mismatch_")
        try {
            val cache = PersistentLibraryCache(cacheFile = tempCacheFile, coversDir = tempCoversDir)
            val entry = CachedSongEntry(
                path = "/music/track.mp3",
                lastModifiedMs = 1000L,
                fileSize = 2000L,
                disk = null,
                track = 1,
                lengthSeconds = 120.0,
                formattedLength = "02:00",
                title = "Track",
                album = "Album",
                artist = "Artist",
                albumArtist = null,
                dateStr = "2023",
                lyricsRaw = null,
                hasArtwork = false,
            )
            cache.put(entry)
            cache.save()

            val loadedCache = PersistentLibraryCache(cacheFile = tempCacheFile, coversDir = tempCoversDir)

            // Exact match succeeds
            loadedCache.get(entry.path, 1000L, 2000L) shouldBe entry

            // mtime mismatch returns null
            loadedCache.get(entry.path, 9999L, 2000L) shouldBe null

            // fileSize mismatch returns null
            loadedCache.get(entry.path, 1000L, 9999L) shouldBe null

            // Both mismatch returns null
            loadedCache.get(entry.path, 9999L, 9999L) shouldBe null
        } finally {
            Files.deleteIfExists(tempCacheFile)
            tempCoversDir.toFile().deleteRecursively()
        }
    }

    test("cover art saving and loading round-trips bytes accurately") {
        val tempCacheFile = Files.createTempFile("lib_cache_test_cover_", ".bin")
        val tempCoversDir = Files.createTempDirectory("lib_covers_test_cover_")
        try {
            val cache = PersistentLibraryCache(cacheFile = tempCacheFile, coversDir = tempCoversDir)
            val songPath = "/music/test/song.flac"
            val dummyArtwork = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) // PNG magic

            cache.saveCover(songPath, dummyArtwork)
            val retrieved = cache.getCover(songPath)
            retrieved shouldNotBe null
            retrieved shouldBe dummyArtwork

            // Unknown song returns null
            cache.getCover("/nonexistent/song.flac") shouldBe null
        } finally {
            Files.deleteIfExists(tempCacheFile)
            tempCoversDir.toFile().deleteRecursively()
        }
    }
})
