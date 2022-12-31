package com.github.Vincentvibe3.efplayer.streaming

import com.github.Vincentvibe3.efplayer.core.EventListener
import com.github.Vincentvibe3.efplayer.core.Player
import com.github.Vincentvibe3.efplayer.core.Track
import com.github.Vincentvibe3.efplayer.extractors.Youtube
import com.github.Vincentvibe3.efplayer.formats.Format
import com.github.Vincentvibe3.efplayer.formats.FormatParseException
import com.github.Vincentvibe3.efplayer.formats.Formats
import com.github.Vincentvibe3.efplayer.formats.Result
import com.github.Vincentvibe3.efplayer.formats.webm.streaming.WebmReader
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

class Stream(private val eventListener: EventListener, private val player: Player):Runnable {

    private val FORMATS_ID_MAX_BYTES= 4

    private val data = LinkedBlockingDeque<Byte>()

    private val isAlive = AtomicBoolean(true)

//    private val client = HttpClient(CIO)
    private val client = OkHttpClient();

    lateinit var track:Track

    val logger: Logger = LoggerFactory.getLogger("ef-streaming")

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

    private fun getContentLength(url:String): Long {
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
            } else {
                val contentLength = response.headers["Content-Length"] ?: break
                return contentLength.toLong()
            }
        }
        throw StreamFailureException(track, "Failed to get content length")
    }

    private fun getBytes(url:String, rangeStart:Long, contentLength:Long): InputStream {
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
            return bytes
        }
        throw StreamFailureException(track, "Missing bytes")
    }
    private suspend fun startStreaming(url:String){
        val contentLength = getContentLength(url)
        var rangeStart = 0L
        var firstRead=true
        lateinit var format:Format
        while (rangeStart < contentLength) {
            var bytes:InputStream? = null
            var lastException:Exception?=null
            for (retries in 0..3){
                try {
                   bytes=getBytes(url, rangeStart, contentLength)
                   break
                } catch (e:FailedResponseException){
                    logger.error("Failed to fetch ${track.url} at attempt $retries")
                    lastException=e
                } catch (e:StreamFailureException){
                    logger.error("No bytes received from ${track.url} at attempt $retries")
                    lastException=e
                }
            }
            if (bytes==null){
                throw lastException ?: StreamFailureException(track, "Bytes were null")
            }
            val buffer = ByteArray(4088)
            if (firstRead){
                try{
                    val formatInfo = getFormatReader(bytes)
                    rangeStart+=formatInfo.second
                    format = formatInfo.first
                    firstRead=false
                } catch (e: UnsupportedFormatException){
                    logger.error("${track.url} contains an unsupported format")
                    throw e
                }
            }
            val readBytes = withContext(Dispatchers.IO) {
                bytes.read(buffer)
            }
            rangeStart += readBytes
            data.write(buffer, readBytes)
            try {
                format.processNextBlock(data)
            } catch (e: FormatParseException) {
                logger.error("Failed to parse ${track.url}")
                throw e
            }
        }
        track.trackFullyStreamed = true
        while (track.trackChunks.isNotEmpty()){
            delay(100L)
        }
    }

    private fun getFormatReader(bytes:InputStream): Pair<Format, Long> {
        val formatResult = getFormat(bytes)
        if (formatResult != null) {
            val format = when (formatResult.value) {
                Formats.WEBM -> {
                    WebmReader(track, this)
                }
            }
            return Pair(format, formatResult.bytesRead)
        }
        throw UnsupportedFormatException()
    }

    private fun getFormat(response: InputStream): Result<Formats>?{

        val buffer = ByteBuffer.wrap(ByteArray(FORMATS_ID_MAX_BYTES))
        //read from the smallest amount to the biggest
        val webmBytes = buffer.put(response.readNBytes(4))
        if (WebmReader.checkIsEBML(buffer.array())){
            return Result(4, Formats.WEBM)
        }
        return null
    }

    fun startSong() {
        ThreadManager.executor.execute(this)
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
    override fun run() {

        isAlive.set(true)
        runBlocking {
            launch {
                val url = track.getStream()
                if (url != null) {
                    eventListener.onTrackStart(track, player)
                    try{
                        startStreaming(url)
                    } catch (e:Exception){
                        eventListener.onTrackError(track)
                        e.printStackTrace()
                    }
                }
            }

        }
        if (isAlive.get()){
            eventListener.onTrackDone(track, player, true)
        } else {
            eventListener.onTrackDone(track, player, false)
        }
    }

}