package com.github.Vincentvibe3.efplayer.formats

import com.github.Vincentvibe3.efplayer.core.Result
import java.io.PipedInputStream
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.LinkedBlockingQueue

interface Format {

    val MINIMUM_BYTES_NEEDED:Long

    suspend fun processNextBlock(streamingDataOut: LinkedBlockingDeque<Byte>)

    fun LinkedBlockingDeque<Byte>.read(i:Int):ByteArray {
        val array = ByteArray(i)
        for (index in 0 until i){
            array[index] = this.removeFirst()
        }
        return array
    }

    fun LinkedBlockingDeque<Byte>.skip(i:Int) {
        for (index in 0 until i){
            this.removeFirst()
        }
    }

}