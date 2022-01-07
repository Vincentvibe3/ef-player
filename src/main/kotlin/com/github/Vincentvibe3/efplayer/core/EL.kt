package com.github.Vincentvibe3.efplayer.core

class EL:EventListener {
    override suspend fun onTrackLoad(track: Track, player: Player) {
        println("track loaded")
        player.play(track)
    }

    override suspend fun onPlaylistLoaded(tracks: List<Track>, player: Player) {
        player.play(tracks.first())
        tracks.forEach {
            println(it.url)
        }
    }

    override fun onTrackStart(track: Track, player: Player) {
        println("Track started")
    }

    override fun onTrackDone(track: Track, player: Player) {
        println("track done")
    }

    override fun onLoadFailed() {
        TODO("Not yet implemented")
    }

    override fun onTrackPaused(track: Track, player: Player) {
        println("paused")
    }

    override fun onTrackResumed(track: Track, player: Player) {
        println("resumed")
    }

}