package com.musicplayer.app.data.repository

import com.musicplayer.app.data.local.MusicDatabase
import com.musicplayer.app.data.local.entity.FavoriteSongEntity
import com.musicplayer.app.data.local.entity.PlaylistEntity
import com.musicplayer.app.data.local.entity.PlaylistSongCrossRef
import com.musicplayer.app.data.local.entity.RecentlyPlayedEntity
import com.musicplayer.app.data.mediastore.MediaStoreScanner
import com.musicplayer.app.model.Album
import com.musicplayer.app.model.Artist
import com.musicplayer.app.model.Folder
import com.musicplayer.app.model.Playlist
import com.musicplayer.app.model.Song
import com.musicplayer.app.model.SortOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class LibraryStats(
    val totalSongs: Int = 0,
    val totalSizeBytes: Long = 0L,
    val totalDurationMs: Long = 0L
)

class MusicRepository(
    private val scanner: MediaStoreScanner,
    private val database: MusicDatabase,
    private val preferencesRepository: PreferencesRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _rawSongs = MutableStateFlow<List<Song>>(emptyList())
    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val favoriteIdsFlow: Flow<List<Long>> = database.favoriteDao().getAllFavoriteIds()
    private val recentlyPlayedIdsFlow: Flow<List<Long>> = database.historyDao().getRecentlyPlayedIds()

    val songsFlow: Flow<List<Song>> = combine(
        _rawSongs,
        favoriteIdsFlow,
        preferencesRepository.hideShortAudioFlow,
        preferencesRepository.excludedFoldersFlow
    ) { songs, favIds, hideShort, excluded ->
        val favSet = favIds.toSet()
        songs.filter { song ->
            if (hideShort && song.duration < 30000L) return@filter false
            if (excluded.isNotEmpty()) {
                val parent = File(song.data).parent ?: ""
                if (excluded.any { parent.startsWith(it) }) return@filter false
            }
            true
        }.map { it.copy(isFavorite = favSet.contains(it.id)) }
    }

    val allSongs: StateFlow<List<Song>> = songsFlow
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val libraryStats: StateFlow<LibraryStats> = allSongs.map { songs ->
        LibraryStats(
            totalSongs = songs.size,
            totalSizeBytes = songs.sumOf { it.size },
            totalDurationMs = songs.sumOf { it.duration }
        )
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), LibraryStats())

    val albumsFlow: Flow<List<Album>> = allSongs.map { songs ->
        songs.groupBy { it.albumId }
            .map { (albumId, songsInAlbum) ->
                val first = songsInAlbum.first()
                Album(
                    id = albumId,
                    title = first.album,
                    artist = first.artist,
                    songCount = songsInAlbum.size,
                    year = songsInAlbum.map { it.year }.maxOrNull() ?: 0
                )
            }
            .sortedBy { it.title.lowercase() }
    }

    val artistsFlow: Flow<List<Artist>> = allSongs.map { songs ->
        songs.groupBy { it.artist.lowercase() }
            .map { (_, songsForArtist) ->
                val first = songsForArtist.first()
                val distinctAlbums = songsForArtist.map { it.albumId }.distinct().size
                Artist(
                    id = first.id,
                    name = first.artist,
                    songCount = songsForArtist.size,
                    albumCount = distinctAlbums
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    val foldersFlow: Flow<List<Folder>> = allSongs.map { songs ->
        songs.mapNotNull { song ->
            val file = File(song.data)
            val parent = file.parentFile
            if (parent != null && parent.exists()) {
                parent.name to parent.absolutePath
            } else null
        }
            .groupBy { it.second }
            .map { (path, pairs) ->
                Folder(
                    name = pairs.first().first,
                    path = path,
                    songCount = pairs.size
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    val favoritesFlow: Flow<List<Song>> = songsFlow.map { songs ->
        songs.filter { it.isFavorite }
    }

    val recentlyAddedFlow: Flow<List<Song>> = songsFlow.map { songs ->
        songs.sortedByDescending { it.dateAdded }
    }

    val recentlyPlayedFlow: Flow<List<Song>> = combine(songsFlow, recentlyPlayedIdsFlow) { songs, playedIds ->
        val songMap = songs.associateBy { it.id }
        playedIds.mapNotNull { songMap[it] }
    }

    val playlistsFlow: Flow<List<Playlist>> = database.playlistDao().getAllPlaylists().map { entities ->
        entities.map { entity ->
            Playlist(
                id = entity.id,
                name = entity.name,
                createdAt = entity.createdAt
            )
        }
    }

    suspend fun refreshLibrary() = withContext(Dispatchers.IO) {
        _isScanning.value = true
        try {
            val scanned = scanner.scanSongs()
            _rawSongs.value = scanned
        } finally {
            _isScanning.value = false
        }
    }

    fun sortSongs(songs: List<Song>, sortOrder: SortOrder): List<Song> {
        return when (sortOrder) {
            SortOrder.TITLE_ASC -> songs.sortedBy { it.title.lowercase() }
            SortOrder.TITLE_DESC -> songs.sortedByDescending { it.title.lowercase() }
            SortOrder.ARTIST_ASC -> songs.sortedBy { it.artist.lowercase() }
            SortOrder.ARTIST_DESC -> songs.sortedByDescending { it.artist.lowercase() }
            SortOrder.ALBUM_ASC -> songs.sortedBy { it.album.lowercase() }
            SortOrder.DATE_ADDED_DESC -> songs.sortedByDescending { it.dateAdded }
            SortOrder.DATE_ADDED_ASC -> songs.sortedBy { it.dateAdded }
            SortOrder.DURATION_DESC -> songs.sortedByDescending { it.duration }
        }
    }

    fun filterSongs(songs: List<Song>, query: String): List<Song> {
        if (query.isBlank()) return songs
        val q = query.trim().lowercase()
        return songs.filter {
            it.title.lowercase().contains(q) ||
                    it.artist.lowercase().contains(q) ||
                    it.album.lowercase().contains(q)
        }
    }

    suspend fun toggleFavorite(songId: Long, isCurrentFavorite: Boolean) = withContext(Dispatchers.IO) {
        if (isCurrentFavorite) {
            database.favoriteDao().removeFavorite(songId)
        } else {
            database.favoriteDao().addFavorite(FavoriteSongEntity(songId = songId))
        }
    }

    suspend fun recordPlayback(songId: Long) = withContext(Dispatchers.IO) {
        database.historyDao().insertRecentlyPlayed(
            RecentlyPlayedEntity(songId = songId, playedAt = System.currentTimeMillis())
        )
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        database.historyDao().clearHistory()
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        val entity = PlaylistEntity(name = name)
        database.playlistDao().insertPlaylist(entity)
    }

    suspend fun renamePlaylist(id: Long, newName: String) = withContext(Dispatchers.IO) {
        database.playlistDao().updatePlaylist(PlaylistEntity(id = id, name = newName))
    }

    suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) {
        database.playlistDao().deletePlaylistById(id)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        database.playlistDao().insertSongToPlaylist(
            PlaylistSongCrossRef(playlistId = playlistId, songId = songId)
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        database.playlistDao().removeSongFromPlaylist(playlistId, songId)
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> {
        return combine(songsFlow, database.playlistDao().getSongIdsForPlaylist(playlistId)) { songs, ids ->
            val map = songs.associateBy { it.id }
            ids.mapNotNull { map[it] }
        }
    }
}
