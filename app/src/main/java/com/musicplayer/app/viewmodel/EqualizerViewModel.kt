package com.musicplayer.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.musicplayer.app.MusicPlayerApplication
import com.musicplayer.app.player.EqualizerState
import kotlinx.coroutines.flow.StateFlow

class EqualizerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MusicPlayerApplication
    private val controller = app.playerConnection.equalizerController

    val state: StateFlow<EqualizerState> = controller.state

    fun setEnabled(enabled: Boolean) {
        controller.setEnabled(enabled)
    }

    fun setBandLevel(bandIndex: Int, level: Short) {
        controller.setBandLevel(bandIndex, level)
    }

    fun usePreset(presetIndex: Int) {
        controller.applyPreset(presetIndex)
    }

    fun setBassBoost(strength: Short) {
        controller.setBassBoost(strength)
    }

    fun setVirtualizer(strength: Short) {
        controller.setVirtualizer(strength)
    }
}
