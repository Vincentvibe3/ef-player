package com.github.Vincentvibe3.efplayer.core

import com.github.Vincentvibe3.efplayer.extractors.Youtube
import kotlinx.coroutines.delay

class Player(val eventListener: EventListener) {

    var currentTrack:Track? = null
    var stream:Stream? = null

    suspend fun play(url:String){
        currentTrack = Youtube.getTrack(url)
        val streamUrl = currentTrack?.getStream()
        if (streamUrl!=null&&currentTrack!=null){
            currentTrack?.let {
                stream = Stream(streamUrl, it)
            }
        }
        Thread(stream).start()
    }

    suspend fun load(url:String){
        val track = Youtube.getTrack(url)
        if (track != null) {
            eventListener.onTrackLoad(track)
        } else {
            eventListener.onTrackLoadFailed()
        }
    }

    suspend fun play(track: Track){
        stop()
        currentTrack = track
        val streamUrl = track.getStream()
        if (streamUrl!=null){
            stream = Stream(streamUrl, track)
        }
        Thread(stream).start()
    }

    fun stop(){
        //clear the track buffer to prevent blocking
        currentTrack?.trackChunks?.clear()
        stream?.stop()
    }

    fun provide(): ByteArray {
        return currentTrack!!.trackChunks.remove()
    }

    fun canProvide():Boolean{
        return currentTrack?.trackChunks?.isNotEmpty() ?: false
    }

}