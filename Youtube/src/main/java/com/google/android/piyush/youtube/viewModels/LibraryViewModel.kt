package com.google.android.piyush.youtube.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.piyush.youtube.model.Youtube
import com.google.android.piyush.youtube.repository.YoutubeRepositoryImpl
import com.google.android.piyush.youtube.utilities.NetworkResult
import com.google.android.piyush.youtube.utilities.YoutubeClient
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val youtubeRepositoryImpl: YoutubeRepositoryImpl
) : ViewModel() {

    private val _codingVideos = MutableLiveData<NetworkResult<Youtube>>()
    val codingVideos: LiveData<NetworkResult<Youtube>> = _codingVideos

    private val _sportsVideos = MutableLiveData<NetworkResult<Youtube>>()
    val sportsVideos: LiveData<NetworkResult<Youtube>> = _sportsVideos

    private val _technologyVideos = MutableLiveData<NetworkResult<Youtube>>()
    val technologyVideos: LiveData<NetworkResult<Youtube>> = _technologyVideos

    private val _reGetCodingVideos = MutableLiveData<NetworkResult<Youtube>>()
    val reGetCodingVideos: LiveData<NetworkResult<Youtube>> = _reGetCodingVideos

    private val _reGetSportsVideos = MutableLiveData<NetworkResult<Youtube>>()
    val reGetSportsVideos: LiveData<NetworkResult<Youtube>> = _reGetSportsVideos

    private val _reGetTechnologyVideos = MutableLiveData<NetworkResult<Youtube>>()
    val reGetTechnologyVideos: LiveData<NetworkResult<Youtube>> = _reGetTechnologyVideos

    init {
        getCodingVideos()
        getSportsVideos()
        getTechnologyVideos()
    }

    private fun getCodingVideos() = viewModelScope.launch {
        _codingVideos.postValue(NetworkResult.Loading)
        val response = youtubeRepositoryImpl.getLibraryVideos(YoutubeClient.CODING_VIDEOS)
        when (response) {
            is NetworkResult.Success -> {
                if (response.data.items.isNullOrEmpty()) {
                    _codingVideos.postValue(NetworkResult.Error(message = "No coding videos found."))
                } else {
                    _codingVideos.postValue(response)
                }
            }
            is NetworkResult.Error -> _codingVideos.postValue(response)
            is NetworkResult.Loading -> Unit
        }
    }

    private fun getSportsVideos() = viewModelScope.launch {
        _sportsVideos.postValue(NetworkResult.Loading)
        val response = youtubeRepositoryImpl.getLibraryVideos(YoutubeClient.SPORTS_VIDEOS)
        when (response) {
            is NetworkResult.Success -> {
                if (response.data.items.isNullOrEmpty()) {
                    _sportsVideos.postValue(NetworkResult.Error(message = "No sports videos found."))
                } else {
                    _sportsVideos.postValue(response)
                }
            }
            is NetworkResult.Error -> _sportsVideos.postValue(response)
            is NetworkResult.Loading -> Unit
        }
    }

    private fun getTechnologyVideos() = viewModelScope.launch {
        _technologyVideos.postValue(NetworkResult.Loading)
        val response = youtubeRepositoryImpl.getLibraryVideos(YoutubeClient.TECH_VIDEOS)
        when (response) {
            is NetworkResult.Success -> {
                if (response.data.items.isNullOrEmpty()) {
                    _technologyVideos.postValue(NetworkResult.Error(message = "No tech videos found."))
                } else {
                    _technologyVideos.postValue(response)
                }
            }
            is NetworkResult.Error -> _technologyVideos.postValue(response)
            is NetworkResult.Loading -> Unit
        }
    }

    fun reGetCodingVideos() = viewModelScope.launch {
        try {
            _reGetCodingVideos.postValue(NetworkResult.Loading)
            val response = youtubeRepositoryImpl.reGetLibraryVideos(YoutubeClient.CODING_VIDEOS)
            if (response.items.isNullOrEmpty()) {
                _reGetCodingVideos.postValue(NetworkResult.Error(message = "API quota exceeded."))
            } else {
                _reGetCodingVideos.postValue(NetworkResult.Success(response))
            }
        } catch (exception: Exception) {
            _reGetCodingVideos.postValue(NetworkResult.Error(message = exception.message ?: "Failed to load.", exception = exception))
        }
    }

    fun reGetSportsVideos() = viewModelScope.launch {
        try {
            _reGetSportsVideos.postValue(NetworkResult.Loading)
            val response = youtubeRepositoryImpl.reGetLibraryVideos(YoutubeClient.SPORTS_VIDEOS)
            if (response.items.isNullOrEmpty()) {
                _reGetSportsVideos.postValue(NetworkResult.Error(message = "API quota exceeded."))
            } else {
                _reGetSportsVideos.postValue(NetworkResult.Success(response))
            }
        } catch (exception: Exception) {
            _reGetSportsVideos.postValue(NetworkResult.Error(message = exception.message ?: "Failed to load.", exception = exception))
        }
    }

    fun reGetTechnologyVideos() = viewModelScope.launch {
        try {
            _reGetTechnologyVideos.postValue(NetworkResult.Loading)
            val response = youtubeRepositoryImpl.reGetLibraryVideos(YoutubeClient.TECH_VIDEOS)
            if (response.items.isNullOrEmpty()) {
                _reGetTechnologyVideos.postValue(NetworkResult.Error(message = "API quota exceeded."))
            } else {
                _reGetTechnologyVideos.postValue(NetworkResult.Success(response))
            }
        } catch (exception: Exception) {
            _reGetTechnologyVideos.postValue(NetworkResult.Error(message = exception.message ?: "Failed to load.", exception = exception))
        }
    }
}

@Suppress("UNCHECKED_CAST")
class LibraryViewModelFactory(
    private val youtubeRepositoryImpl: YoutubeRepositoryImpl
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            return LibraryViewModel(youtubeRepositoryImpl) as T
        } else {
            throw IllegalArgumentException("Unknown class name")
        }
    }
}
