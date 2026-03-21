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
import com.google.android.piyush.youtube.model.subscriptions.SubscriptionInsertSnippet
import com.google.android.piyush.youtube.model.subscriptions.ResourceId
import com.google.android.piyush.youtube.model.subscriptions.SubscriptionsResponse
import com.google.android.piyush.youtube.utilities.ErrorMapper
import com.google.android.piyush.youtube.utilities.NetworkResult
import com.google.android.piyush.youtube.utilities.QuotaManager
import com.google.android.piyush.youtube.utilities.YoutubeClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class YoutubeRepositoryImpl : YoutubeRepository {

    private inline fun <T> safeApiCall(operation: String, block: () -> T): NetworkResult<T> {
        return try {
            QuotaManager.trackUsage(operation)
            NetworkResult.Success(block())
        } catch (e: Exception) {
            val message = ErrorMapper.getUserMessage(e)
            NetworkResult.Error(message = message, exception = e)
        }
    }

    // ==================== VIDEOS ====================

    override suspend fun getHomeVideos(): NetworkResult<Youtube> = safeApiCall("videos.list") {
        val response = YoutubeClient.CLIENT.get(YoutubeClient.YOUTUBE + YoutubeClient.VIDEO) {
            url {
                parameters.append("part", YoutubeClient.PART)
                parameters.append("chart", YoutubeClient.CHART)
                parameters.append("regionCode", YoutubeClient.REGION_CODE)
                parameters.append("maxResults", YoutubeClient.MAX_RESULTS)
                parameters.append("key", YoutubeClient.API_KEY)
            }
        }
        response.body<Youtube>()
    }

    override suspend fun getVideoDetails(videoId: String): NetworkResult<Youtube> = safeApiCall("videos.list") {
        val response = YoutubeClient.CLIENT.get(YoutubeClient.YOUTUBE + YoutubeClient.VIDEO) {
            url {
                parameters.append("part", "${YoutubeClient.PART},topicDetails")
                parameters.append("id", videoId)
                parameters.append("key", YoutubeClient.API_KEY)
            }
        }
        response.body<Youtube>()
    }

    override suspend fun getVideosByIds(videoIds: String): NetworkResult<Youtube> = safeApiCall("videos.list") {
        val response = YoutubeClient.CLIENT.get(YoutubeClient.YOUTUBE + YoutubeClient.VIDEO) {
            url {
                parameters.append("part", "${YoutubeClient.PART},contentDetails")
                parameters.append("id", videoIds)
                parameters.append("key", YoutubeClient.API_KEY)
            }
        }
        response.body<Youtube>()
    }

    // ==================== SEARCH ====================

    override suspend fun getSearchVideos(
        query: String,
        order: String,
        type: String?,
        videoDuration: String?,
        regionCode: String,
        safeSearch: String
    ): NetworkResult<SearchTube> = safeApiCall("search.list") {
        val response = YoutubeClient.CLIENT.get(YoutubeClient.YOUTUBE + YoutubeClient.SEARCH) {
            url {
                parameters.append("part", YoutubeClient.SEARCH_PART)
                parameters.append("q", query)
                parameters.append("maxResults", YoutubeClient.MAX_RESULTS)
                parameters.append("order", order)
                parameters.append("regionCode", regionCode)
                parameters.append("safeSearch", safeSearch)
                type?.let { parameters.append("type", it) }
                videoDuration?.let { parameters.append("videoDuration", it) }
                parameters.append("key", YoutubeClient.API_KEY)
            }
        }
        response.body<SearchTube>()
    }

    override suspend fun getSearchVideosPaged(
        query: String,
        pageToken: String?,
        maxResults: Int,
        order: String,
        type: String?,
        videoDuration: String?
    ): NetworkResult<SearchTube> = safeApiCall("search.list") {
        val response = YoutubeClient.CLIENT.get(YoutubeClient.YOUTUBE + YoutubeClient.SEARCH) {
            url {
                parameters.append("part", YoutubeClient.SEARCH_PART)
                parameters.append("q", query)
                parameters.append("maxResults", maxResults.toString())
                parameters.append("order", order)
                parameters.append("key", YoutubeClient.API_KEY)
                pageToken?.let { parameters.append("pageToken", it) }
                type?.let { parameters.append("type", it) }
                videoDuration?.let { parameters.append("videoDuration", it) }
            }
        }
        response.body<SearchTube>()
    }

    // ==================== SHORTS ====================

    override suspend fun getYoutubeShorts(): NetworkResult<List<Shorts>> = safeApiCall("shorts") {
        val response = YoutubeClient.CLIENT.get(YoutubeClient.HIDDEN_CLIENT + YoutubeClient.SHORTS_PART)
        response.body<List<Shorts>>()
    }

    // ==================== CHANNELS ====================

    override suspend fun getChannelDetails(channelId: String): NetworkResult<YoutubeChannel> = safeApiCall("channels.list") {
        val response = YoutubeClient.CLIENT.get(YoutubeClient.YOUTUBE + YoutubeClient.CHANNEL) {
            url {
                parameters.append("part", "${YoutubeClient.CHANNEL_PART},topicDetails")
                parameters.append("id", channelId)
                parameters.append("key", YoutubeClient.API_KEY)
            }
        }
        response.body<YoutubeChannel>()
    }

    override suspend fun getChannelsPlaylists(channelId: String): NetworkResult<ChannelPlaylists> = safeApiCall("playlists.list") {
        val response = YoutubeClient.CLIENT.get(YoutubeClient.YOUTUBE + YoutubeClient.PLAYLISTS) {
            url {
                parameters.append("part", YoutubeClient.PLAYLIST_PART)
                parameters.append("channelId", channelId)
                parameters.append("maxResults", YoutubeClient.MAX_RESULTS)
                parameters.append("key", YoutubeClient.API_KEY)
            }
        }
        response.body<ChannelPlaylists>()
    }

    // ==================== PLAYLISTS ====================

    override suspend fun getLibraryVideos(playListId: String): NetworkResult<Youtube> = safeApiCall("playlistItems.list") {
        val response = YoutubeClient.CLIENT.get(YoutubeClient.YOUTUBE + YoutubeClient.PLAYLIST) {
            url {
                parameters.append("part", YoutubeClient.PLAYLIST_PART)
                parameters.append("playlistId", playListId)
                parameters.append("maxResults", YoutubeClient.MAX_RESULTS)
                parameters.append("key", YoutubeClient.API_KEY)
            }
        }
        response.body<Youtube>()
    }

    override suspend fun getPlaylistVideos(playListId: String): NetworkResult<Youtube> = safeApiCall("playlistItems.list") {
        val response = YoutubeClient.CLIENT.get(YoutubeClient.YOUTUBE + YoutubeClient.PLAYLIST) {
            url {
                parameters.append("part", YoutubeClient.PLAYLIST_PART)
                parameters.append("playlistId", playListId)
                parameters.append("maxResults", YoutubeClient.MAX_RESULTS)
                parameters.append("key", YoutubeClient.API_KEY)
            }
        }
        response.body<Youtube>()
    }

    // ==================== COMMENTS ====================

    override suspend fun getCommentThreads(
        videoId: String,
        order: String,
        pageToken: String?,
        maxResults: Int
    ): NetworkResult<CommentThreadsResponse> = safeApiCall("commentThreads.list") {
        val response = YoutubeClient.CLIENT.get(YoutubeClient.YOUTUBE + YoutubeClient.COMMENT_THREADS) {
            url {
                parameters.append("part", YoutubeClient.COMMENT_THREADS_PART)
                parameters.append("videoId", videoId)
                parameters.append("order", order)
                parameters.append("maxResults", maxResults.toString())
                parameters.append("key", YoutubeClient.API_KEY)
                pageToken?.let { parameters.append("pageToken", it) }
            }
        }
        response.body<CommentThreadsResponse>()
    }

    override suspend fun getCommentReplies(
        parentId: String,
        pageToken: String?,
        maxResults: Int
    ): NetworkResult<CommentsResponse> = safeApiCall("comments.list") {
        val response = YoutubeClient.CLIENT.get(YoutubeClient.YOUTUBE + YoutubeClient.COMMENTS) {
            url {
                parameters.append("part", "snippet")
                parameters.append("parentId", parentId)
                parameters.append("maxResults", maxResults.toString())
                parameters.append("key", YoutubeClient.API_KEY)
                pageToken?.let { parameters.append("pageToken", it) }
            }
        }
        response.body<CommentsResponse>()
    }

    override suspend fun insertComment(comment: CommentBody): NetworkResult<CommentBody> = safeApiCall("comments.insert") {
        val response = YoutubeClient.CLIENT.post(YoutubeClient.YOUTUBE + YoutubeClient.COMMENT_THREADS) {
            url {
                parameters.append("part", "snippet")
                parameters.append("key", YoutubeClient.API_KEY)
            }
            contentType(ContentType.Application.Json)
            setBody(comment)
        }
        response.body<CommentBody>()
    }

    override suspend fun updateComment(commentId: String, textOriginal: String): NetworkResult<CommentBody> = safeApiCall("comments.update") {
        val body = CommentBody(
            snippet = com.google.android.piyush.youtube.model.comments.CommentBodySnippet(
                textOriginal = textOriginal
            )
        )
        val response = YoutubeClient.CLIENT.put(YoutubeClient.YOUTUBE + YoutubeClient.COMMENTS) {
            url {
                parameters.append("part", "snippet")
                parameters.append("key", YoutubeClient.API_KEY)
            }
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        response.body<CommentBody>()
    }

    override suspend fun deleteComment(commentId: String): NetworkResult<Unit> = safeApiCall("comments.delete") {
        val response = YoutubeClient.CLIENT.delete(YoutubeClient.YOUTUBE + YoutubeClient.COMMENTS) {
            url {
                parameters.append("id", commentId)
                parameters.append("key", YoutubeClient.API_KEY)
            }
        }
        if (!response.status.isSuccess()) {
            throw Exception("Failed to delete comment: ${response.status}")
        }
    }

    // ==================== RATINGS ====================

    override suspend fun getVideoRating(videoId: String, accessToken: String): NetworkResult<VideoRatingResponse> = safeApiCall("videos.getRating") {
        val response = YoutubeClient.CLIENT.get(YoutubeClient.YOUTUBE + YoutubeClient.VIDEO_RATINGS) {
            url {
                parameters.append("part", YoutubeClient.RATING_PART)
                parameters.append("id", videoId)
            }
            header("Authorization", "Bearer $accessToken")
        }
        response.body<VideoRatingResponse>()
    }

    override suspend fun rateVideo(videoId: String, rating: String, accessToken: String): NetworkResult<Unit> = safeApiCall("videos.rate") {
        val response = YoutubeClient.CLIENT.post(YoutubeClient.YOUTUBE + YoutubeClient.VIDEO) {
            url {
                parameters.append("id", videoId)
                parameters.append("rating", rating)
            }
            header("Authorization", "Bearer $accessToken")
        }
        if (!response.status.isSuccess()) {
            throw Exception("Failed to rate video: ${response.status}")
        }
    }

    // ==================== SUBSCRIPTIONS ====================

    override suspend fun getSubscriptions(
        channelId: String,
        pageToken: String?,
        maxResults: Int
    ): NetworkResult<SubscriptionsResponse> = safeApiCall("subscriptions.list") {
        val response = YoutubeClient.CLIENT.get(YoutubeClient.YOUTUBE + YoutubeClient.SUBSCRIPTIONS) {
            url {
                parameters.append("part", YoutubeClient.SUBSCRIPTION_PART)
                parameters.append("channelId", channelId)
                parameters.append("maxResults", maxResults.toString())
                parameters.append("key", YoutubeClient.API_KEY)
                pageToken?.let { parameters.append("pageToken", it) }
            }
        }
        response.body<SubscriptionsResponse>()
    }

    override suspend fun checkSubscription(channelId: String, accessToken: String): NetworkResult<Boolean> = safeApiCall("subscriptions.list") {
        val response = YoutubeClient.CLIENT.get(YoutubeClient.YOUTUBE + YoutubeClient.SUBSCRIPTIONS) {
            url {
                parameters.append("part", "snippet")
                parameters.append("forChannelId", channelId)
                parameters.append("mine", "true")
                parameters.append("maxResults", "1")
            }
            header("Authorization", "Bearer $accessToken")
        }
        val body = response.body<SubscriptionsResponse>()
        body.items?.isNotEmpty() == true
    }

    override suspend fun subscribeToChannel(channelId: String, accessToken: String): NetworkResult<String> = safeApiCall("subscriptions.insert") {
        val body = SubscriptionInsertBody(
            snippet = SubscriptionInsertSnippet(
                resourceId = ResourceId(
                    kind = "youtube#channel",
                    channelId = channelId
                )
            )
        )
        val response = YoutubeClient.CLIENT.post(YoutubeClient.YOUTUBE + YoutubeClient.SUBSCRIPTIONS) {
            url {
                parameters.append("part", "snippet")
            }
            header("Authorization", "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val result = response.body<com.google.android.piyush.youtube.model.subscriptions.SubscriptionItem>()
        result.id ?: throw Exception("Failed to subscribe: no subscription ID returned")
    }

    override suspend fun unsubscribeFromChannel(subscriptionId: String, accessToken: String): NetworkResult<Unit> = safeApiCall("subscriptions.delete") {
        val response = YoutubeClient.CLIENT.delete(YoutubeClient.YOUTUBE + YoutubeClient.SUBSCRIPTIONS) {
            url {
                parameters.append("id", subscriptionId)
            }
            header("Authorization", "Bearer $accessToken")
        }
        if (!response.status.isSuccess()) {
            throw Exception("Failed to unsubscribe: ${response.status}")
        }
    }

    // ==================== ACTIVITIES ====================

    override suspend fun getChannelActivities(
        channelId: String,
        pageToken: String?,
        maxResults: Int
    ): NetworkResult<Youtube> = safeApiCall("activities.list") {
        val response = YoutubeClient.CLIENT.get(YoutubeClient.YOUTUBE + YoutubeClient.ACTIVITIES) {
            url {
                parameters.append("part", YoutubeClient.ACTIVITIES_PART)
                parameters.append("channelId", channelId)
                parameters.append("maxResults", maxResults.toString())
                parameters.append("key", YoutubeClient.API_KEY)
                pageToken?.let { parameters.append("pageToken", it) }
            }
        }
        response.body<Youtube>()
    }

    // ==================== FALLBACK METHODS ====================

    suspend fun reGetHomeVideos(): Youtube {
        val response = YoutubeClient.CLIENT.get(YoutubeClient.YOUTUBE + YoutubeClient.VIDEO) {
            url {
                parameters.append("part", YoutubeClient.PART)
                parameters.append("chart", YoutubeClient.CHART)
                parameters.append("regionCode", YoutubeClient.REGION_CODE)
                parameters.append("maxResults", YoutubeClient.MAX_RESULTS)
                parameters.append("key", YoutubeClient.EXTRA_KEYS)
            }
        }
        return response.body()
    }

    suspend fun reGetLibraryVideos(playListId: String): Youtube {
        val response = YoutubeClient.CLIENT.get(YoutubeClient.YOUTUBE + YoutubeClient.PLAYLIST) {
            url {
                parameters.append("part", YoutubeClient.PLAYLIST_PART)
                parameters.append("playlistId", playListId)
                parameters.append("maxResults", YoutubeClient.MAX_RESULTS)
                parameters.append("key", YoutubeClient.EXTRA_KEYS)
            }
        }
        return response.body()
    }

    suspend fun reGetSearchVideos(query: String): SearchTube {
        val response = YoutubeClient.CLIENT.get(YoutubeClient.YOUTUBE + YoutubeClient.SEARCH) {
            url {
                parameters.append("part", YoutubeClient.SEARCH_PART)
                parameters.append("q", query)
                parameters.append("maxResults", YoutubeClient.MAX_RESULTS)
                parameters.append("key", YoutubeClient.EXTRA_KEYS)
            }
        }
        return response.body()
    }

    suspend fun reGetChannelDetails(channelId: String): Youtube {
        val response = YoutubeClient.CLIENT.get(YoutubeClient.YOUTUBE + YoutubeClient.CHANNEL) {
            url {
                parameters.append("part", YoutubeClient.PART)
                parameters.append("id", channelId)
                parameters.append("key", YoutubeClient.EXTRA_KEYS)
            }
        }
        return response.body()
    }

    suspend fun experimentalDefaultVideos(): Youtube {
        val response = YoutubeClient.CLIENT.get(YoutubeClient.EXPERIMENTAL_API + YoutubeClient.VIDEO) {
            url {
                parameters.append("part", YoutubeClient.PART)
                parameters.append("chart", YoutubeClient.CHART)
                parameters.append("regionCode", YoutubeClient.REGION_CODE)
                parameters.append("maxResults", YoutubeClient.MAX_RESULTS)
            }
        }
        return response.body()
    }

    suspend fun experimentalSearchVideos(search: String): SearchTube {
        val response = YoutubeClient.CLIENT.get(YoutubeClient.EXPERIMENTAL_API + YoutubeClient.SEARCH) {
            url {
                parameters.append("part", YoutubeClient.SEARCH_PART)
                parameters.append("q", search)
                parameters.append("maxResults", YoutubeClient.MAX_RESULTS)
            }
        }
        return response.body()
    }
}
