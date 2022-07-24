package com.github.Vincentvibe3.efplayer.core

import com.github.Vincentvibe3.efplayer.extractors.Extractor
import com.github.Vincentvibe3.efplayer.extractors.Spotify
import com.github.Vincentvibe3.efplayer.extractors.Youtube
import com.github.Vincentvibe3.efplayer.streaming.Stream
import kotlinx.coroutines.runBlocking


/**
 * The central component to start playing audio
 *
 * The player uses `load()` to get a [Track] that can be played with `play()`.
 * The player supports other basic operations like pausing and stopping.
 * Getting audio is by using `provide()`.
 * Whether it can provide can be gotten from `canProvide()`.
 *
 * @param eventListener The [EventListener] called when the playback's state is changed
 *
 * @see EventListener
 *
 */
class Player(val eventListener: EventListener) {

    /**
     * The active [Track]
     *
     * @see Track
     *
     */
    var currentTrack:Track? = null

    private var currentStream:Stream? = null

    var paused = false

    init {
        Config.load()
    }

    fun getExtractor(url:String): Extractor {
        if (url.contains("open.spotify.com")){
            return Spotify
        } else {
            return Youtube
        }
    }

    /**
     * Load a [Track] from a query
     *
     * Will call [EventListener.onTrackLoad] once the Track is loaded
     *
     * @param query a search query or a URL
     *
     * @See EventListener.onTrackLoad
     *
     */
    fun load(query:String){
        val player = this
        runBlocking {
            val extractor = getExtractor(query)
            val type = extractor.getUrlType(query)
            when (type){
                Extractor.URL_TYPE.TRACK->{
                    val track = extractor.getTrack(query)
                    if (track != null) {
                        eventListener.onTrackLoad(track, player)
                    } else {
                        eventListener.onLoadFailed("Cannot find track")
                    }
                }
                Extractor.URL_TYPE.PLAYLIST->{
                    val tracks = extractor.getPlaylistTracks(query)
                    if (tracks.isNotEmpty()) {
                        eventListener.onPlaylistLoaded(tracks, player)
                    } else {
                        eventListener.onLoadFailed("Cannot find playlist")
                    }
                }
                Extractor.URL_TYPE.INVALID->{
                    val track = extractor.search(query)
                    if (track != null) {
                        eventListener.onTrackLoad(track, player)
                    } else {
                        eventListener.onLoadFailed("Could not find match")
                    }
                }
                Extractor.URL_TYPE.ALBUM ->{
                    val tracks = extractor.getAlbumTracks(query)
                    if (tracks.isNotEmpty()) {
                        eventListener.onPlaylistLoaded(tracks, player)
                    } else {
                        eventListener.onLoadFailed("Cannot load album")
                    }
                }
                Extractor.URL_TYPE.ARTIST -> {
                    val tracks = extractor.getArtistTracks(query)
                    if (tracks.isNotEmpty()) {
                        eventListener.onPlaylistLoaded(tracks, player)
                    } else {
                        eventListener.onLoadFailed("Cannot load artist")
                    }
                }
            }
        }
    }

    /**
     * Play a [Track] and stops the current [Track] if there is one
     *
     * [EventListener.onTrackStart] will be called once done
     *
     * @param track the [Track] to load
     *
     * @See EventListener.onTrackStart
     *
     */
    fun play(track: Track){
        stop()
        val stream = Stream(eventListener, this)
        stream.track = track
        currentStream = stream
        stream.startSong()
        currentTrack = track
    }

    /**
     * Stops the current [Track]
     *
     * Will set `canStartNext` as false in [EventListener.onTrackDone]
     *
     * @See EventListener.onTrackDone
     *
     */
    fun stop(){
        //clear the track buffer to prevent blocking
        currentTrack?.trackChunks?.clear()
        currentStream?.stop()
        currentTrack = null
    }

    /**
     * Pauses the current [Track]
     *
     * Will call [EventListener.onTrackPaused]
     *
     * @See EventListener.onTrackPaused
     *
     */
    fun pause(){
        paused = true
        currentTrack?.let { eventListener.onTrackPaused(it, this) }
    }

    /**
     * Resumes the current [Track]
     *
     * Will call [EventListener.onTrackResumed]
     *
     * @See EventListener.onTrackResumed
     *
     */
    fun resume(){
        paused = false
        currentTrack?.let { eventListener.onTrackResumed(it, this) }
    }

    /**
     * Provides 20ms of audio
     *
     * @return a [ByteArray] with the audio [Bytes][Byte]
     *
     */
    fun provide(): ByteArray {
        return currentTrack!!.trackChunks.remove()
    }

    /**
     * Whether or not the the player has audio to provide with [provide]
     *
     * @return `true` if the track has audio
     *
     * false if there isn't
     *
     */
    fun canProvide():Boolean{
        return if (!paused){
            currentTrack?.trackChunks?.isNotEmpty() ?: false
        } else {
            false
        }
    }

}