package com.musicplayer.app.navigation

import com.musicplayer.app.model.Album
import com.musicplayer.app.model.Artist
import com.musicplayer.app.model.Folder
import com.musicplayer.app.model.Playlist

sealed interface Screen {
    data object Library : Screen
    data class AlbumDetail(val album: Album) : Screen
    data class ArtistDetail(val artist: Artist) : Screen
    data class FolderDetail(val folder: Folder) : Screen
    data class PlaylistDetail(val playlist: Playlist) : Screen
    data object NowPlaying : Screen
    data object Equalizer : Screen
    data object Settings : Screen
}
