package com.musicplayer.app.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musicplayer.app.model.Album
import com.musicplayer.app.model.Artist
import com.musicplayer.app.model.Folder
import com.musicplayer.app.model.Playlist
import com.musicplayer.app.model.Song
import com.musicplayer.app.navigation.Navigator
import com.musicplayer.app.navigation.Screen
import com.musicplayer.app.ui.components.EmptyStateView
import com.musicplayer.app.ui.components.MiniPlayer
import com.musicplayer.app.ui.components.PermissionView
import com.musicplayer.app.ui.components.SearchBar
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
import java.io.File

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
    val context = LocalContext.current
    val activity = context as? Activity

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

    // Dedicated Navigation Stack
    val navigator = remember { Navigator(Screen.Library) }

    // Preserve tab selection across navigation
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    // Dialog / Sheet states
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
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

    // Hardware / Gesture Back Handling
    BackHandler(enabled = true) {
        when {
            showQueueSheet -> showQueueSheet = false
            songForDetails != null -> songForDetails = null
            songToAddToPlaylist != null -> songToAddToPlaylist = null
            showCreatePlaylistDialog -> showCreatePlaylistDialog = false
            showSortDialog -> showSortDialog = false
            showSleepTimerDialog -> showSleepTimerDialog = false
            searchQuery.isNotBlank() -> mainViewModel.setSearchQuery("")
            navigator.canPop -> navigator.pop()
            else -> {
                // At root screen (Library): move task to background without stopping playback!
                activity?.moveTaskToBack(true)
            }
        }
    }

    if (!hasPermission) {
        PermissionView(onRequestPermission = onRequestPermission)
        return
    }

    AnimatedContent(
        targetState = navigator.currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "ScreenTransition"
    ) { currentScreen ->
        when (currentScreen) {
            is Screen.Library -> {
                Scaffold(
                    topBar = {
                        Column {
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

                                IconButton(onClick = { navigator.navigateTo(Screen.Settings) }) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            SearchBar(
                                query = searchQuery,
                                onQueryChange = { mainViewModel.setSearchQuery(it) },
                                onClear = { mainViewModel.setSearchQuery("") }
                            )

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
                            onExpandClick = { navigator.navigateTo(Screen.NowPlaying) }
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
                                onAlbumClick = { navigator.navigateTo(Screen.AlbumDetail(it)) },
                                onRefresh = { mainViewModel.refreshLibrary() }
                            )

                            2 -> ArtistsTab(
                                artists = artists,
                                onArtistClick = { navigator.navigateTo(Screen.ArtistDetail(it)) },
                                onRefresh = { mainViewModel.refreshLibrary() }
                            )

                            3 -> FoldersTab(
                                folders = folders,
                                onFolderClick = { navigator.navigateTo(Screen.FolderDetail(it)) },
                                onRefresh = { mainViewModel.refreshLibrary() }
                            )

                            4 -> PlaylistsTab(
                                playlists = playlists,
                                onCreatePlaylistClick = { showCreatePlaylistDialog = true },
                                onPlaylistClick = { navigator.navigateTo(Screen.PlaylistDetail(it)) }
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
            }

            is Screen.AlbumDetail -> {
                val album = currentScreen.album
                val albumSongs = songs.filter { it.albumId == album.id }
                AlbumDetailScreen(
                    album = album,
                    songs = albumSongs,
                    currentSongId = playbackState.currentSong?.id,
                    isPlaying = playbackState.isPlaying,
                    onBackClick = { navigator.pop() },
                    onSongClick = { mainViewModel.playSong(it, albumSongs) },
                    onPlayAll = { mainViewModel.playAll(albumSongs) },
                    onShuffleAll = { mainViewModel.shuffleAll(albumSongs) },
                    onFavoriteToggle = { mainViewModel.toggleFavorite(it) },
                    onPlayNext = { mainViewModel.playNext(it) },
                    onAddToQueue = { mainViewModel.addToQueue(it) },
                    onAddToPlaylist = { songToAddToPlaylist = it },
                    onShowDetails = { songForDetails = it }
                )
            }

            is Screen.ArtistDetail -> {
                val artist = currentScreen.artist
                val artistSongs = songs.filter { it.artist.equals(artist.name, ignoreCase = true) }
                ArtistDetailScreen(
                    artist = artist,
                    songs = artistSongs,
                    currentSongId = playbackState.currentSong?.id,
                    isPlaying = playbackState.isPlaying,
                    onBackClick = { navigator.pop() },
                    onSongClick = { mainViewModel.playSong(it, artistSongs) },
                    onPlayAll = { mainViewModel.playAll(artistSongs) },
                    onShuffleAll = { mainViewModel.shuffleAll(artistSongs) },
                    onFavoriteToggle = { mainViewModel.toggleFavorite(it) },
                    onPlayNext = { mainViewModel.playNext(it) },
                    onAddToQueue = { mainViewModel.addToQueue(it) },
                    onAddToPlaylist = { songToAddToPlaylist = it },
                    onShowDetails = { songForDetails = it }
                )
            }

            is Screen.FolderDetail -> {
                val folder = currentScreen.folder
                val folderSongs = songs.filter { File(it.data).parent == folder.path }
                FolderDetailScreen(
                    folder = folder,
                    songs = folderSongs,
                    currentSongId = playbackState.currentSong?.id,
                    isPlaying = playbackState.isPlaying,
                    onBackClick = { navigator.pop() },
                    onSongClick = { mainViewModel.playSong(it, folderSongs) },
                    onPlayAll = { mainViewModel.playAll(folderSongs) },
                    onShuffleAll = { mainViewModel.shuffleAll(folderSongs) },
                    onFavoriteToggle = { mainViewModel.toggleFavorite(it) },
                    onPlayNext = { mainViewModel.playNext(it) },
                    onAddToQueue = { mainViewModel.addToQueue(it) },
                    onAddToPlaylist = { songToAddToPlaylist = it },
                    onShowDetails = { songForDetails = it }
                )
            }

            is Screen.PlaylistDetail -> {
                val pl = currentScreen.playlist
                val plSongs by playlistViewModel.getSongsForPlaylist(pl.id).collectAsState(initial = emptyList())
                PlaylistDetailScreen(
                    playlist = pl,
                    songs = plSongs,
                    currentSongId = playbackState.currentSong?.id,
                    isPlaying = playbackState.isPlaying,
                    onBackClick = { navigator.pop() },
                    onSongClick = { mainViewModel.playSong(it, plSongs) },
                    onPlayAll = { mainViewModel.playAll(plSongs) },
                    onShuffleAll = { mainViewModel.shuffleAll(plSongs) },
                    onRenamePlaylist = { playlistViewModel.renamePlaylist(pl.id, it) },
                    onDeletePlaylist = {
                        playlistViewModel.deletePlaylist(pl.id)
                        navigator.pop()
                    },
                    onRemoveSong = { playlistViewModel.removeSongFromPlaylist(pl.id, it.id) },
                    onFavoriteToggle = { mainViewModel.toggleFavorite(it) },
                    onPlayNext = { mainViewModel.playNext(it) },
                    onAddToQueue = { mainViewModel.addToQueue(it) },
                    onShowDetails = { songForDetails = it }
                )
            }

            is Screen.NowPlaying -> {
                NowPlayingScreen(
                    playbackState = playbackState,
                    sleepTimerActive = sleepTimerState.isActive,
                    onCollapse = { navigator.pop() },
                    onPlayPause = { mainViewModel.togglePlayPause() },
                    onNext = { mainViewModel.skipNext() },
                    onPrevious = { mainViewModel.skipPrevious() },
                    onSeek = { mainViewModel.seekTo(it) },
                    onToggleShuffle = { mainViewModel.toggleShuffle() },
                    onToggleRepeat = { mainViewModel.toggleRepeat() },
                    onSpeedSelected = { mainViewModel.setPlaybackSpeed(it) },
                    onFavoriteToggle = { playbackState.currentSong?.let { mainViewModel.toggleFavorite(it) } },
                    onOpenQueue = { showQueueSheet = true },
                    onOpenEqualizer = { navigator.navigateTo(Screen.Equalizer) },
                    onOpenSleepTimer = { showSleepTimerDialog = true },
                    onShowDetails = { playbackState.currentSong?.let { songForDetails = it } }
                )
            }

            is Screen.Equalizer -> {
                EqualizerScreen(
                    state = equalizerState,
                    onEnableChange = { equalizerViewModel.setEnabled(it) },
                    onBandChange = { band, level -> equalizerViewModel.setBandLevel(band, level) },
                    onPresetSelect = { equalizerViewModel.usePreset(it) },
                    onBassBoostChange = { equalizerViewModel.setBassBoost(it) },
                    onVirtualizerChange = { equalizerViewModel.setVirtualizer(it) },
                    onBackClick = { navigator.pop() }
                )
            }

            is Screen.Settings -> {
                SettingsScreen(
                    settingsViewModel = settingsViewModel,
                    onOpenEqualizer = { navigator.navigateTo(Screen.Equalizer) },
                    onOpenSleepTimer = { showSleepTimerDialog = true },
                    onBackClick = { navigator.pop() }
                )
            }
        }
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

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            playlistViewModel.createPlaylist(newPlaylistName.trim())
                            newPlaylistName = ""
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

    // Add to Playlist Dialog
    val songToAdd = songToAddToPlaylist
    if (songToAdd != null) {
        AlertDialog(
            onDismissRequest = { songToAddToPlaylist = null },
            title = { Text("Add to Playlist") },
            text = {
                if (playlists.isEmpty()) {
                    Text("No playlists created yet. Create a playlist first.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(240.dp)) {
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
            },
            confirmButton = {
                TextButton(onClick = { songToAddToPlaylist = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
