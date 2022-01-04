package com.github.Vincentvibe3.efplayer.core

import io.github.vincentvibe3.emergencyfood.utils.exceptions.RequestFailedException
import io.ktor.client.*
import io.ktor.client.features.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToLong
import kotlin.properties.Delegates
import io.ktor.client.request.*
import io.ktor.client.statement.*
import java.net.ConnectException
import java.net.URISyntaxException

object RequestHandler {

    private val queue = ConcurrentHashMap<String, ConcurrentHashMap<Long, Long>>()
    val rateLimits = HashMap<String, Long>()
    private val mutex = Mutex()

    suspend fun get(originalUrl: String):String{
        println(originalUrl)
        val host = if (rateLimits.containsKey(originalUrl)){
            originalUrl
        } else {
            try {
                URI(originalUrl).host
            } catch (e:URISyntaxException){
                e.printStackTrace()
                throw RequestFailedException()
            }
        }
        var queueTime by Delegates.notNull<Long>()
        //sync queue position fetching
        mutex.withLock {
            queueTime = getQueuePos(host)
        }
        while (System.currentTimeMillis()/1000 < queueTime) {
            delay(100L)
        }
        cleanQueue(queueTime)

        var body = ""
        var success:Boolean
        val client = HttpClient()
        try {
            val response: HttpResponse = client.get(originalUrl)
            body = response.readText()
            success = true
        } catch (e:ConnectException){
            success = false
        } catch (e:RedirectResponseException){
            success = false
        } catch (e:ClientRequestException){
            success = false
        } catch (e:ServerResponseException){
            success = false
        }

        if (!success){
            throw RequestFailedException()
        } else {
            return body
        }
    }

    private fun getQueuePos(entry: String):Long{
        val queueToCheck = queue[entry]
        val currentTime = (System.currentTimeMillis().toDouble() / 1000).roundToLong()
        if (queueToCheck != null) {
            if (queueToCheck.isEmpty()){
                queue[entry]?.set(currentTime, 1)
                return currentTime
            } else {
                val lastTime = queueToCheck.keys.maxOrNull()!!
                if (currentTime > lastTime){
                    queueToCheck[currentTime] = 1
                    return currentTime
                }
                return if (queueToCheck[lastTime]!! == rateLimits.getOrDefault(entry, 5)){
                    val queuedTime = lastTime+1
                    queueToCheck[queuedTime] = 1
                    queuedTime
                } else {
                    queueToCheck.replace(lastTime, queueToCheck[lastTime]!!+1)
                    lastTime
                }
            }

        } else {
            queue[entry] = ConcurrentHashMap()
            queue[entry]?.set(currentTime, 1)
            return currentTime
        }
    }

    private fun cleanQueue(currentTime:Long){
        queue.forEach { entry ->
            entry.value
                .filter { it.key < currentTime }
                .forEach { entry.value.remove(it.key) }
        }
    }

}