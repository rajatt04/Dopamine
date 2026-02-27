package com.google.android.piyush.youtube.viewModels

import androidx.lifecycle.LiveData
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
class HomeViewModel @Inject constructor(
    private val youtubeRepository: YoutubeRepository
) : ViewModel() {

    private val _videos : MutableLiveData<YoutubeResource<Youtube>> = MutableLiveData()
    val videos : LiveData<YoutubeResource<Youtube>> = _videos

    private val _reGetVideos : MutableLiveData<YoutubeResource<Youtube>> = MutableLiveData()
    val reGetVideos : LiveData<YoutubeResource<Youtube>> = _reGetVideos

    init {
        getHomeVideos()
    }

    private fun getHomeVideos() = viewModelScope.launch {
        try {
            _videos.postValue(
                YoutubeResource.Loading
            )
            val response = youtubeRepository.getHomeVideos()
            if(response.items.isNullOrEmpty()){
                _videos.postValue(
                    YoutubeResource.Error(
                        Exception(
                            "No results found."
                        )
                    )
                )
            }else{
                _videos.postValue(
                    YoutubeResource.Success(
                        response
                    )
                )
            }

        } catch (e: Exception) {
            _videos.postValue(YoutubeResource.Error(e))
        }

    }

     fun reGetHomeVideos() = viewModelScope.launch {
        try {
            _reGetVideos.postValue(
                YoutubeResource.Loading
            )
            val response = youtubeRepository.getHomeVideos(useExtraKey = true)
            if(response.items.isNullOrEmpty()){
                _reGetVideos.postValue(
                    YoutubeResource.Error(
                        Exception(
                            "No results found."
                        )
                    )
                )
            }else{
                _reGetVideos.postValue(
                    YoutubeResource.Success(
                        response
                    )
                )
            }

        } catch (e: Exception) {
            _reGetVideos.postValue(YoutubeResource.Error(e))
        }

    }
}