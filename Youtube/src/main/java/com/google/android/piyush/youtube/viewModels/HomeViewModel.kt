package com.google.android.piyush.youtube.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.piyush.youtube.model.Youtube
import com.google.android.piyush.youtube.repository.YoutubeRepositoryImpl
import com.google.android.piyush.youtube.utilities.YoutubeResource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val youtubeRepositoryImpl: YoutubeRepositoryImpl
) : ViewModel() {

    private val _videos : MutableLiveData<YoutubeResource<Youtube>> = MutableLiveData()
    val videos : LiveData<YoutubeResource<Youtube>> = _videos

    private val _reGetVideos : MutableLiveData<YoutubeResource<Youtube>> = MutableLiveData()
    val reGetVideos : LiveData<YoutubeResource<Youtube>> = _reGetVideos

    init {
        fetchHomeVideos()
    }

    fun fetchHomeVideos(categoryId: String? = null) = viewModelScope.launch {
        try {
            _videos.postValue(
                YoutubeResource.Loading
            )
            val response = youtubeRepositoryImpl.getHomeVideos(categoryId)
            if(response.items.isNullOrEmpty()){
                _videos.postValue(
                    YoutubeResource.Error(
                        Exception(
                            "The request cannot be completed because you have exceeded your quota."
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
        }catch (exception : Exception){
            _videos.postValue(
                YoutubeResource.Error(
                    exception = exception
                )
            )
            exception.printStackTrace()
        }
    }

    fun reGetHomeVideos() {
        viewModelScope.launch {
            try {
                _reGetVideos.postValue(
                    YoutubeResource.Loading
                )
                val response = youtubeRepositoryImpl.reGetHomeVideos()
                if(response.items.isNullOrEmpty()) {
                    _reGetVideos.postValue(
                        YoutubeResource.Error(
                            Exception(
                                "The request cannot be completed because you have exceeded your quota."
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
            }catch (exception : Exception){
                _reGetVideos.postValue(
                    YoutubeResource.Error(
                        exception = exception
                    )
                )
            }
        }
    }
}