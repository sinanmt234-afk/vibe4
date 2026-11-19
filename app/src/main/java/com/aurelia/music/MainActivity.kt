package com.aurelia.music

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import com.aurelia.music.ui.components.MiniPlayerBar
import com.aurelia.music.ui.screens.AmbientLandscapeScreen
import com.aurelia.music.ui.screens.HomeSearchScreen
import com.aurelia.music.ui.screens.NowPlayingScreen
import com.aurelia.music.ui.screens.PartyRoomScreen
import com.aurelia.music.ui.screens.SettingsScreen
import com.aurelia.music.ui.theme.AureliaTheme
import com.aurelia.music.ui.theme.ChampagneGold
import com.aurelia.music.ui.theme.GlassSurface
import com.aurelia.music.ui.theme.MutedSilver

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val playerController = (application as AureliaApp).playerController

        setContent {
            val settings by playerController.settings.collectAsState()
            val currentTrack by playerController.currentTrack.collectAsState()
            val isPlaying by playerController.isPlaying.collectAsState()
            val isBuffering by playerController.isBuffering.collectAsState()
            val positionMs by playerController.positionMs.collectAsState()
            val durationMs by playerController.durationMs.collectAsState()
            val dominantColor by playerController.dominantColor.collectAsState()

            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            var selectedTab by remember { mutableIntStateOf(0) }
            var nowPlayingExpanded by remember { mutableStateOf(false) }
            var manualAmbient by remember { mutableStateOf(false) }

            val showAmbient = currentTrack != null &&
                (manualAmbient || (isLandscape && settings.autoAmbientOnRotate))

            AureliaTheme(oledPureBlack = settings.oledPureBlack) {
                AnimatedContent(targetState = showAmbient, label = "ambient_switch") { ambientActive ->
                    if (ambientActive) {
                        AmbientLandscapeScreen(
                            playerController = playerController,
                            onExitAmbient = if (manualAmbient) {
                                { manualAmbient = false }
                            } else null
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            Scaffold(
                                containerColor = Color.Transparent,
                                bottomBar = {
                                    Column(modifier = Modifier.navigationBarsPadding()) {
                                        currentTrack?.let { track ->
                                            val progress = if (durationMs > 0L) {
                                                positionMs.toFloat() / durationMs.toFloat()
                                            } else 0f
                                            MiniPlayerBar(
                                                track = track,
                                                isPlaying = isPlaying,
                                                isBuffering = isBuffering,
                                                progress = progress,
                                                accentColor = dominantColor,
                                                onTogglePlay = { playerController.togglePlayPause() },
                                                onNext = { playerController.playNext() },
                                                onExpand = { nowPlayingExpanded = true }
                                            )
                                        }
                                        NavigationBar(containerColor = GlassSurface.copy(alpha = 0.95f)) {
                                            NavigationBarItem(
                                                selected = selectedTab == 0,
                                                onClick = { selectedTab = 0 },
                                                icon = { Icon(Icons.Rounded.Explore, contentDescription = "Discover") },
                                                label = { Text("Discover") },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = ChampagneGold,
                                                    selectedTextColor = ChampagneGold,
                                                    unselectedIconColor = MutedSilver,
                                                    unselectedTextColor = MutedSilver,
                                                    indicatorColor = ChampagneGold.copy(alpha = 0.14f)
                                                )
                                            )
                                            NavigationBarItem(
                                                selected = selectedTab == 1,
                                                onClick = { selectedTab = 1 },
                                                icon = { Icon(Icons.Rounded.Groups, contentDescription = "Party") },
                                                label = { Text("Party") },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = ChampagneGold,
                                                    selectedTextColor = ChampagneGold,
                                                    unselectedIconColor = MutedSilver,
                                                    unselectedTextColor = MutedSilver,
                                                    indicatorColor = ChampagneGold.copy(alpha = 0.14f)
                                                )
                                            )
                                            NavigationBarItem(
                                                selected = selectedTab == 2,
                                                onClick = { selectedTab = 2 },
                                                icon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings") },
                                                label = { Text("Settings") },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = ChampagneGold,
                                                    selectedTextColor = ChampagneGold,
                                                    unselectedIconColor = MutedSilver,
                                                    unselectedTextColor = MutedSilver,
                                                    indicatorColor = ChampagneGold.copy(alpha = 0.14f)
                                                )
                                            )
                                        }
                                    }
                                }
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .statusBarsPadding()
                                        .padding(innerPadding)
                                ) {
                                    when (selectedTab) {
                                        0 -> HomeSearchScreen(playerController = playerController)
                                        1 -> PartyRoomScreen(playerController = playerController)
                                        2 -> SettingsScreen(playerController = playerController)
                                    }
                                }
                            }

                            AnimatedVisibility(
                                visible = nowPlayingExpanded && currentTrack != null,
                                enter = slideInVertically { it },
                                exit = slideOutVertically { it },
                                modifier = Modifier.align(Alignment.BottomCenter)
                            ) {
                                NowPlayingScreen(
                                    playerController = playerController,
                                    onCollapse = { nowPlayingExpanded = false },
                                    onEnterAmbient = { manualAmbient = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
