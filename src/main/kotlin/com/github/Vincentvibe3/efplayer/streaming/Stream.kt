package com.github.Vincentvibe3.efplayer.streaming

import com.github.Vincentvibe3.efplayer.core.EventListener
import com.github.Vincentvibe3.efplayer.core.Player
import com.github.Vincentvibe3.efplayer.formats.Result
import com.github.Vincentvibe3.efplayer.core.Track
import com.github.Vincentvibe3.efplayer.formats.Formats
import com.github.Vincentvibe3.efplayer.formats.webm.streaming.WebmReader
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import java.lang.Runnable
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.AtomicBoolean

class Stream(val url: String, val track: Track, val eventListener: EventListener, val player: Player):Runnable {

    //bytes needed to identify all possible formats
    val FORMATS_ID_MAX_BYTES= 4

    val data = LinkedBlockingDeque<Byte>()

    val isAlive = AtomicBoolean(true)

    fun stop(){
        isAlive.set(false)
    }

    private suspend fun startStreaming(url:String){
        val client = HttpClient(CIO)
        client.get<HttpStatement>(url){
            headers{}
        }.execute { httpResponse ->
            val contentLength = httpResponse.contentLength()?.toInt()
            var offset = 0L

            /**

             Steps:
             1. Identify file type (for now only webm is supported)
             2. Preprocess header and other information
             3. Start writing clusters to output stream
             4. Process other blocks if encountered otherwise return to step 2

            */

            if (httpResponse.status!=HttpStatusCode.OK){
                eventListener.onTrackError(track)
            }

            fun LinkedBlockingDeque<Byte>.write(b:ByteArray) {
                b.forEach {
                    this.add(it)
                }
            }

            val formatResult = getFormat(httpResponse)
            if (formatResult != null){
                offset+=formatResult.bytesRead
                val format = when (formatResult.value){
                    Formats.WEBM -> {
                        WebmReader(track)
                    }
                }
                while (offset < contentLength!!) {
                    //set to stop stream and exit
                    if (!isAlive.get()){
                        httpResponse.cancel()
                    } else {
                        val canStream = httpResponse.content.availableForRead
                        val bytes = httpResponse.readBytes(canStream)
                        offset += canStream
                        data.write(bytes)
                        format.processNextBlock(data)
                    }
                    if (!httpResponse.isActive){
                        data.clear()
                        track.trackChunks.clear()
                        break
                    }
                }
            }
        }
    }

    private suspend fun getFormat(response: HttpResponse): Result<Formats>?{

        val buffer = ByteBuffer.wrap(ByteArray(FORMATS_ID_MAX_BYTES))
        //read from smallest amount to biggest
        val webmBytes = buffer.put(response.readBytes(4))
        if (WebmReader.checkIsEBML(buffer.array())){
            return Result(4, Formats.WEBM)
        }
        return null
    }

    override fun run() {
        runBlocking {
            launch {
                startStreaming(url)
            }
        }
        eventListener.onTrackDone(track, player)
    }

}