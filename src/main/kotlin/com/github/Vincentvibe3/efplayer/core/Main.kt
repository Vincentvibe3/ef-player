package com.github.Vincentvibe3.efplayer.core

import com.github.Vincentvibe3.efplayer.extractors.Youtube
import kotlinx.coroutines.runBlocking
import java.net.URLDecoder
import java.nio.charset.Charset

fun main(){


//    runBlocking {
//        val url = "https://www.youtube.com/watch?v=F64yFFnZfkI"
//        val js = Youtube.getPlayer(url)
//        val stream = Youtube.getStream(url)
//        if (js != null) {
//            if (stream != null) {
//                val sig = stream.split("&sp=").first().removePrefix("s=")
//                val url = stream.split("&url=")[1]
//                println(URLDecoder.decode(url, Charset.defaultCharset()))
//                val decoded = URLDecoder.decode(sig, Charset.defaultCharset())
//                Youtube.getSignature(js, decoded)
//            }
//
//        }
//    }


//    val el = EL()
//    val player = Player(el)
//    runBlocking {
//        launch {
//            player.load("https://www.youtube.com/watch?v=cd5QuZq5jmg")
//        }
//    }
//    while (true){
//        player.stop()
//        if(player.canProvide()){
//            player.provide()
//            println("playing")
//        }
//    }
}
