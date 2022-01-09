package com.github.Vincentvibe3.efplayer.core

/**
 * The event listener where all player events are called
 *
 * This must be implemented to receive events from [Player]
 *
 * @see Player
 *
 */
abstract class EventListener {

    /**
     * Called when a track gets loaded
     *
     * @param track The track that is loaded
     * @param player The Player that called the function
     */
    open fun onTrackLoad(track: Track, player: Player){}

    /**
     * Called when a playlist is loaded
     *
     * @param tracks A list of Track that were loaded
     * @param player The Player that called the function
     */
    open fun onPlaylistLoaded(tracks: List<Track>, player: Player){}

    /**
     * Called when a track gets loaded
     *
     * @param track The Track that is loaded
     * @param player The Player that called the function
     */
    open fun onTrackStart(track: Track, player: Player){}

    /**
     * Called when a track finishes playing
     *
     * @param track The Track that is done
     * @param player The Player that called the function
     * @param canStartNext this is <code>false</code> if the Track was interrupted (stopped or a new track was started)
     *
     */
    open fun onTrackDone(track: Track, player: Player, canStartNext:Boolean){}

    /**
     * Called when a track fails to load
     *
     */
    open fun onLoadFailed(){}

    /**
     * Called when a track is paused
     *
     * @param track The Track that is loaded
     * @param player The player that called the function
     */
    open fun onTrackPaused(track: Track, player: Player){}

    /**
     * Called when a track is resumed
     *
     * @param track The Track that is loaded
     * @param player The player that called the function
     */
    open fun onTrackResumed(track: Track, player: Player){}

    /**
     * Called when a track fails to play
     *
     * @param track The Track that failed to play
     */
    open fun onTrackError(track: Track){}

}