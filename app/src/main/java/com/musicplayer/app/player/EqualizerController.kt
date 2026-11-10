package com.musicplayer.app.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqualizerBand(
    val index: Int,
    val centerFreqHz: Int,
    val minLevel: Short,
    val maxLevel: Short,
    val currentLevel: Short
)

data class EqualizerState(
    val isSupported: Boolean = false,
    val isEnabled: Boolean = false,
    val bands: List<EqualizerBand> = emptyList(),
    val presets: List<String> = emptyList(),
    val currentPreset: Int = -1,
    val bassBoostSupported: Boolean = false,
    val bassBoostStrength: Short = 0, // 0 to 1000
    val virtualizerSupported: Boolean = false,
    val virtualizerStrength: Short = 0 // 0 to 1000
)

class EqualizerController {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var currentAudioSessionId: Int = 0

    private val _state = MutableStateFlow(EqualizerState())
    val state = _state.asStateFlow()

    fun attachAudioSession(audioSessionId: Int) {
        if (audioSessionId < 0 || (audioSessionId == currentAudioSessionId && equalizer != null)) return
        release()
        currentAudioSessionId = audioSessionId

        try {
            val eq = Equalizer(0, audioSessionId).apply {
                enabled = _state.value.isEnabled
            }
            equalizer = eq

            val numBands = eq.numberOfBands.toInt()
            val minLevel = eq.bandLevelRange[0]
            val maxLevel = eq.bandLevelRange[1]

            val bands = (0 until numBands).map { i ->
                val freq = eq.getCenterFreq(i.toShort()) / 1000 // mHz to Hz
                val level = eq.getBandLevel(i.toShort())
                EqualizerBand(
                    index = i,
                    centerFreqHz = freq,
                    minLevel = minLevel,
                    maxLevel = maxLevel,
                    currentLevel = level
                )
            }

            val presets = (0 until eq.numberOfPresets).map {
                eq.getPresetName(it.toShort()) ?: "Preset $it"
            }

            // Bass boost
            var bbSupported = false
            try {
                bassBoost = BassBoost(0, audioSessionId).apply {
                    if (strengthSupported) {
                        bbSupported = true
                        enabled = _state.value.isEnabled
                        setStrength(_state.value.bassBoostStrength)
                    }
                }
            } catch (e: Exception) {
                bassBoost = null
            }

            // Virtualizer
            var virtSupported = false
            try {
                virtualizer = Virtualizer(0, audioSessionId).apply {
                    if (strengthSupported) {
                        virtSupported = true
                        enabled = _state.value.isEnabled
                        setStrength(_state.value.virtualizerStrength)
                    }
                }
            } catch (e: Exception) {
                virtualizer = null
            }

            _state.value = _state.value.copy(
                isSupported = true,
                bands = bands,
                presets = presets,
                currentPreset = eq.currentPreset.toInt(),
                bassBoostSupported = bbSupported,
                virtualizerSupported = virtSupported
            )
        } catch (e: Exception) {
            e.printStackTrace()
            _state.value = EqualizerState(isSupported = false)
        }
    }

    fun setEnabled(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
            _state.value = _state.value.copy(isEnabled = enabled)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setBandLevel(bandIndex: Int, level: Short) {
        try {
            equalizer?.setBandLevel(bandIndex.toShort(), level)
            val updatedBands = _state.value.bands.map {
                if (it.index == bandIndex) it.copy(currentLevel = level) else it
            }
            _state.value = _state.value.copy(bands = updatedBands, currentPreset = -1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun usePreset(presetIndex: Int) {
        try {
            if (presetIndex in 0 until (equalizer?.numberOfPresets ?: 0)) {
                equalizer?.usePreset(presetIndex.toShort())
                val eq = equalizer ?: return
                val updatedBands = _state.value.bands.map {
                    it.copy(currentLevel = eq.getBandLevel(it.index.toShort()))
                }
                _state.value = _state.value.copy(
                    currentPreset = presetIndex,
                    bands = updatedBands
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setBassBoost(strength: Short) {
        try {
            bassBoost?.setStrength(strength)
            _state.value = _state.value.copy(bassBoostStrength = strength)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setVirtualizer(strength: Short) {
        try {
            virtualizer?.setStrength(strength)
            _state.value = _state.value.copy(virtualizerStrength = strength)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            equalizer = null
            bassBoost = null
            virtualizer = null
            currentAudioSessionId = 0
        }
    }
}
