package com.musicplayer.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.app.MusicPlayerApplication
import com.musicplayer.app.model.Album
import com.musicplayer.app.model.Artist
import com.musicplayer.app.model.Folder
import com.musicplayer.app.model.PlaybackState
import com.musicplayer.app.model.Playlist
import com.musicplayer.app.model.RepeatMode
import com.musicplayer.app.model.Song
import com.musicplayer.app.model.SortOrder
import com.musicplayer.app.util.PermissionUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MusicPlayerApplication
    val repository = app.musicRepository
    val playerConnection = app.playerConnection
    val preferences = app.preferencesRepository

    private val _hasPermission = MutableStateFlow(PermissionUtil.hasAudioPermission(application))
    val hasPermission = _hasPermission.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.TITLE_ASC)
    val sortOrder = _sortOrder.asStateFlow()

    val isScanning = repository.isScanning

    init {
        viewModelScope.launch {
            preferences.sortOrderFlow.collect {
                _sortOrder.value = it
            }
        }
        if (_hasPermission.value) {
            refreshLibrary()
        }
    }

    fun onPermissionGranted() {
        _hasPermission.value = true
        refreshLibrary()
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            repository.refreshLibrary()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        viewModelScope.launch {
            preferences.setSortOrder(order)
        }
    }

    // Filtered and sorted songs
    val allSongs: StateFlow<List<Song>> = combine(
        repository.songsFlow,
        _searchQuery,
        _sortOrder
    ) { songs, query, order ->
        val filtered = repository.filterSongs(songs, query)
        repository.sortSongs(filtered, order)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<Album>> = combine(
        repository.albumsFlow,
        _searchQuery
    ) { albums, query ->
        if (query.isBlank()) albums
        else albums.filter {
            it.title.lowercase().contains(query.trim().lowercase()) ||
                    it.artist.lowercase().contains(query.trim().lowercase())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<Artist>> = combine(
        repository.artistsFlow,
        _searchQuery
    ) { artists, query ->
        if (query.isBlank()) artists
        else artists.filter { it.name.lowercase().contains(query.trim().lowercase()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<Folder>> = combine(
        repository.foldersFlow,
        _searchQuery
    ) { folders, query ->
        if (query.isBlank()) folders
        else folders.filter { it.name.lowercase().contains(query.trim().lowercase()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<Song>> = combine(
        repository.favoritesFlow,
        _searchQuery,
        _sortOrder
    ) { songs, query, order ->
        val filtered = repository.filterSongs(songs, query)
        repository.sortSongs(filtered, order)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyAdded: StateFlow<List<Song>> = combine(
        repository.recentlyAddedFlow,
        _searchQuery
    ) { songs, query ->
        repository.filterSongs(songs, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<Song>> = combine(
        repository.recentlyPlayedFlow,
        _searchQuery
    ) { songs, query ->
        repository.filterSongs(songs, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = repository.playlistsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playbackState: StateFlow<PlaybackState> = playerConnection.playbackState

    fun playSong(song: Song, fromList: List<Song>? = null) {
        val list = fromList ?: allSongs.value
        val index = list.indexOfFirst { it.id == song.id }.let { if (it == -1) 0 else it }
        playerConnection.playSongs(list, index)
    }

    fun playAll(songs: List<Song>, startIndex: Int = 0) {
        playerConnection.playSongs(songs, startIndex)
    }

    fun shuffleAll(songs: List<Song>) {
        if (songs.isNotEmpty()) {
            val shuffled = songs.shuffled()
            playerConnection.playSongs(shuffled, 0)
        }
    }

    fun playNext(song: Song) {
        playerConnection.playNext(song)
    }

    fun addToQueue(song: Song) {
        playerConnection.addToQueue(song)
    }

    fun togglePlayPause() {
        playerConnection.togglePlayPause()
    }

    fun skipNext() {
        playerConnection.skipNext()
    }

    fun skipPrevious() {
        playerConnection.skipPrevious()
    }

    fun seekTo(positionMs: Long) {
        playerConnection.seekTo(positionMs)
    }

    fun toggleShuffle() {
        playerConnection.toggleShuffleMode()
    }

    fun toggleRepeat() {
        playerConnection.toggleRepeatMode()
    }

    fun setRepeatMode(mode: RepeatMode) {
        playerConnection.setRepeatMode(mode)
    }

    fun setPlaybackSpeed(speed: Float) {
        playerConnection.setPlaybackSpeed(speed)
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.toggleFavorite(song.id, song.isFavorite)
        }
    }

    fun removeFromQueue(index: Int) {
        playerConnection.removeFromQueue(index)
    }

    fun moveQueueItem(from: Int, to: Int) {
        playerConnection.moveQueueItem(from, to)
    }

    fun playQueueIndex(index: Int) {
        playerConnection.playQueueIndex(index)
    }

    fun clearQueue() {
        playerConnection.clearQueue()
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
