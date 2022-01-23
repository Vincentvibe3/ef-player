package com.github.Vincentvibe3.efplayer.core

import com.github.Vincentvibe3.efplayer.extractors.Extractor
import io.ktor.utils.io.streams.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.LinkedTransferQueue

/**
 * A track for the [Player]
 *
 * @param url the original URL to access the content from
 * @param extractor An [Extractor] used to get the streaming URL from
 * @param title Is `null` if there is no title
 * @param author Is `null` if there is no author
 * @param duration In milliseconds. Is `-1` if no duration was found
 *
 * @see Player
 *
 */
class Track(val url:String, val extractor: Extractor, val title:String?, val author:String?, val duration:Long) {

    /**
     * The loaded chunks of audio as [ByteArray]
     *
     */
    val trackChunks = LinkedBlockingQueue<ByteArray>(Config.maxOpusChunks)

    /**
     * Calls the extractor to get the track's streaming URL
     *
     * Calls `getStream()` on the track's [Extractor]
     *
     * @see Extractor.getStream
     *
     */
    suspend fun getStream():String?{
        return extractor.getStream(url)
    }

}