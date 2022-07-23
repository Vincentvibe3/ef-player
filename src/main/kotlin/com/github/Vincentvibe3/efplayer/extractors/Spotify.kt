package com.github.Vincentvibe3.efplayer.extractors

import com.github.Vincentvibe3.efplayer.core.Config
import com.github.Vincentvibe3.efplayer.core.Track
import com.github.Vincentvibe3.efplayer.extractors.Extractor.URL_TYPE
import com.github.Vincentvibe3.efplayer.streaming.RequestHandler
import io.ktor.client.request.forms.*
import io.ktor.http.*
import org.json.JSONObject
import java.util.*

/**
 *
 * [Extractor] for Spotify
 *
 */
object Spotify:Extractor() {

    private suspend fun getNewToken(){
        val requestBody = FormDataContent(Parameters.build {
            append("grant_type", "client_credentials")
        })
        val authString = Base64.getEncoder().encodeToString("${Config.spotifyClient}:${Config.spotifySecret}".toByteArray())
        val headers = hashMapOf("Authorization" to "Basic $authString")
        val response = RequestHandler.post("https://accounts.spotify.com/api/token", requestBody, headers)
        val jsonResponse = JSONObject(response)
        Config.spotifyToken = jsonResponse.getString("access_token")
        Config.spotifyTokenExpiry = System.currentTimeMillis()/1000+jsonResponse.getLong("expires_in")
    }

    suspend fun getToken(): String {
        if (Config.spotifyToken.isEmpty()){
            getNewToken()
        } else if (Config.spotifyTokenExpiry <= System.currentTimeMillis()/1000){
            getNewToken()
        }
        return Config.spotifyToken
    }

    /**
     * Get a URL to stream audio from
     *
     * @param url the URL for the video/audio to be extracted
     *
     * @return the URL of the resource requested
     *
     */
    override suspend fun getStream(url: String): String? {
        TODO("Not yet implemented")
    }

    /**
     * create a [Track] from a URL to a resource
     *
     * @param url the URL to get a track from
     *
     * @return The [Track] with the resource's information
     *
     */
    override suspend fun getTrack(url: String): Track? {
        val id = url.removePrefix("https://open.spotify.com/track/").split("?")[0]
        val token = getToken()
        val headers = hashMapOf("Authorization" to "Bearer $token")
        val response = RequestHandler.get("https://accounts.spotify.com/api/tracks/$id", headers)
        val jsonResponse = JSONObject(response)
        return Track("", Youtube, "", "", 0)
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
        return super.getUrlType(url)
    }

    /**
     * Gets [Tracks][Track] from a playlist
     *
     * @param url The URL where to find the playlist
     *
     * @return an empty list if no tracks could be created
     *
     */
    override suspend fun getPlaylistTracks(url: String): List<Track> {
        return super.getPlaylistTracks(url)
    }

}