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

### 🎧 Long-Session Background Playback
- **MediaSessionService Architecture**: Playback runs in an independent foreground service decoupled from the Activity lifecycle. Music continues effortlessly whether the phone is locked, the display is turned off, or you switch between memory-heavy applications.
- **Extended Listening**: Configured with `C.WAKE_MODE_LOCAL` to keep the audio subsystem active for continuous multi-hour sessions (tested for 48h+ playback without interruption).
- **Smart Audio Focus**: Automatically pauses for incoming phone calls and resumes once finished.
- **Audio Becoming Noisy Guard**: Instant auto-pause when wired earphones are unplugged or Bluetooth headphones disconnect, preventing unexpected audio playback over loudspeakers.
- **Lock Screen & Headset Controls**: Full notification media controls, lock screen scrubber, and standard Bluetooth hardware button integration.

### 📂 Comprehensive Music Library
- **MediaStore Integration**: Automatically queries local storage with defensive handling for missing metadata and tags.
- **8 Dedicated Library Tabs**:
  - **Songs**: Chronological or alphabetical list of every track.
  - **Albums**: Grouped collections with embedded album art.
  - **Artists**: Discographies organized by performing artist.
  - **Folders**: Direct directory-level browsing across storage partitions.
  - **Recently Added**: Newly imported or downloaded tracks.
  - **Recently Played**: Real-time listening history tracked locally.
  - **Favorites**: One-tap favorited songs stored in Room database.
  - **Playlists**: Custom user-created playlists with full editing support.
- **Instant Search**: Real-time multi-field search across titles, artists, and album names.
- **Flexible Sorting**: Sort any view by title, artist, album, or date added in ascending or descending order.

### 🎚 Hardware Audio Tuning
- **5-Band Graphic Equalizer**: Directly interfaces with Android's audio effects pipeline.
- **Preset Tunings**: Switch between 9 presets including *Rock, Classical, Dance, Flat, Heavy Metal, Hip Hop, Jazz, Pop,* and *Normal*.
- **Bass Boost & Virtualizer**: Granular sliders to boost low-end frequencies and expand spatial 3D surround sound.

### ⏱ Smart Sleep Timer
- Fall asleep to your favorite music without running down the device battery overnight.
- Choose between preset countdowns (15m, 30m, 45m, 60m), custom minute values, or let the player automatically turn off after the **current track finishes**.

### 🎨 Material 3 Customization
- Seamless **Dark** and **Light** themes.
- Five distinct accent palettes: *Royal Purple, Ocean Blue, Electric Teal, Sunset Orange,* and *Emerald Green*.
- Persistent bottom mini-player for smooth, single-tap access back to the full-screen player.

### 🔒 100% Offline & Private
- Strictly zero network dependencies: no ads, no trackers, no cloud sync, and zero AI telemetry.
- All playlists, playback history, and user settings stay entirely on your device.

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
├── player             # PlayerConnection (MediaController bridge), Equalizer, SleepTimer
├── service            # MusicService (MediaSessionService, AudioFocus, WakeLock)
├── ui
│   ├── components     # Reusable Compose widgets (MiniPlayer, SongItem, AlbumCard, SearchBar)
│   ├── screens        # Full screens (MainScreen, NowPlayingScreen, EqualizerScreen, SettingsScreen)
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
