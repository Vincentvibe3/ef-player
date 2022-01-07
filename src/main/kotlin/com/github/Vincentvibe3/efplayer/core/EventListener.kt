package com.github.Vincentvibe3.efplayer.core

interface EventListener {

    suspend fun onTrackLoad(track: Track, player: Player)

    suspend fun onPlaylistLoaded(track: List<Track>, player: Player)

    fun onTrackStart(track: Track, player: Player)

    fun onTrackDone(track: Track, player: Player)

    fun onLoadFailed()

    fun onTrackPaused(track: Track, player: Player)

    fun onTrackResumed(track: Track, player: Player)

}