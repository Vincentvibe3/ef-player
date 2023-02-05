package com.github.Vincentvibe3.efplayer.extractors.serialization

import kotlinx.serialization.Serializable

@Serializable
data class YoutubePlaylistResponse(
    val contents:Content
) {
    @Serializable
    data class Content(val twoColumnBrowseResultsRenderer: TwoColumnBrowseResultsRenderer)
    @Serializable
    data class TwoColumnBrowseResultsRenderer(val tabs:List<Tab>)

    @Serializable
    data class Tab(val tabRenderer:TabRenderer)

    @Serializable
    data class TabRenderer(val content:TabRendererContent)

    @Serializable
    data class TabRendererContent(val sectionListRenderer: SectionListRenderer)

    @Serializable
    data class SectionListRenderer(val contents:List<SectionListRendererContent>)

    @Serializable
    data class SectionListRendererContent(val itemSectionRenderer: ItemSectionRenderer)

    @Serializable
    data class ItemSectionRenderer(val contents:List<ItemSectionRendererContent>)

    @Serializable
    data class ItemSectionRendererContent(val playlistVideoListRenderer:PlaylistVideoListRenderer)

    @Serializable
    data class PlaylistVideoListRenderer(val contents:List<PlaylistVideoRendererContent>)

    @Serializable
    data class PlaylistVideoRendererContent(
        val playlistVideoRenderer:PlaylistVideoRenderer?,
        val continuationItemRenderer:ContinuationItemRenderer?
    )

    @Serializable
    data class ContinuationItemRenderer(val continuationEndpoint:ContinuationEndpoint)

    @Serializable
    data class ContinuationEndpoint(val continuationCommand:ContinuationCommand)

    @Serializable
    data class ContinuationCommand(val token:String)

    @Serializable
    data class PlaylistVideoRenderer(
        val videoId:String,
        val title:TextHolder,
        val lengthSeconds:String?,
        val shortBylineText:TextHolder?
    )

    @Serializable
    data class TextHolder(val runs:List<Text>)

    @Serializable
    data class Text(val text:String)


}
