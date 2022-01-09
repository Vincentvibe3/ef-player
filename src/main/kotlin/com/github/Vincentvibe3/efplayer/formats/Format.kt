package com.github.Vincentvibe3.efplayer.formats

import java.util.concurrent.LinkedBlockingDeque

/**
 * Interface to represent a format/container to decode
 */
interface Format {

    /**
     * Minimum bytes needed to identify any segment
     */
    val MINIMUM_BYTES_NEEDED:Long

    /**
     * Method called when data is available to process
     *
     * @param data A [LinkedBlockingDeque] containing the bytes to be processed
     */
    suspend fun processNextBlock(data: LinkedBlockingDeque<Byte>)

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