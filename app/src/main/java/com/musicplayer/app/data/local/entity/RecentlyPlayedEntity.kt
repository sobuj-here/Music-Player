package com.musicplayer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recently_played")
data class RecentlyPlayedEntity(
    @PrimaryKey
    val songId: Long,
    val playedAt: Long = System.currentTimeMillis()
)
