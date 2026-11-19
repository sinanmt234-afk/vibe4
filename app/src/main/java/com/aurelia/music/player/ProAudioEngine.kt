package com.aurelia.music.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import com.aurelia.music.data.model.EqualizerPreset
import com.aurelia.music.data.model.PlayerSettings

/**
 * Pro-grade hardware DSP Audio Engine wrapping Android Equalizer, BassBoost, and LoudnessEnhancer.
 */
class ProAudioEngine {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var attachedSessionId: Int = -1

    fun attachToAudioSession(audioSessionId: Int, settings: PlayerSettings) {
        if (audioSessionId <= 0 || audioSessionId == attachedSessionId) return
        release()
        attachedSessionId = audioSessionId

        runCatching {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = settings.equalizerEnabled
            }
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = settings.equalizerEnabled && settings.bassBoostStrength > 0
            }
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                enabled = settings.audioNormalization
                setTargetGain(if (settings.audioNormalization) 300 else 0)
            }
            applySettings(settings)
        }
    }

    fun applySettings(settings: PlayerSettings) {
        runCatching {
            equalizer?.let { eq ->
                eq.enabled = settings.equalizerEnabled
                val levels = if (settings.equalizerPreset == EqualizerPreset.CUSTOM) {
                    settings.customEqBands
                } else {
                    settings.equalizerPreset.bandLevelsMb
                }
                val numBands = eq.numberOfBands.toInt()
                val minLevel = eq.bandLevelRange[0].toInt()
                val maxLevel = eq.bandLevelRange[1].toInt()
                for (band in 0 until minOf(numBands, levels.size)) {
                    val clamped = levels[band].coerceIn(minLevel, maxLevel).toShort()
                    eq.setBandLevel(band.toShort(), clamped)
                }
            }
            bassBoost?.let { bb ->
                bb.enabled = settings.equalizerEnabled && settings.bassBoostStrength > 0
                if (bb.strengthSupported) {
                    bb.setStrength(settings.bassBoostStrength.coerceIn(0, 1000).toShort())
                }
            }
            loudnessEnhancer?.let { le ->
                le.enabled = settings.audioNormalization
                le.setTargetGain(if (settings.audioNormalization) 300 else 0)
            }
        }
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { loudnessEnhancer?.release() }
        equalizer = null
        bassBoost = null
        loudnessEnhancer = null
        attachedSessionId = -1
    }
}
