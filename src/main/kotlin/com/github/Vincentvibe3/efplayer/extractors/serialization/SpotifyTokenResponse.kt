@file:Suppress("PropertyName")

package com.github.Vincentvibe3.efplayer.extractors.serialization

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyTokenResponse(
    val access_token:String,
    val expires_in:Long
)
