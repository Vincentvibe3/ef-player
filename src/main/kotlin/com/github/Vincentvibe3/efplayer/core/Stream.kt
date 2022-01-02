package com.github.Vincentvibe3.efplayer.core

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*

class Stream {

    val data = ArrayList<ByteArray>()

    suspend fun startStreaming(url:String){
        val client = HttpClient(CIO)
        client.get<HttpStatement>(url).execute { httpResponse ->
            val channel: ByteReadChannel = httpResponse.receive()
            while (!channel.isClosedForRead) {
                val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                while (!packet.isEmpty) {
                    val bytes = packet.readBytes(4)
                    data.add(bytes)
                    println("adding data")
                }
            }
        }
        println("done")
    }

}