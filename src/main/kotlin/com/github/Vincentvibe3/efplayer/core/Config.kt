package com.github.Vincentvibe3.efplayer.core

import org.json.JSONObject
import java.io.File

object Config {

    var maxOpusChunks:Int = 4000

    fun getMaxChunks(){
        val file = File("efplayer.config.json")
        if (file.exists()){
            val content =  file.bufferedReader()
                .lines()
                .toArray()
                .joinToString("\n")
            val json = JSONObject(content)
            maxOpusChunks = json.getInt("maxChunks")
        }
    }

}