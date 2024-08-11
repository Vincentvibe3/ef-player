package com.github.Vincentvibe3.efplayer.extractors

import com.github.Vincentvibe3.efplayer.core.Track
import com.github.Vincentvibe3.efplayer.extractors.serialization.YtDlpDumpResult
import com.github.Vincentvibe3.efplayer.extractors.serialization.YtDlpPlaylistDumpResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

object YoutubeDL:Extractor() {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    override suspend fun getPlaylistTracks(url: String, loadId: String): List<Track> {
        val process = withContext(Dispatchers.IO) {
            ProcessBuilder().command("yt-dlp", "--dump-json", "--flat-playlist",url).start()
        }
        val jsonDump = process.inputReader().readLines()
        val errorText = process.errorReader().readText()
        if (errorText.isNotBlank()) {
            val logger = LoggerFactory.getLogger(this.javaClass)
            logger.error(errorText)
        }
        val tracks = jsonDump.mapNotNull {
            try {
                val results = json.decodeFromString<YtDlpPlaylistDumpResult>(it)
                Track(results.original_url, YoutubeDL, results.title, results.channel, results.duration, loadId)
            } catch (e: SerializationException) {
                null
            }
        }
        return tracks
    }

    override suspend fun getUrlType(url: String): URL_TYPE {
        return Youtube.getUrlType(url)
    }

    override suspend fun getStream(url: String, track: Track): String? {
        val process = withContext(Dispatchers.IO) {
            ProcessBuilder().command("yt-dlp", "--dump-json", url).start()
        }
        val jsonDump = process.inputReader().readText()
        val errorText = process.errorReader().readText()
        if (errorText.isNotBlank()) {
            val logger = LoggerFactory.getLogger(this.javaClass)
            logger.error(errorText)
        }
        val best = try {
            val results = json.decodeFromString<YtDlpDumpResult>(jsonDump)
            results.formats.filter {
                it.ext == "webm" && it.acodec == "opus" && it.vcodec == "none"
            }.sortedBy {
                it.abr
            }.lastOrNull()
        }catch(e:SerializationException) {
            e.printStackTrace()
            null
        }
        return best?.url
    }

    override suspend fun getTrack(url: String, loadId: String): Track? {
        val process = withContext(Dispatchers.IO) {
            ProcessBuilder().command("yt-dlp", "--dump-json", url).start()
        }
        val jsonDump = process.inputReader().readText()
        val errorText = process.errorReader().readText()
        if (errorText.isNotBlank()) {
            val logger = LoggerFactory.getLogger(this.javaClass)
            logger.error(errorText)
        }
        return try {
            val results = json.decodeFromString<YtDlpDumpResult>(jsonDump)
            Track(results.original_url, YoutubeDL, results.title, results.channel, results.duration, loadId)
        } catch (e:SerializationException){
            e.printStackTrace()
            null
        }
    }

    override suspend fun search(query: String, loadId: String): Track? {
        val process = withContext(Dispatchers.IO) {
            ProcessBuilder().command("yt-dlp", "--dump-json", "ytsearch:$query").start()
        }
        val jsonDump = process.inputReader().readText()
        val errorText = process.errorReader().readText()
        if (errorText.isNotBlank()) {
            val logger = LoggerFactory.getLogger(this.javaClass)
            logger.error(errorText)
        }
        return try {
            val results = json.decodeFromString<YtDlpDumpResult>(jsonDump)
            Track(results.original_url, YoutubeDL, results.title, results.channel, results.duration, loadId)
        } catch (e:SerializationException){
            e.printStackTrace()
            null
        }
    }
}