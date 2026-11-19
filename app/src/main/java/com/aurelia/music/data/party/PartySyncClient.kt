package com.aurelia.music.data.party

import com.aurelia.music.data.model.PartyRoomState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Zero-config global Friends Party Sync Engine using MQTT v3.1.1 over Secure WebSockets (WSS).
 * Anyone with the Aurelia app anywhere in the world can create or join a 6-character room
 * and listen in real-time synchronization with automatic drift compensation.
 */
class PartySyncClient(private val scope: CoroutineScope) {

    companion object {
        private const val BROKER_WSS_URL = "wss://broker.emqx.io:8084/mqtt"
        private const val TOPIC_PREFIX = "aurelia/party/v1"
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(25, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var pingJob: Job? = null
    private var currentRoomCode: String? = null
    private var isHostRole: Boolean = false

    private val _connectionState = MutableStateFlow(PartyConnectionState.DISCONNECTED)
    val connectionState: StateFlow<PartyConnectionState> = _connectionState.asStateFlow()

    private val _roomState = MutableStateFlow<PartyRoomState?>(null)
    val roomState: StateFlow<PartyRoomState?> = _roomState.asStateFlow()

    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost.asStateFlow()

    enum class PartyConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, ERROR
    }

    fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    fun createRoom(hostName: String): String {
        val code = generateRoomCode()
        isHostRole = true
        _isHost.value = true
        val initial = PartyRoomState(
            roomCode = code,
            hostName = hostName.ifBlank { "Host" },
            isPlaying = false,
            positionMs = 0L,
            updatedAtUtcMs = System.currentTimeMillis()
        )
        _roomState.value = initial
        connectToRoom(code, initialStateToPublish = initial)
        return code
    }

    fun joinRoom(roomCode: String) {
        val cleanCode = roomCode.trim().uppercase()
        if (cleanCode.length < 4) return
        isHostRole = false
        _isHost.value = false
        _roomState.value = null
        connectToRoom(cleanCode, initialStateToPublish = null)
    }

    fun publishState(state: PartyRoomState) {
        if (!isHostRole) return
        val topic = "$TOPIC_PREFIX/${state.roomCode}/state"
        val payload = json.encodeToString(state.copy(updatedAtUtcMs = System.currentTimeMillis()))
        _roomState.value = state
        sendMqttPublish(topic, payload, retain = true)
    }

    fun sendReaction(emoji: String) {
        val current = _roomState.value ?: return
        val topic = "$TOPIC_PREFIX/${current.roomCode}/reaction"
        sendMqttPublish(topic, emoji, retain = false)
    }

    fun leaveRoom() {
        pingJob?.cancel()
        pingJob = null
        webSocket?.close(1000, "Left party")
        webSocket = null
        currentRoomCode = null
        isHostRole = false
        _isHost.value = false
        _roomState.value = null
        _connectionState.value = PartyConnectionState.DISCONNECTED
    }

    private fun connectToRoom(roomCode: String, initialStateToPublish: PartyRoomState?) {
        leaveRoom()
        currentRoomCode = roomCode
        isHostRole = initialStateToPublish != null
        _isHost.value = isHostRole
        _connectionState.value = PartyConnectionState.CONNECTING

        val request = Request.Builder()
            .url(BROKER_WSS_URL)
            .header("Sec-WebSocket-Protocol", "mqtt")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                val clientId = "aurelia_${Random.nextInt(100000, 999999)}"
                ws.send(buildMqttConnectPacket(clientId))
            }

            override fun onMessage(ws: WebSocket, bytes: ByteString) {
                handleMqttPacket(bytes.toByteArray(), roomCode, initialStateToPublish)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = PartyConnectionState.ERROR
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                _connectionState.value = PartyConnectionState.DISCONNECTED
            }
        })
    }

    private fun handleMqttPacket(
        data: ByteArray,
        roomCode: String,
        initialStateToPublish: PartyRoomState?
    ) {
        if (data.isEmpty()) return
        val packetType = (data[0].toInt() and 0xF0) ushr 4
        when (packetType) {
            2 -> { // CONNACK
                _connectionState.value = PartyConnectionState.CONNECTED
                sendMqttSubscribe("$TOPIC_PREFIX/$roomCode/#")
                if (initialStateToPublish != null) {
                    publishState(initialStateToPublish)
                }
                startKeepAlive()
            }
            3 -> { // PUBLISH
                val parsed = parseMqttPublish(data) ?: return
                val (topic, payload) = parsed
                if (topic.endsWith("/state")) {
                    runCatching { json.decodeFromString<PartyRoomState>(payload) }.getOrNull()?.let { state ->
                        if (!isHostRole) {
                            _roomState.value = state
                        }
                    }
                } else if (topic.endsWith("/reaction")) {
                    _roomState.value = _roomState.value?.copy(latestReaction = payload)
                }
            }
        }
    }

    private fun startKeepAlive() {
        pingJob?.cancel()
        pingJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(20_000)
                webSocket?.send(byteArrayOf(0xC0.toByte(), 0x00).toByteString()) // PINGREQ
            }
        }
    }

    private fun sendMqttSubscribe(topicFilter: String) {
        val out = ByteArrayOutputStream()
        val body = ByteArrayOutputStream()
        // Packet Identifier = 1
        body.write(0x00)
        body.write(0x01)
        writeMqttString(body, topicFilter)
        body.write(0x00) // QoS 0
        val payloadBytes = body.toByteArray()
        out.write(0x82) // SUBSCRIBE
        writeRemainingLength(out, payloadBytes.size)
        out.write(payloadBytes)
        webSocket?.send(out.toByteArray().toByteString())
    }

    private fun sendMqttPublish(topic: String, message: String, retain: Boolean) {
        val out = ByteArrayOutputStream()
        val body = ByteArrayOutputStream()
        writeMqttString(body, topic)
        body.write(message.toByteArray(StandardCharsets.UTF_8))
        val payloadBytes = body.toByteArray()
        val header = if (retain) 0x31 else 0x30
        out.write(header)
        writeRemainingLength(out, payloadBytes.size)
        out.write(payloadBytes)
        webSocket?.send(out.toByteArray().toByteString())
    }

    private fun buildMqttConnectPacket(clientId: String): ByteString {
        val body = ByteArrayOutputStream()
        writeMqttString(body, "MQTT")
        body.write(0x04) // Protocol Level 4 (3.1.1)
        body.write(0x02) // Clean Session
        body.write(0x00)
        body.write(0x3C) // Keep Alive = 60s
        writeMqttString(body, clientId)
        val payloadBytes = body.toByteArray()

        val out = ByteArrayOutputStream()
        out.write(0x10) // CONNECT
        writeRemainingLength(out, payloadBytes.size)
        out.write(payloadBytes)
        return out.toByteArray().toByteString()
    }

    private fun parseMqttPublish(data: ByteArray): Pair<String, String>? {
        if (data.size < 4) return null
        var idx = 1
        // Decode variable remaining length
        var multiplier = 1
        var value = 0
        var encodedByte: Int
        do {
            if (idx >= data.size) return null
            encodedByte = data[idx++].toInt() and 0xFF
            value += (encodedByte and 127) * multiplier
            multiplier *= 128
        } while ((encodedByte and 128) != 0)

        if (idx + 2 > data.size) return null
        val topicLen = ((data[idx].toInt() and 0xFF) shl 8) or (data[idx + 1].toInt() and 0xFF)
        idx += 2
        if (idx + topicLen > data.size) return null
        val topic = String(data, idx, topicLen, StandardCharsets.UTF_8)
        idx += topicLen

        val qos = (data[0].toInt() and 0x06) ushr 1
        if (qos > 0) idx += 2 // Skip packet ID
        if (idx > data.size) return null
        val payload = String(data, idx, data.size - idx, StandardCharsets.UTF_8)
        return topic to payload
    }

    private fun writeMqttString(out: ByteArrayOutputStream, str: String) {
        val bytes = str.toByteArray(StandardCharsets.UTF_8)
        out.write((bytes.size ushr 8) and 0xFF)
        out.write(bytes.size and 0xFF)
        out.write(bytes)
    }

    private fun writeRemainingLength(out: ByteArrayOutputStream, length: Int) {
        var len = length
        do {
            var encodedByte = len % 128
            len /= 128
            if (len > 0) encodedByte = encodedByte or 128
            out.write(encodedByte)
        } while (len > 0)
    }
}
