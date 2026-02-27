package com.google.android.piyush.dopamine.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.piyush.youtube.model.channelDetails.YoutubeChannel
import com.google.android.piyush.youtube.model.channelPlaylists.ChannelPlaylists
import com.google.android.piyush.youtube.repository.YoutubeRepository
import com.google.android.piyush.youtube.utilities.YoutubeResource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class YoutubeChannelViewModel @Inject constructor(
    private val youtubeRepository: YoutubeRepository
) : ViewModel() {

    private val _channelDetails : MutableLiveData<YoutubeResource<YoutubeChannel>> = MutableLiveData()
    val channelDetails : MutableLiveData<YoutubeResource<YoutubeChannel>> = _channelDetails

    private val _channelsPlaylists : MutableLiveData<YoutubeResource<ChannelPlaylists>> = MutableLiveData()
    val channelsPlaylists : MutableLiveData<YoutubeResource<ChannelPlaylists>> = _channelsPlaylists

    fun getChannelDetails(channelId : String) {
       viewModelScope.launch {
           try {
               _channelDetails.postValue(
                   YoutubeResource.Loading
               )
               val response = youtubeRepository.getChannelDetails(channelId)
               if(response.items.isNullOrEmpty()) {
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
           }catch (exception : Exception) {
               _channelDetails.postValue(
                   YoutubeResource.Error(
                       exception
                   )
               )
           }
       }
    }

    fun getChannelsPlaylist(channelId : String) {
        viewModelScope.launch {
            try{
                _channelsPlaylists.postValue(
                    YoutubeResource.Loading
                )
                val response = youtubeRepository.getChannelsPlaylists(channelId)
                if(response.items.isNullOrEmpty()) {
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
            }catch (exception : Exception) {
                _channelsPlaylists.postValue(
                    YoutubeResource.Error(
                        exception
                    )
                )
            }
        }
    }
}