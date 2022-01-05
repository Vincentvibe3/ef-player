package com.github.Vincentvibe3.efplayer.core

interface EventListener {

    fun onTrackLoad(track: Track)

    fun onTrackDone(track: Track)

    fun onTrackLoadFailed()

}