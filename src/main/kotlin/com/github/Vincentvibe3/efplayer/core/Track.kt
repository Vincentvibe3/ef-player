package com.github.Vincentvibe3.efplayer.core

import com.github.Vincentvibe3.efplayer.extractors.Extractor
import java.util.concurrent.LinkedBlockingQueue

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
class Track(
        var url:String,
        var extractor: Extractor,
        var title:String?,
        var author:String?,
        var duration:Long,
        val loadId:String
    ) {

    internal var started = false

    /**
     * The loaded chunks of audio as [ByteArray]
     *
     */
    internal val trackChunks = CounterChannel<ByteArray>(Player.config.maxOpusChunks)
    internal var trackFullyStreamed = false


    /**
     * returns a pair containing a chunk and whether it is the last
     */
    suspend fun getChunk(): Pair<ByteArray, Boolean> {
        return if (!started){
            started=true
            Pair(trackChunks.receive(), false)
        } else {
            if (trackChunks.size == 1L && trackFullyStreamed){
                started = false
                trackFullyStreamed = false
                Pair(trackChunks.receive(), true)
            } else {
                Pair(trackChunks.receive(), false)
            }
        }
    }

    /**
     * Calls the extractor to get the track's streaming URL
     *
     * Calls `getStream()` on the track's [Extractor]
     *
     * @see Extractor.getStream
     *
     */
    suspend fun getStream():String?{
        return extractor.getStream(url, this)
    }

}