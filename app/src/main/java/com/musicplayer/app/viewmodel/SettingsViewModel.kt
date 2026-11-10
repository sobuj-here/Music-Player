package com.musicplayer.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.app.MusicPlayerApplication
import com.musicplayer.app.player.SleepTimerState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MusicPlayerApplication
    private val preferences = app.preferencesRepository
    val sleepTimerManager = app.playerConnection.sleepTimerManager

    val themeMode: StateFlow<String> = preferences.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val accentColorIndex: StateFlow<Int> = preferences.accentColorFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val sleepTimerState: StateFlow<SleepTimerState> = sleepTimerManager.state

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferences.setThemeMode(mode)
        }
    }

    fun setAccentColorIndex(index: Int) {
        viewModelScope.launch {
            preferences.setAccentColorIndex(index)
        }
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerManager.startTimer(minutes)
    }

    fun startEndOfTrackTimer() {
        sleepTimerManager.startEndOfTrackTimer()
    }

    fun cancelSleepTimer() {
        sleepTimerManager.cancelTimer()
    }
}
