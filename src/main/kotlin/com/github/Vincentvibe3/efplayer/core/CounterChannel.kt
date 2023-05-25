package com.github.Vincentvibe3.efplayer.core

import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicLong
import kotlin.properties.Delegates

class CounterChannel<T>(
    private val capacity:Int
){
    private var atomicSize = AtomicLong()
    private var channel = Channel<T>(capacity)

    fun clear(){
        val oldChannel = channel
        channel = Channel(capacity)
        oldChannel.close()
    }
    val size
        get() = atomicSize.get()

    fun isEmpty():Boolean{
        return atomicSize.get()==0L
    }

    fun isNotEmpty():Boolean{
        return !isEmpty()
    }

    suspend fun send(element:T) {
        channel.send(element)
        atomicSize.incrementAndGet()
    }

    suspend fun receive():T {
        val value = channel.receive()
        atomicSize.decrementAndGet()
        return value
    }
}