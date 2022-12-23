package com.github.Vincentvibe3.efplayer.core

import org.json.JSONObject
import java.io.File

object Config {

    var maxOpusChunks:Int = 4000
    var spotifySecret:String = ""
    var spotifyClient:String = ""
    var spotifyToken:String = ""
    var spotifyTokenExpiry:Long = -1

    init {
        val file = File("efplayer.config.json")
        if (file.exists()){
            val content =  file.bufferedReader()
                .lines()
                .toArray()
                .joinToString("\n")
            val json = JSONObject(content)
            maxOpusChunks = json.getInt("maxChunks")
            spotifyClient = json.optString("spotifyClient")
            spotifySecret = json.optString("spotifySecret")
            if (spotifyClient.isEmpty()){
                spotifyClient = System.getenv("SPOTIFY_CLIENT")
            }
            if (spotifySecret.isEmpty()){
                spotifySecret = System.getenv("SPOTIFY_SECRET")
            }
        }
    }

}