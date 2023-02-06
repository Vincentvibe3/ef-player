package com.github.Vincentvibe3.efplayer.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
class Config(
    @SerialName("maxChunks") var maxOpusChunks: Int = 4000,
    var spotifySecret: String = "",
    var spotifyClient: String = "",
    var spotifyToken: String = "",
    var spotifyTokenExpiry: Long = -1
)