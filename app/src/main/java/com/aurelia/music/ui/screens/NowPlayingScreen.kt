package com.aurelia.music.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aurelia.music.data.model.EqualizerPreset
import com.aurelia.music.player.PlayerController
import com.aurelia.music.ui.theme.ChampagneGold
import com.aurelia.music.ui.theme.GlassSurface
import com.aurelia.music.ui.theme.GoldBorder
import com.aurelia.music.ui.theme.IvoryText
import com.aurelia.music.ui.theme.MutedSilver
import com.aurelia.music.ui.theme.ObsidianBg
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playerController: PlayerController,
    onCollapse: () -> Unit,
    onEnterAmbient: () -> Unit
) {
    val track by playerController.currentTrack.collectAsState()
    val isPlaying by playerController.isPlaying.collectAsState()
    val positionMs by playerController.positionMs.collectAsState()
    val durationMs by playerController.durationMs.collectAsState()
    val dominantColor by playerController.dominantColor.collectAsState()
    val lyrics by playerController.lyrics.collectAsState()
    val settings by playerController.settings.collectAsState()

    val scope = rememberCoroutineScope()
    var showLyrics by remember { mutableStateOf(false) }
    var showEqSheet by remember { mutableStateOf(false) }

    val animatedGlow by animateColorAsState(
        targetValue = if (settings.dynamicArtworkGlow) dominantColor else ChampagneGold,
        animationSpec = tween(900),
        label = "glow"
    )

    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.92f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 240f),
        label = "art_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "breath")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.48f,
        animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow_alpha"
    )

    val currentTrack = track ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        animatedGlow.copy(alpha = if (isPlaying) glowAlpha else 0.22f),
                        ObsidianBg,
                        Color(0xFF060609)
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Collapse", tint = IvoryText)
                }
                Text(
                    text = "NOW PLAYING",
                    style = MaterialTheme.typography.labelSmall
                )
                Row {
                    IconButton(onClick = onEnterAmbient) {
                        Icon(Icons.Rounded.ScreenRotation, contentDescription = "Ambient Mode", tint = ChampagneGold)
                    }
                    IconButton(onClick = { showLyrics = !showLyrics }) {
                        Icon(
                            Icons.Rounded.Lyrics,
                            contentDescription = "Lyrics",
                            tint = if (showLyrics) ChampagneGold else IvoryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Center Artwork OR Lyrics
            AnimatedContent(
                targetState = showLyrics,
                modifier = Modifier.weight(1f),
                label = "art_or_lyrics"
            ) { lyricsMode ->
                if (!lyricsMode) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = currentTrack.highResThumbnailUrl,
                            contentDescription = currentTrack.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .aspectRatio(1f)
                                .scale(artScale)
                                .clip(RoundedCornerShape(28.dp))
                                .border(1.5.dp, GoldBorder, RoundedCornerShape(28.dp))
                        )
                    }
                } else {
                    val listState = rememberLazyListState()
                    val activeIndex = remember(positionMs, lyrics) {
                        if (lyrics.isSynced) {
                            lyrics.syncedLines.indexOfLast { it.timestampMs <= positionMs }.coerceAtLeast(0)
                        } else -1
                    }
                    LaunchedEffect(activeIndex) {
                        if (activeIndex >= 0) {
                            listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
                        }
                    }

                    if (lyrics.isEmpty) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Lyrics unavailable for this track", color = MutedSilver)
                        }
                    } else if (lyrics.isSynced) {
                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(lyrics.syncedLines) { idx, line ->
                                val isActive = idx == activeIndex
                                Text(
                                    text = line.text,
                                    fontSize = if (isActive) 22.sp else 17.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isActive) IvoryText else MutedSilver.copy(alpha = 0.55f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { playerController.seekTo(line.timestampMs) }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(lyrics.plainLines) { _, line ->
                                Text(text = line, fontSize = 18.sp, color = IvoryText)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Track Info
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = currentTrack.title,
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentTrack.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSilver,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Seek Slider
            val safeDuration = durationMs.coerceAtLeast(1L)
            Slider(
                value = (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f),
                onValueChange = { frac ->
                    playerController.seekTo((frac * safeDuration).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = ChampagneGold,
                    activeTrackColor = ChampagneGold,
                    inactiveTrackColor = Color.White.copy(alpha = 0.16f)
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatDuration(positionMs), style = MaterialTheme.typography.bodyMedium)
                Text(formatDuration(durationMs), style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pro Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showEqSheet = true }) {
                    Icon(Icons.Rounded.Equalizer, contentDescription = "Pro Equalizer", tint = ChampagneGold)
                }
                IconButton(onClick = { playerController.playPrevious() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = IvoryText, modifier = Modifier.size(34.dp))
                }
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(ChampagneGold, Color(0xFFB38B22)))
                        )
                        .clickable { playerController.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color(0xFF141106),
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = { playerController.playNext() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = IvoryText, modifier = Modifier.size(34.dp))
                }
                IconButton(onClick = { showLyrics = !showLyrics }) {
                    Icon(Icons.Rounded.Lyrics, contentDescription = "Lyrics", tint = if (showLyrics) ChampagneGold else MutedSilver)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showEqSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEqSheet = false },
            containerColor = GlassSurface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Pro Audio DSP & Equalizer", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(EqualizerPreset.entries) { _, preset ->
                        FilterChip(
                            selected = settings.equalizerPreset == preset,
                            onClick = {
                                scope.launch { playerController.settingsRepository.setEqualizerPreset(preset) }
                            },
                            label = { Text(preset.label) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Bass Boost: ${(settings.bassBoostStrength / 10)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = IvoryText
                )
                Slider(
                    value = settings.bassBoostStrength.toFloat(),
                    onValueChange = { v ->
                        scope.launch { playerController.settingsRepository.setBassBoostStrength(v.toInt()) }
                    },
                    valueRange = 0f..1000f,
                    colors = SliderDefaults.colors(thumbColor = ChampagneGold, activeTrackColor = ChampagneGold)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
