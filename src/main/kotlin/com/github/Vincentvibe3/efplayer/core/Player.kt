package com.github.Vincentvibe3.efplayer.core

import com.github.Vincentvibe3.efplayer.extractors.Youtube
import kotlinx.coroutines.delay

class Player {

    var currentTrack:Track? = null

    suspend fun play(url:String){
        currentTrack = Youtube.getTrack(url)
        val stream = currentTrack?.getStream()
        val thread = Thread(stream?.let { currentTrack?.let { it1 -> Stream(it, it1) } })
        thread.start()
    }

    fun provide(){

    }

    fun canProvide():Boolean{
        return true
    }

}