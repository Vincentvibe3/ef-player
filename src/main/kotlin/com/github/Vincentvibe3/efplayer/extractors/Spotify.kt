package com.github.Vincentvibe3.efplayer.extractors

import com.github.Vincentvibe3.efplayer.core.Config
import com.github.Vincentvibe3.efplayer.core.Track
import com.github.Vincentvibe3.efplayer.streaming.RequestHandler
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
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

    private suspend fun getNewToken(){
        val requestBody = "grant_type=client_credentials"
        val authString = Base64.getEncoder().encodeToString("${Config.spotifyClient}:${Config.spotifySecret}".toByteArray())
        val headers = hashMapOf("Authorization" to "Basic $authString", "Content-Type" to "application/x-www-form-urlencoded")
        val response = RequestHandler.post("https://accounts.spotify.com/api/token", requestBody, headers)
        try {
            val jsonResponse = JSONObject(response)
            Config.spotifyToken = jsonResponse.getString("access_token")
            Config.spotifyTokenExpiry = System.currentTimeMillis()/1000+jsonResponse.getLong("expires_in")
        }catch (e:JSONException){
            println(response)
            e.printStackTrace()
        }

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
    override suspend fun getStream(url: String, track: Track): String? {
        val ytTrack = Youtube.search("${track.title} ${track.author}")
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
    override suspend fun getTrack(url: String): Track? {
        val id = url.removePrefix("https://open.spotify.com/track/").split("?")[0]
        val token = getToken()
        val headers = hashMapOf("Authorization" to "Bearer $token")
        val response = RequestHandler.get("https://api.spotify.com/v1/tracks/$id", headers)
        try {
            val jsonResponse = JSONObject(response)
            return if (jsonResponse.has("error")){
                null
            } else {
                val artistsList = ArrayList<String>()
                val artists = jsonResponse.getJSONArray("artists")
                for (index in 0 until artists.length()){
                    val artistData = artists.getJSONObject(index)
                    artistsList.add(artistData.getString("name"))
                }
                val artistString = artistsList.joinToString(", ")
                val title = jsonResponse.getString("name")
                return Youtube.search("$title $artistString")
            }
        } catch (e:JSONException){
            e.printStackTrace()
            return null
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

    private fun parsePlaylistTracks(items:JSONArray, tracks:ArrayList<Track>) {
        for (index in 0 until items.length()){
            val track = items.getJSONObject(index).getJSONObject("track")
            val artistsList = ArrayList<String>()
            val artists = track.getJSONArray("artists")
            for (indexArtists in 0 until artists.length()){
                val artistData = artists.getJSONObject(indexArtists)
                artistsList.add(artistData.getString("name"))
            }
            val artistString = artistsList.joinToString(", ")
            val title = track.getString("name")
            val duration = track.getLong("duration_ms")
            val url = track.getJSONObject("external_urls").getString("spotify")
            tracks.add(Track(url, Spotify, title, artistString, duration))
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
    override suspend fun getPlaylistTracks(url: String): List<Track> {
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
                val jsonResponse = JSONObject(response)
                if (jsonResponse.has("error")){
                    return tracks
                } else {
                    val items = jsonResponse.getJSONArray("items")
                    parsePlaylistTracks(items, tracks)
                    if (jsonResponse.isNull("next")){
                        hasNext = false
                    } else {
                        endpoint = jsonResponse.getString("next")+
                            URLEncoder.encode(
                                "&fields=items(track(artists.name, name, duration_ms, external_urls.spotify)),next",
                                Charset.defaultCharset()
                            )
                    }
                }
            } catch (e:JSONException){
                e.printStackTrace()
                break
            }
        }
        return tracks
    }

    override suspend fun search(query: String): Track? {
        return null
    }

}