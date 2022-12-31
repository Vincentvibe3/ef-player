package com.github.Vincentvibe3.efplayer.formats

/**
 * Result of a byte operation
 *
 * @param bytesRead The amount of bytes read
 * @param value The value that was read as a [Long]
 */
data class Result<T>(val bytesRead:Long, val value:T)