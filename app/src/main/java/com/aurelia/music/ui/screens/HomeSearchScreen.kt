package com.aurelia.music.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aurelia.music.data.model.Track
import com.aurelia.music.player.PlayerController
import com.aurelia.music.ui.components.TrackRowItem
import com.aurelia.music.ui.theme.ChampagneGold
import com.aurelia.music.ui.theme.GlassSurface
import com.aurelia.music.ui.theme.GoldBorder
import com.aurelia.music.ui.theme.IvoryText
import com.aurelia.music.ui.theme.MutedSilver
import com.aurelia.music.ui.theme.SubtleBorder
import kotlinx.coroutines.launch

@Composable
fun HomeSearchScreen(
    playerController: PlayerController,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    var query by remember { mutableStateOf("") }
    var trendingTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var searchResults by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val currentTrack by playerController.currentTrack.collectAsState()
    val isPlaying by playerController.isPlaying.collectAsState()
    val networkLabel by playerController.activeNetworkLabel.collectAsState()
    val settings by playerController.settings.collectAsState()

    val activeQuality = if (networkLabel == "Wi-Fi") settings.wifiQuality else settings.cellularQuality

    LaunchedEffect(Unit) {
        isLoading = true
        trendingTracks = playerController.innerTubeClient.getHomeTrending()
        isLoading = false
    }

    fun runSearch(q: String) {
        if (q.isBlank()) {
            searchResults = emptyList()
            return
        }
        scope.launch {
            isLoading = true
            searchResults = playerController.innerTubeClient.searchSongs(q)
            isLoading = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "VIBE4MUSICZ",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "Curated Sound",
                    style = MaterialTheme.typography.displaySmall
                )
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(GlassSurface)
                    .border(1.dp, GoldBorder, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Wifi,
                    contentDescription = null,
                    tint = ChampagneGold,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$networkLabel • ${activeQuality.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = IvoryText
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = {
                Text("Search YouTube Music songs, artists, albums...", color = MutedSilver)
            },
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = ChampagneGold)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        query = ""
                        searchResults = emptyList()
                    }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = MutedSilver)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                keyboard?.hide()
                runSearch(query)
            }),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = GlassSurface,
                unfocusedContainerColor = GlassSurface,
                focusedBorderColor = ChampagneGold,
                unfocusedBorderColor = SubtleBorder,
                focusedTextColor = IvoryText,
                unfocusedTextColor = IvoryText
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ChampagneGold)
            }
        } else if (searchResults.isNotEmpty()) {
            Text(
                text = "SEARCH RESULTS",
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                items(searchResults, key = { it.videoId }) { track ->
                    TrackRowItem(
                        track = track,
                        isCurrent = currentTrack?.videoId == track.videoId,
                        isPlaying = isPlaying,
                        onClick = { playerController.playTrack(track, searchResults) }
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                if (trendingTracks.isNotEmpty()) {
                    item {
                        Text(
                            text = "FEATURED PREMIERES",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(trendingTracks.take(8), key = { "card_${it.videoId}" }) { track ->
                                FeaturedArtworkCard(
                                    track = track,
                                    onClick = { playerController.playTrack(track, trendingTracks) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "TRENDING ON YOUTUBE MUSIC",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    items(trendingTracks, key = { it.videoId }) { track ->
                        AnimatedVisibility(visible = true, enter = fadeIn()) {
                            TrackRowItem(
                                track = track,
                                isCurrent = currentTrack?.videoId == track.videoId,
                                isPlaying = isPlaying,
                                onClick = { playerController.playTrack(track, trendingTracks) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedArtworkCard(
    track: Track,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(156.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1D1D28), Color(0xFF111118))
                )
            )
            .border(1.dp, SubtleBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        AsyncImage(
            model = track.highResThumbnailUrl,
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(136.dp)
                .clip(RoundedCornerShape(14.dp))
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = track.title,
            style = MaterialTheme.typography.titleMedium,
            color = IvoryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = MutedSilver,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
