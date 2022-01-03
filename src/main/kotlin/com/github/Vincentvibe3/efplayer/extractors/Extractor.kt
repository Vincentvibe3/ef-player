package com.github.Vincentvibe3.efplayer.extractors

import com.github.Vincentvibe3.efplayer.core.Track

abstract class Extractor {

    abstract suspend fun getStream(url:String):String?

    abstract suspend fun getTrack(url: String): Track?

    open fun getPlaylistTracks():List<Track>{
        return ArrayList()
    }

}