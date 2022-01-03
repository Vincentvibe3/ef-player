package com.github.Vincentvibe3.efplayer.core

import com.github.Vincentvibe3.efplayer.formats.Formats
import com.github.Vincentvibe3.efplayer.formats.webm.EBMLHeader
import com.github.Vincentvibe3.efplayer.formats.webm.WebmDocument
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.utils.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.*
import java.io.ByteArrayInputStream
import java.lang.Runnable
import java.nio.ByteBuffer

class Stream(val url: String, val track:Track):Runnable {

    suspend fun startStreaming(url:String){
        val client = HttpClient(CIO)
        client.get<HttpStatement>(url){
            headers{}
        }.execute { httpResponse ->
            var readSize = 0
            println("reading")
            val contentLength = httpResponse.contentLength()?.toInt()
            val byteArray = contentLength?.let { ByteArray(it) }
            var offset = 0
            println("$contentLength length")
            val format = getFormat(httpResponse)
            val doc = WebmDocument()
            offset+=4
            do {
                val currentRead = 4//byteArray?.size?.let { httpResponse.content.readAvailable(byteArray, offset, it) }
                val bytes = httpResponse.readBytes(4)
                if (currentRead != null) {
                    offset += currentRead
                }
                if (byteArray != null) {
                    withContext(Dispatchers.IO){
                        println("writing")
                        track.streamingData.write(bytes)
                    }
                    println(track.streamingDataOut.available())
                }
                println("Download in progress, offset: ${offset}, current read ${currentRead} / ${contentLength}")
            } while (offset < contentLength!!)
            println("Download done")
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

    suspend fun getFormat(response: HttpResponse):Formats?{
        val bytes = response.readBytes(4)
        if (WebmDocument.checkIsEBML(ByteArrayInputStream(bytes))){
            return Formats.WEBM
        } else {
            return null
        }
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