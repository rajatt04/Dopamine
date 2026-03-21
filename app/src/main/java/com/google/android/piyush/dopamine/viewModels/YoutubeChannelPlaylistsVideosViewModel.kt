package com.google.android.piyush.dopamine.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.piyush.youtube.model.Youtube
import com.google.android.piyush.youtube.repository.YoutubeRepositoryImpl
import com.google.android.piyush.youtube.utilities.NetworkResult
import kotlinx.coroutines.launch

class YoutubeChannelPlaylistsVideosViewModel(
    private val youtubeRepositoryImpl: YoutubeRepositoryImpl
) : ViewModel() {

    private val _playlistsVideos = MutableLiveData<NetworkResult<Youtube>>()
    val playlistsVideos: MutableLiveData<NetworkResult<Youtube>> = _playlistsVideos

    fun getPlaylistsVideos(playlistId: String) {
        viewModelScope.launch {
            _playlistsVideos.postValue(NetworkResult.Loading)
            val response = youtubeRepositoryImpl.getPlaylistVideos(playlistId)
            when (response) {
                is NetworkResult.Success -> {
                    if (response.data.items.isNullOrEmpty()) {
                        _playlistsVideos.postValue(NetworkResult.Error(message = "No videos in this playlist."))
                    } else {
                        _playlistsVideos.postValue(response)
                    }
                }
                is NetworkResult.Error -> _playlistsVideos.postValue(response)
                is NetworkResult.Loading -> Unit
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
class YoutubeChannelPlaylistsViewModelFactory(
    private val youtubeRepositoryImpl: YoutubeRepositoryImpl
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(YoutubeChannelPlaylistsVideosViewModel::class.java)) {
            return YoutubeChannelPlaylistsVideosViewModel(youtubeRepositoryImpl) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
