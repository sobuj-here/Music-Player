package com.musicplayer.app.model

data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val bufferedPosition: Long = 0L,
    val shuffleMode: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val playbackSpeed: Float = 1.0f,
    val queue: List<Song> = emptyList(),
    val currentQueueIndex: Int = -1,
    val isServiceConnected: Boolean = false,
    val audioSessionId: Int = 0
) {
    val progressFraction: Float
        get() = if (duration > 0) (currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f

    val remainingTime: Long
        get() = (duration - currentPosition).coerceAtLeast(0L)
}
