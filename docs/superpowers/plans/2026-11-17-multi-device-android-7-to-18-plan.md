# Krypton Player — Full Android 7–17 (+18 Ready) & Multi-Device Support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Krypton Player crash-free and fully functional across **Android 7.0 through Android 17 (API 24–37)**, future-ready for **Android 18 (API 38)**, and properly supported across every major Android Studio device form factor: **Phones, Tablets, Foldables/Desktop Mode, Android TV / Google TV, Android Auto / Automotive OS, and Wear OS**.

**Architecture:** Fix all confirmed API 24–36 compatibility bugs first across existing modules (`:app`, `:core:*`, `:feature:*`), then upgrade `targetSdk` to `37` (Android 17) with adaptive large-screen layouts, add TV Leanback + D-pad focus support in-place, upgrade `PlayerService` to a Media3 `MediaLibraryService` for Android Auto media browsing, and add a lightweight `:wear` companion module for Wear OS watches.

**Tech Stack:** Kotlin 2.3, Gradle 9.6, AGP 9.5.1, Jetpack Compose + Material 3 Adaptive, Media3 (`1.10.0`) `MediaLibraryService`, Hilt, Room, Compose for Wear OS (`androidx.wear.compose`).

---

## Global Constraints

- **SDK Versions:** Keep `minSdk = 24` (Android 7.0), `compileSdk = 37` (Android 17), upgrade `targetSdk` from `36` → `37` in Phase 2.
- **Android 18 (API 38) Status:** Official SDK/Beta is **not yet released** as of Nov 2026 (Beta 1 expected ~Feb 2027, stable ~June 2027). Phase 2 prepares the codebase cleanly so bumping to API 38 later is a single version-catalog change.
- **Preserved Identifiers & External Libs:** Keep `applicationId = "dev.roxy.vip.archplayer"`, Kotlin package `dev.roxy.kryptonplayer`, and external `io.github.anilbeesetti:nextlib-*` untouched.
- **No Unrequested Local Builds:** Do not invoke `./gradlew` automatically unless the user explicitly asks; verify changes cleanly for Android Studio builds.
- **Platform Policy Compliance:**
  - **Android Auto:** Exposes **audio playback / MediaLibraryService browsing** (Google Play & Android Auto prohibit video playback while driving).
  - **Wear OS:** Ships as a separate `:wear` companion module (Now Playing remote controller & rotary volume control) rather than forcing a phone video player onto a watch screen.

## Review Focus

1. **Android 7.0–7.1 (API 24–25) without desugaring:** Any API 26+ JDK class (`java.util.Base64`, `java.time.*`, `Stream.toList()`) throws `NoClassDefFoundError` / `NoSuchMethodError` (`java.lang.Error`, bypassing `catch (Exception)`).
2. **Android 14–16 Partial Media Access (`READ_MEDIA_VISUAL_USER_SELECTED`):** Choosing *"Select photos and videos"* denies `READ_MEDIA_VIDEO` and permanently locks users out unless both permissions are handled.
3. **Android 14–17 Foreground Service & BroadcastReceiver enforcement:** Missing `RECEIVER_EXPORTED`/`NOT_EXPORTED`, missing `CONNECTED_DEVICE` prerequisites, or missing Android 15 `Service.onTimeout()` cause instant `SecurityException` or `RemoteServiceException` crashes.
4. **Android 17 (`targetSdk = 37`) Large-Screen Resizability & Local Network:** Orientation locks (`ReelsActivity`) are ignored on `sw >= 600dp`, and `UsbMediaServer` loopback/LAN access must comply with API 37 rules.
5. **TV / Auto / Wear non-regression on Phones:** Adding `leanback`, `automotive`, and `:wear` must keep `required="false"` for TV/touchscreen features so Play Store device filtering never drops phones, tablets, or foldables.

---

## Compatibility & Device Matrix (Target State)

| Target | Current Status | After This Plan |
|---|---|---|
| **Android 7–9 (API 24–28)** | Crashes in Shazam (`java.util.Base64`), `LogcatDumper` (`Stream.toList()`), OEM PiP settings | Fully safe (`android.util.Base64`, `readLines()`, safe PiP fallback) |
| **Android 10–12 (API 29–32)** | Video listing fixed; delete/rename/move/share, `FileProvider` `.provider` typo, Ringtone `WRITE_SETTINGS` & FGS stop bugs remain | All storage, `FileProvider`, Ringtone, and FGS lifecycle bugs fixed |
| **Android 13–16 (API 33–36)** | `registerReceiver` crash risk, `UsbCinemaService` `CONNECTED_DEVICE` crash, missing `onTimeout()`, API 34+ partial media lockout | Fully compliant with `targetSdk = 36` |
| **Android 17 (API 37)** | Compiles on 37 (`compileSdk = 37`), targets 36 | Targets `targetSdk = 37` with adaptive large-screen UI, background audio hardening & loopback compliance |
| **Android 18 (API 38)** | Not yet released (no SDK/Beta yet) | Clean upgrade checklist & zero deprecated/blocked APIs ready for Feb 2027 Beta 1 |
| **Tablets / Foldables / Desktop** | Phone-first portrait/single-pane layouts (`ReelsActivity` locked portrait) | Material 3 adaptive layouts (`WindowSizeClass`), resizable multi-window & foldable postures |
| **Android TV / Google TV** | `isTv()` helper exists, no Leanback manifest/banner/D-pad focus | Full Leanback manifest, 320×180 banner, D-pad focus & TV remote playback controls |
| **Android Auto / Automotive OS** | Not configured (`PlayerService` is `MediaSessionService` only) | `automotive_app_desc.xml` + `MediaLibraryService` browseable audio library |
| **Wear OS** | Not configured | Dedicated `:wear` companion module (Now Playing remote + rotary volume control) |

---

## Phase 1: Android 7–16 Compatibility Fixes (All Confirmed Bugs)

### Task 1: Fix Android 7.0–9.0 (API 24–28) Crashes

**Files:**
- Modify: `core/data/src/main/java/dev/roxy/kryptonplayer/core/data/shazam/ShazamSignatureGenerator.kt:6, 282`
- Modify: `core/data/src/main/java/dev/roxy/kryptonplayer/core/data/reporting/LogcatDumper.kt:64-70`
- Modify: `feature/player/src/main/java/dev/roxy/kryptonplayer/feature/player/state/PictureInPictureState.kt:132-145`

- [x] **Step 1:** In `ShazamSignatureGenerator.kt`, replace `import java.util.Base64` with `import android.util.Base64`, and change `Base64.getEncoder().encodeToString(encodeToBinary())` to `Base64.encodeToString(encodeToBinary(), Base64.NO_WRAP)`.
- [x] **Step 2:** In `LogcatDumper.kt:64-70`, remove the `Build.VERSION_CODES.N` `reader.lines().toList()` branch (which resolves to API 34+ `Stream.toList()`) and use `BufferedReader(InputStreamReader(process.inputStream)).use { it.readLines() }`.
- [x] **Step 3:** In `PictureInPictureState.kt:132-145`, wrap `activity.enterPictureInPictureMode(...)` in `try/catch (IllegalStateException)` returning `false`, and wrap `openPictureInPictureSettings()` in `runCatching` with a fallback to `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`.

---

### Task 2: Fix Android 10–12 (API 29–32) Storage, `FileProvider`, Ringtone & Share Bugs

**Files:**
- Modify: `feature/videopicker/src/main/java/dev/roxy/kryptonplayer/feature/videopicker/screens/telegram/StorageControlBottomSheet.kt:388-392`
- Modify: `core/media/src/main/java/dev/roxy/kryptonplayer/core/media/services/LocalMediaService.kt:35-160`
- Modify: `feature/videopicker/src/main/java/dev/roxy/kryptonplayer/feature/videopicker/screens/mediapicker/MediaPickerScreen.kt:375-420`
- Modify: `feature/videopicker/src/main/java/dev/roxy/kryptonplayer/feature/videopicker/screens/telegram/TelegramViewModel.kt:310-325`
- Modify: `app/src/main/AndroidManifest.xml` (declare `WRITE_SETTINGS`)
- Modify: `feature/player/src/main/java/dev/roxy/kryptonplayer/feature/player/ui/ringtone/RingtoneMakerBottomSheet.kt:295-360`
- Modify: `core/data/src/main/java/dev/roxy/kryptonplayer/core/data/media/AudioRingtoneExtractor.kt:120-165`

- [x] **Step 1:** In `StorageControlBottomSheet.kt:390`, change `"${context.packageName}.provider"` → `"${context.packageName}.fileprovider"` to match `AndroidManifest.xml:73`.
- [x] **Step 2:** In `LocalMediaService.kt`:
  - Add `Intent.FLAG_GRANT_READ_URI_PERMISSION` in `shareMedia()` (lines 69–79).
  - Guard `createDeleteRequest` / `createWriteRequest` (API 30+) against empty URI lists, non-MediaStore URIs, null `mediaRequestLauncher`, and concurrent requests (via `Mutex` + `continuation.isActive`).
  - Handle Android 10 (API 29) `RecoverableSecurityException` during `deleteMediaBelowR` and `renameMediaBelowR` by launching `e.userAction.actionIntent.intentSender` via `mediaRequestLauncher`, and use `MediaStore.MediaColumns.DISPLAY_NAME` (without `_data`) when renaming under scoped API 29.
- [x] **Step 3:** In `MediaPickerScreen.kt:395-401`, for `FileOperationType.MOVE`, collect successfully copied `sourceUri`s and delete them through `MediaPickerUiEvent.DeleteVideos` / `MediaService.deleteMedia(...)` instead of raw `contentResolver.delete()`.
- [x] **Step 4:** In `TelegramViewModel.kt:313-320`, replace the stale `"NextPlayer/${it.nameWithExtension}"` `File.exists()` check with `telegramDownloadManager.isDownloadedToWatch(it)`.
- [x] **Step 5:** Add `<uses-permission android:name="android.permission.WRITE_SETTINGS" tools:ignore="ProtectedPermissions" />` to `app/src/main/AndroidManifest.xml`; guard notification-sound setting in `RingtoneMakerBottomSheet.kt:341-356` with `Settings.System.canWrite(context)`; and wrap API 29+ MediaStore inserts in `AudioRingtoneExtractor.kt:124-162` with `IS_PENDING = 1` → write → `IS_PENDING = 0` (deleting `newUri` on failure).

---

### Task 3: Fix Android 13–16 (API 33–36, `targetSdk = 36`) Enforcement Crashes

**Files:**
- Modify: `feature/player/src/main/java/dev/roxy/kryptonplayer/feature/player/state/VolumeState.kt:108-118`
- Modify: `feature/player/src/main/java/dev/roxy/kryptonplayer/feature/player/state/AudioOutputState.kt:148-158`
- Modify: `core/ui/src/main/java/dev/roxy/kryptonplayer/core/ui/components/PlayerStatusHUD.kt:70-80`
- Modify: `core/data/src/main/java/dev/roxy/kryptonplayer/core/data/server/UsbCinemaService.kt`
- Modify: `core/data/src/main/java/dev/roxy/kryptonplayer/core/data/download/TelegramDownloadService.kt`
- Modify: `core/data/src/main/AndroidManifest.xml` & `app/src/main/AndroidManifest.xml`
- Modify: `core/common/src/main/java/dev/roxy/kryptonplayer/core/common/Utils.kt:10-15`
- Modify: `app/src/main/java/dev/roxy/kryptonplayer/MainActivity.kt:292-393`
- Modify: `feature/videopicker/src/main/java/dev/roxy/kryptonplayer/feature/videopicker/screens/mediapicker/MediaPickerScreen.kt:329-335, 1280-1305`

- [x] **Step 1:** Replace all 3 unguarded `registerReceiver(...)` calls with `ContextCompat.registerReceiver(...)`:
  - `VolumeState.kt:112` → `ContextCompat.RECEIVER_EXPORTED` (+ `runCatching` on `unregisterReceiver`)
  - `AudioOutputState.kt:152` → `ContextCompat.RECEIVER_EXPORTED`
  - `PlayerStatusHUD.kt:74` → `ContextCompat.RECEIVER_NOT_EXPORTED`
- [x] **Step 2:** Harden `UsbCinemaService` & `TelegramDownloadService`:
  - Remove `CONNECTED_DEVICE` FGS type from `UsbCinemaService.kt` and both manifests (use `ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC` via `ServiceCompat.startForeground`), eliminating the unconditional API 34+ `SecurityException`.
  - Call `startForegroundWithNotification()` in `UsbCinemaService.onCreate()` before handling `ACTION_STOP`.
  - Change both services' `stop(context)` from `context.startService(...)` → `context.stopService(...)` and return `START_NOT_STICKY`.
  - Call `stopSelf()` and reset `isRunning = false` in `startForeground()` error catch blocks.
  - Override `override fun onTimeout(startId: Int, fgsType: Int)` (Android 15+ API 35) in both services to pause downloads / stop the HTTP server, release locks, and call `stopSelf(startId)`.
- [x] **Step 3:** Support Android 14+ Partial Media Access (`READ_MEDIA_VISUAL_USER_SELECTED`):
  - Add `<uses-permission android:name="android.permission.READ_MEDIA_VISUAL_USER_SELECTED" />` to `app/src/main/AndroidManifest.xml`.
  - Add `storagePermissions: Array<String>` and `Context.hasMediaAccessPermission(): Boolean` in `Utils.kt`.
  - Update `MainActivity.kt` (`RequestMultiplePermissions`) and `MediaPickerScreen.kt` (`rememberMultiplePermissionsState`) so users selecting *"Select photos and videos"* can browse/play their chosen videos cleanly (plus decouple `POST_NOTIFICATIONS` prompting from full storage access).

---

## Phase 2: Android 17 (`targetSdk = 37`) Upgrade & Android 18 Readiness

### Task 4: Bump `targetSdk` to `37` (Android 17) & Verify API 37 Rules

**Files:**
- Modify: `gradle/libs.versions.toml:2-4`
- Modify: `feature/player/src/main/AndroidManifest.xml:760-766`
- Modify: `app/src/main/AndroidManifest.xml` (verify loopback / local network declarations if needed for `UsbMediaServer`)
- Modify: `feature/player/src/main/java/dev/roxy/kryptonplayer/feature/player/ReelsActivity.kt`

- [x] **Step 1:** Update `android-targetSdk = "37"` in `gradle/libs.versions.toml` (matching `android-compileSdk = "37"`).
- [x] **Step 2:** Remove fixed `android:screenOrientation="portrait"` from `ReelsActivity` in `feature/player/src/main/AndroidManifest.xml:764` and ensure `ReelsActivity` adapts cleanly on wide/landscape/tablet windows (mandatory on `sw >= 600dp` under `targetSdk = 37`).
- [x] **Step 3:** Audit `UsbMediaServer.kt` (localhost/LAN HTTP server) against Android 17 local network & loopback permission rules (`USE_LOOPBACK_INTERFACE` / same-profile loopback), and verify `PlayerService` keeps all audio/focus/volume calls inside active foreground lifecycle (Android 17 background audio hardening).
- [x] **Step 4:** Verify zero runtime reflection writes to `static final` fields and zero `System.load()` calls on writable native files.
- [x] **Step 5 (Android 18 / API 38 Readiness Note):** Document the single-line upgrade path once Android 18 Beta 1 SDK ships (~Feb 2027): bump `android-compileSdk = "38"` and `android-targetSdk = "38"` in `gradle/libs.versions.toml` and test against Canary/Beta 1 behavioral changes (Compose mesh/blur APIs & adaptive windowing).

---

## Phase 3: Tablets, Foldables & Desktop Mode (Adaptive Large Screens)

### Task 5: Material 3 Adaptive Layouts (`WindowSizeClass` & Foldable Postures)

**Files:**
- Modify: `gradle/libs.versions.toml` & `core/ui/build.gradle.kts` / `feature/videopicker/build.gradle.kts` (add `androidx.compose.material3.adaptive` / `material3-window-size-class` if not already included via BOM)
- Modify: `feature/videopicker/src/main/java/dev/roxy/kryptonplayer/feature/videopicker/screens/mediapicker/MediaPickerScreen.kt`
- Modify: `feature/videopicker/src/main/java/dev/roxy/kryptonplayer/feature/videopicker/composables/MochyNavigationDock.kt`
- Modify: `feature/player/src/main/java/dev/roxy/kryptonplayer/feature/player/ReelsActivity.kt`

- [x] **Step 1:** Compute `WindowWidthSizeClass` (`Compact` / `Medium` / `Expanded`) in `MediaPickerScreen`:
  - `Compact` (phones): keep existing bottom dock (`MochyNavigationDock`) and 2-column grid.
  - `Medium` / `Expanded` (tablets, unfolded foldables, Android 17 desktop windows): switch to a left `NavigationRail` and scale grid columns (`GridCells.Adaptive(minSize = 180.dp)`) with centered max-width dialogs.
- [x] **Step 2:** In `ReelsActivity`, center a 9:16 video container on `Medium`/`Expanded` screens instead of stretching portrait video across wide tablet/desktop windows.

---

## Phase 4: Android TV & Google TV Support (`Leanback` + D-Pad Navigation)

### Task 6: Add Leanback Manifest, TV Banner & D-Pad Focus/Remote Controls

**Files:**
- Create: `app/src/main/res/drawable-xhdpi/tv_banner.png` (320×180 Krypton Player TV banner derived from `feature_graphic_1024x500.png`)
- Create: `core/ui/src/main/java/dev/roxy/kryptonplayer/core/ui/components/TvFocusModifier.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `feature/videopicker/src/main/java/dev/roxy/kryptonplayer/feature/videopicker/composables/VideoItem.kt` & `FolderItem.kt`
- Modify: `feature/player/src/main/java/dev/roxy/kryptonplayer/feature/player/PlayerActivity.kt`

- [x] **Step 1:** In `app/src/main/AndroidManifest.xml`:
  - Add `<uses-feature android:name="android.software.leanback" android:required="false" />`
  - Add `<uses-feature android:name="android.hardware.touchscreen" android:required="false" />`
  - Add `android:banner="@drawable/tv_banner"` on `<application>`
  - Add `<category android:name="android.intent.category.LEANBACK_LAUNCHER" />` to `MainActivity`'s launcher `<intent-filter>` (so Krypton Player appears directly on the Google TV / Android TV home screen).
- [x] **Step 2:** Add a reusable `Modifier.tvFocusable()` helper (`onFocusChanged` + bright focus border + subtle scale animation) and apply it to video cards, folder cards, and navigation items when `context.isTv()` is true.
- [x] **Step 3:** In `PlayerActivity.kt`, handle TV remote hardware keys (`KEYCODE_DPAD_CENTER`/`KEYCODE_ENTER` toggle play/pause or show controls, `KEYCODE_DPAD_LEFT`/`RIGHT` seek ±10s, `KEYCODE_MEDIA_PLAY_PAUSE`, `KEYCODE_MEDIA_NEXT`/`PREVIOUS`, `KEYCODE_BACK` dismiss overlay first).

---

## Phase 5: Android Auto & Android Automotive OS Support

### Task 7: Expose Krypton Player Audio Library via `MediaLibraryService`

**Files:**
- Create: `app/src/main/res/xml/automotive_app_desc.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `feature/player/src/main/AndroidManifest.xml:767-774`
- Modify: `feature/player/src/main/java/dev/roxy/kryptonplayer/feature/player/service/PlayerService.kt`

- [x] **Step 1:** Create `app/src/main/res/xml/automotive_app_desc.xml` with `<automotiveApp><uses name="media" /></automotiveApp>` and register `<meta-data android:name="com.google.android.gms.car.application" android:resource="@xml/automotive_app_desc" />` in `app/src/main/AndroidManifest.xml`.
- [x] **Step 2:** Update `PlayerService` in `feature/player/src/main/AndroidManifest.xml` to include `<action android:name="android.media.browse.MediaBrowserService" />` alongside `androidx.media3.session.MediaSessionService` (enabling Android Auto & Automotive OS discovery).
- [x] **Step 3:** Upgrade `PlayerService` from `MediaSessionService` → `MediaLibraryService` with `MediaLibrarySession.Callback`:
  - `onGetLibraryRoot(...)` → returns root browse item (`"krypton_root"`).
  - `onGetChildren(...)` → returns browseable categories (`"recent"`, `"folders"`, `"all_media"`) backed by `MediaRepository`, returning `MediaItem` entries playable as audio in Android Auto (complying with driver distraction rules).
  - `onAddMediaItems(...)` / `onSetMediaItems(...)` → resolves browsed `mediaId` to playable `MediaItem` URIs.

---

## Phase 6: Wear OS Companion Module (`:wear`)

### Task 8: Add Lightweight `:wear` Companion App (Now Playing Remote)

**Files:**
- Modify: `settings.gradle.kts` (add `include(":wear")`)
- Modify: `gradle/libs.versions.toml` (add `androidx-wear-compose-material3`, `androidx-wear-compose-foundation`, `play-services-wearable` / Wear Media controller deps)
- Create: `wear/build.gradle.kts` (`applicationId = "dev.roxy.vip.archplayer"`, `namespace = "dev.roxy.kryptonplayer.wear"`, `minSdk = 30`, `compileSdk = 37`, `targetSdk = 36`/`37`)
- Create: `wear/src/main/AndroidManifest.xml` (`<uses-feature android:name="android.hardware.type.watch" />`, standalone meta-data)
- Create: `wear/src/main/java/dev/roxy/kryptonplayer/wear/WearMainActivity.kt`
- Create: `wear/src/main/java/dev/roxy/kryptonplayer/wear/ui/WearNowPlayingScreen.kt`
- Create: `app/src/main/java/dev/roxy/kryptonplayer/wear/WearControlListenerService.kt` (phone-side `WearableListenerService` bridging watch play/pause/skip/volume commands to `PlayerService`)

- [x] **Step 1:** Configure `:wear` Gradle module with Compose for Wear OS Material 3, matching `applicationId = "dev.roxy.vip.archplayer"` and signing config.
- [x] **Step 2:** Build `WearNowPlayingScreen` showing current title, play/pause, previous/next track buttons, and rotary-crown volume control via Wearable Data Layer (`MessageClient` / `DataClient`).
- [x] **Step 3:** Add phone-side `WearControlListenerService` forwarding watch remote actions to the active `MediaController` / `PlayerService` and pushing now-playing metadata back to the watch.

---

## Verification Checklist (Run in Android Studio After Each Phase)

- [ ] **Phase 1:** Test on **Android 7.0 (API 24)** emulator (Shazam + PiP), **Android 10 (API 29)** emulator (media scan, delete, rename, share), and **Android 14/15/16** emulator (choose *"Select photos and videos"*, test Bluetooth/volume controls, Telegram download & USB Cinema FGS).
- [ ] **Phase 2 & 3:** Test on **Android 17 (API 37)** Phone + **Resizable / Foldable / Tablet** emulator in portrait, landscape, and split-screen modes.
- [ ] **Phase 4:** Launch on **Android TV / Google TV** emulator in Android Studio and verify launcher banner, D-pad browsing, and remote playback controls.
- [ ] **Phase 5:** Test with **Desktop Head Unit (DHU) / Android Automotive OS** emulator in Android Studio and verify Krypton Player appears in Media apps and plays audio.
- [ ] **Phase 6:** Run `:wear` module on a **Wear OS** emulator paired with the phone emulator and verify remote Play/Pause/Skip/Volume sync.
