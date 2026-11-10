package com.musicplayer.app.model

data class Artist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val albumCount: Int = 1
)
