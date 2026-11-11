package com.musicplayer.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musicplayer.app.model.Album
import com.musicplayer.app.model.Artist
import com.musicplayer.app.model.Folder
import com.musicplayer.app.model.Playlist
import com.musicplayer.app.model.Song
import com.musicplayer.app.ui.components.EmptyStateView
import com.musicplayer.app.ui.components.MiniPlayer
import com.musicplayer.app.ui.components.PermissionView
import com.musicplayer.app.ui.components.SearchBar
import com.musicplayer.app.ui.components.SongItem
import com.musicplayer.app.ui.components.SortDialog
import com.musicplayer.app.ui.screens.tabs.AlbumsTab
import com.musicplayer.app.ui.screens.tabs.ArtistsTab
import com.musicplayer.app.ui.screens.tabs.FavoritesTab
import com.musicplayer.app.ui.screens.tabs.FoldersTab
import com.musicplayer.app.ui.screens.tabs.PlaylistsTab
import com.musicplayer.app.ui.screens.tabs.RecentlyAddedTab
import com.musicplayer.app.ui.screens.tabs.RecentlyPlayedTab
import com.musicplayer.app.ui.screens.tabs.SongsTab
import com.musicplayer.app.viewmodel.EqualizerViewModel
import com.musicplayer.app.viewmodel.MainViewModel
import com.musicplayer.app.viewmodel.PlaylistViewModel
import com.musicplayer.app.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    playlistViewModel: PlaylistViewModel,
    equalizerViewModel: EqualizerViewModel,
    settingsViewModel: SettingsViewModel,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasPermission by mainViewModel.hasPermission.collectAsState()
    val isScanning by mainViewModel.isScanning.collectAsState()
    val searchQuery by mainViewModel.searchQuery.collectAsState()
    val sortOrder by mainViewModel.sortOrder.collectAsState()
    val playbackState by mainViewModel.playbackState.collectAsState()

    val songs by mainViewModel.allSongs.collectAsState()
    val albums by mainViewModel.albums.collectAsState()
    val artists by mainViewModel.artists.collectAsState()
    val folders by mainViewModel.folders.collectAsState()
    val favorites by mainViewModel.favorites.collectAsState()
    val recentlyAdded by mainViewModel.recentlyAdded.collectAsState()
    val recentlyPlayed by mainViewModel.recentlyPlayed.collectAsState()
    val playlists by mainViewModel.playlists.collectAsState()

    val equalizerState by equalizerViewModel.state.collectAsState()
    val sleepTimerState by settingsViewModel.sleepTimerState.collectAsState()
    val themeMode by settingsViewModel.themeMode.collectAsState()
    val accentColorIndex by settingsViewModel.accentColorIndex.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showNowPlaying by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showEqualizerScreen by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }

    // Playlist details state
    var activePlaylist by remember { mutableStateOf<Playlist?>(null) }
    val playlistSongs by (activePlaylist?.let {
        playlistViewModel.getSongsForPlaylist(it.id)
    } ?: kotlinx.coroutines.flow.flowOf(emptyList())).collectAsState(initial = emptyList())

    // Sub-list dialog state (Album, Artist, Folder songs)
    var activeSubListTitle by remember { mutableStateOf<String?>(null) }
    var activeSubListSongs by remember { mutableStateOf<List<Song>>(emptyList()) }

    // Add to playlist modal state
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    // Song details dialog
    var songForDetails by remember { mutableStateOf<Song?>(null) }

    val tabs = listOf(
        "Songs",
        "Albums",
        "Artists",
        "Folders",
        "Playlists",
        "Favorites",
        "Recent",
        "History"
    )

    if (!hasPermission) {
        PermissionView(onRequestPermission = onRequestPermission)
        return
    }

    Scaffold(
        topBar = {
            Column {
                // Header with Search and Settings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Music Player",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = { showSettingsScreen = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Search Bar
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { mainViewModel.setSearchQuery(it) },
                    onClear = { mainViewModel.setSearchQuery("") }
                )

                // Scrollable Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = (selectedTabIndex == index),
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
        },
        bottomBar = {
            MiniPlayer(
                playbackState = playbackState,
                onPlayPauseClick = { mainViewModel.togglePlayPause() },
                onNextClick = { mainViewModel.skipNext() },
                onExpandClick = { showNowPlaying = true }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTabIndex) {
                0 -> SongsTab(
                    songs = songs,
                    currentSongId = playbackState.currentSong?.id,
                    isPlaying = playbackState.isPlaying,
                    onSongClick = { mainViewModel.playSong(it, songs) },
                    onPlayAll = { mainViewModel.playAll(songs) },
                    onShuffleAll = { mainViewModel.shuffleAll(songs) },
                    onSortClick = { showSortDialog = true },
                    onFavoriteToggle = { mainViewModel.toggleFavorite(it) },
                    onPlayNext = { mainViewModel.playNext(it) },
                    onAddToQueue = { mainViewModel.addToQueue(it) },
                    onAddToPlaylist = { songToAddToPlaylist = it },
                    onShowDetails = { songForDetails = it },
                    onRefresh = { mainViewModel.refreshLibrary() }
                )
                1 -> AlbumsTab(
                    albums = albums,
                    onAlbumClick = { album ->
                        activeSubListTitle = album.title
                        activeSubListSongs = songs.filter { it.albumId == album.id }
                    },
                    onRefresh = { mainViewModel.refreshLibrary() }
                )
                2 -> ArtistsTab(
                    artists = artists,
                    onArtistClick = { artist ->
                        activeSubListTitle = artist.name
                        activeSubListSongs = songs.filter { it.artist.equals(artist.name, ignoreCase = true) }
                    },
                    onRefresh = { mainViewModel.refreshLibrary() }
                )
                3 -> FoldersTab(
                    folders = folders,
                    onFolderClick = { folder ->
                        activeSubListTitle = folder.name
                        activeSubListSongs = songs.filter {
                            java.io.File(it.data).parentFile?.absolutePath == folder.path
                        }
                    },
                    onRefresh = { mainViewModel.refreshLibrary() }
                )
                4 -> PlaylistsTab(
                    playlists = playlists,
                    onCreatePlaylistClick = {
                        newPlaylistName = ""
                        showCreatePlaylistDialog = true
                    },
                    onPlaylistClick = { activePlaylist = it }
                )
                5 -> FavoritesTab(
                    favorites = favorites,
                    currentSongId = playbackState.currentSong?.id,
                    isPlaying = playbackState.isPlaying,
                    onSongClick = { mainViewModel.playSong(it, favorites) },
                    onPlayAll = { mainViewModel.playAll(favorites) },
                    onShuffleAll = { mainViewModel.shuffleAll(favorites) },
                    onFavoriteToggle = { mainViewModel.toggleFavorite(it) },
                    onPlayNext = { mainViewModel.playNext(it) },
                    onAddToQueue = { mainViewModel.addToQueue(it) },
                    onAddToPlaylist = { songToAddToPlaylist = it },
                    onShowDetails = { songForDetails = it }
                )
                6 -> RecentlyAddedTab(
                    songs = recentlyAdded,
                    currentSongId = playbackState.currentSong?.id,
                    isPlaying = playbackState.isPlaying,
                    onSongClick = { mainViewModel.playSong(it, recentlyAdded) },
                    onFavoriteToggle = { mainViewModel.toggleFavorite(it) },
                    onPlayNext = { mainViewModel.playNext(it) },
                    onAddToQueue = { mainViewModel.addToQueue(it) },
                    onAddToPlaylist = { songToAddToPlaylist = it },
                    onShowDetails = { songForDetails = it }
                )
                7 -> RecentlyPlayedTab(
                    songs = recentlyPlayed,
                    currentSongId = playbackState.currentSong?.id,
                    isPlaying = playbackState.isPlaying,
                    onSongClick = { mainViewModel.playSong(it, recentlyPlayed) },
                    onClearHistory = { mainViewModel.clearHistory() },
                    onFavoriteToggle = { mainViewModel.toggleFavorite(it) },
                    onPlayNext = { mainViewModel.playNext(it) },
                    onAddToQueue = { mainViewModel.addToQueue(it) },
                    onAddToPlaylist = { songToAddToPlaylist = it },
                    onShowDetails = { songForDetails = it }
                )
            }
        }
    }

    // Now Playing Fullscreen Screen
    AnimatedVisibility(
        visible = showNowPlaying,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        NowPlayingScreen(
            playbackState = playbackState,
            sleepTimerActive = sleepTimerState.isActive,
            onCollapse = { showNowPlaying = false },
            onPlayPause = { mainViewModel.togglePlayPause() },
            onNext = { mainViewModel.skipNext() },
            onPrevious = { mainViewModel.skipPrevious() },
            onSeek = { mainViewModel.seekTo(it) },
            onToggleShuffle = { mainViewModel.toggleShuffle() },
            onToggleRepeat = { mainViewModel.toggleRepeat() },
            onSpeedSelected = { mainViewModel.setPlaybackSpeed(it) },
            onFavoriteToggle = {
                playbackState.currentSong?.let { mainViewModel.toggleFavorite(it) }
            },
            onOpenQueue = { showQueueSheet = true },
            onOpenEqualizer = { showEqualizerScreen = true },
            onOpenSleepTimer = { showSleepTimerDialog = true },
            onShowDetails = {
                playbackState.currentSong?.let { songForDetails = it }
            }
        )
    }

    // Queue Bottom Sheet
    if (showQueueSheet) {
        QueueBottomSheet(
            queue = playbackState.queue,
            currentIndex = playbackState.currentQueueIndex,
            isPlaying = playbackState.isPlaying,
            onSongClick = { mainViewModel.playQueueIndex(it) },
            onRemoveSong = { mainViewModel.removeFromQueue(it) },
            onMoveItem = { from, to -> mainViewModel.moveQueueItem(from, to) },
            onClearQueue = { mainViewModel.clearQueue() },
            onDismiss = { showQueueSheet = false }
        )
    }

    // Equalizer Screen Modal
    AnimatedVisibility(
        visible = showEqualizerScreen,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        EqualizerScreen(
            state = equalizerState,
            onEnableChange = { equalizerViewModel.setEnabled(it) },
            onBandChange = { band, level -> equalizerViewModel.setBandLevel(band, level) },
            onPresetSelect = { equalizerViewModel.usePreset(it) },
            onBassBoostChange = { equalizerViewModel.setBassBoost(it) },
            onVirtualizerChange = { equalizerViewModel.setVirtualizer(it) },
            onBackClick = { showEqualizerScreen = false }
        )
    }

    // Settings Screen Modal
    AnimatedVisibility(
        visible = showSettingsScreen,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        SettingsScreen(
            themeMode = themeMode,
            accentColorIndex = accentColorIndex,
            onThemeModeChange = { settingsViewModel.setThemeMode(it) },
            onAccentColorChange = { settingsViewModel.setAccentColorIndex(it) },
            onOpenEqualizer = {
                showSettingsScreen = false
                showEqualizerScreen = true
            },
            onOpenSleepTimer = {
                showSettingsScreen = false
                showSleepTimerDialog = true
            },
            onBackClick = { showSettingsScreen = false }
        )
    }

    // Playlist Details Screen Modal
    val playlist = activePlaylist
    if (playlist != null) {
        PlaylistDetailScreen(
            playlist = playlist,
            songs = playlistSongs,
            currentSongId = playbackState.currentSong?.id,
            isPlaying = playbackState.isPlaying,
            onBackClick = { activePlaylist = null },
            onSongClick = { mainViewModel.playSong(it, playlistSongs) },
            onPlayAll = { mainViewModel.playAll(playlistSongs) },
            onShuffleAll = { mainViewModel.shuffleAll(playlistSongs) },
            onRenamePlaylist = { playlistViewModel.renamePlaylist(playlist.id, it) },
            onDeletePlaylist = {
                playlistViewModel.deletePlaylist(playlist.id)
                activePlaylist = null
            },
            onRemoveSong = { playlistViewModel.removeSongFromPlaylist(playlist.id, it.id) },
            onFavoriteToggle = { mainViewModel.toggleFavorite(it) },
            onPlayNext = { mainViewModel.playNext(it) },
            onAddToQueue = { mainViewModel.addToQueue(it) },
            onShowDetails = { songForDetails = it }
        )
    }

    // Sub-list dialog (Album, Artist, or Folder songs)
    if (activeSubListTitle != null) {
        AlertDialog(
            onDismissRequest = { activeSubListTitle = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeSubListTitle ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { activeSubListTitle = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                mainViewModel.playAll(activeSubListSongs)
                                activeSubListTitle = null
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Play All")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                mainViewModel.shuffleAll(activeSubListSongs)
                                activeSubListTitle = null
                            },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Shuffle")
                        }
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = activeSubListSongs,
                            key = { it.id }
                        ) { song ->
                            SongItem(
                                song = song,
                                isPlaying = playbackState.isPlaying,
                                isCurrentTrack = (song.id == playbackState.currentSong?.id),
                                onClick = {
                                    mainViewModel.playSong(song, activeSubListSongs)
                                    activeSubListTitle = null
                                },
                                onFavoriteToggle = { mainViewModel.toggleFavorite(song) },
                                onPlayNext = { mainViewModel.playNext(song) },
                                onAddToQueue = { mainViewModel.addToQueue(song) },
                                onAddToPlaylist = { songToAddToPlaylist = song },
                                onShowDetails = { songForDetails = song }
                            )
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Add to Playlist Bottom Sheet
    val songToAdd = songToAddToPlaylist
    if (songToAdd != null) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { songToAddToPlaylist = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Add to Playlist",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = songToAdd.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        songToAddToPlaylist = null
                        newPlaylistName = ""
                        showCreatePlaylistDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("+ Create New Playlist")
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.height(240.dp)) {
                    items(items = playlists, key = { it.id }) { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playlistViewModel.addSongToPlaylist(p.id, songToAdd.id)
                                    songToAddToPlaylist = null
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(p.name, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    singleLine = true,
                    label = { Text("Playlist Name") },
                    placeholder = { Text("My Favorites, Workout, etc.") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            playlistViewModel.createPlaylist(newPlaylistName)
                            showCreatePlaylistDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Sort Dialog
    if (showSortDialog) {
        SortDialog(
            currentSort = sortOrder,
            onSortSelected = { mainViewModel.setSortOrder(it) },
            onDismiss = { showSortDialog = false }
        )
    }

    // Sleep Timer Dialog
    if (showSleepTimerDialog) {
        SleepTimerDialog(
            state = sleepTimerState,
            onStartTimer = { settingsViewModel.startSleepTimer(it) },
            onStartEndOfTrack = { settingsViewModel.startEndOfTrackTimer() },
            onCancelTimer = { settingsViewModel.cancelSleepTimer() },
            onDismiss = { showSleepTimerDialog = false }
        )
    }

    // Song Details Dialog
    val detailsSong = songForDetails
    if (detailsSong != null) {
        SongDetailsDialog(
            song = detailsSong,
            onDismiss = { songForDetails = null }
        )
    }
}
