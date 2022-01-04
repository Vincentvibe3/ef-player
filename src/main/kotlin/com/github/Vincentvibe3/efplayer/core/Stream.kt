package com.github.Vincentvibe3.efplayer.core

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

class Stream(val url: String, val track:Track):Runnable {

    //bytes needed to identify all possible formats
    val FORMATS_ID_MAX_BYTES= 4

    val data = LinkedBlockingDeque<Byte>(300000)
    var missingBytes = 0L

    suspend fun startStreaming(url:String){
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
                do {
                    val available = data.size
                    val minimumBytesNeeded = format.MINIMUM_BYTES_NEEDED
                    val canStream = httpResponse.content.availableForRead
                    format.processNextBlock(data)
//                    val currentRead = canStream
//                    println(currentRead)
                    val bytes = httpResponse.readBytes(canStream)
                    offset += canStream
                    data.write(bytes)
//                    println("Download in progress, offset: ${offset}, current read ${canStream} / ${contentLength}")
                } while (offset < contentLength!!)
            }

//            println("Download done")
//            while (!channel.isClosedForRead) {
//                val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
//                println(packet.isEmpty)
//                while (!packet.isEmpty) {
//                    val bytes = packet.readBytes(10)
//                    readSize++
//                    println("reading ${readSize*10}/$total")
////                    track.streamingData.writeBytes(bytes)
////                    println("adding data")
////                    println(channel.isClosedForRead)
//                }
//            }
        }
        println("done")
    }

    suspend fun getFormat(response: HttpResponse):Result<Formats>?{

        val buffer = ByteBuffer.wrap(ByteArray(FORMATS_ID_MAX_BYTES))
        //read from smallest amount to biggest
        val webmBytes = buffer.put(response.readBytes(4))
        if (WebmReader.checkIsEBML(buffer.array())){
            return Result(4, Formats.WEBM)
        }
        return null
    }

    override fun run() {
        println("Stream started")
        runBlocking {
            launch {
                startStreaming(url)
            }
        }
        println("stream done")
    }

}