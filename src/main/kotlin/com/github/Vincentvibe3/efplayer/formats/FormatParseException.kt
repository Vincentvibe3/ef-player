package com.github.Vincentvibe3.efplayer.formats

class FormatParseException(
    val format: Formats,
    override val message: String?
):Exception(message)