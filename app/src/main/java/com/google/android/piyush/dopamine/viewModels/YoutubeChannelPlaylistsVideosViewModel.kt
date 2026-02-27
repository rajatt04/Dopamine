package com.google.android.piyush.dopamine.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.piyush.youtube.model.Youtube
import com.google.android.piyush.youtube.repository.YoutubeRepository
import com.google.android.piyush.youtube.utilities.YoutubeResource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class YoutubeChannelPlaylistsVideosViewModel @Inject constructor(
    private val youtubeRepository: YoutubeRepository
) : ViewModel() {

    private val _playlistsVideos  : MutableLiveData<YoutubeResource<Youtube>> = MutableLiveData()
    val playlistsVideos : MutableLiveData<YoutubeResource<Youtube>> = _playlistsVideos

    fun getPlaylistsVideos(channelId: String) {
        viewModelScope.launch {
            try {
                _playlistsVideos.postValue(YoutubeResource.Loading)
                val response = youtubeRepository.getPlaylistVideos(channelId)
                if(response.items.isNullOrEmpty()) {
                    _playlistsVideos.postValue(
                        YoutubeResource.Error(
                            Exception(
                                "No results found."
                            )
                        )
                    )
                } else {
                    _playlistsVideos.postValue(
                        YoutubeResource.Success(response)
                    )
                }
            }catch (exception: Exception) {
                _playlistsVideos.postValue(
                    YoutubeResource.Error(exception)
                )
            }
        }
    }
}