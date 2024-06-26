package com.github.Vincentvibe3.efplayer.streaming

import com.github.Vincentvibe3.efplayer.core.EventListener
import com.github.Vincentvibe3.efplayer.core.Player
import com.github.Vincentvibe3.efplayer.core.Track
import com.github.Vincentvibe3.efplayer.extractors.Youtube
import com.github.Vincentvibe3.efplayer.formats.Format
import com.github.Vincentvibe3.efplayer.formats.Format.Companion.read
import com.github.Vincentvibe3.efplayer.formats.FormatParseException
import com.github.Vincentvibe3.efplayer.formats.Formats
import com.github.Vincentvibe3.efplayer.formats.Result
import com.github.Vincentvibe3.efplayer.formats.webm.streaming.WebmReader
import kotlinx.coroutines.*
import okhttp3.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

class Stream(private val eventListener: EventListener, private val player: Player) {

    private val FORMATS_ID_MAX_BYTES= 4

    companion object {
        val coroutineScope = CoroutineScope(Dispatchers.Default+SupervisorJob())
    }

    private val data = LinkedBlockingDeque<Byte>()

    private val isAlive = AtomicBoolean(true)

//    private val client = HttpClient(CIO)
    private val client = OkHttpClient()

    lateinit var track:Track

    /**
     * Stop the streaming process
     *
     * This will kill the http request and flush all read data including in the attached [Track]
     *
     */
    fun stop(){
        isAlive.set(false)
    }

    private fun LinkedBlockingDeque<Byte>.write(b:ByteArray, toWrite: Int) {
        val initSize = this.size
        b.forEach {
            if (this.size!=initSize+toWrite) {
                this.add(it)
            }
        }
    }

    fun isRunning():Boolean{
        return isAlive.get()
    }

    private suspend fun getContentLength(url:String): Long {
        var failureReason = ""
        for (retries in 0..3){
            val request: Request = Request.Builder()
                .url(url)
                .head()
                .build()
            val call = client.newCall(request)
            val response = call.execute()
            if (!response.isSuccessful){
                response.close()
                call.cancel()
                failureReason = "failed request ${response.code} $url"
            } else {
                val contentLength = response.headers["Content-Length"]
                response.close()
                if (contentLength==null){
                    failureReason = "no header found"
                    continue
                }
                return contentLength.toLong()
            }
            delay(1000)
        }
        throw StreamFailureException(track, "Failed to get content length with reason: $failureReason")
    }

    private suspend fun getBytes(url:String, rangeStart:Long, contentLength:Long): Int {
        val rangeEnd = rangeStart+min(contentLength-rangeStart, 4088)
        val request= if (track.extractor is Youtube){
            Request.Builder()
                .url("$url&range=$rangeStart-$rangeEnd")
                .build()
        } else {
            Request.Builder()
                .url(url)
                .addHeader("Range", "bytes=$rangeStart-$rangeEnd")
                .build()
        }
        val call = client.newCall(request)
        val response = call.execute()
        if (!response.isSuccessful){
            response.close()
            call.cancel()
            throw FailedResponseException("${track.url} at $url")
        }
        val bytes = response.body?.byteStream()
        if (bytes!=null){
            val buffer = ByteArray(4088)
            val readBytes = withContext(Dispatchers.IO) {
                bytes.read(buffer)
            }
            data.write(buffer, readBytes)
            response.close()
            return readBytes
        }
        throw StreamFailureException(track, "Missing bytes")
    }
    private suspend fun startStreaming(initUrl:String){
        var url = initUrl
        var contentLength=-1L
        for (retries in 0..3){
            try {
                contentLength=getContentLength(url)
                break
            } catch (e:StreamFailureException){
                if (retries==3) {
                    throw e
                }
                Player.logger.error("Failed to fetch content length from ${track.url} @ $url at attempt $retries")
                url = track.getStream() ?: ""
            }
            delay(1000)
        }
        var rangeStart = 0L
        var firstRead=true
        lateinit var format:Format
        while (rangeStart < contentLength) {
            if (!isRunning()){
                data.clear()
                break
            }
            var lastException:Exception?=null
            var fetchOk = false
            for (retries in 0..3){
                try {
                    rangeStart += getBytes(url, rangeStart, contentLength)
                    fetchOk = true
                    break
                } catch (e:FailedResponseException){
                    Player.logger.error("Failed to fetch ${track.url} at attempt $retries")
                    lastException=e
                } catch (e:StreamFailureException){
                    Player.logger.error("No bytes received from ${track.url} at attempt $retries")
                    lastException=e
                }
                delay(1000)
            }
            if (!fetchOk){
                throw lastException ?: StreamFailureException(track, "Bytes were null")
            }
            if (firstRead){
                try{
                    format = getFormatReader(data)
                    firstRead=false
                } catch (e: UnsupportedFormatException){
                    Player.logger.error("${track.url} contains an unsupported format")
                    throw e
                }
            }
            try {
                format.processNextBlock(data)
            } catch (e: FormatParseException) {
                Player.logger.error("Failed to parse ${track.url}")
                throw e
            }
        }
    }

    private fun getFormatReader(bytes: LinkedBlockingDeque<Byte>): Format {
        val formatResult = getFormat(bytes)
        if (formatResult != null) {
            val format = when (formatResult.value) {
                Formats.WEBM -> {
                    WebmReader(track, this)
                }
            }
            return format
        }
        throw UnsupportedFormatException()
    }

    private fun getFormat(response: LinkedBlockingDeque<Byte>): Result<Formats>?{

        val buffer = ByteBuffer.wrap(ByteArray(FORMATS_ID_MAX_BYTES))
        //read from the smallest amount to the biggest
        val webmBytes = buffer.put(response.read(4))
        if (WebmReader.checkIsEBML(buffer.array())){
            return Result(4, Formats.WEBM)
        }
        return null
    }

    fun startSong() {
        isAlive.set(true)
        coroutineScope.launch {
            val url = track.getStream()
            if (url != null) {
                eventListener.onTrackStart(track, player)
                try{
                    startStreaming(url)
                } catch (e:Exception){
                    eventListener.onTrackError(track)
                    e.printStackTrace()
                } finally {
                    track.trackFullyStreamed = true
                    stop()
                }
            }
        }
    }

    /**
     *
     * Starts a stream.
     *
     * Called when the Stream is wrapped in a Thread
     *
     * *Warning* Do not call in the same `Thread` as the [Player] as it will cause the `Thread` to block
     *
     */
//    override fun run() {
//        isAlive.set(true)
//        runBlocking {
//            launch {
//                val url = track.getStream()
//                if (url != null) {
//                    eventListener.onTrackStart(track, player)
//                    try{
//                        startStreaming(url)
//                    } catch (e:Exception){
//                        eventListener.onTrackError(track)
//                        e.printStackTrace()
//                    } finally {
//                        track.trackFullyStreamed = true
//                    }
//                }
//            }
//
//        }
//    }

}