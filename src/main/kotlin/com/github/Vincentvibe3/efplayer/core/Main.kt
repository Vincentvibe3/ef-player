package com.github.Vincentvibe3.efplayer.core

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(){

    val player = Player()
    runBlocking {
        launch {
            player.play("https://www.youtube.com/watch?v=cd5QuZq5jmg")
        }
    }
    while (true){
        if(player.canProvide()){
            player.provide()
            println("playing")
        }
    }
}
