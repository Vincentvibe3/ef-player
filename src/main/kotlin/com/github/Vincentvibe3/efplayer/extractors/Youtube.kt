package com.github.Vincentvibe3.efplayer.extractors

import com.github.Vincentvibe3.efplayer.core.RequestHandler
import com.github.Vincentvibe3.efplayer.core.Track
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.Charset

object Youtube: Extractor() {

    private const val INNERTUBE_API_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"

    private fun buildInnertubePostBody(params:HashMap<String, String>):String{
        val topLevel = JSONObject()
        val context = JSONObject()
        val client = JSONObject()
            .put("clientName", "WEB")
            .put("clientVersion", "2.20220106.01.00")
        context.put("client",client)
        params.forEach {
            topLevel.put(it.key, it.value)
        }
        topLevel.put("context", context)
        return topLevel.toString()
    }

    override suspend fun getTrack(url:String): Track {
        var duration:Long = -1
        var title: String? = null
        var author: String? = null

        val id = url.removePrefix("https://www.youtube.com/watch?v=")
        val params = hashMapOf("videoId" to id)
        val body = buildInnertubePostBody(params)
        val response = RequestHandler.post("https://www.youtube.com/youtubei/v1/player?key=$INNERTUBE_API_KEY", body)
        val jsonResponse = JSONObject(response)
        if (jsonResponse.has("streamingData")){
            val streamingData = jsonResponse.getJSONObject("streamingData")
            if (streamingData.has("adaptiveFormats")){
                val formats = streamingData.getJSONArray("adaptiveFormats")
                duration = getDuration(formats)
            }
        }
        if (jsonResponse.has("videoDetails")){
            val details = jsonResponse.getJSONObject("videoDetails")
            title = details.getString("title")
            author = details.getString("author")
        }
        return Track(url, this, title, author, duration)
    }

    private suspend fun getBestFormatStream(formats:JSONArray, originalUrl:String): String? {
        var highestBitrate = -1L
        var streamUrl:String? = null
        for  (index in 0 until formats.length()){
            val format = formats.getJSONObject(index)
            val mimeType = format.getString("mimeType")
            if (mimeType.equals("audio/webm; codecs=\"opus\"")){
                val bitrate = format.getLong("bitrate")
                if (highestBitrate<bitrate){
                    if (format.has("url")){
                        streamUrl = format.getString("url")
                    } else {
                        streamUrl = format.getString("signatureCipher")
                    }
                    highestBitrate = bitrate
                }
            }
        }
        if (streamUrl != null) {
            if (streamUrl.startsWith("s=")){
                val js = getPlayer(originalUrl)
                if (js != null) {
                    val urlDecoded = URLDecoder.decode(streamUrl, Charset.defaultCharset())
                    val sig = urlDecoded.split("&sp=").first().removePrefix("s=")
                    val url = urlDecoded.split("&url=")[1]
                    val decodedSig = getSignature(js, sig)
                    streamUrl = "$url&sig=$decodedSig"

                }
            }
        }
        println("$streamUrl streamurl")
        return streamUrl
    }

    private fun getDuration(formats:JSONArray):Long{
        var highestBitrate = -1L
        var duration = -1L
        for  (index in 0 until formats.length()){
            val format = formats.getJSONObject(index)
            val mimeType = format.getString("mimeType")
            if (mimeType.equals("audio/webm; codecs=\"opus\"")){
                val bitrate = format.getLong("bitrate")
                if (highestBitrate<bitrate){
                    duration = format.getLong("approxDurationMs")
                }
            }
        }
        return duration
    }

    private fun getSignature(js:String, sig:String): String? {
        val array = sig.split("") as ArrayList
        array.removeFirst()
        array.removeLast()
        val functionStartPattern = "(?<=function\\(a\\)\\{a=a\\.split\\(\"\"\\);)(.*)(?=return a\\.join\\(\"\"\\)};)".toRegex()
        val funcs = HashMap<String,Pair<String, Int>>()
        val matches = functionStartPattern.find(js)
        if (matches != null) {
            println(matches.value)
            val functions = matches.value.split(";")
            val funcClass = functions.first().split(".").first()
            val classPattern = "(?<=var $funcClass=\\{)(.*?)(?=};)".toRegex(RegexOption.DOT_MATCHES_ALL)
            val classMatch = classPattern.find(js)
            if (classMatch != null) {
                val classFuncs = classMatch.value.split(",\n")
                classFuncs.forEach {
                    println(it)
                    val splitNameBody = it.split(":")
                    val splitBody = splitNameBody[1].split("{")
                    val argCount = splitBody.first().split(",").size
                    funcs[splitNameBody.first()] = Pair(splitNameBody[1], argCount)
                }
            }
            functions.forEach {
                if (it.isNotEmpty()) {
                    val funcName = it.split(".")[1].split("(").first()
                    val args = it.split(".")[1].split("(")[1].removeSuffix(")").split(",")
                    val funcdata = funcs[funcName]
                    if (funcdata != null) {
                        if (funcdata.second == 1) {
                            jsFn1Arg(array, funcdata.first)
                        } else {
                            jsFn2Arg(array, args[1].toInt(), funcdata.first)
                        }
                    }
                    println(array.joinToString(""))
                }
            }
            return array.joinToString("")
        }
        return null
    }

    private fun jsFn1Arg(a: ArrayList<String>, js:String){
        val splitjs = js.split("{")
        val steps = splitjs[1].removeSuffix(",").removeSuffix("}").split(";")
        steps.forEach {
            if (it.contains("a.reverse()")){
                a.reverse()
            }
        }
    }

    private fun jsFn2Arg(a: ArrayList<String>, b:Int, js:String){
        var c = ""
        val splitjs = js.split("{")
        val steps = splitjs[1].removeSuffix(",").removeSuffix("}").split(";")
        steps.forEach {
            if (it.startsWith("var c")){
                val value = it.split("=")[1]
                if (value=="a[b%a.length]"){
                    c = a[b%a.size]
                } else if (value == "a[0]"){
                    c = a[0]
                }
            } else if (it.startsWith("a.splice(")) {
                (0 until b).forEach{ num ->
                    a.removeAt(num)
                }
            } else if (it.startsWith("a[0]")){
                val value = it.split("=")[1]
                if (value=="a[b%a.length]"){
                    a[0] = a[b%a.size]
                } else if (value == "c"){
                    a[0] = c
                }
            } else if (it.startsWith("a[b%a.length]")){
                val value = it.split("=")[1]
                if (value=="a[0]"){
                    a[b%a.size] = a[0]
                } else if (value == "c"){
                    a[b%a.size] = c
                }
            }
        }
    }

    private suspend fun getPlayer(url:String):String? {
        val id = url.removePrefix("https://www.youtube.com/watch?v=")
        val matchpattern = "(?<=<script src=\")(?!.*?www-embed-player\\.js)(.*?/base.js)(?=\" nonce=\")".toRegex()
        val response = RequestHandler.get("https://www.youtube.com/embed/$id")
        val player = matchpattern.find(response)?.value
        if (player!=null) {
            return RequestHandler.get("https://www.youtube.com$player")
        }
        return null
    }

    override suspend fun getStream(url:String):String?{
        val id = url.removePrefix("https://www.youtube.com/watch?v=")
        val params = hashMapOf("videoId" to id)
        val body = buildInnertubePostBody(params)
        val response = RequestHandler.post("https://www.youtube.com/youtubei/v1/player?key=$INNERTUBE_API_KEY", body)
        val jsonResponse = JSONObject(response)
        if (jsonResponse.has("streamingData")){
            val streamingData = jsonResponse.getJSONObject("streamingData")
            if (streamingData.has("adaptiveFormats")){
                val formats = streamingData.getJSONArray("adaptiveFormats")
                return getBestFormatStream(formats, url)
            }
        }
        return null
    }

    override suspend fun getUrlType(url: String): URL_TYPE {
        return if (url.contains("://www.youtube.com/watch?v=")){
             URL_TYPE.TRACK
        } else if (url.contains("://www.youtube.com/playlist?list=")){
            URL_TYPE.PLAYLIST
        } else{
            URL_TYPE.INVALID
        }
    }

    override suspend fun getPlaylistTracks(url:String):List<Track>{
        val id = url.removePrefix("https://www.youtube.com/playlist?list=")
        val params = hashMapOf(
            "browseId" to "VL$id",
            "params" to "wgYCCAA="
        )
        val body = buildInnertubePostBody(params)
        val tracks = ArrayList<Track>()
        val response = RequestHandler.post("https://www.youtube.com/youtubei/v1/browse?key=AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8", body)
        val jsonResponse = JSONObject(response)
        val contents = jsonResponse.getJSONObject("contents")
            .getJSONObject("twoColumnBrowseResultsRenderer")
            .getJSONArray("tabs")
            .getJSONObject(0)
            .getJSONObject("tabRenderer")
            .getJSONArray("contents")

        for (index in 0 until contents.length()){
            val item = contents.getJSONObject(index)
                .getJSONObject("playlistVideoRenderer")
            val id = item.getString("videoId")
            val title = item.getJSONObject("title")
                .getJSONArray("runs")
                .getJSONObject(0)
                .getString("text")
            val duration = item.getString("lengthSeconds").toLong()*1000
            val author = item.getJSONObject("shortBylineText")
                .getJSONArray("runs")
                .getJSONObject(0)
                .getString("text")

            tracks.add(Track("://www.youtube.com/watch?v=$id", this, title, author, duration))
        }
        return tracks
    }

}