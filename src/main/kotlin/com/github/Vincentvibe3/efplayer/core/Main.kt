package com.github.Vincentvibe3.efplayer.core

import kotlinx.coroutines.runBlocking

fun main(){

    val player = Player()
    runBlocking {
        player.play("https://www.youtube.com/watch?v=I0kytvnHG-Q")
    }
}
