package com.github.Vincentvibe3.efplayer.formats

import com.github.Vincentvibe3.efplayer.streaming.Stream
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.LinkedBlockingQueue

/**
 * Interface to represent a format/container to decode
 */
abstract class Format {

    companion object {
        /**
         * Extension function to read N bytes from a [LinkedBlockingDeque]
         *
         * @param i Amount of bytes to read
         *
         * @return The bytes in a [ByteArray]
         */
        fun LinkedBlockingDeque<Byte>.read(i:Int):ByteArray {
            val array = ByteArray(i)
            for (index in 0 until i){
                array[index] = this.removeFirst()
            }
            return array
        }

        /**
         * Extension function to skip N bytes from a [LinkedBlockingDeque]
         *
         * @param i Amount of bytes to skip
         */
        fun LinkedBlockingDeque<Byte>.skip(i:Int) {
            for (index in 0 until i){
                this.removeFirst()
            }
        }

    }

    abstract val stream: Stream

    /**
     * Minimum bytes needed to identify any segment
     */
    abstract val MINIMUM_BYTES_NEEDED:Long

    /**
     * Method called when data is available to process
     *
     * @param data A [LinkedBlockingDeque] containing the bytes to be processed
     */
    abstract suspend fun processNextBlock(data: LinkedBlockingDeque<Byte>)

//    fun LinkedBlockingQueue<ByteArray>.putToQueue(array:ByteArray){
//        if (stream.isRunning()){
//            this.put(array)
//        }
//    }
}