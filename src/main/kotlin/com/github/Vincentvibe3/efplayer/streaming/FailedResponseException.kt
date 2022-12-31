package com.github.Vincentvibe3.efplayer.streaming

class FailedResponseException(
    host:String
):Exception("Unsuccessful response $host")