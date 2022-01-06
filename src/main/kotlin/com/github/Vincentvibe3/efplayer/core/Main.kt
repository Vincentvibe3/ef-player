package com.github.Vincentvibe3.efplayer.core

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(){


    val el = EL()
    val player = Player(el)
    runBlocking {
        launch {
            player.load("https://www.youtube.com/watch?v=cd5QuZq5jmg")
        }
    }
    while (true){
        player.stop()
        if(player.canProvide()){
            player.provide()
            println("playing")
        }
    }
}
