package com.github.Vincentvibe3.efplayer.extractors

import com.github.Vincentvibe3.efplayer.core.Player
import com.github.Vincentvibe3.efplayer.core.Track
import com.github.Vincentvibe3.efplayer.extractors.serialization.SpotifyPlaylistResponse
import com.github.Vincentvibe3.efplayer.extractors.serialization.SpotifyTokenResponse
import com.github.Vincentvibe3.efplayer.extractors.serialization.SpotifyTrackResponse
import com.github.Vincentvibe3.efplayer.streaming.RequestHandler
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.lang.IllegalArgumentException
import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.*
import kotlin.collections.ArrayList

/**
 *
 * [Extractor] for Spotify
 *
 */
object Spotify:Extractor() {

    private val json = Json {
        ignoreUnknownKeys=true
    }

    private suspend fun getNewToken(){
        val requestBody = "grant_type=client_credentials"
        val authString = Base64.getEncoder().encodeToString("${Player.config.spotifyClient}:${Player.config.spotifySecret}".toByteArray())
        val headers = hashMapOf("Authorization" to "Basic $authString", "Content-Type" to "application/x-www-form-urlencoded")
        val response = RequestHandler.post("https://accounts.spotify.com/api/token", requestBody, headers)
        try {
            val jsonResponse = json.decodeFromString<SpotifyTokenResponse>(response)
            Player.config.spotifyToken = jsonResponse.access_token
            Player.config.spotifyTokenExpiry = System.currentTimeMillis()/1000+jsonResponse.expires_in
        }catch (e:IllegalArgumentException){
            println(response)
            e.printStackTrace()
        }

    }

    private suspend fun getToken(): String {
        if (Player.config.spotifyToken.isEmpty()){
            getNewToken()
        } else if (Player.config.spotifyTokenExpiry <= System.currentTimeMillis()/1000){
            getNewToken()
        }
        return Player.config.spotifyToken
    }

    /**
     * Get a URL to stream audio from
     *
     * @param url the URL for the video/audio to be extracted
     *
     * @return the URL of the resource requested
     *
     */
    override suspend fun getStream(url: String, track: Track): String? {
        val ytTrack = Youtube.search("${track.title} ${track.author}", track.loadId)
        if (ytTrack != null) {
            track.author = ytTrack.author
            track.url = ytTrack.url
            track.duration = ytTrack.duration
            track.extractor = ytTrack.extractor
            track.title = ytTrack.title
            return ytTrack.getStream()
        }
        return null
    }

    /**
     * create a [Track] from a URL to a resource
     *
     * @param url the URL to get a track from
     *
     * @return The [Track] with the resource's information
     *
     */
    override suspend fun getTrack(url: String, loadId: String): Track? {
        val id = url.removePrefix("https://open.spotify.com/track/").split("?")[0]
        val token = getToken()
        val headers = hashMapOf("Authorization" to "Bearer $token")
        val response = RequestHandler.get("https://api.spotify.com/v1/tracks/$id", headers)
        return try {
            val jsonResponse = json.decodeFromString<SpotifyTrackResponse>(response)
            val artistsList = jsonResponse.artists.map { it.name }
            val artistString = artistsList.joinToString(", ")
            val title = jsonResponse.name
            Youtube.search("$title $artistString", loadId)

        } catch (e:IllegalArgumentException){
            Player.logger.error("Failed to decode track response from: https://api.spotify.com/v1/tracks/$id \n Received $response")
            null
        }


    }

    /**
     * Sort an input URL in different to extract differently
     *
     * @param url The input URL
     *
     * @return [URL_TYPE.TRACK] is returned by default if not overridden
     *
     * @see URL_TYPE
     */
    override suspend fun getUrlType(url: String): URL_TYPE {
        return if (url.contains("://open.spotify.com/track/")){
            URL_TYPE.TRACK
        } else if (url.contains("://open.spotify.com/playlist/")) {
            URL_TYPE.PLAYLIST
        } else if (url.contains("://open.spotify.com/artist/")) {
            URL_TYPE.ARTIST
        } else if (url.contains("://open.spotify.com/album/")) {
            URL_TYPE.ALBUM
        } else {
            URL_TYPE.INVALID
        }
    }

    private fun parsePlaylistTracks(items: List<SpotifyPlaylistResponse.SpotifyPlaylistTrack>, tracks: ArrayList<Track>, loadId: String) {
        for (element in items){
            val track = element.track
            val artistsList = track.artists.map { it.name }
            val artistString = artistsList.joinToString(", ")
            val title = track.name
            val duration = track.duration_ms
            val url = track.external_urls.spotify
            tracks.add(Track(url, Spotify, title, artistString, duration, loadId))
        }
    }

    /**
     * Gets [Tracks][Track] from a playlist
     *
     * @param url The URL where to find the playlist
     *
     * @return an empty list if no tracks could be created
     *
     */
    override suspend fun getPlaylistTracks(url: String, loadId: String): List<Track> {
        val id = url.removePrefix("https://open.spotify.com/playlist/").split("?")[0]
        val token = getToken()
        val headers = hashMapOf("Authorization" to "Bearer $token")
        var hasNext = true
        var endpoint = "https://api.spotify.com/v1/playlists/$id/tracks?fields=${
            URLEncoder.encode(
                "items(track(artists.name, name, duration_ms, external_urls.spotify)),next", 
                Charset.defaultCharset()
            )}"

        val tracks = ArrayList<Track>()
        while (hasNext){
            val response = RequestHandler.get(endpoint, headers)
            try {
                val jsonResponse = json.decodeFromString<SpotifyPlaylistResponse>(response)
                val items = jsonResponse.items
                parsePlaylistTracks(items, tracks, loadId)
                if (jsonResponse.next==null){
                    hasNext = false
                } else {
                    endpoint = jsonResponse.next+
                        URLEncoder.encode(
                            "&fields=items(track(artists.name, name, duration_ms, external_urls.spotify)),next",
                            Charset.defaultCharset()
                        )
                }
            } catch (e:IllegalArgumentException){
                Player.logger.error("Failed to serialize a playlist page for $endpoint")
                break
            }
        }
        return tracks
    }

    override suspend fun search(query: String, loadId: String): Track? {
        return null
    }

}