package com.github.Vincentvibe3.efplayer.core

import com.github.Vincentvibe3.efplayer.extractors.Youtube

class Player(val eventListener: EventListener) {

    var currentTrack:Track? = null
    var stream:Stream? = null
    var paused = false

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
            eventListener.onTrackLoad(track, this)
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
        eventListener.onTrackStart(track, this)
    }

    fun stop(){
        //clear the track buffer to prevent blocking
        currentTrack?.trackChunks?.clear()
        stream?.stop()
        currentTrack?.let { eventListener.onTrackDone(it, this) }
        currentTrack = null
    }

    fun pause(){
        paused = true
        currentTrack?.let { eventListener.onTrackPaused(it, this) }
    }

    fun resume(){
        paused = false
        currentTrack?.let { eventListener.onTrackResumed(it, this) }
    }

    fun provide(): ByteArray {
        return currentTrack!!.trackChunks.remove()
    }

    fun canProvide():Boolean{
        return if (!paused){
            currentTrack?.trackChunks?.isNotEmpty() ?: false
        } else {
            false
        }
    }

}