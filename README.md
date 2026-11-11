# Music Player

<div align="center">
  <img src="docs/screenshots/00_hero_banner.png" alt="Music Player Banner" width="100%" />
</div>

<p align="center">
  <b>A modern, offline native Android music player crafted for seamless, high-fidelity audio listening and rock-solid background playback.</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.0.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Compose Material 3" />
  <img src="https://img.shields.io/badge/ExoPlayer-Media3%201.4.1-FF6F00?style=for-the-badge&logo=google&logoColor=white" alt="Media3" />
  <img src="https://img.shields.io/badge/Database-Room%20SQLite-F58220?style=for-the-badge&logo=sqlite&logoColor=white" alt="Room" />
  <img src="https://img.shields.io/badge/Android-7.0%20to%2015%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android Support" />
  <img src="https://img.shields.io/badge/Privacy-100%25%20Offline-107C41?style=for-the-badge&logo=shield&logoColor=white" alt="100% Offline" />
</p>

---

## 📖 Overview

**Music Player** is a clean, offline-first audio application built with modern Android development standards. Designed for listeners who prefer storing their music locally without tracking, intrusive subscriptions, or unnecessary battery drain, it provides an uninterrupted listening experience powered by Google's latest **AndroidX Media3** architecture.

From dynamic folder exploration and fast local search to an integrated 5-band graphic equalizer and reliable lock-screen controls, Music Player delivers complete control over your music library.

---

## 📱 Visual Showcase

<div align="center">
  <table>
    <tr>
      <th width="33%">Now Playing</th>
      <th width="33%">Library & Search</th>
      <th width="33%">5-Band Equalizer</th>
    </tr>
    <tr>
      <td><img src="docs/screenshots/01_now_playing.png" alt="Now Playing Screen" width="100%" /></td>
      <td><img src="docs/screenshots/02_library.png" alt="Music Library" width="100%" /></td>
      <td><img src="docs/screenshots/03_equalizer.png" alt="Equalizer Screen" width="100%" /></td>
    </tr>
    <tr>
      <td align="center"><i>Album Art, Progress & Speed</i></td>
      <td align="center"><i>8 Categorized Tabs & Mini-Player</i></td>
      <td align="center"><i>Frequency Sliders & Bass Boost</i></td>
    </tr>
  </table>
</div>

---

## ✨ Features

### 🎧 Persistent Playback & Background Operation
- **MediaSessionService Architecture**: Audio playback runs inside an independent foreground service decoupled from the Activity lifecycle. Music continues effortlessly whether the screen is locked, display is turned off, or you switch between heavy applications.
- **Exact Position Restoration**: Automatically saves playback progress, track ID, complete queue, queue index, repeat mode, shuffle state, and playback speed to local DataStore. When reopening the application, playback resumes from the exact saved millisecond without starting over.
- **Smart System Back Navigation**: Implements an explicit navigation stack (`Navigator` + `BackHandler`). Pressing the system Back button on sub-screens (Settings, Equalizer, Album, Artist, Folder, Playlist, and Now Playing) returns to the previous screen. Pressing Back on the main library smoothly sends the application to the background (`moveTaskToBack`) without stopping music.
- **Extended Listening Sessions**: Engineered for continuous multi-hour and multi-day playback sessions with battery-conscious audio wake locks.
- **Smart Audio Focus**: Automatically pauses for incoming calls and resumes cleanly when finished.
- **Audio Becoming Noisy Protection**: Instantly auto-pauses when headphones are unplugged or Bluetooth disconnects, preventing unexpected playback through loudspeakers.

### 📂 Comprehensive Music Library & Collection Views
- **MediaStore Discovery**: Fast asynchronous scanning with defensive null-safety for incomplete or missing audio tags.
- **8 Library Tabs**:
  - **Songs**: Complete list of every local track with sorting and quick action menus.
  - **Albums**: Grid of album collections with embedded artwork and release years.
  - **Artists**: Discographies organized by artist with album and track counts.
  - **Folders**: Direct directory-level storage browsing.
  - **Recently Added**: Chronologically ordered by newest additions.
  - **Recently Played**: Real-time local listening history.
  - **Favorites**: One-tap favorited tracks persisted in Room SQLite.
  - **Playlists**: Custom playlists with creation, reordering, and editing tools.
- **Dedicated Collection Detail Screens**: Tapping any album, artist, or folder opens an immersive detail view with full track listings, total duration counters, "Play All", and "Shuffle" buttons.
- **Instant Search & Sort**: Real-time filtering across titles, artists, and albums, plus multi-criteria sorting.

### 🎚 Hardware Audio Equalizer
- **Active Audio Session Attachment**: Directly connected to ExoPlayer's active audio session ID (`audioSessionId > 0`) via service analytics listeners for reliable hardware effect initialization.
- **Standard Presets**: Includes *Normal, Rock, Pop, Jazz, Classical, Bass Boost, Vocal,* and *Custom* presets.
- **5-Band Sliders & Effects**: Granular frequency sliders, hardware bass boost, and spatial virtualizer.
- **Honest Diagnostics**: Displays clear, real-time hardware status messages when effects are active or unsupported on specific device hardware.

### ⚙️ Comprehensive Settings Suite (8 Sections)
- **Appearance**: Dark theme and true pitch-black AMOLED mode toggles.
- **Playback Control**: Toggle position memory, configurable crossfade (0–10s), and gapless playback.
- **Background Playback**: Disconnect auto-pause toggle and device-specific battery optimization guide.
- **Library Management**: Automatic scanner trigger and short audio exclusion filter (< 30 seconds) to hide ringtones and voice notes.
- **Notification Styling**: System media notification style configuration.
- **Storage & Playlist Backup**: Full JSON export and import for user playlists (`playlists_backup_*.json`) and Coil album artwork cache clearing.
- **Privacy Assurance**: 100% offline verification confirming zero internet access, zero trackers, and zero telemetry.
- **About**: Version information, package identifier, and open-source license.

### ⏱ Smart Sleep Timer
- Presets for 15, 30, 45, and 60 minutes, custom minute input, or automatic shut-off when the current song completes.

### 🔒 100% Offline & Private
- Strictly zero network permissions: no ads, no trackers, no accounts, and zero cloud telemetry. All database records and settings stay entirely on device.

---

## 📥 Download & Installation

The latest release is pre-compiled, signed, and ready to install on Android devices running **Android 7.0 (API 24)** or higher.

<p align="center">
  <a href="https://github.com/sobuj-here/Music-Player/releases/download/v1.0.0/MusicPlayer-v1.0.0.apk">
    <img src="https://img.shields.io/badge/Download%20APK-v1.0.0%20(Latest)-00E5FF?style=for-the-badge&logo=android&logoColor=black" alt="Download APK" />
  </a>
  <a href="https://github.com/sobuj-here/Music-Player/releases">
    <img src="https://img.shields.io/badge/Release%20Notes-v1.0.0-181717?style=for-the-badge&logo=github&logoColor=white" alt="Release Notes" />
  </a>
</p>

### Direct Installation
1. Tap the **Download APK** button above to download `MusicPlayer-v1.0.0.apk`.
2. Open the downloaded file on your Android device and confirm installation.
3. Grant audio permission on initial startup to allow the app to discover your local library.

### Installation via ADB
```bash
adb install -r releases/MusicPlayer-v1.0.0-release.apk
```

---

## 🏗 Architecture & Code Structure

The application adopts the official Android Architecture patterns (MVVM + Repository) with reactive state management:

```
com.musicplayer.app
├── data
│   ├── local          # Room Database, DAOs, & Entities (Playlists, Favorites, History)
│   ├── mediastore     # Async MediaStore scanner with robust column null-safety
│   └── repository     # Single sources of truth: MusicRepository & PreferencesRepository
├── model              # Immutable domain models (Song, Album, Artist, Playlist, PlaybackState)
├── navigation         # Navigation stack (Navigator & Screen hierarchy with BackHandler)
├── player             # PlayerConnection (MediaController bridge), EqualizerController, SleepTimer
├── service            # MusicService (MediaSessionService, AudioFocus, AudioSessionId hooks)
├── settings           # BackupManager (JSON playlist export & import)
├── ui
│   ├── components     # Reusable Compose widgets (MiniPlayer, SongItem, AlbumCard, SearchBar)
│   ├── screens        # Full screens (MainScreen, NowPlayingScreen, EqualizerScreen, SettingsScreen)
│   │   └── detail     # Collection detail views (AlbumDetailScreen, ArtistDetailScreen, FolderDetailScreen)
│   └── theme          # Dynamic Material 3 palettes, Typography, and System Bar styling
├── util               # Modern permission utilities (API 33+ READ_MEDIA_AUDIO), time formatters
└── viewmodel          # ViewModels managing UI state flows (Main, Playlist, Equalizer, Settings)
```

---

## 🛠 Building From Source

### Prerequisites
- Java Development Kit (JDK) 17
- Android SDK (compileSdk 35, build-tools 35.0.0)
- Visual Studio Code or Android Studio

### Compilation Steps

```bash
# Clone the repository
git clone https://github.com/sobuj-here/Music-Player.git
cd Music-Player

# Build debug APK
./gradlew assembleDebug

# Build signed release APK
./gradlew assembleRelease
```

Generated APK paths:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

---

## 🔋 Recommended Battery Settings

Certain Android distributions (such as Samsung One UI, Xiaomi MIUI/HyperOS, OnePlus OxygenOS, and Huawei EMUI) aggressively restrict background services after extended periods of screen inactivity.

To ensure uninterrupted multi-day playback:
1. Open **Settings > Apps > Music Player > Battery**.
2. Set battery usage to **Unrestricted** (or turn off *Battery Optimization*).
3. Ensure background activity permissions are permitted.

---

## 📄 License

This project is licensed under the **MIT License**. Free for personal and developer use.
