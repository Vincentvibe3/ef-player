package com.github.Vincentvibe3.efplayer.extractors

import com.github.Vincentvibe3.efplayer.core.Track

abstract class Extractor {

    enum class URL_TYPE {
        TRACK, PLAYLIST, INVALID
    }

    abstract suspend fun getStream(url:String):String?

    abstract suspend fun getTrack(url: String): Track?

    open suspend fun getUrlType(url: String):URL_TYPE{
        return URL_TYPE.TRACK
    }

    open suspend fun getPlaylistTracks(url: String):List<Track>{
        return ArrayList()
    }

}