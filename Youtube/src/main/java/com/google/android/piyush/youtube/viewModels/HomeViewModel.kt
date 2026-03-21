package com.google.android.piyush.youtube.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.piyush.youtube.model.Youtube
import com.google.android.piyush.youtube.repository.YoutubeRepositoryImpl
import com.google.android.piyush.youtube.utilities.NetworkResult
import kotlinx.coroutines.launch

class HomeViewModel(
    private val youtubeRepositoryImpl: YoutubeRepositoryImpl
) : ViewModel() {

    private val _videos = MutableLiveData<NetworkResult<Youtube>>()
    val videos: LiveData<NetworkResult<Youtube>> = _videos

    private val _reGetVideos = MutableLiveData<NetworkResult<Youtube>>()
    val reGetVideos: LiveData<NetworkResult<Youtube>> = _reGetVideos

    init {
        getHomeVideos()
    }

    private fun getHomeVideos() = viewModelScope.launch {
        _videos.postValue(NetworkResult.Loading)
        val response = youtubeRepositoryImpl.getHomeVideos()
        when (response) {
            is NetworkResult.Success -> {
                if (response.data.items.isNullOrEmpty()) {
                    _videos.postValue(NetworkResult.Error(message = "No videos found."))
                } else {
                    _videos.postValue(response)
                }
            }
            is NetworkResult.Error -> {
                _videos.postValue(response)
            }
            is NetworkResult.Loading -> Unit
        }
    }

    fun reGetHomeVideos() {
        viewModelScope.launch {
            _reGetVideos.postValue(NetworkResult.Loading)
            try {
                val response = youtubeRepositoryImpl.reGetHomeVideos()
                if (response.items.isNullOrEmpty()) {
                    _reGetVideos.postValue(NetworkResult.Error(message = "API quota exceeded. Please try again later."))
                } else {
                    _reGetVideos.postValue(NetworkResult.Success(response))
                }
            } catch (exception: Exception) {
                _reGetVideos.postValue(
                    NetworkResult.Error(
                        message = exception.message ?: "Failed to load videos.",
                        exception = exception
                    )
                )
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
class HomeViewModelFactory(
    private val repository: YoutubeRepositoryImpl
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown class name")
    }
}
