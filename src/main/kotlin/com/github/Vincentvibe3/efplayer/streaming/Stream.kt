package com.github.Vincentvibe3.efplayer.streaming

import com.github.Vincentvibe3.efplayer.core.EventListener
import com.github.Vincentvibe3.efplayer.core.Player
import com.github.Vincentvibe3.efplayer.core.Track
import com.github.Vincentvibe3.efplayer.formats.Formats
import com.github.Vincentvibe3.efplayer.formats.Result
import com.github.Vincentvibe3.efplayer.formats.webm.streaming.WebmReader
import kotlinx.coroutines.*
import okhttp3.*
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean

class Stream(private val eventListener: EventListener, private val player: Player):Runnable {

    private val FORMATS_ID_MAX_BYTES= 4

    private val data = LinkedBlockingDeque<Byte>()

    private val isAlive = AtomicBoolean(true)

//    private val client = HttpClient(CIO)
    private val client2 = OkHttpClient();

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

    fun isRunning():Boolean{
        return isAlive.get()
    }

    private suspend fun startStreaming(url:String){
        val request: Request = Request.Builder()
            .url(url)
            .build()
        val call = client2.newCall(request)
        val response = call.execute()
        if (!response.isSuccessful){
            response.close()
            call.cancel()
            throw RuntimeException("Failed connection to stream")
        }
        val clen = response.body?.contentLength()
        val bytes = response.body?.byteStream()
        var offset = 0L
        fun LinkedBlockingDeque<Byte>.write(b:ByteArray, toWrite: Int) {
            val initSize = this.size
            b.forEach {
                if (this.size!=initSize+toWrite) {
                    this.add(it)
                }
            }
        }
        if (bytes != null){
            val formatResult = getFormat(bytes)
            if (formatResult != null){
                offset+=formatResult.bytesRead
                val format = when (formatResult.value){
                    Formats.WEBM -> {
                        WebmReader(track, this)
                    }
                }
                while (offset < clen!!) {
                    //set to stop stream and exit
                    if (!isAlive.get()){
                        call.cancel()
                    } else {
                        val buffer = ByteArray(4088)
                        val readBytes = bytes.read(buffer)
                        offset += readBytes
                        data.write(buffer, readBytes)
                        try {
                            format.processNextBlock(data)
                        } catch (e:RuntimeException){
                            response.close()
                            call.cancel()
                            throw e
                        }
                    }
                    if (call.isCanceled()){
                        data.clear()
                        track.trackChunks.clear()
                        break
                    }
                }
            }
        }
        while (track.trackChunks.isNotEmpty()){
            delay(100L)
        }
        response.close()
    }

    private fun getFormat(response: InputStream): Result<Formats>?{

        val buffer = ByteBuffer.wrap(ByteArray(FORMATS_ID_MAX_BYTES))
        //read from smallest amount to biggest
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