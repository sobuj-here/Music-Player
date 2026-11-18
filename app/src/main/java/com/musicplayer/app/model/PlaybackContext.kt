package com.musicplayer.app.model

enum class PlaybackContextType {
    ALL_SONGS,
    ALBUM,
    ARTIST,
    FOLDER,
    PLAYLIST
}

data class PlaybackContext(
    val type: PlaybackContextType,
    val identifier: String
)
