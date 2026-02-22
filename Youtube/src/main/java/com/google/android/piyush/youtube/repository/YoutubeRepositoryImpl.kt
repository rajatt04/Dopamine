package com.google.android.piyush.youtube.repository

import com.google.android.piyush.youtube.model.CommentThreads
import com.google.android.piyush.youtube.model.SearchTube
import com.google.android.piyush.youtube.utilities.YoutubeClient
import com.google.android.piyush.youtube.model.Youtube
import com.google.android.piyush.youtube.model.channelDetails.YoutubeChannel
import com.google.android.piyush.youtube.model.channelPlaylists.ChannelPlaylists
import io.ktor.client.call.body
import io.ktor.client.request.get

class YoutubeRepositoryImpl : YoutubeRepository {
    override suspend fun getCommentThreads(videoId: String, order: String, pageToken: String?): CommentThreads {
        val response = YoutubeClient.CLIENT.get(
            YoutubeClient.YOUTUBE + YoutubeClient.COMMENT_THREADS
        ){
            url {
                parameters.append("part", YoutubeClient.COMMENT_PART)
                parameters.append("videoId", videoId)
                parameters.append("order", order)
                parameters.append("maxResults", "5")
                if (pageToken != null) {
                    parameters.append("pageToken", pageToken)
                }
                parameters.append("key", YoutubeClient.API_KEY)
            }
        }
        return response.body()
    }

    override suspend fun getHomeVideos(): Youtube {
        val response = YoutubeClient.CLIENT.get(
            YoutubeClient.YOUTUBE + YoutubeClient.VIDEO
        ){
            url {
                parameters.append("part", YoutubeClient.PART)
                parameters.append("chart", YoutubeClient.CHART)
                parameters.append("regionCode", YoutubeClient.REGION_CODE)
                parameters.append("maxResults", YoutubeClient.MAX_RESULTS)
                parameters.append("key", YoutubeClient.API_KEY)
            }
        }
        return response.body()
    }

    override suspend fun getLibraryVideos(playListId: String): Youtube {
        val response = YoutubeClient.CLIENT.get(
            YoutubeClient.YOUTUBE + YoutubeClient.PLAYLIST
        ){
            url {
                parameters.append("part", YoutubeClient.PLAYLIST_PART)
                parameters.append("playlistId", playListId)
                parameters.append("maxResults", YoutubeClient.MAX_RESULTS)
                parameters.append("key", YoutubeClient.API_KEY)
            }
        }
        return response.body()
    }

    override suspend fun getSearchVideos(query: String): SearchTube {
        val response = YoutubeClient.CLIENT.get(
            YoutubeClient.YOUTUBE + YoutubeClient.SEARCH
        ){
            url {
                parameters.append("part", YoutubeClient.SEARCH_PART)
                parameters.append("q", query)
                parameters.append("maxResults", YoutubeClient.MAX_RESULTS)
                parameters.append("key", YoutubeClient.API_KEY)
            }
        }
        return response.body()
    }

    override suspend fun getChannelDetails(channelId: String): YoutubeChannel {
        val response = YoutubeClient.CLIENT.get(
            YoutubeClient.YOUTUBE + YoutubeClient.CHANNEL
        ){
            url {
                parameters.append("part", YoutubeClient.CHANNEL_PART)
                parameters.append("id", channelId)
                parameters.append("key", YoutubeClient.API_KEY)
            }
        }
        return response.body()
    }

    override suspend fun getChannelsPlaylists(channelId: String): ChannelPlaylists {
        val response = YoutubeClient.CLIENT.get(
            YoutubeClient.YOUTUBE + YoutubeClient.PLAYLISTS
        ) {
            url {
                parameters.append("part", YoutubeClient.PLAYLIST_PART)
                parameters.append("channelId", channelId)
                parameters.append("maxResults", YoutubeClient.MAX_RESULTS)
                parameters.append("key", YoutubeClient.API_KEY)
            }
        }
        return response.body()
    }

    override suspend fun getPlaylistVideos(playListId: String): Youtube {
        val response = YoutubeClient.CLIENT.get(
            YoutubeClient.YOUTUBE + YoutubeClient.PLAYLIST
            ){
            url {
                parameters.append("part", YoutubeClient.PLAYLIST_PART)
                parameters.append("playlistId", playListId)
                parameters.append("maxResults", YoutubeClient.MAX_RESULTS)
                parameters.append("key", YoutubeClient.API_KEY)
            }
        }
        return response.body()
    }

    override suspend fun getVideoDetails(videoId: String): Youtube {
        val response = YoutubeClient.CLIENT.get(
            YoutubeClient.YOUTUBE + YoutubeClient.VIDEO
        ){
            url {
                parameters.append("part", YoutubeClient.PART)
                parameters.append("id", videoId)
                parameters.append("key", YoutubeClient.API_KEY)
            }
        }
        return response.body()
    }

    suspend fun reGetHomeVideos() : Youtube {
        val response = YoutubeClient.CLIENT.get(
            YoutubeClient.YOUTUBE + YoutubeClient.VIDEO
        ){
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
        val response = YoutubeClient.CLIENT.get(
            YoutubeClient.YOUTUBE + YoutubeClient.PLAYLIST
        ){
            url {
                parameters.append("part", YoutubeClient.PLAYLIST_PART)
                parameters.append("playlistId", playListId)
                parameters.append("maxResults", YoutubeClient.MAX_RESULTS)
                parameters.append("key",YoutubeClient.EXTRA_KEYS)
            }
        }
        return response.body()
    }

    suspend fun reGetSearchVideos(query: String): SearchTube {
        val response = YoutubeClient.CLIENT.get(
            YoutubeClient.YOUTUBE + YoutubeClient.SEARCH
        ){
            url {
                parameters.append("part", YoutubeClient.SEARCH_PART)
                parameters.append("q", query)
                parameters.append("maxResults", YoutubeClient.MAX_RESULTS)
                parameters.append("key",YoutubeClient.EXTRA_KEYS)
            }
        }
        return response.body()
    }

    suspend fun reGetChannelDetails(channelId: String): Youtube {
        val response = YoutubeClient.CLIENT.get(
            YoutubeClient.YOUTUBE + YoutubeClient.CHANNEL
        ){
            url {
                parameters.append("part", YoutubeClient.PART)
                parameters.append("id", channelId)
                parameters.append("key", YoutubeClient.EXTRA_KEYS)
            }
        }
        return response.body()
    }

    suspend fun reGetChannelsPlaylists(channelId: String): Youtube {
        val response = YoutubeClient.CLIENT.get(
            YoutubeClient.YOUTUBE + YoutubeClient.PLAYLISTS
        ) {
            url {
                parameters.append("part", YoutubeClient.PLAYLIST_PART)
                parameters.append("channelId", channelId)
                parameters.append("maxResults", YoutubeClient.MAX_RESULTS)
                parameters.append("key", YoutubeClient.EXTRA_KEYS)
            }
        }
        return response.body()
    }

    suspend fun reGetPlaylistVideos(playListId: String): Youtube {
        val response = YoutubeClient.CLIENT.get(
            YoutubeClient.YOUTUBE + YoutubeClient.PLAYLIST
        ){
            url {
                parameters.append("part", YoutubeClient.PLAYLIST_PART)
                parameters.append("playlistId", playListId)
                parameters.append("maxResults", YoutubeClient.MAX_RESULTS)
                parameters.append("key", YoutubeClient.EXTRA_KEYS)
            }
        }
        return response.body()
    }

    suspend fun reGetVideoDetails(videoId: String): Youtube {
        val response = YoutubeClient.CLIENT.get(
            YoutubeClient.YOUTUBE + YoutubeClient.VIDEO
        ){
            url {
                parameters.append("part", YoutubeClient.PART)
                parameters.append("id", videoId)
                parameters.append("key", YoutubeClient.EXTRA_KEYS)
            }
        }
        return response.body()
    }


    //Experimental
    suspend fun experimentalDefaultVideos(): Youtube {
        val response = YoutubeClient.CLIENT.get(
            YoutubeClient.EXPERIMENTAL_API + YoutubeClient.VIDEO
        ) {
            url {
                parameters.append("part", YoutubeClient.PART)
                parameters.append("chart", YoutubeClient.CHART)
                parameters.append("regionCode", YoutubeClient.REGION_CODE)
                parameters.append("maxResults", YoutubeClient.MAX_RESULTS)
            }
        }
        return response.body()
    }

    suspend fun experimentalSearchVideos(search : String): SearchTube {
        val response = YoutubeClient.CLIENT.get(
            YoutubeClient.EXPERIMENTAL_API + YoutubeClient.SEARCH
        ){
            url {
                parameters.append("part", YoutubeClient.SEARCH_PART)
                parameters.append("q", search)
                parameters.append("maxResults", YoutubeClient.MAX_RESULTS)
            }
        }
        return response.body()
    }
}