package com.google.android.piyush.youtube.repository

import com.google.android.piyush.youtube.model.CommentThreads
import com.google.android.piyush.youtube.model.SearchTube
import com.google.android.piyush.youtube.model.Youtube
import com.google.android.piyush.youtube.model.channelDetails.YoutubeChannel
import com.google.android.piyush.youtube.model.channelPlaylists.ChannelPlaylists

interface YoutubeRepository {
    suspend fun getHomeVideos(useExtraKey: Boolean = false) : Youtube
    suspend fun getLibraryVideos(playListId : String, useExtraKey: Boolean = false) : Youtube
    suspend fun getSearchVideos(query : String, useExtraKey: Boolean = false) : SearchTube
    suspend fun getChannelDetails(channelId : String) : YoutubeChannel
    suspend fun getChannelsPlaylists(channelId : String) : ChannelPlaylists
    suspend fun getPlaylistVideos(playListId : String, useExtraKey: Boolean = false) : Youtube
    suspend fun getVideoDetails(videoId : String, useExtraKey: Boolean = false) : Youtube
    suspend fun getCommentThreads(videoId: String, order: String = "relevance", pageToken: String? = null): CommentThreads
}