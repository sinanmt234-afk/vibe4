package com.aurelia.music.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aurelia.music.data.model.AudioQuality
import com.aurelia.music.data.model.EqualizerPreset
import com.aurelia.music.data.model.PlayerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aurelia_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val WIFI_QUALITY = stringPreferencesKey("wifi_quality")
        val CELLULAR_QUALITY = stringPreferencesKey("cellular_quality")
        val AUDIO_NORMALIZATION = booleanPreferencesKey("audio_normalization")
        val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        val SKIP_SILENCE = booleanPreferencesKey("skip_silence")
        val CROSSFADE_SECONDS = intPreferencesKey("crossfade_seconds")
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_PRESET = stringPreferencesKey("eq_preset")
        val CUSTOM_EQ_BANDS = stringPreferencesKey("custom_eq_bands")
        val BASS_BOOST = intPreferencesKey("bass_boost")
        val AUTO_AMBIENT_ROTATE = booleanPreferencesKey("auto_ambient_rotate")
        val DYNAMIC_GLOW = booleanPreferencesKey("dynamic_glow")
        val OLED_BLACK = booleanPreferencesKey("oled_black")
        val MAX_CACHE_MB = intPreferencesKey("max_cache_mb")
    }

    val settingsFlow: Flow<PlayerSettings> = context.dataStore.data.map { prefs ->
        val customBands = prefs[Keys.CUSTOM_EQ_BANDS]
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.takeIf { it.size == 5 }
            ?: listOf(0, 0, 0, 0, 0)

        PlayerSettings(
            wifiQuality = prefs[Keys.WIFI_QUALITY]?.let { runCatching { AudioQuality.valueOf(it) }.getOrNull() } ?: AudioQuality.BEST,
            cellularQuality = prefs[Keys.CELLULAR_QUALITY]?.let { runCatching { AudioQuality.valueOf(it) }.getOrNull() } ?: AudioQuality.MEDIUM,
            audioNormalization = prefs[Keys.AUDIO_NORMALIZATION] ?: true,
            gaplessPlayback = prefs[Keys.GAPLESS_PLAYBACK] ?: true,
            skipSilence = prefs[Keys.SKIP_SILENCE] ?: false,
            crossfadeSeconds = prefs[Keys.CROSSFADE_SECONDS] ?: 3,
            equalizerEnabled = prefs[Keys.EQ_ENABLED] ?: true,
            equalizerPreset = prefs[Keys.EQ_PRESET]?.let { runCatching { EqualizerPreset.valueOf(it) }.getOrNull() } ?: EqualizerPreset.AUDIOPHILE,
            customEqBands = customBands,
            bassBoostStrength = prefs[Keys.BASS_BOOST] ?: 250,
            autoAmbientOnRotate = prefs[Keys.AUTO_AMBIENT_ROTATE] ?: true,
            dynamicArtworkGlow = prefs[Keys.DYNAMIC_GLOW] ?: true,
            oledPureBlack = prefs[Keys.OLED_BLACK] ?: false,
            maxCacheMb = prefs[Keys.MAX_CACHE_MB] ?: 512
        )
    }

    suspend fun updateWifiQuality(quality: AudioQuality) {
        context.dataStore.edit { it[Keys.WIFI_QUALITY] = quality.name }
    }

    suspend fun updateCellularQuality(quality: AudioQuality) {
        context.dataStore.edit { it[Keys.CELLULAR_QUALITY] = quality.name }
    }

    suspend fun setAudioNormalization(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUDIO_NORMALIZATION] = enabled }
    }

    suspend fun setGaplessPlayback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GAPLESS_PLAYBACK] = enabled }
    }

    suspend fun setSkipSilence(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SKIP_SILENCE] = enabled }
    }

    suspend fun setCrossfadeSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.CROSSFADE_SECONDS] = seconds.coerceIn(0, 12) }
    }

    suspend fun setEqualizerEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.EQ_ENABLED] = enabled }
    }

    suspend fun setEqualizerPreset(preset: EqualizerPreset) {
        context.dataStore.edit { it[Keys.EQ_PRESET] = preset.name }
    }

    suspend fun setCustomEqBands(bands: List<Int>) {
        context.dataStore.edit {
            it[Keys.EQ_PRESET] = EqualizerPreset.CUSTOM.name
            it[Keys.CUSTOM_EQ_BANDS] = bands.joinToString(",")
        }
    }

    suspend fun setBassBoostStrength(strength: Int) {
        context.dataStore.edit { it[Keys.BASS_BOOST] = strength.coerceIn(0, 1000) }
    }

    suspend fun setAutoAmbientOnRotate(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_AMBIENT_ROTATE] = enabled }
    }

    suspend fun setDynamicGlow(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_GLOW] = enabled }
    }

    suspend fun setOledPureBlack(enabled: Boolean) {
        context.dataStore.edit { it[Keys.OLED_BLACK] = enabled }
    }

    suspend fun setMaxCacheMb(mb: Int) {
        context.dataStore.edit { it[Keys.MAX_CACHE_MB] = mb }
    }
}
