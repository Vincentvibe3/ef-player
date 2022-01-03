package com.github.Vincentvibe3.efplayer.core

import com.github.Vincentvibe3.efplayer.extractors.Extractor
import io.ktor.utils.io.streams.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

class Track(val url:String, val extractor: Extractor, val title:String, val duration:Long) {

    val streamingDataOut = PipedInputStream()
    val streamingData = PipedOutputStream(streamingDataOut)


    suspend fun getStream():String?{
        return extractor.getStream(url)
    }

    fun readData(): PipedInputStream {
        return streamingDataOut
    }

}