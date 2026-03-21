package com.google.android.piyush.dopamine.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.piyush.youtube.model.channelDetails.YoutubeChannel
import com.google.android.piyush.youtube.model.channelPlaylists.ChannelPlaylists
import com.google.android.piyush.youtube.repository.YoutubeRepositoryImpl
import com.google.android.piyush.youtube.utilities.NetworkResult
import kotlinx.coroutines.launch

class YoutubeChannelViewModel(
    private val youtubeRepositoryImpl: YoutubeRepositoryImpl
) : ViewModel() {

    private val _channelDetails = MutableLiveData<NetworkResult<YoutubeChannel>>()
    val channelDetails: MutableLiveData<NetworkResult<YoutubeChannel>> = _channelDetails

    private val _channelsPlaylists = MutableLiveData<NetworkResult<ChannelPlaylists>>()
    val channelsPlaylists: MutableLiveData<NetworkResult<ChannelPlaylists>> = _channelsPlaylists

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
}

@Suppress("UNCHECKED_CAST")
class YoutubeChannelViewModelFactory(
    private val youtubeRepositoryImpl: YoutubeRepositoryImpl
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(YoutubeChannelViewModel::class.java)) {
            return YoutubeChannelViewModel(youtubeRepositoryImpl) as T
        } else {
            throw IllegalArgumentException("Unknown ViewModel Class")
        }
    }
}
