package com.musicplayer.app.model

import android.net.Uri

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val songCount: Int,
    val year: Int = 0
) {
    val artworkUri: Uri
        get() = Uri.parse("content://media/external/audio/albumart/$id")
}
