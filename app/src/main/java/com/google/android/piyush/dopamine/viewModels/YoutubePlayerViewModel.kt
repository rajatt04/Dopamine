package com.google.android.piyush.dopamine.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.piyush.youtube.model.Youtube
import com.google.android.piyush.youtube.model.channelDetails.YoutubeChannel
import com.google.android.piyush.youtube.model.channelPlaylists.ChannelPlaylists
import com.google.android.piyush.youtube.model.comments.CommentThreadsResponse
import com.google.android.piyush.youtube.repository.YoutubeRepositoryImpl
import com.google.android.piyush.youtube.utilities.NetworkResult
import kotlinx.coroutines.launch

class YoutubePlayerViewModel(
    private val youtubeRepositoryImpl: YoutubeRepositoryImpl
) : ViewModel() {

    private val _videoDetails = MutableLiveData<NetworkResult<Youtube>>()
    val videoDetails: LiveData<NetworkResult<Youtube>> = _videoDetails

    private val _channelDetails = MutableLiveData<NetworkResult<YoutubeChannel>>()
    val channelDetails: LiveData<NetworkResult<YoutubeChannel>> = _channelDetails

    private val _channelsPlaylists = MutableLiveData<NetworkResult<ChannelPlaylists>>()
    val channelsPlaylists: LiveData<NetworkResult<ChannelPlaylists>> = _channelsPlaylists

    private val _comments = MutableLiveData<NetworkResult<CommentThreadsResponse>>()
    val comments: LiveData<NetworkResult<CommentThreadsResponse>> = _comments

    private var commentsNextPageToken: String? = null
    private var isLoadingComments = false

    fun getVideoDetails(videoId: String) {
        viewModelScope.launch {
            _videoDetails.postValue(NetworkResult.Loading)
            val response = youtubeRepositoryImpl.getVideoDetails(videoId)
            when (response) {
                is NetworkResult.Success -> {
                    if (response.data.items.isNullOrEmpty()) {
                        _videoDetails.postValue(NetworkResult.Error(message = "Video not found."))
                    } else {
                        _videoDetails.postValue(response)
                    }
                }
                is NetworkResult.Error -> _videoDetails.postValue(response)
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun getChannelDetails(channelId: String) {
        viewModelScope.launch {
            _channelDetails.postValue(NetworkResult.Loading)
            val response = youtubeRepositoryImpl.getChannelDetails(channelId)
            when (response) {
                is NetworkResult.Success -> {
                    if (response.data.items.isNullOrEmpty()) {
                        _channelDetails.postValue(NetworkResult.Error(message = "Channel not found."))
                    } else {
                        _channelDetails.postValue(response)
                    }
                }
                is NetworkResult.Error -> _channelDetails.postValue(response)
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun getChannelsPlaylist(channelId: String) {
        viewModelScope.launch {
            _channelsPlaylists.postValue(NetworkResult.Loading)
            val response = youtubeRepositoryImpl.getChannelsPlaylists(channelId)
            when (response) {
                is NetworkResult.Success -> {
                    if (response.data.items.isNullOrEmpty()) {
                        _channelsPlaylists.postValue(NetworkResult.Error(message = "No playlists found."))
                    } else {
                        _channelsPlaylists.postValue(response)
                    }
                }
                is NetworkResult.Error -> _channelsPlaylists.postValue(response)
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun getComments(videoId: String, reset: Boolean = false) {
        if (isLoadingComments) return
        if (reset) commentsNextPageToken = null

        viewModelScope.launch {
            isLoadingComments = true
            if (reset) _comments.postValue(NetworkResult.Loading)

            val response = youtubeRepositoryImpl.getCommentThreads(
                videoId = videoId,
                pageToken = commentsNextPageToken
            )

            when (response) {
                is NetworkResult.Success -> {
                    commentsNextPageToken = response.data.nextPageToken
                    _comments.postValue(response)
                }
                is NetworkResult.Error -> _comments.postValue(response)
                is NetworkResult.Loading -> Unit
            }
            isLoadingComments = false
        }
    }

    fun hasMoreComments(): Boolean = commentsNextPageToken != null

    fun loadVideoData(videoId: String, channelId: String) {
        getVideoDetails(videoId)
        getChannelDetails(channelId)
        getComments(videoId, reset = true)
    }
}

class YoutubePlayerViewModelFactory(
    private val youtubeRepositoryImpl: YoutubeRepositoryImpl
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(YoutubePlayerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return YoutubePlayerViewModel(youtubeRepositoryImpl) as T
        } else {
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
