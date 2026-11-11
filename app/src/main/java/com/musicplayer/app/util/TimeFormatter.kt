package com.musicplayer.app.util

import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeFormatter {

    fun formatMs(ms: Long): String {
        if (ms <= 0L) return "00:00"
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    fun formatDuration(ms: Long): String = formatMs(ms)
}
