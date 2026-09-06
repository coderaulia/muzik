package io.github.coderaulia.muzikplayer.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

/** Small persistence boundary for playlists managed by the redesigned UI. */
object PlaylistStore {
    suspend fun create(directory: Path, name: String): Path = withContext(Dispatchers.IO) {
        val safeName = validateName(name)
        Files.createDirectories(directory)
        val file = directory.resolve("$safeName.m3u").normalize()
        require(file.parent == directory.normalize()) { "Playlist name must not contain path separators" }
        require(!Files.exists(file)) { "A playlist with that name already exists" }
        Files.writeString(file, "#EXTM3U\n")
        file
    }

    suspend fun append(playlist: Playlist, songs: Collection<Song>): Unit = withContext(Dispatchers.IO) {
        require(playlist.file.extension.equals("m3u", ignoreCase = true)) { "Unsupported playlist format" }
        val existing = playlist.songs.toSet()
        val additions = songs.map { it.file.toAbsolutePath().normalize() }
            .filterNot { it in existing.map { key -> key.file.toAbsolutePath().normalize() }.toSet() }
        if (additions.isNotEmpty()) {
            Files.write(playlist.file, additions.map(Path::toString), Charsets.UTF_8, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND)
        }
    }

    private fun validateName(name: String): String {
        val value = name.trim()
        require(value.isNotEmpty()) { "Playlist name cannot be empty" }
        require(value != "." && value != "..") { "Playlist name is invalid" }
        require(value.none { it == '/' || it == '\\' }) { "Playlist name must not contain path separators" }
        return value.removeSuffix(".m3u").removeSuffix(".M3U").trim().also {
            require(it.isNotEmpty()) { "Playlist name cannot be empty" }
        }
    }
}
