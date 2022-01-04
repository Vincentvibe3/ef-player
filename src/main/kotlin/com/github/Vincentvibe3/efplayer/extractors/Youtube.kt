package com.github.Vincentvibe3.efplayer.extractors

import com.github.Vincentvibe3.efplayer.core.RequestHandler
import com.github.Vincentvibe3.efplayer.core.Track
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException

object Youtube: Extractor() {

    suspend fun getPlaylistTracks(url:String):List<Track>{
        val tracks = ArrayList<Track>()
        val response = RequestHandler.get(url)
        val doc = Jsoup.parse(response)
        doc.select("script").forEach { script ->
            if (script.data().startsWith("var ytInitialData = ")) {
                val json = JSONObject(script.data().removePrefix("var ytInitialData = "))
                if (json.has("contents")){
                    val videos = json.getJSONObject("contents")
                        .getJSONObject("twoColumnBrowseResultsRenderer")
                        .getJSONArray("tabs")
                        .getJSONObject(0)
                        .getJSONObject("tabRenderer")
                        .getJSONObject("content")
                        .getJSONObject("sectionListRenderer")
                        .getJSONArray("content")
                        .getJSONObject(0)
                        .getJSONObject("itemSectionRenderer")
                        .getJSONArray("contents")
                        .getJSONObject(0)
                        .getJSONObject("playlistVideoListRenderer")
                        .getJSONArray("contents")

                    for (i in 0..videos.length()){
                        val video = videos.getJSONObject(i)
                        val duration = video.getString("lengthSeconds").toLong()
                        val title = video.getJSONObject("title")
                            .getJSONArray("runs")
                            .getJSONObject(0)
                            .getString("text")
                        val url = "https://www.youtube.com/watch?v="+video.getString("videoId")
                        val track = Track(url, Youtube, title, duration)
                        tracks.add(track)
                    }

                }
            }

        }
        return tracks
    }

    override suspend fun getTrack(url: String):Track?{
        val response = RequestHandler.get(url)
        val doc = Jsoup.parse(response)
        doc.select("script").forEach { script ->
            if (script.data().startsWith("var ytInitialPlayerResponse = ")) {
                val json = JSONObject(script.data().removePrefix("var ytInitialPlayerResponse = "))
                if (json.has("videoDetails")){
                    val videoDetails = json.getJSONObject("videoDetails")
                    val title = videoDetails.getString("title")
                    val duration = videoDetails.getString("lengthSeconds").toLong()
                    return Track(url, Youtube, title, duration)
                }
            }
        }
        return null
    }

    override suspend fun getStream(url:String):String?{
        val installed = checkInstalled()
        println("$installed installed")
        if (installed) {
            val out = "-J $url".run()
            println(out)
            val streams = JSONObject(out)
                .getJSONArray("formats")
            for (i in 0..streams.length()){
                val codec = streams.getJSONObject(i).getString("acodec")
                if (codec == "opus"){
                    val stream = streams.getJSONObject(i).getString("url")
                    return stream
                }
            }
            return null
        }
        return null
    }

    private suspend fun checkInstalled():Boolean{
        val out = "--version".run()
        return !out.isNullOrEmpty()
    }

    private fun String.run():String? {
        try{
            val command = "yt-dlp $this"
            val parts = command.split(" ")
            var out: String? = null
            val proc = ProcessBuilder(parts)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()
            val stdout = proc.inputStream.bufferedReader()
            val stderr = proc.errorStream.bufferedReader()
            runBlocking {
                out = withTimeoutOrNull(5000L) {
                    var tempOutput = ""
                    while (!stdout.ready()&&!stderr.ready()&&isActive){
//                        println("waiting")
                        delay(100L)
                    }
                    if (isActive&&stdout.ready()){
                        for (line in stdout.readLines()){
//                            println(line)
                            tempOutput += line
                        }
                    } else if (isActive&&stderr.ready()) {
                        for (line in stderr.readLines()){
//                            println(line)
                            tempOutput += line
                        }
                    }
                    tempOutput
                }
//                println("Result is $out")
            }
            return try {
                val exit = proc.exitValue()
                if (exit == 0){
                    out
                } else {
                    println("yt-dlp error $out")
                    null
                }
            } catch (e:IllegalThreadStateException) {
                proc.destroy()
                println("yt-dlp took to long to respond")
                null
            }

        } catch(e: IOException) {
            println("yt-dlp is not installed")
            return null
        } catch (e: InterruptedException){
            e.printStackTrace()
            return null
        }
    }

}