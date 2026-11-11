package com.musicplayer.app.player

import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

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
    val currentPreset: Int = 0,
    val bassBoostSupported: Boolean = false,
    val bassBoostStrength: Short = 0,
    val virtualizerSupported: Boolean = false,
    val virtualizerStrength: Short = 0,
    val statusMessage: String = "Waiting for active audio session..."
)

class EqualizerController {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var currentAudioSessionId: Int = 0

    // Standard preset frequency curves in normalized decibels (-10dB to +10dB)
    companion object {
        val STANDARD_PRESETS = listOf(
            "Normal" to listOf(0f, 0f, 0f, 0f, 0f),
            "Rock" to listOf(4.5f, 2.5f, -1.0f, 2.0f, 5.0f),
            "Pop" to listOf(-1.5f, 1.5f, 4.0f, 2.0f, -1.5f),
            "Jazz" to listOf(3.5f, 1.5f, -1.5f, 2.0f, 3.5f),
            "Classical" to listOf(4.0f, 3.0f, 2.0f, 2.0f, -1.0f),
            "Bass Boost" to listOf(6.0f, 4.5f, 1.0f, 0.0f, 0.0f),
            "Vocal" to listOf(-2.0f, 1.5f, 4.5f, 3.5f, 0.5f),
            "Custom" to listOf(0f, 0f, 0f, 0f, 0f)
        )
    }

    private val _state = MutableStateFlow(
        EqualizerState(
            presets = STANDARD_PRESETS.map { it.first }
        )
    )
    val state = _state.asStateFlow()

    @Suppress("DEPRECATION")
    @Synchronized
    fun attachAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0) {
            _state.value = _state.value.copy(
                isSupported = false,
                statusMessage = "Audio session inactive. Start music playback to activate equalizer."
            )
            return
        }

        if (audioSessionId == currentAudioSessionId && equalizer != null) {
            return
        }

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
                val freq = eq.getCenterFreq(i.toShort()) / 1000
                val level = eq.getBandLevel(i.toShort())
                EqualizerBand(
                    index = i,
                    centerFreqHz = freq,
                    minLevel = minLevel,
                    maxLevel = maxLevel,
                    currentLevel = level
                )
            }

            // Bass Boost
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
                bassBoostSupported = bbSupported,
                virtualizerSupported = virtSupported,
                statusMessage = "Equalizer active on audio session $audioSessionId"
            )

            // Re-apply preset or band levels if already selected
            applyPreset(_state.value.currentPreset)

        } catch (e: Exception) {
            e.printStackTrace()
            _state.value = _state.value.copy(
                isSupported = false,
                statusMessage = "Equalizer not supported by this device's audio hardware."
            )
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
            val updatedBands = _state.value.bands.map { band ->
                if (band.index == bandIndex) band.copy(currentLevel = level) else band
            }
            // Mark preset as Custom
            val customIndex = STANDARD_PRESETS.indexOfFirst { it.first == "Custom" }.coerceAtLeast(0)
            _state.value = _state.value.copy(
                bands = updatedBands,
                currentPreset = customIndex
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun applyPreset(presetIndex: Int) {
        if (presetIndex < 0 || presetIndex >= STANDARD_PRESETS.size) return
        val preset = STANDARD_PRESETS[presetIndex]
        val eq = equalizer ?: return

        try {
            val minRange = eq.bandLevelRange[0].toFloat()
            val maxRange = eq.bandLevelRange[1].toFloat()
            val gains = preset.second

            val updatedBands = _state.value.bands.mapIndexed { i, band ->
                if (preset.first == "Custom") {
                    band
                } else {
                    val targetDb = gains.getOrElse(i) { 0f }
                    // Map -10dB..+10dB to minRange..maxRange (typically -1500mB to +1500mB)
                    val normalized = (targetDb / 10f).coerceIn(-1f, 1f)
                    val rawLevel = if (normalized >= 0) {
                        (normalized * maxRange).roundToInt().toShort()
                    } else {
                        (-normalized * minRange).roundToInt().toShort()
                    }
                    val clamped = rawLevel.coerceIn(band.minLevel, band.maxLevel)
                    eq.setBandLevel(i.toShort(), clamped)
                    band.copy(currentLevel = clamped)
                }
            }

            _state.value = _state.value.copy(
                bands = updatedBands,
                currentPreset = presetIndex
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Suppress("DEPRECATION")
    fun setBassBoost(strength: Short) {
        try {
            bassBoost?.setStrength(strength)
            _state.value = _state.value.copy(bassBoostStrength = strength)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Suppress("DEPRECATION")
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
