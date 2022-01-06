package com.github.Vincentvibe3.efplayer.core

import com.github.Vincentvibe3.efplayer.extractors.Extractor
import io.ktor.utils.io.streams.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.LinkedTransferQueue

class Track(val url:String, val extractor: Extractor, val title:String, val duration:Long) {

    val trackChunks = LinkedBlockingQueue<ByteArray>(4000)

    suspend fun getStream():String?{
        return extractor.getStream(url)
    }

}