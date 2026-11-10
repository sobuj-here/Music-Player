package com.musicplayer.app.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val contentUri: Uri,
    val data: String, // file path
    val dateAdded: Long,
    val size: Long,
    val trackNumber: Int = 0,
    val year: Int = 0,
    val isFavorite: Boolean = false
) {
    val artworkUri: Uri
        get() = Uri.parse("content://media/external/audio/albumart/$albumId")
}
