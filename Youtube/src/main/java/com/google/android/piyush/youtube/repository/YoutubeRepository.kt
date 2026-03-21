package com.google.android.piyush.youtube.repository

import com.google.android.piyush.youtube.model.SearchTube
import com.google.android.piyush.youtube.model.Shorts
import com.google.android.piyush.youtube.model.Youtube
import com.google.android.piyush.youtube.model.channelDetails.YoutubeChannel
import com.google.android.piyush.youtube.model.channelPlaylists.ChannelPlaylists
import com.google.android.piyush.youtube.model.comments.CommentBody
import com.google.android.piyush.youtube.model.comments.CommentThreadsResponse
import com.google.android.piyush.youtube.model.comments.CommentsResponse
import com.google.android.piyush.youtube.model.ratings.VideoRatingResponse
import com.google.android.piyush.youtube.model.subscriptions.SubscriptionInsertBody
import com.google.android.piyush.youtube.model.subscriptions.SubscriptionsResponse
import com.google.android.piyush.youtube.utilities.NetworkResult

interface YoutubeRepository {
    // Videos
    suspend fun getHomeVideos(): NetworkResult<Youtube>
    suspend fun getVideoDetails(videoId: String): NetworkResult<Youtube>
    suspend fun getVideosByIds(videoIds: String): NetworkResult<Youtube>

    // Search
    suspend fun getSearchVideos(
        query: String,
        order: String = "relevance",
        type: String? = null,
        videoDuration: String? = null,
        regionCode: String = "IN",
        safeSearch: String = "moderate"
    ): NetworkResult<SearchTube>

    suspend fun getSearchVideosPaged(
        query: String,
        pageToken: String? = null,
        maxResults: Int = 20,
        order: String = "relevance",
        type: String? = null,
        videoDuration: String? = null
    ): NetworkResult<SearchTube>

    // Shorts
    suspend fun getYoutubeShorts(): NetworkResult<List<Shorts>>

    // Channels
    suspend fun getChannelDetails(channelId: String): NetworkResult<YoutubeChannel>
    suspend fun getChannelsPlaylists(channelId: String): NetworkResult<ChannelPlaylists>

    // Playlists
    suspend fun getLibraryVideos(playListId: String): NetworkResult<Youtube>
    suspend fun getPlaylistVideos(playListId: String): NetworkResult<Youtube>

    // Comments
    suspend fun getCommentThreads(
        videoId: String,
        order: String = "relevance",
        pageToken: String? = null,
        maxResults: Int = 20
    ): NetworkResult<CommentThreadsResponse>

    suspend fun getCommentReplies(
        parentId: String,
        pageToken: String? = null,
        maxResults: Int = 20
    ): NetworkResult<CommentsResponse>

    suspend fun insertComment(comment: CommentBody): NetworkResult<CommentBody>
    suspend fun updateComment(commentId: String, textOriginal: String): NetworkResult<CommentBody>
    suspend fun deleteComment(commentId: String): NetworkResult<Unit>

    // Ratings
    suspend fun getVideoRating(videoId: String, accessToken: String): NetworkResult<VideoRatingResponse>
    suspend fun rateVideo(videoId: String, rating: String, accessToken: String): NetworkResult<Unit>

    // Subscriptions
    suspend fun getSubscriptions(
        channelId: String,
        pageToken: String? = null,
        maxResults: Int = 50
    ): NetworkResult<SubscriptionsResponse>

    suspend fun checkSubscription(channelId: String, accessToken: String): NetworkResult<Boolean>
    suspend fun subscribeToChannel(channelId: String, accessToken: String): NetworkResult<String>
    suspend fun unsubscribeFromChannel(subscriptionId: String, accessToken: String): NetworkResult<Unit>

    // Activities (Home feed personalization)
    suspend fun getChannelActivities(
        channelId: String,
        pageToken: String? = null,
        maxResults: Int = 20
    ): NetworkResult<Youtube>
}
