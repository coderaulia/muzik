# MuzikPlayer Improvement Plan (post-1.5.7)

This plan lists features that are currently placeholders, read-only, or only
partly wired up, and describes how to make each one real. It is based on an
audit of the 1.5.7 codebase (`src/`, `shared/`, `androidApp/`).

Paths below are relative to `src/src/main/kotlin/io/github/coderaulia/muzikplayer/`
unless they start with `src/`, `shared/` or `androidApp/`.

Open-source dependencies are allowed where they save real work. Each one is
named with its license.

---

## Summary

| #  | Item                                   | Area            | Size   | Priority |
|----|----------------------------------------|-----------------|--------|----------|
| 1  | Real peak waveform on Home             | Desktop UI      | S      | P1       |
| 2  | Dead settings toggles                  | Desktop settings| S      | P1       |
| 3  | Honest bit-perfect mode                | Audio engine    | M      | P1       |
| 4  | Sample-accurate A-B loop               | Audio engine    | M      | P1       |
| 5  | Real-time spectrum analyzer            | Audio + UI      | M      | P2       |
| 6  | Remove Android demo tracks             | Android         | S      | P2       |
| 7  | Verify Android EQ format handling      | Android         | S      | P2       |
| 8  | Drag-to-reorder on the Queue page      | Desktop UI      | M      | P2       |
| 9  | Tag editor (with read-only guard)      | Data + UI       | L      | P3       |
| 10 | Lyrics editor and `.lrc` export        | Data + UI       | M      | P3       |
| 11 | Code hygiene and log noise             | All             | S      | P3       |
| 12 | CI action upgrades                     | CI              | S      | P3       |

Sizes: S = under half a day, M = about one day, L = several days.

Suggested order: 1, 2, 3, 4, 5, 6, 7, then 8, 9, 10, with 11 and 12 slotted in
whenever convenient.

---

## 1. Real peak waveform on Home

**Current state.** The "Peak Waveform / Local Cache" strip in
`ui/HomePage.kt` (around line 733) draws bar heights from a fixed sum of
sine waves. The shape is identical for every song and has nothing to do with
the audio.

**What already exists.** `audio/SongDecoder.kt` decodes the whole song in the
background and publishes `DecodedSongData` with `waveformsPerChannel`
(`WAVEFORM_SIZE = 240` buckets per channel, average absolute amplitude per
bucket) and `maxAmplitude`. `PlayerController.DecodedSongData()` exposes it as
Compose state. `ui/Spectrometers.kt` already reads it.

**Plan.**
1. In the waveform composable, read `player.DecodedSongData()`.
2. Merge channels per bucket (max or mean of the channels).
3. Resample the 240 buckets to `barCount` by taking the max over each bar's
   bucket range, so short peaks stay visible.
4. Normalize by `maxAmplitude` and apply a mild curve (`sqrt`) so quiet
   passages are still visible. Clamp to `0.08f..1f`.
5. While `decoded == null` or `!decoded.done`, draw the bars that are
   available and render the rest as a flat, low placeholder so the strip
   "fills in" as decoding progresses.
6. Keep the existing seek-by-click/drag behaviour and played/unplayed colors.
7. Rename the label to "Peak Waveform" (drop "Local Cache" unless a disk
   cache is actually added).

**Optional.** Cache waveforms on disk next to the existing library cache
(`data/PersistentLibraryCache.kt`), keyed by file path + mtime, so the strip is
instant on replay. Only worth it if decoding is visibly slow on large files.

**Tests.** Unit test for the resample/normalize helper (pure function: input
`DoubleArray`, output `FloatArray` of `barCount`).

**Done when.** Different songs show visibly different waveforms that match
loud and quiet sections, and nothing is generated from `sin()`.

---

## 2. Dead settings toggles

Three preferences in `utils/Preferences.kt` are shown in Settings but nothing
outside `ui/AppSettings.kt` reads them.

### 2a. `showTableThumbnails`
**Plan.** Read it in `ui/SongRow.kt` / `ui/AlbumRow.kt` and hide the album
cover column when it is off. In `AlbumRow`, when thumbnails are off, also drop
the 128 dp side panel so rows reclaim the width.
**Done when.** Toggling it immediately shows or hides covers in the Library
list.

### 2b. `audioTelemetry`
**Plan.** Use it to show or hide the "Audio Backend Monitor" card on Home
(`AudioBackendMonitorCard` in `ui/HomePage.kt`) and the format/latency pills
in `ui/LibraryHeader.kt`. Update the setting's subtitle to say exactly that.
**Done when.** Turning it off hides all audio diagnostics in the UI.

### 2c. `readOnlyMode`
**Plan.** There is currently no code that writes to media files, so the
toggle has nothing to guard. Two options:
- **Short term:** keep the toggle but make it the gate for item 9 (tag
  editor) and item 10 (writing `.lrc` sidecars). Add a single helper
  `fun canWriteToMediaFiles(): Boolean = !Preferences.readOnlyMode.get()` in
  `utils/` and call it from every write path.
- **If item 9 is not done soon:** hide the toggle so the UI does not promise
  something that does not exist.

---

## 3. Honest bit-perfect mode

**Current state.** `Preferences.bitPerfect` only affects decoding in
`data/Library.kt` (keeps 24/32-bit PCM instead of converting to 16-bit).
Meanwhile the signal is still altered by:
- the equalizer (`audio/Equalizer.kt` → `BiquadEqualizerProcessor`),
- ReplayGain and peak protection (`AudioMetadataResolver.computeReplayGainScale`
  in `audio/PlayerController.kt`),
- volume via `MASTER_GAIN` in `audio/SourceDataLineWrapper.kt`,
- and the system mixer (PipeWire/PulseAudio) may resample.

**Plan.**
1. Define bit-perfect as: "decoded samples reach the output line unmodified,
   at the file's native sample rate and bit depth".
2. When `bitPerfect` is on:
   - bypass `Equalizer.process` (return `null`),
   - force `replayGainScale = 1f`,
   - skip `setLevel` (leave line gain at 0 dB) and show the volume slider as
     disabled with a tooltip "Volume fixed in bit-perfect mode".
3. Open the line with the file's native `AudioFormat` (already the case) and
   log the actual line format returned by `line.format` so mismatches are
   visible.
4. In the EQ dialog and the ReplayGain setting, show "Disabled while
   bit-perfect is on" instead of silently ignoring changes.
5. Update the Audio Backend Monitor to show "Bit-perfect: active" only when
   all of the above hold, otherwise list what is modifying the signal
   (e.g. "EQ, ReplayGain").
6. Document in the settings subtitle that true exclusive access depends on the
   system audio server (PipeWire can be configured for it; the app cannot
   guarantee it from Java Sound).

**Tests.** Unit test that `Equalizer.process` returns `null` and the computed
level is `1f` when bit-perfect is on.

**Done when.** With bit-perfect on, the bytes written to the line equal the
decoded bytes, and the UI says so honestly.

---

## 4. Sample-accurate A-B loop

**Current state.** `ui/HomePage.kt` keeps `loopStart` / `loopEnd` in UI state
and a `LaunchedEffect` keyed on `position` seeks back when `position >= end`.
Accuracy depends on how often the UI position ticks (tens of milliseconds) and
the loop is lost when leaving Home.

**Plan.**
1. Add loop state to the player: `PlayerController.setLoop(start: Duration?,
   end: Duration?)` sending a new `PlayerCommand.SetLoop`.
2. Store the loop on `CurrentlyPlaying` and clear it when the song changes.
3. In `Player.playFrame`, convert the loop end to frames and pass
   `limitFrames = loopEndFrame - currentFrame`. When the read hits the limit,
   call `source.seekTo(loopStartFrame)` without flushing, so the line keeps
   playing seamlessly.
4. Expose `PlayerController.loop` as Compose state; the Home button and the
   waveform strip read it (draw a shaded A-B region on the waveform).
5. Optional: persist the last loop per song key in Preferences.

**Tests.** Unit test for the frame math (start/end conversion, end-of-song
clamping). Manual test: loop a 2-second region and listen for gaps.

**Done when.** The loop repeats without an audible gap or overshoot and
survives navigating away from Home.

---

## 5. Real-time spectrum analyzer

**Current state.** `SmallFakeSpectrometers` in `ui/Spectrometers.kt`
animates three bars from `sin()`-style functions of the playback position,
scaled by the precomputed waveform amplitude. It reacts to loudness only, not
to frequency content.

**Plan.**
1. Add a PCM tap in `Player.playFrame`: after (optional) EQ, copy the last
   N frames written (e.g. 2048) into a lock-free ring buffer.
2. Run an FFT on a UI-frame cadence (about 30 Hz) on `Dispatchers.Default`:
   - downmix to mono, apply a Hann window,
   - FFT with **JTransforms** (`com.github.wendykierp:JTransforms:3.1`,
     BSD-2-Clause) or a small in-house radix-2 FFT (about 60 lines, no
     dependency).
3. Group bins into log-spaced bands (3 bands for the small icon, 24–32 bands
   for an optional large visualizer on the full lyrics view).
4. Apply attack/decay smoothing so bars do not flicker.
5. Account for output latency: delay the displayed spectrum by
   `currentOutputBuffer` so it lines up with what is heard.
6. Rename `SmallFakeSpectrometers` to `SmallSpectrometers`.
7. Stop the FFT job when paused or when no spectrum composable is on screen.

**Tests.** Feed a 1 kHz sine into the band mapper and assert the 1 kHz band
dominates.

**Done when.** Bars follow bass and treble separately and freeze when paused.

---

## 6. Remove Android demo tracks

**Current state.** `androidApp/.../AndroidMediaScanner.kt` returns
`fallbackShowcaseTracks()` (four `demo://` tracks and a demo playlist) when
the scan finds nothing. `AndroidAudioEngine.kt` and `MainActivity.kt` contain
special `demo://` code paths.

**Plan.**
1. Return an empty list when MediaStore has no audio.
2. Show an empty state in `shared/.../MobileAppUi.kt`: "No music found",
   with a button to re-scan and a hint about storage permission.
3. If the permission is denied, show a clear prompt instead of an empty list.
4. Delete `fallbackShowcaseTracks()`, the demo playlist, and every
   `startsWith("demo://")` branch.
5. Keep demo data only in tests (`MobileMuzikControllerTest` already has its
   own sample tracks).

**Done when.** A fresh install on a device with no music shows the empty
state, and `grep -r "demo://" androidApp shared` finds nothing.

---

## 7. Verify Android EQ format handling

**Current state.** `AndroidAudioEngine.kt` (around line 53) builds the shared
`BiquadEqualizerProcessor` with a fixed `sampleSizeInBits = 16`.

**Plan.**
1. Confirm which PCM encoding Media3 delivers to the `AudioProcessor`
   (`C.ENCODING_PCM_16BIT` vs float/24-bit).
2. Either declare in `onConfigure` that only 16-bit PCM is accepted (so
   ExoPlayer converts before the processor), or map the encoding to the right
   `sampleSizeInBits` / float path.
3. Add a shared test for 24-bit and 32-bit input to
   `BiquadEqualizerProcessorTest`.

**Done when.** EQ works on high-resolution files on Android with no noise or
distortion.

---

## 8. Drag-to-reorder on the Queue page

**Current state.** Only the Home play-queue card supports dragging
(`SongQueue.moveTo`). The Queue page (`ui/SongQueueUI.kt` → `ui/SongListUI.kt`)
does not, and the Home version assumes a fixed 54 dp row height.

**Plan.**
1. Use **Reorderable** (`sh.calvin.reorderable:reorderable`, Apache-2.0),
   which supports Compose Multiplatform `LazyColumn`, or implement drag with
   `LazyListState.layoutInfo` to find the drop target from real item offsets.
2. Wire drops to `player.transformQueue { it?.moveTo(from, to) to Position.Current }`.
3. Show a drag handle on hover and a drop indicator line.
4. Replace the Home card's fixed-height math with the same approach.
5. Keyboard alternative: `Alt+Up` / `Alt+Down` on a focused row.

**Done when.** Rows can be dragged on both the Queue page and Home with
correct drop positions regardless of row height.

---

## 9. Tag editor (guarded by read-only mode)

**Current state.** Tags are read only. The app already ships **jaudiotagger**
(LGPL-2.1), which can also write tags for FLAC, MP3, M4A and OGG.

**Plan.**
1. Add `data/TagWriter.kt` with
   `suspend fun writeTags(file: Path, changes: TagChanges): Result<Unit>`
   using jaudiotagger `AudioFileIO.read(...).tag.setField(...)` and
   `AudioFile.commit()`.
2. Check `canWriteToMediaFiles()` (item 2c) first. If read-only mode is on,
   the editor opens in view-only mode with a link to the setting.
3. Write to a temp copy and atomically replace the original
   (`Files.move(..., ATOMIC_MOVE)`) so a crash cannot corrupt the file.
4. Editable fields: title, artist, album, album artist, track/disc number,
   year, genre, embedded lyrics, cover art (replace/remove).
5. Entry points: "Edit tags" in the song context menu and the track info
   dialog on Home.
6. After writing, `LiveLibrary` picks up the change through the filesystem
   watcher; if `watchFilesystem` is off, trigger a re-read of that one file.
7. Batch edit for multiple selected songs (only fields that are the same
   across all selections are prefilled).

**Tests.** Write and read back tags on a small FLAC and MP3 fixture in a temp
directory.

**Done when.** Edits persist to the file, the library updates, and nothing is
written while read-only mode is on.

---

## 10. Lyrics editor and `.lrc` export

**Current state.** `data/LyricsRepository.kt` resolves lyrics from sidecar
`.lrc`, a disk cache, and LRCLIB. There is no way to correct them, and the
lyrics offset (`Preferences.lyricsOffsetMs`) is global rather than per song.

**Plan.**
1. Make the offset per song: store offsets in a small map keyed by song key
   (same pattern as `favoriteSongKeys`), falling back to the global value.
2. Add "Edit lyrics" in the full lyrics view: a text editor showing LRC
   lines, plus a "tap to sync" mode that stamps the current playback time on
   the next line when `Space` is pressed.
3. "Save" writes a sidecar `.lrc` next to the audio file (guarded by
   `canWriteToMediaFiles()`), otherwise saves into the app's lyrics disk cache.
4. "Report/Refetch" button re-queries LRCLIB, ignoring the cache.
5. Optional: word-level highlighting for enhanced LRC (`<mm:ss.xx>` word
   tags), parsed in `data/Lyrics.kt`.

**Done when.** A user can fix timing or text of a song's lyrics and the fix
survives restart.

---

## 11. Code hygiene and log noise

1. Fix current compiler warnings:
   - unnecessary safe calls in `audio/PlayerController.kt:193` and
     `ui/HomePage.kt:152,173`,
   - deprecated `Icons.Filled.VolumeUp` → `Icons.AutoMirrored.Filled.VolumeUp`
     (`ui/HomePage.kt`, `ui/PlaylistsPage.kt`),
   - deprecated `Divider` → `HorizontalDivider` (`ui/PlaylistsPage.kt`).
2. `data/LiveLibrary.kt`: skip files whose extension is not a known audio,
   playlist, image or lyrics type before trying to parse them. Today
   `desktop.ini` is logged as an `ERROR` on every scan.
3. Split `ui/HomePage.kt` (~1,700 lines) into per-card files
   (`home/StudioPlayerStage.kt`, `home/InspectorDeck.kt`,
   `home/PlayQueueCard.kt`, `home/AudioMonitorCard.kt`, `home/FullLyricsView.kt`).
   Same for `ui/AppSettings.kt`.
4. Resolve the `TODO` in `color/MMCQ.kt:23` (use a `PriorityQueue`) or delete it.

---

## 12. CI action upgrades

The tagged-release workflow logs deprecation warnings.

1. `actions/setup-java@v4` → `@v5`.
2. Bump `actions/checkout`, `actions/upload-artifact`,
   `actions/download-artifact` and `gradle/actions/setup-gradle` to their
   Node 24 releases.
3. Cache Gradle dependencies (already handled by `setup-gradle`) and add a
   `./gradlew test` step before packaging so a failing test blocks the release.

---

## Open-source dependencies referenced

| Library      | Use                       | License      |
|--------------|---------------------------|--------------|
| JTransforms  | FFT for spectrum (item 5) | BSD-2-Clause |
| Reorderable  | Drag-to-reorder (item 8)  | Apache-2.0   |
| jaudiotagger | Tag writing (item 9), already a dependency | LGPL-2.1 |

All are compatible with the project's GPL-3.0 license.

---

## Release grouping

- **1.6.0:** items 1, 2, 3, 4, 11, 12. Makes everything shown in the UI real.
- **1.6.1:** items 5, 6, 7.
- **1.7.0:** items 8, 9, 10.

Each release: bump with `scripts/bump-version.sh`, add a `CHANGELOG.md`
section (the release workflow uses it as release notes), tag `vX.Y.Z`, push.
