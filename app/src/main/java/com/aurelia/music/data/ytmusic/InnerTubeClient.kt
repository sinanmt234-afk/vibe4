package com.aurelia.music.data.ytmusic

import com.aurelia.music.data.model.AudioQuality
import com.aurelia.music.data.model.LyricLine
import com.aurelia.music.data.model.LyricsResult
import com.aurelia.music.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class InnerTubeClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        private const val BASE_YTM_API = "https://music.youtube.com/youtubei/v1"
        private const val WEB_REMIX_VERSION = "1.20241106.01.00"
        private const val WEB_REMIX_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
        private const val ANDROID_VR_USER_AGENT =
            "com.google.android.apps.youtube.vr.oculus/1.56.21 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip"
        private const val IOS_MUSIC_USER_AGENT =
            "com.google.ios.youtube/19.45.4 (iPhone16,2; U; CPU iOS 18_1_0 like Mac OS X;)"
    }

    private fun webRemixContext(): JsonObject = buildJsonObject {
        putJsonObject("context") {
            putJsonObject("client") {
                put("clientName", "WEB_REMIX")
                put("clientVersion", WEB_REMIX_VERSION)
                put("hl", "en")
                put("gl", "US")
            }
        }
    }

    private fun postWebRemix(endpoint: String, extraFields: Map<String, JsonElement>): JsonObject? {
        val bodyObj = buildJsonObject {
            put("context", webRemixContext()["context"]!!)
            extraFields.forEach { (k, v) -> put(k, v) }
        }
        val req = Request.Builder()
            .url("$BASE_YTM_API/$endpoint?prettyPrint=false")
            .header("User-Agent", WEB_REMIX_USER_AGENT)
            .header("Origin", "https://music.youtube.com")
            .header("Referer", "https://music.youtube.com/")
            .header("X-YouTube-Client-Name", "67")
            .header("X-YouTube-Client-Version", WEB_REMIX_VERSION)
            .post(bodyObj.toString().toRequestBody(jsonMediaType))
            .build()

        return client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val raw = resp.body?.string() ?: return null
            runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull()
        }
    }

    suspend fun getHomeTrending(): List<Track> = withContext(Dispatchers.IO) {
        val root = postWebRemix(
            "browse",
            mapOf("browseId" to kotlinx.serialization.json.JsonPrimitive("FEmusic_explore"))
        ) ?: return@withContext emptyList()

        val tracks = mutableListOf<Track>()
        collectTracksRecursive(root, tracks)
        tracks.distinctBy { it.videoId }.take(30)
    }

    suspend fun searchSongs(query: String): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val root = postWebRemix(
            "search",
            mapOf(
                "query" to kotlinx.serialization.json.JsonPrimitive(query),
                // Filter specifically for Songs
                "params" to kotlinx.serialization.json.JsonPrimitive("EgWKAQIIAWoKEAMQBBAJEAoQBQ%3D%3D")
            )
        ) ?: return@withContext emptyList()

        val tracks = mutableListOf<Track>()
        collectTracksRecursive(root, tracks)
        if (tracks.isEmpty()) {
            // Fallback unfiltered search
            val fallback = postWebRemix(
                "search",
                mapOf("query" to kotlinx.serialization.json.JsonPrimitive(query))
            )
            if (fallback != null) collectTracksRecursive(fallback, tracks)
        }
        tracks.distinctBy { it.videoId }.take(35)
    }

    suspend fun getUpNextQueue(videoId: String): List<Track> = withContext(Dispatchers.IO) {
        val root = postWebRemix(
            "next",
            mapOf(
                "videoId" to kotlinx.serialization.json.JsonPrimitive(videoId),
                "isAudioOnly" to kotlinx.serialization.json.JsonPrimitive(true)
            )
        ) ?: return@withContext emptyList()

        val tracks = mutableListOf<Track>()
        collectTracksRecursive(root, tracks)
        tracks.filter { it.videoId != videoId }.distinctBy { it.videoId }.take(20)
    }

    suspend fun getLyrics(videoId: String, trackTitle: String = "", artist: String = ""): LyricsResult =
        withContext(Dispatchers.IO) {
            // 1. Try YouTube Music InnerTube lyrics browseId
            val nextRoot = postWebRemix(
                "next",
                mapOf(
                    "videoId" to kotlinx.serialization.json.JsonPrimitive(videoId),
                    "isAudioOnly" to kotlinx.serialization.json.JsonPrimitive(true)
                )
            )

            val lyricsBrowseId = nextRoot?.let { findLyricsBrowseId(it) }
            if (lyricsBrowseId != null) {
                val browseRoot = postWebRemix(
                    "browse",
                    mapOf("browseId" to kotlinx.serialization.json.JsonPrimitive(lyricsBrowseId))
                )
                if (browseRoot != null) {
                    val synced = extractTimedLyrics(browseRoot)
                    if (synced.isNotEmpty()) {
                        return@withContext LyricsResult(syncedLines = synced, source = "YouTube Music Synced")
                    }
                    val plain = extractPlainLyrics(browseRoot)
                    if (plain.isNotEmpty()) {
                        return@withContext LyricsResult(plainLines = plain, source = "YouTube Music")
                    }
                }
            }

            // 2. Fallback to LRCLIB open synced lyrics API if YT Music has no lyrics for this track
            if (trackTitle.isNotBlank()) {
                val lrcResult = fetchFromLrcLib(trackTitle, artist)
                if (!lrcResult.isEmpty) return@withContext lrcResult
            }

            LyricsResult()
        }

    private fun fetchFromLrcLib(title: String, artist: String): LyricsResult {
        return runCatching {
            val cleanTitle = java.net.URLEncoder.encode(title.substringBefore("(").trim(), "UTF-8")
            val cleanArtist = java.net.URLEncoder.encode(artist.substringBefore(",").trim(), "UTF-8")
            val req = Request.Builder()
                .url("https://lrclib.net/api/get?track_name=$cleanTitle&artist_name=$cleanArtist")
                .header("User-Agent", "AureliaMusic/1.0")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return LyricsResult()
                val obj = json.parseToJsonElement(resp.body?.string() ?: return LyricsResult()).jsonObject
                val syncedLrc = obj["syncedLyrics"]?.jsonPrimitive?.contentOrNull
                if (!syncedLrc.isNullOrBlank()) {
                    val parsed = parseLrcString(syncedLrc)
                    if (parsed.isNotEmpty()) {
                        return LyricsResult(syncedLines = parsed, source = "LRCLIB Synced")
                    }
                }
                val plain = obj["plainLyrics"]?.jsonPrimitive?.contentOrNull
                if (!plain.isNullOrBlank()) {
                    return LyricsResult(plainLines = plain.lines().filter { it.isNotBlank() }, source = "LRCLIB")
                }
                LyricsResult()
            }
        }.getOrDefault(LyricsResult())
    }

    private fun parseLrcString(lrc: String): List<LyricLine> {
        val regex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})](.*)""")
        return lrc.lines().mapNotNull { line ->
            val match = regex.find(line) ?: return@mapNotNull null
            val min = match.groupValues[1].toLongOrNull() ?: 0L
            val sec = match.groupValues[2].toLongOrNull() ?: 0L
            val fracStr = match.groupValues[3]
            val ms = if (fracStr.length == 2) (fracStr.toLongOrNull() ?: 0L) * 10L else (fracStr.toLongOrNull() ?: 0L)
            val text = match.groupValues[4].trim()
            if (text.isBlank()) null else LyricLine(min * 60_000L + sec * 1_000L + ms, text)
        }.sortedBy { it.timestampMs }
    }

    /**
     * Resolves a direct playable audio stream URL from YouTube Music InnerTube /player.
     * Selects the best matching bitrate based on the user's Wi-Fi / Cellular AudioQuality setting.
     */
    suspend fun resolveStreamUrl(videoId: String, quality: AudioQuality): String? = withContext(Dispatchers.IO) {
        // Try ANDROID_VR first (returns direct un-ciphered adaptiveFormats), then IOS_MUSIC
        resolveWithClient(
            videoId = videoId,
            clientName = "ANDROID_VR",
            clientVersion = "1.56.21",
            userAgent = ANDROID_VR_USER_AGENT,
            quality = quality
        ) ?: resolveWithClient(
            videoId = videoId,
            clientName = "IOS_MUSIC",
            clientVersion = "7.04",
            userAgent = IOS_MUSIC_USER_AGENT,
            quality = quality
        )
    }

    private fun resolveWithClient(
        videoId: String,
        clientName: String,
        clientVersion: String,
        userAgent: String,
        quality: AudioQuality
    ): String? {
        val body = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", clientName)
                    put("clientVersion", clientVersion)
                    put("hl", "en")
                    put("gl", "US")
                }
            }
            put("videoId", videoId)
            put("contentCheckOk", true)
            put("racyCheckOk", true)
        }

        val req = Request.Builder()
            .url("https://music.youtube.com/youtubei/v1/player?prettyPrint=false")
            .header("User-Agent", userAgent)
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        return runCatching {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val root = json.parseToJsonElement(resp.body?.string() ?: return null).jsonObject
                val streamingData = root["streamingData"]?.jsonObject ?: return null
                val adaptiveFormats = streamingData["adaptiveFormats"]?.jsonArray ?: JsonArray(emptyList())
                val formats = streamingData["formats"]?.jsonArray ?: JsonArray(emptyList())

                val audioCandidates = adaptiveFormats
                    .mapNotNull { it.jsonObject }
                    .filter { fmt ->
                        val mime = fmt["mimeType"]?.jsonPrimitive?.contentOrNull.orEmpty()
                        mime.startsWith("audio/") && fmt["url"]?.jsonPrimitive?.contentOrNull != null
                    }

                if (audioCandidates.isNotEmpty()) {
                    val targetBps = quality.targetBitrateKbps * 1000
                    val chosen = when (quality) {
                        AudioQuality.BEST -> audioCandidates.maxByOrNull { it["bitrate"]?.jsonPrimitive?.intOrNull ?: 0 }
                        AudioQuality.LOW -> audioCandidates.minByOrNull { it["bitrate"]?.jsonPrimitive?.intOrNull ?: Int.MAX_VALUE }
                        else -> audioCandidates.minByOrNull { fmt ->
                            val br = fmt["bitrate"]?.jsonPrimitive?.intOrNull ?: 128000
                            kotlin.math.abs(br - targetBps)
                        }
                    }
                    chosen?.get("url")?.jsonPrimitive?.contentOrNull
                } else {
                    formats.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
                }
            }
        }.getOrNull()
    }

    // ---- JSON Tree Walkers for YT Music InnerTube ----

    private fun collectTracksRecursive(element: JsonElement, out: MutableList<Track>) {
        when (element) {
            is JsonObject -> {
                element["musicResponsiveListItemRenderer"]?.jsonObject?.let { renderer ->
                    parseResponsiveItem(renderer)?.let { out.add(it) }
                }
                element["musicTwoRowItemRenderer"]?.jsonObject?.let { renderer ->
                    parseTwoRowItem(renderer)?.let { out.add(it) }
                }
                element["playlistPanelVideoRenderer"]?.jsonObject?.let { renderer ->
                    parsePanelVideo(renderer)?.let { out.add(it) }
                }
                element.values.forEach { collectTracksRecursive(it, out) }
            }
            is JsonArray -> element.forEach { collectTracksRecursive(it, out) }
            else -> Unit
        }
    }

    private fun parseResponsiveItem(obj: JsonObject): Track? {
        val videoId = obj["playlistItemData"]?.jsonObject?.get("videoId")?.jsonPrimitive?.contentOrNull
            ?: obj["overlay"]?.jsonObject
                ?.get("musicItemThumbnailOverlayRenderer")?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("musicPlayButtonRenderer")?.jsonObject
                ?.get("playNavigationEndpoint")?.jsonObject
                ?.get("watchEndpoint")?.jsonObject
                ?.get("videoId")?.jsonPrimitive?.contentOrNull
            ?: return null

        val flexColumns = obj["flexColumns"]?.jsonArray ?: return null
        val title = flexColumns.getOrNull(0)?.jsonObject
            ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
            ?.get("text")?.jsonObject
            ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("text")?.jsonPrimitive?.contentOrNull ?: return null

        val subtitleRuns = flexColumns.getOrNull(1)?.jsonObject
            ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
            ?.get("text")?.jsonObject
            ?.get("runs")?.jsonArray
            ?.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }
            ?: emptyList()

        val artist = subtitleRuns.firstOrNull { it != " • " && it != "Song" && it != "Video" } ?: "Unknown Artist"
        val thumb = extractBestThumbnail(obj["thumbnail"])

        return Track(
            videoId = videoId,
            title = title,
            artist = artist,
            thumbnailUrl = thumb,
            highResThumbnailUrl = upgradeThumbnailResolution(thumb)
        )
    }

    private fun parseTwoRowItem(obj: JsonObject): Track? {
        val videoId = obj["navigationEndpoint"]?.jsonObject
            ?.get("watchEndpoint")?.jsonObject
            ?.get("videoId")?.jsonPrimitive?.contentOrNull ?: return null

        val title = obj["title"]?.jsonObject?.get("runs")?.jsonArray
            ?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: return null

        val artist = obj["subtitle"]?.jsonObject?.get("runs")?.jsonArray
            ?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: "Unknown Artist"

        val thumb = extractBestThumbnail(obj["thumbnailRenderer"])
        return Track(
            videoId = videoId,
            title = title,
            artist = artist,
            thumbnailUrl = thumb,
            highResThumbnailUrl = upgradeThumbnailResolution(thumb)
        )
    }

    private fun parsePanelVideo(obj: JsonObject): Track? {
        val videoId = obj["videoId"]?.jsonPrimitive?.contentOrNull ?: return null
        val title = obj["title"]?.jsonObject?.get("runs")?.jsonArray
            ?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: return null
        val artist = obj["longBylineText"]?.jsonObject?.get("runs")?.jsonArray
            ?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: "Unknown Artist"
        val thumb = extractBestThumbnail(obj["thumbnail"])
        return Track(
            videoId = videoId,
            title = title,
            artist = artist,
            thumbnailUrl = thumb,
            highResThumbnailUrl = upgradeThumbnailResolution(thumb)
        )
    }

    private fun extractBestThumbnail(element: JsonElement?): String {
        val thumbnails = findThumbnailsArray(element)
        return thumbnails.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull.orEmpty()
    }

    private fun findThumbnailsArray(element: JsonElement?): JsonArray {
        if (element == null) return JsonArray(emptyList())
        if (element is JsonObject) {
            element["thumbnails"]?.jsonArray?.let { return it }
            element.values.forEach { child ->
                val found = findThumbnailsArray(child)
                if (found.isNotEmpty()) return found
            }
        } else if (element is JsonArray) {
            element.forEach { child ->
                val found = findThumbnailsArray(child)
                if (found.isNotEmpty()) return found
            }
        }
        return JsonArray(emptyList())
    }

    private fun upgradeThumbnailResolution(url: String): String {
        if (url.contains("lh3.googleusercontent.com")) {
            return url.replace(Regex("=w\\d+-h\\d+.*"), "=w1080-h1080-l90-rj")
        }
        return url
    }

    private fun findLyricsBrowseId(element: JsonElement): String? {
        when (element) {
            is JsonObject -> {
                val title = element["tabRenderer"]?.jsonObject?.get("title")?.jsonPrimitive?.contentOrNull
                if (title.equals("Lyrics", ignoreCase = true)) {
                    element["tabRenderer"]?.jsonObject
                        ?.get("endpoint")?.jsonObject
                        ?.get("browseEndpoint")?.jsonObject
                        ?.get("browseId")?.jsonPrimitive?.contentOrNull?.let { return it }
                }
                element.values.forEach { child ->
                    findLyricsBrowseId(child)?.let { return it }
                }
            }
            is JsonArray -> element.forEach { child ->
                findLyricsBrowseId(child)?.let { return it }
            }
            else -> Unit
        }
        return null
    }

    private fun extractTimedLyrics(element: JsonElement): List<LyricLine> {
        val out = mutableListOf<LyricLine>()
        fun walk(el: JsonElement) {
            when (el) {
                is JsonObject -> {
                    el["lyricLine"]?.jsonPrimitive?.contentOrNull?.let { text ->
                        val startMs = el["cueRange"]?.jsonObject?.get("startTimeMilliseconds")
                            ?.jsonPrimitive?.longOrNull ?: 0L
                        if (text.isNotBlank()) out.add(LyricLine(startMs, text))
                    }
                    el.values.forEach { walk(it) }
                }
                is JsonArray -> el.forEach { walk(it) }
                else -> Unit
            }
        }
        walk(element)
        return out.sortedBy { it.timestampMs }
    }

    private fun extractPlainLyrics(element: JsonElement): List<String> {
        val out = mutableListOf<String>()
        fun walk(el: JsonElement) {
            when (el) {
                is JsonObject -> {
                    el["musicDescriptionShelfRenderer"]?.jsonObject
                        ?.get("description")?.jsonObject
                        ?.get("runs")?.jsonArray
                        ?.forEach { run ->
                            run.jsonObject["text"]?.jsonPrimitive?.contentOrNull
                                ?.lines()
                                ?.filter { it.isNotBlank() }
                                ?.let { out.addAll(it) }
                        }
                    el.values.forEach { walk(it) }
                }
                is JsonArray -> el.forEach { walk(it) }
                else -> Unit
            }
        }
        walk(element)
        return out
    }
}
