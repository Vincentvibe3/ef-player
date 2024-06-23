package com.github.Vincentvibe3.efplayer.extractors

import com.github.Vincentvibe3.efplayer.core.Track
import com.github.Vincentvibe3.efplayer.extractors.serialization.YtDlpDumpResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object YoutubeDL:Extractor() {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    override suspend fun getPlaylistTracks(url: String, loadId: String): List<Track> {
        val tracks = Youtube.getPlaylistTracks(url, loadId)
        tracks.forEach {
            it.extractor = YoutubeDL
        }
        return  tracks
    }

    override suspend fun getUrlType(url: String): URL_TYPE {
        return Youtube.getUrlType(url)
    }

    override suspend fun getStream(url: String, track: Track): String? {
        val jsonDump = withContext(Dispatchers.IO) {
            ProcessBuilder().command("yt-dlp", "--dump-json", url).start()
        }.inputReader().readText()
        val results = json.decodeFromString<YtDlpDumpResult>(jsonDump)
        val best = results.formats.filter {
            it.ext == "webm" && it.acodec == "opus" && it.vcodec == "none"
        }.sortedBy {
            it.abr
        }.last()
        return best.url
    }

    override suspend fun getTrack(url: String, loadId: String): Track? {
        val track =  Youtube.getTrack(url, loadId)
        if (track != null) {
            track.extractor = YoutubeDL
        }
        return track
    }

    override suspend fun search(query: String, loadId: String): Track? {
        val track = Youtube.search(query, loadId)
        if (track != null) {
            track.extractor = YoutubeDL
        }
        return track
    }
}