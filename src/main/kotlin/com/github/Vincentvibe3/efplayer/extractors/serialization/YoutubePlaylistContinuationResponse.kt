package com.github.Vincentvibe3.efplayer.extractors.serialization

import kotlinx.serialization.Serializable

@Serializable
data class YoutubePlaylistContinuationResponse(
    val onResponseReceivedActions:List<ResponseReceivedAction>
){
    @Serializable
    data class ResponseReceivedAction(val appendContinuationItemsAction:AppendContinuationItemsAction)

    @Serializable
    data class AppendContinuationItemsAction(val continuationItems:List<YoutubePlaylistResponse.PlaylistVideoRendererContent>)
}
