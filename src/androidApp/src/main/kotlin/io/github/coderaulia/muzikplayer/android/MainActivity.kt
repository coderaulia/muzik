package io.github.coderaulia.muzikplayer.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import io.github.coderaulia.muzikplayer.ui.MobileMuzikController
import io.github.coderaulia.muzikplayer.ui.MobileTab
import io.github.coderaulia.muzikplayer.ui.MuzikMobileApp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Android Mobile [ComponentActivity] hosting MuzikPlayer.
 *
 * Integrates:
 * - Edge-to-edge Android system bars (`enableEdgeToEdge()`)
 * - Runtime audio permission handling (`READ_MEDIA_AUDIO` on API 33+, `READ_EXTERNAL_STORAGE` on API <= 32)
 * - Storage Access Framework (`OpenDocumentTree`) for adding custom music folders
 * - Live position ticker synced with [AndroidAudioEngine] (`Media3 ExoPlayer` + 10-band biquad EQ)
 * - Predictive back gesture handling (`BackHandler`)
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        setContent {
            lateinit var controllerRef: MobileMuzikController
            val audioEngine = remember {
                AndroidAudioEngine(
                    context = applicationContext,
                    onTrackEnded = { controllerRef.skipNext(fromAutoAdvance = true) },
                )
            }

            DisposableEffect(Unit) {
                onDispose {
                    audioEngine.release()
                }
            }

            val folderPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                if (uri != null) {
                    val label = uri.lastPathSegment ?: uri.toString()
                    if (label !in controllerRef.libraryFolders) {
                        controllerRef.libraryFolders.add(label)
                    }
                    controllerRef.updateLibraryTracks(AndroidMediaScanner.scanDeviceLibrary(applicationContext))
                }
            }

            val controller = remember {
                val initialTracks = AndroidMediaScanner.scanDeviceLibrary(applicationContext)
                val initialPlaylists = AndroidMediaScanner.loadM3uPlaylists(applicationContext)
                MobileMuzikController(
                    initialTracks = initialTracks,
                    initialPlaylists = initialPlaylists,
                    onPlayTrack = { track, pos -> audioEngine.playTrack(track, pos) },
                    onSetPlaying = { playing -> audioEngine.setPlaying(playing) },
                    onSeekTo = { pos -> audioEngine.seekTo(pos) },
                    onSetVolume = { vol -> audioEngine.setVolume(vol) },
                    onUpdateEqualizer = { enabled, gains -> audioEngine.updateEqualizer(enabled, gains) },
                    onRequestAddFolder = { folderPickerLauncher.launch(null) },
                    onRequestRescan = {
                        controllerRef.updateLibraryTracks(AndroidMediaScanner.scanDeviceLibrary(applicationContext))
                    },
                    onPersistPlaylist = { pl -> AndroidMediaScanner.saveM3uPlaylist(applicationContext, pl) },
                    onDeletePlaylist = { pl -> AndroidMediaScanner.deleteM3uPlaylist(applicationContext, pl) },
                ).also { ctrl ->
                    ctrl.hasAudioPermission = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        requiredPermission,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (initialTracks.isNotEmpty()) {
                        ctrl.queue.addAll(initialTracks)
                        ctrl.currentQueueIndex = 0
                    }
                }
            }
            controllerRef = controller

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                controller.hasAudioPermission = granted
                if (granted) {
                    controller.updateLibraryTracks(AndroidMediaScanner.scanDeviceLibrary(applicationContext))
                }
            }

            // Live playback position ticker (updates both real ExoPlayer streams and demo showcase streams)
            LaunchedEffect(controller.isPlaying, controller.currentTrack?.id) {
                while (controller.isPlaying) {
                    delay(200L)
                    val track = controller.currentTrack ?: break
                    if (track.uri.value.startsWith("demo://")) {
                        val nextPos = controller.currentPosition + 200.milliseconds
                        if (nextPos >= track.duration) {
                            controller.skipNext(fromAutoAdvance = true)
                        } else {
                            controller.currentPosition = nextPos
                        }
                    } else {
                        controller.currentPosition = audioEngine.currentPosition()
                    }
                }
            }

            // Android system Back gesture navigates back to Library before exiting
            BackHandler(enabled = controller.activeTab != MobileTab.LIBRARY) {
                controller.activeTab = MobileTab.LIBRARY
            }

            MuzikMobileApp(
                controller = controller,
                onRequestPermission = { permissionLauncher.launch(requiredPermission) },
            )
        }
    }
}
