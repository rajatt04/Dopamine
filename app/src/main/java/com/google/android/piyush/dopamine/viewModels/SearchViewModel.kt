package com.google.android.piyush.dopamine.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.piyush.youtube.model.SearchTube
import com.google.android.piyush.youtube.repository.YoutubeRepository
import com.google.android.piyush.youtube.utilities.YoutubeResource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val youtubeRepository: YoutubeRepository
) : ViewModel() {

    private val _searchVideos : MutableLiveData<YoutubeResource<SearchTube>> = MutableLiveData()
    val searchVideos : MutableLiveData<YoutubeResource<SearchTube>> = _searchVideos

    private val _reGetSearchVideos : MutableLiveData<YoutubeResource<SearchTube>> = MutableLiveData()
    val reGetSearchVideos : MutableLiveData<YoutubeResource<SearchTube>> = _reGetSearchVideos

    fun searchVideos(query : String) {
        viewModelScope.launch {
            try {
                _searchVideos.postValue(YoutubeResource.Loading)
                val videos = youtubeRepository.getSearchVideos(query)
                if(videos.items.isNullOrEmpty()){
                    _searchVideos.postValue(
                        YoutubeResource.Error(
                            Exception(
                                "No results found."
                            )
                        )
                    )
                } else {
                    _searchVideos.postValue(YoutubeResource.Success(videos))
                }
            } catch (e : Exception) {
                _searchVideos.postValue(YoutubeResource.Error(e))
            }
        }
    }

    fun reSearchVideos(query : String) {
        viewModelScope.launch {
            try {
                _reGetSearchVideos.postValue(YoutubeResource.Loading)
                val videos = youtubeRepository.getSearchVideos(query, useExtraKey = true)
                if(videos.items.isNullOrEmpty()){
                    _reGetSearchVideos.postValue(
                        YoutubeResource.Error(
                            Exception(
                                "No results found."
                            )
                        )
                    )
                } else {
                    _reGetSearchVideos.postValue(YoutubeResource.Success(videos))
                }
            } catch (e : Exception) {
                _reGetSearchVideos.postValue(YoutubeResource.Error(e))
            }
        }
    }
}