package com.github.Vincentvibe3.efplayer.core

class EL:EventListener {
    override suspend fun onTrackLoad(track: Track, player: Player) {
        println("track loaded")
        player.play(track)
    }

    override fun onTrackStart(track: Track, player: Player) {
        println("Track started")
    }

    override fun onTrackDone(track: Track, player: Player) {
        println("track done")
    }

    override fun onTrackLoadFailed() {
        TODO("Not yet implemented")
    }

    override fun onTrackPaused(track: Track, player: Player) {
        println("paused")
    }

    override fun onTrackResumed(track: Track, player: Player) {
        println("resumed")
    }

}