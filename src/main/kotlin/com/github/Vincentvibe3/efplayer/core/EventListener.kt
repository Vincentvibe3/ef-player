package com.github.Vincentvibe3.efplayer.core

abstract class EventListener() {

    /**
     * Called when a track gets loaded
     *
     * @param track The track that is loaded
     * @param player The player that called the function
     */
    open fun onTrackLoad(track: Track, player: Player){}

    /**
     * Called when a playlist is loaded
     *
     * @param tracks A list of <a href="#{@link}">{@link Track}</a> that were loaded
     * @param player The player that called the function
     */
    open fun onPlaylistLoaded(tracks: List<Track>, player: Player){}

    /**
     * Called when a track gets loaded
     *
     * @param track The track that is loaded
     * @param player The player that called the function
     */
    open fun onTrackStart(track: Track, player: Player){}

    /**
     * Called when a track finishes playing
     *
     * @param track The <a href="#{@link}">{@link Track}</a> that is done
     * @param player The player that called the function
     * @param canStartNext this is <code>false</code> if the <a href="#{@link}">{@link Track}</a> was interrupted(stopped on a new track was started)
     * this
     */
    open fun onTrackDone(track: Track, player: Player, canStartNext:Boolean){}

    /**
     * Called when a track fails to load
     *
     * @param track The track that that failed to load
     * @param player The player that called the function
     */
    open fun onLoadFailed(){}

    /**
     * Called when a track is paused
     *
     * @param track The track that is loaded
     * @param player The player that called the function
     */
    open fun onTrackPaused(track: Track, player: Player){}

    /**
     * Called when a track is resumed
     *
     * @param track The track that is loaded
     * @param player The player that called the function
     */
    open fun onTrackResumed(track: Track, player: Player){}

    /**
     * Called when a track fails to play
     *
     * @param track The track that failed to play
     */
    open fun onTrackError(track: Track){}

}