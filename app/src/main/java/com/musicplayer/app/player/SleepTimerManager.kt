package com.musicplayer.app.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SleepTimerState(
    val isActive: Boolean = false,
    val remainingSeconds: Long = 0L,
    val isEndOfTrack: Boolean = false
)

class SleepTimerManager(
    private val scope: CoroutineScope,
    private val onTimerExpired: () -> Unit
) {
    private var timerJob: Job? = null
    private val _state = MutableStateFlow(SleepTimerState())
    val state = _state.asStateFlow()

    fun startTimer(minutes: Int) {
        cancelTimer()
        if (minutes <= 0) return

        val totalSeconds = minutes * 60L
        _state.value = SleepTimerState(
            isActive = true,
            remainingSeconds = totalSeconds,
            isEndOfTrack = false
        )

        timerJob = scope.launch(Dispatchers.Default) {
            var remaining = totalSeconds
            while (isActive && remaining > 0) {
                delay(1000L)
                remaining--
                _state.value = _state.value.copy(remainingSeconds = remaining)
            }
            if (isActive && remaining <= 0) {
                _state.value = SleepTimerState(isActive = false, remainingSeconds = 0L)
                onTimerExpired()
            }
        }
    }

    fun startEndOfTrackTimer() {
        cancelTimer()
        _state.value = SleepTimerState(
            isActive = true,
            remainingSeconds = 0L,
            isEndOfTrack = true
        )
    }

    fun onTrackEnded() {
        if (_state.value.isActive && _state.value.isEndOfTrack) {
            cancelTimer()
            onTimerExpired()
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _state.value = SleepTimerState(isActive = false, remainingSeconds = 0L, isEndOfTrack = false)
    }
}
