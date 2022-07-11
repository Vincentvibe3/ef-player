package com.github.Vincentvibe3.efplayer.streaming

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object ThreadManager {

    val executor = ThreadPoolExecutor(1,
        Int.MAX_VALUE,
        5,
        TimeUnit.MINUTES,
        SynchronousQueue()
    )

}