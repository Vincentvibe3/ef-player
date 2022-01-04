package com.github.Vincentvibe3.efplayer.core

import com.github.Vincentvibe3.efplayer.extractors.Youtube
import kotlinx.coroutines.delay

class Player {

    var currentTrack:Track? = null
    lateinit var thread:Thread

    suspend fun play(url:String){
        currentTrack = Youtube.getTrack(url)
        val stream = currentTrack?.getStream()
        thread = Thread(stream?.let { currentTrack?.let { it1 -> Stream(it, it1) } })
        thread.start()
    }

    fun provide(): ByteArray {
        return currentTrack!!.trackChunks.remove()
    }

    fun canProvide():Boolean{
        return currentTrack?.trackChunks?.isNotEmpty() ?: false
    }

}