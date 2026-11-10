# Music Player

A lightweight, professional, reliable native offline Android music player built with **Kotlin** and **Jetpack Compose**.

---

## 1. Overview & Architecture

**Music Player** is designed from the ground up for high-fidelity offline audio playback, rock-solid background playback stability, and modern user experience following official Android architecture guidelines.

- **No AI Models or Cloud Services**: 100% conventional, deterministic application code. Zero LLMs, machine learning models, cloud APIs, tracking, or network telemetry.
- **Offline-First & Privacy Preserving**: Operates without internet connectivity. All library indexing, playlists, favorites, and settings are stored locally.
- **Strict Separation of Concerns (MVVM)**: Audio playback is decoupled from Activity lifecycles, ensuring uninterrupted playback even when the UI is destroyed or the device is locked.

```
com.musicplayer.app/
├── MusicPlayerApplication.kt
├── data/
│   ├── local/
│   │   ├── dao/ (PlaylistDao, FavoriteDao, HistoryDao)
│   │   ├── entity/ (PlaylistEntity, PlaylistSongCrossRef, FavoriteSongEntity, RecentlyPlayedEntity)
│   │   └── MusicDatabase.kt (Room Database)
│   ├── mediastore/
│   │   └── MediaStoreScanner.kt (Querying MediaStore audio files)
│   └── repository/
│       ├── MusicRepository.kt
│       └── PreferencesRepository.kt (DataStore Preferences)
├── model/
│   ├── Song.kt
│   ├── Album.kt
│   ├── Artist.kt
│   ├── Folder.kt
│   ├── Playlist.kt
│   ├── PlaybackState.kt
│   ├── RepeatMode.kt
│   └── SortOrder.kt
├── player/
│   ├── PlayerConnection.kt (MediaController connection & queue state)
│   ├── EqualizerController.kt (Hardware audiofx wrapper)
│   └── SleepTimerManager.kt (Countdown & end-of-track sleep timer)
├── service/
│   └── MusicService.kt (MediaSessionService & background notification)
├── ui/
│   ├── MainActivity.kt
│   ├── components/ (MiniPlayer, SongItem, AlbumCard, ArtistCard, FolderItem, SearchBar, SortDialog, PermissionView, EmptyStateView)
│   ├── screens/
│   │   ├── MainScreen.kt
│   │   ├── NowPlayingScreen.kt
│   │   ├── QueueBottomSheet.kt
│   │   ├── PlaylistDetailScreen.kt
│   │   ├── EqualizerScreen.kt
│   │   ├── SleepTimerDialog.kt
│   │   ├── SongDetailsDialog.kt
│   │   ├── SettingsScreen.kt
│   │   └── tabs/ (SongsTab, AlbumsTab, ArtistsTab, FoldersTab, PlaylistsTab, FavoritesTab, RecentlyAddedTab, RecentlyPlayedTab)
│   └── theme/ (Color, Theme, Type)
└── util/
    ├── PermissionUtil.kt
    ├── TimeFormatter.kt
    └── FileUtil.kt
```

---

## 2. Technology Stack

- **Language**: Kotlin 2.0+
- **UI Toolkit**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM with Kotlin Coroutines & StateFlow
- **Playback Engine**: AndroidX Media3 ExoPlayer (`1.4.1`)
- **Background Playback**: `MediaSessionService`
- **Local Database**: Room (`2.6.1`)
- **Settings & State Storage**: Jetpack DataStore Preferences (`1.1.1`)
- **Audio Discovery**: Android `MediaStore`
- **Image Loading**: Coil Compose (`2.7.0`)
- **Build System**: Gradle 9.6 with Android Gradle Plugin (AGP)

---

## 3. Key Features

### Complete Music Library Management
- **All Songs**: Complete list of indexed audio files with title, artist, album, duration, and artwork.
- **Albums**: Visual grid of albums with song counts and artwork.
- **Artists**: Catalog of artists with album and track breakdowns.
- **Folders**: Physical directory browsing of music storage locations.
- **Recently Added**: Newest tracks added to device storage.
- **Recently Played**: Local listening history with one-tap clear option.
- **Favorites**: Heart toggle on any song with dedicated favorites tab.
- **User Playlists**: Create, rename, edit, and delete custom playlists persisted across app restarts.
- **Search & Sort**: Real-time filtering by keyword and sorting by Title (A-Z, Z-A), Artist (A-Z, Z-A), Album, Date Added, and Duration.

### Robust Background Playback (48-Hour Continuous Listening)
- **Independent Playback Service**: Managed by `MusicService` extending `MediaSessionService`. Does not depend on `MainActivity` staying alive.
- **Audio Focus**: Automatically ducks or pauses when phone calls or navigation directions occur, and resumes upon focus regain.
- **Becoming Noisy Handling**: Automatically pauses when headphones are unplugged or Bluetooth disconnects.
- **Media Controls**: Integrated system notification controls and lock screen playback actions.
- **Wake Lock Protection**: Uses `C.WAKE_MODE_LOCAL` to keep the CPU awake during active playback without excessive battery drain.
- **State Restoration**: Last played track and seek position are saved to local DataStore and restored across application restarts.

### Modern Now Playing Screen
- Immersive rounded album artwork display.
- Interactive progress seek bar with elapsed and remaining time display.
- Prominent Play/Pause, Skip Next, Skip Previous buttons.
- Shuffle mode toggle.
- Repeat mode cycling (Off, Repeat All, Repeat One).
- Playback speed adjustment (0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 2.0x).
- Live interactive Queue Bottom Sheet (view, jump, reorder, remove, clear).
- Favorite heart toggle.

### Audio Enhancements & Utilities
- **5-Band Equalizer**: Center frequencies (60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz) with -15 dB to +15 dB gain adjustment.
- **Hardware Presets**: Normal, Rock, Pop, Jazz, Classical, Heavy Metal, etc.
- **Bass Boost & Virtualizer (3D Audio)**: Slider controls for devices with hardware effect engines.
- **Sleep Timer**: Presets (5, 10, 15, 30, 45, 60, 90 minutes) or "End of Track" timer with live countdown and one-tap cancellation.
- **Themes & Accent Colors**: Light, Dark, or System mode with 5 distinct accent color palettes (Cyan Neon, Deep Purple, Electric Blue, Sunset Orange, Emerald Green).

---

## 4. Building the Application

The project is configured for command-line build using the Gradle wrapper (`gradlew`). Visual Studio Code or any terminal can be used directly without requiring Android Studio.

### Prerequisites
- **JDK**: Java 17 or higher
- **Android SDK**: API 34/35 installed with platform-tools and build-tools
- **Environment**: Set `sdk.dir` in `local.properties` (or set `ANDROID_HOME`)

### Build Commands

#### 1. Compile Kotlin Sources
```bash
./gradlew compileDebugKotlin
```

#### 2. Assemble Debug APK
```bash
./gradlew assembleDebug
```
The output APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

#### 3. Assemble Release APK
```bash
./gradlew assembleRelease
```
The output APK is generated at:
`app/build/outputs/apk/release/app-release-unsigned.apk`

#### 4. Clean Build Cache
```bash
./gradlew clean
```

---

## 5. Device Battery Optimization Guidance

To guarantee continuous 48-hour background listening on devices with aggressive manufacturer RAM management (e.g., Samsung, Xiaomi, Huawei, OnePlus):

1. Open device **Settings**.
2. Navigate to **Apps** > **Music Player**.
3. Select **Battery** (or **App Battery Usage**).
4. Choose **Unrestricted** (or disable "Optimize battery usage").
5. Exclude **Music Player** from aggressive third-party task killers.
