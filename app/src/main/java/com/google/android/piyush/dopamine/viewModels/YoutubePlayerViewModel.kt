package com.google.android.piyush.dopamine.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

import androidx.lifecycle.viewModelScope
import com.google.android.piyush.youtube.model.Youtube
import com.google.android.piyush.youtube.model.channelPlaylists.ChannelPlaylists
import com.google.android.piyush.youtube.repository.YoutubeRepository
import com.google.android.piyush.youtube.utilities.YoutubeResource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class YoutubePlayerViewModel @Inject constructor(
    private val youtubeRepository: YoutubeRepository
) : ViewModel() {

    private val _videoDetails: MutableLiveData<YoutubeResource<Youtube>> = MutableLiveData()
    val videoDetails: LiveData<YoutubeResource<Youtube>> = _videoDetails

    private val _channelDetails: MutableLiveData<YoutubeResource<com.google.android.piyush.youtube.model.channelDetails.YoutubeChannel>> =
        MutableLiveData()
    val channelDetails: MutableLiveData<YoutubeResource<com.google.android.piyush.youtube.model.channelDetails.YoutubeChannel>> =
        _channelDetails

    private val _channelsPlaylists: MutableLiveData<YoutubeResource<ChannelPlaylists>> =
        MutableLiveData()
    val channelsPlaylists: MutableLiveData<YoutubeResource<ChannelPlaylists>> = _channelsPlaylists

    private val _commentThreads: MutableLiveData<YoutubeResource<com.google.android.piyush.youtube.model.CommentThreads>> =
        MutableLiveData()
    val commentThreads: LiveData<YoutubeResource<com.google.android.piyush.youtube.model.CommentThreads>> = _commentThreads

    private var currentCommentsList = mutableListOf<com.google.android.piyush.youtube.model.CommentThreadItem>()
    var commentNextPageToken: String? = null
        private set

    fun getCommentThreads(videoId: String, isLoadMore: Boolean = false) {
        viewModelScope.launch {
            try {
                if (!isLoadMore) {
                    _commentThreads.postValue(YoutubeResource.Loading)
                    currentCommentsList.clear()
                    commentNextPageToken = null
                }

                val response = youtubeRepository.getCommentThreads(videoId, pageToken = if (isLoadMore) commentNextPageToken else null)
                
                commentNextPageToken = response.nextPageToken
                response.items?.let { currentCommentsList.addAll(it) }

                if (currentCommentsList.isEmpty()) {
                    _commentThreads.postValue(YoutubeResource.Error(Exception("No comments found")))
                } else {
                    _commentThreads.postValue(YoutubeResource.Success(com.google.android.piyush.youtube.model.CommentThreads(items = currentCommentsList, nextPageToken = commentNextPageToken)))
                }
            } catch (exception: Exception) {
                _commentThreads.postValue(YoutubeResource.Error(exception))
            }
        }
    }

    fun getVideoDetails(videoId: String) {
        viewModelScope.launch {
            try {
                _videoDetails.postValue(YoutubeResource.Loading)
                val response = youtubeRepository.getVideoDetails(videoId)
                if (response.items.isNullOrEmpty()) {
                    _videoDetails.postValue(
                        YoutubeResource.Error(
                            Exception(
                                "No results found."
                            )
                        )
                    )
                } else {
                    _videoDetails.postValue(YoutubeResource.Success(response))
                }
            } catch (exception: Exception) {
                _videoDetails.postValue(YoutubeResource.Error(exception))
            }
        }
    }

    fun getChannelDetails(channelId: String) {
        viewModelScope.launch {
            try {
                _channelDetails.postValue(
                    YoutubeResource.Loading
                )
                val response = youtubeRepository.getChannelDetails(channelId)
                if (response.items.isNullOrEmpty()) {
                    _channelDetails.postValue(
                        YoutubeResource.Error(
                            Exception(
                                "The request cannot be completed because server unreachable !"
                            )
                        )
                    )
                } else {
                    _channelDetails.postValue(
                        YoutubeResource.Success(
                            response
                        )
                    )
                }
            } catch (exception: Exception) {
                _channelDetails.postValue(
                    YoutubeResource.Error(
                        exception
                    )
                )
            }
        }
    }

    fun getChannelsPlaylist(channelId: String) {
        viewModelScope.launch {
            try {
                _channelsPlaylists.postValue(
                    YoutubeResource.Loading
                )
                val response = youtubeRepository.getChannelsPlaylists(channelId)
                if (response.items.isNullOrEmpty()) {
                    _channelsPlaylists.postValue(
                        YoutubeResource.Error(
                            Exception(
                                "No results found."
                            )
                        )
                    )
                } else {
                    _channelsPlaylists.postValue(
                        YoutubeResource.Success(
                            response
                        )
                    )
                }
            } catch (exception: Exception) {
                _channelsPlaylists.postValue(
                    YoutubeResource.Error(
                        exception
                    )
                )
            }
        }
    }
}