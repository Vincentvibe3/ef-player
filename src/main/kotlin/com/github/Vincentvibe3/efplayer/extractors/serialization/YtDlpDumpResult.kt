package com.github.Vincentvibe3.efplayer.extractors.serialization
import kotlinx.serialization.Serializable

@Serializable
data class YtDlpDumpResult(
    val formats: List<YtDlpDumpFormats>
)

@Serializable
data class YtDlpDumpFormats(
    val acodec:String?=null,
    val vcodec:String?=null,
    val ext:String?=null,
    val url:String?=null,
    val abr: Float?=null
)