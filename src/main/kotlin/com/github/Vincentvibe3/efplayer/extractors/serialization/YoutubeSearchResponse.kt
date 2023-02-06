package com.github.Vincentvibe3.efplayer.extractors.serialization

import kotlinx.serialization.Serializable

@Serializable
data class YoutubeSearchResponse(
    val estimatedResults:String,
    val contents:Contents
){
    @Serializable
    data class Contents(val twoColumnSearchResultsRenderer: TwoColumnSearchResultsRenderer)

    @Serializable
    data class TwoColumnSearchResultsRenderer(val primaryContents: PrimaryContents)

    @Serializable
    data class PrimaryContents(val sectionListRenderer: SectionListRenderer)

    @Serializable
    data class SectionListRenderer(val contents:List<SectionListRendererContent>)

    @Serializable
    data class SectionListRendererContent(
        val itemSectionRenderer: ItemSectionRenderer?
    )

    @Serializable
    data class ItemSectionRenderer(val contents:List<ItemSectionContent>)

    @Serializable
    data class ItemSectionContent(val videoRenderer: VideoRenderer?=null)

    @Serializable
    data class  VideoRenderer(
        val videoId:String
    )

}
