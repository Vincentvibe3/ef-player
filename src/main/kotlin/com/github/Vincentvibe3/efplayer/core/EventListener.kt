package com.github.Vincentvibe3.efplayer.core

abstract class EventListener() {

    open suspend fun onTrackLoad(track: Track, player: Player){}

    open suspend fun onPlaylistLoaded(track: List<Track>, player: Player){}

    open fun onTrackStart(track: Track, player: Player){}

    open fun onTrackDone(track: Track, player: Player, canStartNext:Boolean){}

    open fun onLoadFailed(){}

    open fun onTrackPaused(track: Track, player: Player){}

    open fun onTrackResumed(track: Track, player: Player){}

    open fun onTrackError(track: Track){}

}