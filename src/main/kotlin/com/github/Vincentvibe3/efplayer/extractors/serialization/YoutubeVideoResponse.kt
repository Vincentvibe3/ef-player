package com.github.Vincentvibe3.efplayer.extractors.serialization

import kotlinx.serialization.Serializable

@Serializable
data class YoutubeVideoResponse(
    val streamingData:StreamingData?,
    val videoDetails:VideoDetails?
){
    @Serializable
    data class VideoDetails(
        val title:String?=null,
        val author:String?=null
    )

    @Serializable
    data class StreamingData(
        val adaptiveFormats:List<AdaptiveFormat>
    )

    @Serializable
    data class AdaptiveFormat(
        val mimeType:String,
        val bitrate:Long,
        val approxDurationMs:Long,
        val url:String?=null,
        val signatureCipher:String?=null
    )
}
