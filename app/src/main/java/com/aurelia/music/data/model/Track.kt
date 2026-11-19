package com.aurelia.music.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val videoId: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long = 0L,
    val thumbnailUrl: String = "",
    val highResThumbnailUrl: String = thumbnailUrl
)

@Serializable
data class LyricLine(
    val timestampMs: Long,
    val text: String
)

data class LyricsResult(
    val syncedLines: List<LyricLine> = emptyList(),
    val plainLines: List<String> = emptyList(),
    val source: String = ""
) {
    val isSynced: Boolean get() = syncedLines.isNotEmpty()
    val isEmpty: Boolean get() = syncedLines.isEmpty() && plainLines.isEmpty()
}

enum class AudioQuality(
    val label: String,
    val subtitle: String,
    val targetBitrateKbps: Int
) {
    LOW("Low", "48 kbps • Data Saver", 64),
    MEDIUM("Medium", "128 kbps • Balanced", 128),
    HIGH("High", "256 kbps • Studio AAC/Opus", 256),
    BEST("Lossless Tier", "Best Available Master Stream", 512)
}

enum class EqualizerPreset(
    val label: String,
    val bandLevelsMb: List<Int> // 5 bands in millibels (-1500 to +1500)
) {
    FLAT("Studio Flat", listOf(0, 0, 0, 0, 0)),
    AUDIOPHILE("Audiophile Warm", listOf(300, 200, 0, 150, 350)),
    DEEP_BASS("Velvet Bass", listOf(600, 400, 50, 0, 150)),
    VOCAL_CLARITY("Vocal Presence", listOf(-150, 100, 450, 350, 200)),
    LATE_NIGHT("Late Night Lounge", listOf(250, 150, -100, 100, 300)),
    CUSTOM("Custom Pro", listOf(0, 0, 0, 0, 0))
}

data class PlayerSettings(
    val wifiQuality: AudioQuality = AudioQuality.BEST,
    val cellularQuality: AudioQuality = AudioQuality.MEDIUM,
    val audioNormalization: Boolean = true,
    val gaplessPlayback: Boolean = true,
    val skipSilence: Boolean = false,
    val crossfadeSeconds: Int = 3,
    val equalizerEnabled: Boolean = true,
    val equalizerPreset: EqualizerPreset = EqualizerPreset.AUDIOPHILE,
    val customEqBands: List<Int> = listOf(0, 0, 0, 0, 0),
    val bassBoostStrength: Int = 250, // 0..1000
    val autoAmbientOnRotate: Boolean = true,
    val dynamicArtworkGlow: Boolean = true,
    val oledPureBlack: Boolean = false,
    val maxCacheMb: Int = 512
)

@Serializable
data class PartyRoomState(
    val roomCode: String,
    val hostName: String,
    val track: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val updatedAtUtcMs: Long = System.currentTimeMillis(),
    val listenerCount: Int = 1,
    val latestReaction: String = ""
)
