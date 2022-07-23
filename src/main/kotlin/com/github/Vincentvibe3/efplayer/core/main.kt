package com.github.Vincentvibe3.efplayer.core

import com.github.Vincentvibe3.efplayer.extractors.Spotify
import java.util.concurrent.LinkedBlockingQueue

fun main(){
    Config.load()
    println(Config.maxOpusChunks)
    val trackChunks = LinkedBlockingQueue<ByteArray>()
    val el = object:EventListener(){
        override fun onTrackLoad(track: Track, player: Player) {
            player.play(track)
        }
    }
    val player = Player(el)
    player.load("yorushika itte")
    while (true){
        if (player.canProvide()){
            val chunk = player.provide()
            trackChunks.add(chunk)
            println("provided")
            Thread.sleep(20)
        }
    }
}