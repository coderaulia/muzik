// It turns our it's faster to just use Dispatchers.Default rather than creating 64 threads
@file:Suppress("BlockingMethodInNonBlockingContext")

package io.github.coderaulia.muzikplayer.data

import io.github.coderaulia.muzikplayer.utils.Preferences
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.bjoernpetersen.m3u.M3uParser
import net.bjoernpetersen.m3u.model.M3uEntry
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import java.nio.file.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString
import kotlin.io.path.readText
import kotlin.time.TimeSource

private val logger = KotlinLogging.logger {}

private val PLAYLIST_EXTENSIONS = setOf("m3u")
private val IMAGES_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "bmp", "gif", "tiff")
private val LYRICS_EXTENSIONS = setOf("lrc")

class LiveLibrary(
    private val scope: CoroutineScope,
    private val roots: Set<Path>,
    private val cache: PersistentLibraryCache = PersistentLibraryCache(),
) {
    private val creationTime = TimeSource.Monotonic.markNow()
    private val watchService: WatchService = FileSystems.getDefault().newWatchService()
    private val watchServiceMutex = Mutex()
    private val registeredKeys = mutableMapOf<Path, WatchKey>()
    private val pendingEvents = AtomicInteger(0)

    private var eventChannel = Channel<InternalEvent>()

    private val env = scope.async { getSystemResourceEnvironment() }
    private val partialDateParserCache = PartialDateParserCache()

    private class MetadataCollection<T> {
        val data = mutableMapOf<Path, T>()
        val size get() = data.size
        fun add(path: Path, item: T) {
            data[path] = item
        }

        fun onFileDeleted(deletedPath: Path) {
            data.keys.removeIf { path ->
                path.startsWith(deletedPath)
            }
        }
    }

    suspend fun start(flowCollector: FlowCollector<Library>) {
        suspendCancellableCoroutine<Unit> { cont ->
            scope.launch {
                logger.info { "Started song loading" }
                roots.forEach { root ->
                    onNew(root, decoder = CoversDecoder(scope))
                }
                while (true) {
                    val monitorKey = runInterruptible(Dispatchers.Default) {
                        watchService.take()
                    }
                    val dirPath = monitorKey.watchable() as Path
                    val events = monitorKey.pollEvents()
                    if (Preferences.watchFilesystem.get()) {
                        events.forEach { event ->
                            onFileEvent(dirPath, event, decoder = CoversDecoder(scope))
                        }
                    }
                    if (!monitorKey.reset()) {
                        monitorKey.cancel()
                        break
                    }
                }
            }
            scope.launch {
                val rawMetadatas = MetadataCollection<RawMetadataSong>()
                val rawPlaylists = MetadataCollection<List<M3uEntry>>()
                val rawImages = MetadataCollection<Deferred<AlbumCover?>>()
                val rawLyrics = MetadataCollection<Lyrics>()
                while (true) {
                    when (val event = eventChannel.receive()) {
                        is InternalEvent.NewSong -> rawMetadatas.add(event.file, event.metadata)
                        is InternalEvent.NewPlaylist -> rawPlaylists.add(event.file, event.playlist)
                        is InternalEvent.NewImage -> rawImages.add(event.file, event.image)
                        is InternalEvent.NewLyric -> rawLyrics.add(event.file, event.lyrics)
                        is InternalEvent.FileDeleted -> {
                            rawMetadatas.onFileDeleted(event.fileOrFolder)
                            rawPlaylists.onFileDeleted(event.fileOrFolder)
                            rawImages.onFileDeleted(event.fileOrFolder)
                            rawLyrics.onFileDeleted(event.fileOrFolder)
                            cache.remove(event.fileOrFolder)
                        }

                        is InternalEvent.FolderProcessed -> {}
                        is InternalEvent.FileIgnored -> {}
                    }
                    val remaining = pendingEvents.decrementAndGet()
                    if (remaining == 0) {
                        val library = try {
                            Library.from(rawMetadatas.data, rawPlaylists.data, rawImages.data, rawLyrics.data)
                        } catch (e: Throwable) {
                            logger.error(e) { "Failed to build library" }
                            continue
                        }
                        System.gc()
                        logger.info {
                            val diff = creationTime.elapsedNow()
                            "Processed ${rawMetadatas.size} songs, ${rawPlaylists.size} playlists, ${rawImages.size} images, ${rawLyrics.size} lyrics ($diff since beginning)"
                        }
                        if (cache.isDirty) {
                            try {
                                cache.save()
                            } catch (e: Exception) {
                                logger.error(e) { "Failed to save library cache" }
                            }
                        }
                        flowCollector.emit(library)
                    }
                }
            }
            cont.invokeOnCancellation {
                scope.launch {
                    watchServiceMutex.withLock {
                        registeredKeys.values.forEach {
                            it.cancel()
                        }
                        registeredKeys.clear()
                    }
                }
            }
        }
    }

    private fun onFileEvent(dirPath: Path, event: WatchEvent<*>, decoder: CoversDecoder) {
        val file = dirPath.resolve(event.context() as Path)
        when (event.kind()) {
            StandardWatchEventKinds.ENTRY_CREATE -> onNew(file, decoder)
            StandardWatchEventKinds.ENTRY_DELETE -> onDeleted(file)
            else -> onModified(file, decoder)
        }
    }

    private fun onNew(fileOrFolder: Path, decoder: CoversDecoder) {
        pendingEvents.incrementAndGet()
        scope.launch {
            if (fileOrFolder.isDirectory()) {
                onNewFolder(fileOrFolder, decoder)
            } else {
                onNewFile(fileOrFolder, decoder)
            }
        }
    }

    /** Warning: increment pendingEvents before calling this! */
    private suspend fun onNewFolder(folder: Path, decoder: CoversDecoder) {
        Files.newDirectoryStream(folder).use { stream ->
            stream.forEach { file ->
                onNew(file, decoder)
            }
        }
        watchServiceMutex.withLock {
            registeredKeys.remove(folder)?.cancel()
            if (folder.exists()) {
                registeredKeys[folder] = folder.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE
                )
            }
        }
        eventChannel.send(InternalEvent.FolderProcessed)
    }

    /** Warning: increment pendingEvents before calling this! */
    private suspend fun onNewFile(file: Path, decoder: CoversDecoder) {
        val extension = file.extension.lowercase()
        when (extension) {
            in PLAYLIST_EXTENSIONS -> onNewPlaylist(file)
            in IMAGES_EXTENSIONS -> onNewImage(file, decoder)
            in LYRICS_EXTENSIONS -> onNewLyric(file)
            else -> onNewSong(file, decoder)
        }
    }

    private suspend fun onNewSong(file: Path, decoder: CoversDecoder) {
        try {
            val lastModifiedMs = try {
                Files.getLastModifiedTime(file).toMillis()
            } catch (_: Exception) {
                -1L
            }
            val fileSize = try {
                Files.size(file)
            } catch (_: Exception) {
                -1L
            }

            val cachedEntry = if (lastModifiedMs >= 0 && fileSize >= 0) {
                cache.get(file, lastModifiedMs, fileSize)
            } else {
                null
            }

            val songInfo = if (cachedEntry != null) {
                val coverBytes = if (cachedEntry.hasArtwork) cache.getCover(cachedEntry.path) else null
                RawMetadataSong.fromCachedEntry(file, cachedEntry, decoder, coverBytes, partialDateParserCache)
            } else {
                val parsed = RawMetadataSong.fromMusicFile(file, decoder, env, partialDateParserCache)
                if (lastModifiedMs >= 0 && fileSize >= 0) {
                    val rawArtwork = parsed.rawArtworkBytes
                    val entry = CachedSongEntry(
                        path = file.toAbsolutePath().normalize().pathString,
                        lastModifiedMs = lastModifiedMs,
                        fileSize = fileSize,
                        disk = parsed.disk,
                        track = parsed.track,
                        lengthSeconds = parsed.length.inWholeMilliseconds / 1000.0,
                        formattedLength = parsed.formattedLength,
                        title = parsed.title,
                        album = parsed.album,
                        artist = parsed.artist,
                        albumArtist = parsed.albumArtist,
                        dateStr = parsed.rawDateStr,
                        lyricsRaw = parsed.lyrics?.rawString,
                        hasArtwork = rawArtwork != null,
                    )
                    if (rawArtwork != null) {
                        cache.saveCover(entry.path, rawArtwork)
                    }
                    cache.put(entry)
                }
                parsed
            }
            eventChannel.send(InternalEvent.NewSong(file, songInfo))
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            eventChannel.send(InternalEvent.FileIgnored)
            logger.error(e) { "Error while parsing music file $file: ${e.message}" }
        }
    }

    private suspend fun onNewPlaylist(file: Path) {
        try {
            val playlistInfo = M3uParser.parse(file)
            eventChannel.send(InternalEvent.NewPlaylist(file, playlistInfo))
        } catch (e: Exception) {
            eventChannel.send(InternalEvent.FileIgnored)
            logger.error {
                "Error while parsing playlist file $file: ${e.message}"
            }
        }
    }

    private suspend fun onNewImage(file: Path, decoder: CoversDecoder) {
        try {
            val decoded = decoder.decode(file)
            eventChannel.send(InternalEvent.NewImage(file, decoded))
        } catch (e: Exception) {
            eventChannel.send(InternalEvent.FileIgnored)
            logger.error { "Error while parsing image file $file: ${e.message}" }
        }
    }

    private suspend fun onNewLyric(file: Path) {
        try {
            val lyrics = Lyrics.of(file.readText())
            if (lyrics == null) {
                logger.debug { "Empty lyrics file $file" }
                eventChannel.send(InternalEvent.FileIgnored)
            } else {
                eventChannel.send(InternalEvent.NewLyric(file, lyrics))
            }
        } catch (e: Exception) {
            eventChannel.send(InternalEvent.FileIgnored)
            logger.error { "Error while parsing lyrics file $file: ${e.message}" }
        }
    }

    private fun onDeleted(fileOrFolder: Path) {
        pendingEvents.incrementAndGet()
        scope.launch {
            watchServiceMutex.withLock {
                registeredKeys.remove(fileOrFolder)?.cancel()
            }
            eventChannel.send(InternalEvent.FileDeleted(fileOrFolder))
        }
    }

    private fun onModified(fileOrFolder: Path, decoder: CoversDecoder) {
        onNew(fileOrFolder, decoder)
    }

    private sealed interface InternalEvent {
        data class NewSong(
            val file: Path,
            val metadata: RawMetadataSong,
        ) : InternalEvent

        data class NewPlaylist(
            val file: Path,
            val playlist: List<M3uEntry>,
        ) : InternalEvent

        data class NewImage(
            val file: Path,
            val image: Deferred<AlbumCover?>,
        ) : InternalEvent

        data class NewLyric(
            val file: Path,
            val lyrics: Lyrics,
        ) : InternalEvent

        data class FileDeleted(
            val fileOrFolder: Path,
        ) : InternalEvent

        object FileIgnored : InternalEvent
        object FolderProcessed : InternalEvent
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun Flow<Set<Path>>.toLibrary(): Flow<Library?> {
    return transformLatest { roots ->
        emit(null)
        withContext(Dispatchers.Default) {
            val liveLibrary = LiveLibrary(this, roots)
            liveLibrary.start(this@transformLatest)
        }
    }
}