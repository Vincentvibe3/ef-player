package com.github.Vincentvibe3.efplayer.core

import com.github.Vincentvibe3.efplayer.extractors.Youtube
import com.github.Vincentvibe3.efplayer.formats.webm.streaming.WebmReader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.nio.charset.Charset

fun main(){
    Thread(Stream2("1")).start()
    Thread(Stream2("2")).start()
}