package com.github.Vincentvibe3.efplayer.core

import com.github.Vincentvibe3.efplayer.extractors.Extractor
import com.github.Vincentvibe3.efplayer.extractors.Youtube
import com.github.Vincentvibe3.efplayer.streaming.Stream

class Player(private val eventListener: EventListener) {

    var currentTrack:Track? = null
    private var stream: Stream? = null
    var paused = false

    suspend fun load(query:String){
        val type = Youtube.getUrlType(query)
        when (type){
            Extractor.URL_TYPE.TRACK->{
                val track = Youtube.getTrack(query)
                if (track != null) {
                    eventListener.onTrackLoad(track, this)
                } else {
                    eventListener.onLoadFailed()
                }
            }
            Extractor.URL_TYPE.PLAYLIST->{
                val tracks = Youtube.getPlaylistTracks(query)
                if (tracks.isEmpty()) {
                    eventListener.onPlaylistLoaded(tracks, this)
                } else {
                    eventListener.onLoadFailed()
                }
            }
            Extractor.URL_TYPE.INVALID->{
                val track = Youtube.search(query)
                if (track != null) {
                    eventListener.onTrackLoad(track, this)
                } else {
                    eventListener.onLoadFailed()
                }
            }
        }
    }

    suspend fun play(track: Track){
        stop()
        currentTrack = track
        val streamUrl = track.getStream()
        if (streamUrl!=null){
            stream = Stream(streamUrl, track, eventListener, this)
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