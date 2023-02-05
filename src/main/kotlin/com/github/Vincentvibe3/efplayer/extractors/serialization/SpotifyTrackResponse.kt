@file:Suppress("PropertyName")

package com.github.Vincentvibe3.efplayer.extractors.serialization

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyTrackResponse(
    val name:String,
    val artists:List<SpotifyArtist>,
    val duration_ms:Long,
    val external_urls:SpotifyExternalUrls
){

    @Serializable
    data class SpotifyExternalUrls(
        val spotify: String
    )

    @Serializable
    data class SpotifyArtist(
        val name:String
    )
}
