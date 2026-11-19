package com.aurelia.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aurelia.music.data.model.AudioQuality
import com.aurelia.music.player.PlayerController
import com.aurelia.music.ui.theme.ChampagneGold
import com.aurelia.music.ui.theme.GlassSurface
import com.aurelia.music.ui.theme.IvoryText
import com.aurelia.music.ui.theme.MutedSilver
import com.aurelia.music.ui.theme.SubtleBorder
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    playerController: PlayerController,
    modifier: Modifier = Modifier
) {
    val settings by playerController.settings.collectAsState()
    val repo = playerController.settingsRepository
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text("PREFERENCES", style = MaterialTheme.typography.labelSmall)
        Text("Studio Settings", style = MaterialTheme.typography.displaySmall)
        Spacer(modifier = Modifier.height(18.dp))

        // Wi-Fi Streaming Quality
        SettingsSectionCard(title = "Wi-Fi Streaming Quality") {
            AudioQuality.entries.forEach { quality ->
                QualityOptionRow(
                    quality = quality,
                    selected = settings.wifiQuality == quality,
                    onSelect = { scope.launch { repo.updateWifiQuality(quality) } }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cellular Streaming Quality
        SettingsSectionCard(title = "Cellular Streaming Quality") {
            AudioQuality.entries.forEach { quality ->
                QualityOptionRow(
                    quality = quality,
                    selected = settings.cellularQuality == quality,
                    onSelect = { scope.launch { repo.updateCellularQuality(quality) } }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pro Playback & DSP
        SettingsSectionCard(title = "Pro Audio & Playback") {
            ToggleSettingRow(
                title = "Audio Normalization",
                subtitle = "Maintain consistent studio volume across tracks",
                checked = settings.audioNormalization,
                onCheckedChange = { scope.launch { repo.setAudioNormalization(it) } }
            )
            ToggleSettingRow(
                title = "Gapless Playback",
                subtitle = "Seamless transitions for live albums & mixes",
                checked = settings.gaplessPlayback,
                onCheckedChange = { scope.launch { repo.setGaplessPlayback(it) } }
            )
            ToggleSettingRow(
                title = "Skip Silence",
                subtitle = "Trim dead air at track boundaries",
                checked = settings.skipSilence,
                onCheckedChange = { scope.launch { repo.setSkipSilence(it) } }
            )
            ToggleSettingRow(
                title = "Hardware Equalizer & Bass Boost",
                subtitle = "Active preset: ${settings.equalizerPreset.label}",
                checked = settings.equalizerEnabled,
                onCheckedChange = { scope.launch { repo.setEqualizerEnabled(it) } }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Crossfade: ${settings.crossfadeSeconds}s",
                style = MaterialTheme.typography.titleMedium,
                color = IvoryText
            )
            Slider(
                value = settings.crossfadeSeconds.toFloat(),
                onValueChange = { scope.launch { repo.setCrossfadeSeconds(it.toInt()) } },
                valueRange = 0f..12f,
                steps = 11,
                colors = SliderDefaults.colors(thumbColor = ChampagneGold, activeTrackColor = ChampagneGold)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ambient & Luxury Visuals
        SettingsSectionCard(title = "Ambient Mode & Visuals") {
            ToggleSettingRow(
                title = "Auto Ambient Mode on Rotate",
                subtitle = "Show split-screen Artwork & Lyrics in landscape",
                checked = settings.autoAmbientOnRotate,
                onCheckedChange = { scope.launch { repo.setAutoAmbientOnRotate(it) } }
            )
            ToggleSettingRow(
                title = "Dynamic Artwork Glow",
                subtitle = "Extract ambient lighting from current album cover",
                checked = settings.dynamicArtworkGlow,
                onCheckedChange = { scope.launch { repo.setDynamicGlow(it) } }
            )
            ToggleSettingRow(
                title = "OLED Pure Black Mode",
                subtitle = "True #000000 background for AMOLED displays",
                checked = settings.oledPureBlack,
                onCheckedChange = { scope.launch { repo.setOledPureBlack(it) } }
            )
        }

        Spacer(modifier = Modifier.height(110.dp))
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GlassSurface)
            .border(1.dp, SubtleBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, color = ChampagneGold)
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun QualityOptionRow(
    quality: AudioQuality,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = ChampagneGold, unselectedColor = MutedSilver)
        )
        Column {
            Text(quality.label, style = MaterialTheme.typography.titleMedium, color = IvoryText)
            Text(quality.subtitle, style = MaterialTheme.typography.bodyMedium, color = MutedSilver)
        }
    }
}

@Composable
private fun ToggleSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = IvoryText)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MutedSilver)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianThumb,
                checkedTrackColor = ChampagneGold
            )
        )
    }
}

private val ObsidianThumb = androidx.compose.ui.graphics.Color(0xFF141106)
