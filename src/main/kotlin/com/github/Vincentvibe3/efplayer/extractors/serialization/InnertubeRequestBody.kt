package com.github.Vincentvibe3.efplayer.extractors.serialization

import kotlinx.serialization.Serializable

@Serializable
data class InnertubeRequestBody(
    val context:Context,
    val playbackContext:PlaybackContext?=null,
    val query:String?=null,
    val params:String?=null,
    val continuation:String?=null,
    val browseId:String?=null,
    val videoId:String?=null
){
    @Serializable
    data class Context(
        val client:Client
    )

    @Serializable
    data class Client(
        val clientName:String,
        val clientVersion:String
    )

    @Serializable
    data class PlaybackContext(
        val contentPlaybackContext:ContentPlaybackContext
    )

    @Serializable
    data class ContentPlaybackContext(
        val signatureTimestamp:String?
    )
}
