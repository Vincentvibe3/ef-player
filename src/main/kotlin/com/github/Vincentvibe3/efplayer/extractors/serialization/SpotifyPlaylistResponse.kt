package com.github.Vincentvibe3.efplayer.extractors.serialization

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyPlaylistResponse(
    val items:List<SpotifyPlaylistTrack>,
    val next:String?
){
    @Serializable
    data class SpotifyPlaylistTrack(
        val track:SpotifyTrackResponse
    )
}
