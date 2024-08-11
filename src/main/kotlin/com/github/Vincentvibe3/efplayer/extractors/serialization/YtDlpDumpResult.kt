package com.github.Vincentvibe3.efplayer.extractors.serialization
import kotlinx.serialization.Serializable

@Serializable
data class YtDlpDumpResult(
    val channel: String,
    val title: String,
    val original_url:String,
    val duration: Long,
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

@Serializable
data class YtDlpPlaylistDumpResult(
    val channel: String,
    val title: String,
    val original_url:String,
    val duration: Long,
)