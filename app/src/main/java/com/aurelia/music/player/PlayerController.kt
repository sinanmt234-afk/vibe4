package com.aurelia.music.player

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.aurelia.music.data.model.AudioQuality
import com.aurelia.music.data.model.LyricsResult
import com.aurelia.music.data.model.PartyRoomState
import com.aurelia.music.data.model.PlayerSettings
import com.aurelia.music.data.model.Track
import com.aurelia.music.data.party.PartySyncClient
import com.aurelia.music.data.prefs.SettingsRepository
import com.aurelia.music.data.ytmusic.InnerTubeClient
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerController(
    private val context: Context,
    val settingsRepository: SettingsRepository,
    val innerTubeClient: InnerTubeClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val partySyncClient = PartySyncClient(scope)
    private val proAudioEngine = ProAudioEngine()

    val settings: StateFlow<PlayerSettings> = settingsRepository.settingsFlow
        .stateIn(scope, SharingStarted.Eagerly, PlayerSettings())

    private var mediaController: MediaController? = null
    private var progressJob: Job? = null

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _lyrics = MutableStateFlow(LyricsResult())
    val lyrics: StateFlow<LyricsResult> = _lyrics.asStateFlow()

    private val _dominantColor = MutableStateFlow(Color(0xFFD4AF37))
    val dominantColor: StateFlow<Color> = _dominantColor.asStateFlow()

    private val _activeNetworkLabel = MutableStateFlow("Wi-Fi")
    val activeNetworkLabel: StateFlow<String> = _activeNetworkLabel.asStateFlow()

    init {
        connectMediaService()
        observeSettings()
        observePartyRoom()
    }

    private fun connectMediaService() {
        val sessionToken = SessionToken(context, ComponentName(context, AureliaPlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            val controller = controllerFuture.get()
            mediaController = controller
            controller.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    broadcastPartyIfHost()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    _isBuffering.value = playbackState == Player.STATE_BUFFERING
                    if (playbackState == Player.STATE_READY) {
                        _durationMs.value = controller.duration.coerceAtLeast(0L)
                    } else if (playbackState == Player.STATE_ENDED) {
                        playNext()
                    }
                }
            })
            startProgressTicker()
        }, MoreExecutors.directExecutor())
    }

    private fun observeSettings() {
        scope.launch {
            settings.collect { s ->
                proAudioEngine.applySettings(s)
            }
        }
    }

    private fun observePartyRoom() {
        scope.launch {
            partySyncClient.roomState.collect { roomState ->
                if (roomState != null && !partySyncClient.isHost.value) {
                    syncListenerToRoomState(roomState)
                }
            }
        }
    }

    private fun syncListenerToRoomState(roomState: PartyRoomState) {
        val remoteTrack = roomState.track ?: return
        val elapsedSinceUpdate = (System.currentTimeMillis() - roomState.updatedAtUtcMs).coerceAtLeast(0L)
        val targetPos = if (roomState.isPlaying) roomState.positionMs + elapsedSinceUpdate else roomState.positionMs

        if (_currentTrack.value?.videoId != remoteTrack.videoId) {
            scope.launch {
                playTrackInternal(remoteTrack, seekToMs = targetPos, autoPlay = roomState.isPlaying)
            }
        } else {
            val controller = mediaController ?: return
            val drift = kotlin.math.abs(controller.currentPosition - targetPos)
            if (drift > 1800L) {
                controller.seekTo(targetPos)
            }
            if (roomState.isPlaying && !controller.isPlaying) {
                controller.play()
            } else if (!roomState.isPlaying && controller.isPlaying) {
                controller.pause()
            }
        }
    }

    fun playTrack(track: Track, newQueue: List<Track> = emptyList()) {
        scope.launch {
            if (newQueue.isNotEmpty()) {
                _queue.value = newQueue
            } else if (_queue.value.none { it.videoId == track.videoId }) {
                _queue.value = listOf(track) + _queue.value
            }
            playTrackInternal(track, seekToMs = 0L, autoPlay = true)
            // Fetch up-next queue in background if queue is small
            if (_queue.value.size <= 2) {
                val upNext = innerTubeClient.getUpNextQueue(track.videoId)
                if (upNext.isNotEmpty()) {
                    _queue.value = (listOf(track) + upNext).distinctBy { it.videoId }
                }
            }
        }
    }

    private suspend fun playTrackInternal(track: Track, seekToMs: Long, autoPlay: Boolean) {
        _currentTrack.value = track
        _isBuffering.value = true
        _lyrics.value = LyricsResult()

        // Extract dynamic color from album art
        scope.launch { extractArtworkColor(track.highResThumbnailUrl) }
        // Load lyrics in parallel
        scope.launch {
            _lyrics.value = innerTubeClient.getLyrics(track.videoId, track.title, track.artist)
        }

        val quality = resolveCurrentNetworkQuality()
        val streamUrl = innerTubeClient.resolveStreamUrl(track.videoId, quality)
        if (streamUrl == null) {
            _isBuffering.value = false
            return
        }

        val mediaItem = MediaItem.Builder()
            .setMediaId(track.videoId)
            .setUri(streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setArtworkUri(Uri.parse(track.highResThumbnailUrl))
                    .build()
            )
            .build()

        mediaController?.apply {
            setMediaItem(mediaItem)
            prepare()
            if (seekToMs > 0L) seekTo(seekToMs)
            playWhenReady = autoPlay
        }

        // Attach audio effects
        broadcastPartyIfHost()
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _positionMs.value = positionMs
        broadcastPartyIfHost()
    }

    fun playNext() {
        val list = _queue.value
        val cur = _currentTrack.value ?: return
        val idx = list.indexOfFirst { it.videoId == cur.videoId }
        if (idx >= 0 && idx + 1 < list.size) {
            playTrack(list[idx + 1])
        }
    }

    fun playPrevious() {
        val controller = mediaController
        if (controller != null && controller.currentPosition > 4000L) {
            seekTo(0L)
            return
        }
        val list = _queue.value
        val cur = _currentTrack.value ?: return
        val idx = list.indexOfFirst { it.videoId == cur.videoId }
        if (idx > 0) {
            playTrack(list[idx - 1])
        }
    }

    fun broadcastPartyIfHost() {
        if (!partySyncClient.isHost.value) return
        val currentRoom = partySyncClient.roomState.value ?: return
        val track = _currentTrack.value ?: return
        partySyncClient.publishState(
            currentRoom.copy(
                track = track,
                isPlaying = mediaController?.isPlaying ?: _isPlaying.value,
                positionMs = mediaController?.currentPosition ?: _positionMs.value,
                updatedAtUtcMs = System.currentTimeMillis()
            )
        )
    }

    private fun resolveCurrentNetworkQuality(): AudioQuality {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(network)
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
        _activeNetworkLabel.value = if (isWifi) "Wi-Fi" else "Cellular"
        return if (isWifi) settings.value.wifiQuality else settings.value.cellularQuality
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                mediaController?.let { c ->
                    _positionMs.value = c.currentPosition.coerceAtLeast(0L)
                    if (c.duration > 0L) _durationMs.value = c.duration
                }
                delay(400L)
            }
        }
    }

    private suspend fun extractArtworkColor(url: String) {
        if (url.isBlank()) return
        runCatching {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .build()
            val result = (loader.execute(request) as? SuccessResult)?.drawable
            val bitmap: Bitmap? = (result as? BitmapDrawable)?.bitmap
            if (bitmap != null) {
                val palette = Palette.from(bitmap).generate()
                val rgb = palette.getVibrantColor(
                    palette.getMutedColor(0xFFD4AF37.toInt())
                )
                _dominantColor.value = Color(rgb)
            }
        }
    }
}
