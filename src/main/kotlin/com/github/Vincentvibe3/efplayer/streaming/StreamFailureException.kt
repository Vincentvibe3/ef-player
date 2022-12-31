package com.github.Vincentvibe3.efplayer.streaming

import com.github.Vincentvibe3.efplayer.core.Track

class StreamFailureException(
    val track:Track,
    reason:String
):Exception(reason)