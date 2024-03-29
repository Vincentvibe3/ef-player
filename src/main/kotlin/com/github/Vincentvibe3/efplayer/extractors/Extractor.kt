package com.github.Vincentvibe3.efplayer.extractors

import com.github.Vincentvibe3.efplayer.core.Track

/**
 * Must be implemented to extract info for tracks
 *
 */
abstract class Extractor {

    /**
     * Enum for input from getTrack
     *
     */
    enum class URL_TYPE {
        TRACK, PLAYLIST, INVALID, ARTIST, ALBUM
    }

    /**
     * Get a URL to stream audio from
     *
     * @param url the URL for the video/audio to be extracted
     *
     * @return the URL of the resource requested
     *
     */
    abstract suspend fun getStream(url:String, track: Track):String?

    /**
     * create a [Track] from a URL to a resource
     *
     * @param url the URL to get a track from
     *
     * @return The [Track] with the resource's information
     *
     */
    abstract suspend fun getTrack(url: String, loadId:String): Track?

    /**
     * Gets [Tracks][Track] from an album
     *
     * @param url The URL where to find the album
     *
     * @return an empty list if no tracks could be created
     *
     */
    open suspend fun getAlbumTracks(url: String, loadId:String):List<Track>{
        return ArrayList()
    }

    /**
     * Gets [Tracks][Track] from an artist
     *
     * @param url The URL where to find the artist
     *
     * @return an empty list if no tracks could be created
     *
     */
    open suspend fun getArtistTracks(url: String, loadId:String):List<Track>{
        return ArrayList()
    }

    /**
     * Sort an input URL in different to extract differently
     *
     * @param url The input URL
     *
     * @return [URL_TYPE.TRACK] is returned by default if not overriden
     *
     * @see URL_TYPE
     */
    open suspend fun getUrlType(url: String):URL_TYPE{
        return URL_TYPE.TRACK
    }

    /**
     * Gets [Tracks][Track] from a playlist
     *
     * @param url The URL where to find the playlist
     *
     * @return an empty list if no tracks could be created
     *
     */
    open suspend fun getPlaylistTracks(url: String, loadId:String):List<Track>{
        return ArrayList()
    }

    abstract suspend fun search(query: String, loadId:String): Track?

}