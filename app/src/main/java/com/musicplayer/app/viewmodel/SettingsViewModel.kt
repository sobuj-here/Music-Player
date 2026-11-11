package com.musicplayer.app.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.app.MusicPlayerApplication
import com.musicplayer.app.data.repository.LibraryStats
import com.musicplayer.app.player.SleepTimerState
import com.musicplayer.app.settings.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MusicPlayerApplication
    private val preferences = app.preferencesRepository
    private val repository = app.musicRepository
    val sleepTimerManager = app.playerConnection.sleepTimerManager

    private val _messageEvents = MutableSharedFlow<String>()
    val messageEvents: SharedFlow<String> = _messageEvents.asSharedFlow()

    val themeMode: StateFlow<String> = preferences.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val accentColorIndex: StateFlow<Int> = preferences.accentColorFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val resumePlayback: StateFlow<Boolean> = preferences.resumePlaybackFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val gaplessPlayback: StateFlow<Boolean> = preferences.gaplessPlaybackFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val crossfadeEnabled: StateFlow<Boolean> = preferences.crossfadeEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val crossfadeDuration: StateFlow<Int> = preferences.crossfadeDurationFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    val pauseOnDisconnect: StateFlow<Boolean> = preferences.pauseOnDisconnectFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val rememberPosition: StateFlow<Boolean> = preferences.rememberPositionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val hideShortAudio: StateFlow<Boolean> = preferences.hideShortAudioFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notificationArtwork: StateFlow<Boolean> = preferences.notificationArtworkFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val excludedFolders: StateFlow<Set<String>> = preferences.excludedFoldersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val libraryStats: StateFlow<LibraryStats> = repository.libraryStats

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

    fun setResumePlayback(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setResumePlayback(enabled)
        }
    }

    fun setGaplessPlayback(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setGaplessPlayback(enabled)
        }
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setCrossfadeEnabled(enabled)
        }
    }

    fun setCrossfadeDuration(seconds: Int) {
        viewModelScope.launch {
            preferences.setCrossfadeDuration(seconds)
        }
    }

    fun setPauseOnDisconnect(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setPauseOnDisconnect(enabled)
        }
    }

    fun setRememberPosition(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setRememberPosition(enabled)
        }
    }

    fun setHideShortAudio(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setHideShortAudio(enabled)
        }
    }

    fun setNotificationArtwork(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setNotificationArtwork(enabled)
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            repository.refreshLibrary()
            _messageEvents.emit("Music library refreshed.")
        }
    }

    fun clearListeningHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _messageEvents.emit("Listening history cleared.")
        }
    }

    fun exportPlaylists(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = BackupManager.exportPlaylists(context, app.database)
                _messageEvents.emit("Playlists exported to ${file.name}")
            } catch (e: Exception) {
                _messageEvents.emit("Failed to export playlists: ${e.message}")
            }
        }
    }

    fun openBatteryOptimization(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            viewModelScope.launch {
                _messageEvents.emit("Unable to open system settings.")
            }
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
