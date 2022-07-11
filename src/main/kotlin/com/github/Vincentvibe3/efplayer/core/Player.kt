package com.github.Vincentvibe3.efplayer.core

import com.github.Vincentvibe3.efplayer.extractors.Extractor
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

    /**
     * The [Stream] Runnable that is providing audio
     *
     * @See Stream
     *
     */
    private var stream: Stream = Stream(eventListener, this)

    var paused = false

    init {
        Config.getMaxChunks()
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
            val type = Youtube.getUrlType(query)
            when (type){
                Extractor.URL_TYPE.TRACK->{
                    val track = Youtube.getTrack(query)
                    if (track != null) {
                        eventListener.onTrackLoad(track, player)
                    } else {
                        eventListener.onLoadFailed()
                    }
                }
                Extractor.URL_TYPE.PLAYLIST->{
                    val tracks = Youtube.getPlaylistTracks(query)
                    if (tracks.isNotEmpty()) {
                        eventListener.onPlaylistLoaded(tracks, player)
                    } else {
                        eventListener.onLoadFailed()
                    }
                }
                Extractor.URL_TYPE.INVALID->{
                    val track = Youtube.search(query)
                    if (track != null) {
                        eventListener.onTrackLoad(track, player)
                    } else {
                        eventListener.onLoadFailed()
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
        currentTrack = track
        stream.track = currentTrack as Track
        stream.startSong()
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
        stream.stop()
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