package com.aurelia.music.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aurelia.music.player.PlayerController
import com.aurelia.music.ui.theme.ChampagneGold
import com.aurelia.music.ui.theme.GoldBorder
import com.aurelia.music.ui.theme.IvoryText
import com.aurelia.music.ui.theme.MutedSilver
import com.aurelia.music.ui.theme.ObsidianBg

/**
 * Landscape Ambient Mode:
 * Left Side: Luxury framed Album Thumbnail with dynamic radial glow + minimal track info & controls.
 * Right Side: Auto-scrolling Lyrics panel with active-line highlight and tap-to-seek.
 */
@Composable
fun AmbientLandscapeScreen(
    playerController: PlayerController,
    onExitAmbient: (() -> Unit)? = null
) {
    val track by playerController.currentTrack.collectAsState()
    val isPlaying by playerController.isPlaying.collectAsState()
    val positionMs by playerController.positionMs.collectAsState()
    val durationMs by playerController.durationMs.collectAsState()
    val lyrics by playerController.lyrics.collectAsState()
    val dominantColor by playerController.dominantColor.collectAsState()

    val glowColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(1000),
        label = "ambient_glow"
    )
    val pulse = rememberInfiniteTransition(label = "ambient_pulse")
    val artPulse by pulse.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "art_pulse"
    )

    val currentTrack = track ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.34f),
                        ObsidianBg,
                        Color(0xFF040407)
                    )
                )
            )
            .padding(horizontal = 28.dp, vertical = 20.dp)
    ) {
        if (onExitAmbient != null) {
            IconButton(
                onClick = onExitAmbient,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Exit Ambient", tint = MutedSilver)
            }
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT SIDE: Song Thumbnail + Track Info + Minimal Controls
            Column(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AsyncImage(
                    model = currentTrack.highResThumbnailUrl,
                    contentDescription = currentTrack.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth(0.68f)
                        .aspectRatio(1f)
                        .scale(if (isPlaying) artPulse else 0.95f)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.5.dp, GoldBorder, RoundedCornerShape(24.dp))
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = currentTrack.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = IvoryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentTrack.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChampagneGold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                val progress = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = ChampagneGold,
                    trackColor = Color.White.copy(alpha = 0.14f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { playerController.playPrevious() }) {
                        Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = IvoryText)
                    }
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(ChampagneGold)
                            .clickable { playerController.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color(0xFF141106)
                        )
                    }
                    IconButton(onClick = { playerController.playNext() }) {
                        Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = IvoryText)
                    }
                }
            }

            Spacer(modifier = Modifier.width(28.dp))

            // RIGHT SIDE: Synced / Unsynced Lyrics Panel
            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 24.dp, vertical = 18.dp)
            ) {
                Text(
                    text = if (lyrics.isSynced) "AMBIENT LYRICS • SYNCED" else "AMBIENT LYRICS",
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(modifier = Modifier.height(12.dp))

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
                        Text(
                            text = "Listening in Ambient Mode\nNo lyrics available for this track.",
                            color = MutedSilver,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                } else if (lyrics.isSynced) {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(lyrics.syncedLines) { idx, line ->
                            val isActive = idx == activeIndex
                            Text(
                                text = line.text,
                                fontSize = if (isActive) 24.sp else 18.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isActive) IvoryText else MutedSilver.copy(alpha = 0.45f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { playerController.seekTo(line.timestampMs) }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(lyrics.plainLines) { _, line ->
                            Text(
                                text = line,
                                fontSize = 19.sp,
                                color = IvoryText.copy(alpha = 0.88f)
                            )
                        }
                    }
                }
            }
        }
    }
}
