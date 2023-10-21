package com.github.Vincentvibe3.efplayer.extractors

import com.github.Vincentvibe3.efplayer.streaming.RequestFailedException
import com.github.Vincentvibe3.efplayer.streaming.RequestHandler
import com.github.Vincentvibe3.efplayer.core.Track
import com.github.Vincentvibe3.efplayer.extractors.serialization.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URLDecoder
import java.nio.charset.Charset

/**
 *
 * [Extractor] for YouTube
 *
 */
object Youtube: Extractor() {

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys=true
        explicitNulls = false
    }

    private const val INNERTUBE_API_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"

    private suspend fun buildInnertubePostBody(params:HashMap<String, String>):String{
        val playbackContext = if (params["signatureTimestamp"]!=null){
            InnertubeRequestBody.PlaybackContext(
                InnertubeRequestBody.ContentPlaybackContext(
                    params["signatureTimestamp"]
                )
            )
        } else {
            null
        }
        val requestBody = InnertubeRequestBody(
            InnertubeRequestBody.Context(
                InnertubeRequestBody.Client("WEB", getClientVersion())
            ),
            playbackContext,
            params["query"],
            params["params"],
            params["continuation"],
            params["browseId"],
            params["videoId"]
        )
        return json.encodeToString(InnertubeRequestBody.serializer(), requestBody)
    }

    private suspend fun getClientVersion(): String {
        val default = "2.20220325.00.00"
        val file = File("cver.txt")
        if (file.canRead()){
            val lines = file.readLines()
            if (lines.size == 3){
                val version = lines[1]
                val timestamp = lines[2].toIntOrNull()
                if (timestamp!=null && timestamp >= System.currentTimeMillis()/1000){
                    return version
                }
            }
        }
        try {
            val version = fetchVersion() ?: return default
            cacheClientVersion(version)
            return version
        } catch (e: RequestFailedException){
            return default
        }
    }

    private suspend fun fetchVersion(): String? {
        val pattern = "(?<=\\{\"key\":\"cver\",\"value\":\")(.*?)(?=\"})".toRegex()
        val response = RequestHandler.get("https://www.youtube.com/")
        return pattern.find(response)?.value
    }

    private fun cacheClientVersion(version:String?){
        if (version!=null){
            val out = File("cver.txt")
            out.createNewFile()
            val content = """
                This file was generated by ef-player. Do not manually modify it.
                $version
                ${(System.currentTimeMillis()/1000)+604800}
            """.trimIndent()
            out.writeText(content)
        }
    }

    private fun getId(url: String): String? {
        val idRegex =
            "https?://(?>(?>www\\.)?(?>youtu\\.be/)|(?>(?>www\\.)?youtube\\.com/(?>(?>watch\\?v=)|(?>playlist\\?list=))?))(.*?)(?=\\?|\$)".toRegex()
        return idRegex.find(url)?.groupValues?.first { !it.contains("youtube.com") && !it.contains("youtu.be") }
    }

    override suspend fun getTrack(url: String, loadId: String): Track? {
        val id = getId(url) ?: return null
        val params = hashMapOf("videoId" to id)
        val body = buildInnertubePostBody(params)
        val response = RequestHandler.post("https://www.youtube.com/youtubei/v1/player?key=$INNERTUBE_API_KEY", body)
        val jsonResponseResult = kotlin.runCatching {
            json.decodeFromString<YoutubeVideoResponse>(response)
        }.onFailure {
            it.printStackTrace()
        }
        val jsonResponse = jsonResponseResult.getOrNull()?: return null
        val streamingData = jsonResponse.streamingData
        val formats = streamingData?.adaptiveFormats
        val duration = formats?.let { getDuration(it) }?: -1
        val details = jsonResponse.videoDetails
        val title = details?.title
        val author = details?.author

        return Track(url, this, title, author, duration, loadId)
    }

    private fun getBestFormatStream(formats: List<YoutubeVideoResponse.AdaptiveFormat>, js: String): String? {
        var highestBitrate = -1L
        var streamUrl:String? = null
        for  (format in formats){
            val mimeType = format.mimeType
            if (mimeType == "audio/webm; codecs=\"opus\""){
                val bitrate = format.bitrate
                if (highestBitrate<bitrate){
                    streamUrl = format.url?: format.signatureCipher
                    highestBitrate = bitrate
                }
            }
        }
        if (streamUrl != null) {
            if (streamUrl.startsWith("s=")){
                val urlDecoded = URLDecoder.decode(URLDecoder.decode(streamUrl, Charset.defaultCharset()), Charset.defaultCharset())
                val sig = urlDecoded.split("&sp=").first().removePrefix("s=")
                val url = urlDecoded.split("&url=")[1]
                val decodedSig = getSignature(js, sig)
                streamUrl = "$url&sig=$decodedSig"
            }
        }
        return streamUrl
    }

    private fun getDuration(formats:List<YoutubeVideoResponse.AdaptiveFormat>):Long{
        var highestBitrate = -1L
        var duration = -1L
        for  (format in formats){
            val mimeType = format.mimeType
            if (mimeType == "audio/webm; codecs=\"opus\""){
                val bitrate = format.bitrate
                if (highestBitrate<bitrate){
                    duration = format.approxDurationMs
                    highestBitrate = bitrate
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
            val functions = matches.value.split(";")
            val funcClass = functions.first().split(".").first()
            val classPattern = "(?<=var ${Regex.escape(funcClass)}=\\{)(.*?)(?=};)".toRegex(RegexOption.DOT_MATCHES_ALL)
            val classMatch = classPattern.find(js)
            if (classMatch != null) {
                val classFuncs = classMatch.value.split(",\n")
                classFuncs.forEach {
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
                (0 until b).forEach{ _ ->
                    a.removeAt(0)
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
        val id = getId(url)
        val matchpattern = "(?<=jsUrl\\\":\\\")(/s/.*?base\\.js)".toRegex()
        val response = RequestHandler.get("https://www.youtube.com/embed/$id")
        val matches = matchpattern.find(response)?.groups
        val player = if (matches.isNullOrEmpty()){
            null
        } else {
            matches.mapNotNull {
                it?.value
            }.firstOrNull {
                it.endsWith("base.js")
            }
        }
        if (player!=null) {
            return RequestHandler.get("https://www.youtube.com$player")
        }
        return null
    }

    private fun getSignatureTimestamp(js: String):String? {
        val valuePattern = "(?<=signatureTimestamp:)\\d+".toRegex()
        return valuePattern.find(js)?.value
    }

    override suspend fun getStream(url: String, track: Track): String? {
        val id = getId(url) ?: return null
        val js = getPlayer(url)
        val sigTimestamp = js?.let { getSignatureTimestamp(it) }
        if (js!=null&&sigTimestamp!=null){
            val params = hashMapOf("videoId" to id, "signatureTimestamp" to sigTimestamp)
            val body = buildInnertubePostBody(params)
            val response = RequestHandler.post("https://www.youtube.com/youtubei/v1/player?key=$INNERTUBE_API_KEY", body)
            val jsonResponseResult = kotlin.runCatching {
                json.decodeFromString<YoutubeVideoResponse>(response)
            }.onFailure {
                it.printStackTrace()
            }
            val jsonResponse = jsonResponseResult.getOrNull()?: return null
            val streamingData = jsonResponse.streamingData

            val formats = streamingData?.adaptiveFormats
            return formats?.let { getBestFormatStream(it, js) }
        }
        return null
    }

    override suspend fun getUrlType(url: String): URL_TYPE {
        return if (getId(url)==null){
            URL_TYPE.INVALID
        } else if (url.contains("playlist?list=")) {
            URL_TYPE.PLAYLIST
        } else {
            URL_TYPE.TRACK
        }
    }

    /**
     *
     * Searches for a video on YouTube
     *
     * @param query The search query to use
     * @return A [Track] with the first found result. `null` if no result were found
     *
     */
    override suspend fun search(query: String, loadId: String): Track? {
        val params = hashMapOf(
            "query" to query,
            "params" to "CAASAhAB"
        )
        val body = buildInnertubePostBody(params)
        val response = RequestHandler.post("https://www.youtube.com/youtubei/v1/search?key=AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8", body)
        val jsonResponseResult = kotlin.runCatching {
            json.decodeFromString<YoutubeSearchResponse>(response)
        }.onFailure {
            it.printStackTrace()
        }
        val jsonResponse = jsonResponseResult.getOrNull()?: return null
        val resultCount = jsonResponse.estimatedResults.toLong()
        if (resultCount>0){
            val contents = jsonResponse.contents
                .twoColumnSearchResultsRenderer
                .primaryContents
                .sectionListRenderer
                .contents[0]
                .itemSectionRenderer
                ?.contents
            if (contents != null) {
                for (result in contents){
                    if (result.videoRenderer!=null){
                        val resultInfo = result.videoRenderer
                        val id = resultInfo.videoId
                        return getTrack("https://www.youtube.com/watch?v=$id", loadId)
                    }

                }
            }
        }
        return null
    }

    private suspend fun parsePlaylistContent(tracks:ArrayList<Track>, loadId: String, contents:List<YoutubePlaylistResponse.PlaylistVideoRendererContent>){
        for (item in contents){
            if (item.playlistVideoRenderer!=null) {
                val entry = item.playlistVideoRenderer
                val videoId = entry.videoId
                val title = entry.title
                    .runs[0]
                    .text
                if (title!="[Deleted video]"){
                    val duration = (entry.lengthSeconds?.toLong() ?: 0) * 1000
                    val author = entry.shortBylineText
                        ?.runs?.get(0)
                        ?.text

                    val videoUrl = "https://www.youtube.com/watch?v=$videoId"
                    tracks.add(Track(videoUrl, this, title, author, duration, loadId))
                }
            } else if (item.continuationItemRenderer!=null) {
                val token = item.continuationItemRenderer
                    .continuationEndpoint
                    .continuationCommand
                    .token
                getPlaylistTracksNext(token, tracks, loadId)
            }
        }
    }

    private suspend fun getPlaylistTracksNext(continuation:String, tracks:ArrayList<Track>, loadId: String){
        val params = hashMapOf(
            "continuation" to continuation,
        )
        val body = buildInnertubePostBody(params)
        val response = RequestHandler.post("https://www.youtube.com/youtubei/v1/browse?key=AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8", body)
        val jsonResponseResult = kotlin.runCatching {
            json.decodeFromString<YoutubePlaylistContinuationResponse>(response)
        }.onFailure {
            it.printStackTrace()
        }
        val jsonResponse = jsonResponseResult.getOrNull()?: return
        val contents = jsonResponse.onResponseReceivedActions[0]
            .appendContinuationItemsAction
            .continuationItems
        parsePlaylistContent(tracks, loadId, contents)
    }

    override suspend fun getPlaylistTracks(url: String, loadId: String): List<Track> {
        val id = getId(url)?: return ArrayList()
        val params = hashMapOf(
            "browseId" to "VL$id",
            "params" to "wgYCCAA="
        )
        val body = buildInnertubePostBody(params)
        val tracks = ArrayList<Track>()
        val response = RequestHandler.post("https://www.youtube.com/youtubei/v1/browse?key=AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8", body)
        val jsonResponseResult = kotlin.runCatching {
            json.decodeFromString<YoutubePlaylistResponse>(response)
        }.onFailure {
            it.printStackTrace()
        }
        val jsonResponse = jsonResponseResult.getOrNull()?: return listOf()
        val contents = jsonResponse.contents
            .twoColumnBrowseResultsRenderer
            .tabs[0]
            .tabRenderer
            .content
            .sectionListRenderer
            .contents
        val itemSectionRenderer = contents.first{
            it.itemSectionRenderer != null
        }.itemSectionRenderer
        if (itemSectionRenderer!=null) {
            val playlistTrackData = itemSectionRenderer.contents[0]
                .playlistVideoListRenderer
                .contents
            parsePlaylistContent(tracks, loadId, playlistTrackData)
        }
        return tracks
    }

}