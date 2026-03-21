package com.google.android.piyush.youtube.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.piyush.youtube.model.Shorts
import com.google.android.piyush.youtube.repository.YoutubeRepositoryImpl
import com.google.android.piyush.youtube.utilities.NetworkResult
import kotlinx.coroutines.launch

class ShortsViewModel(
    private val youtubeRepositoryImpl: YoutubeRepositoryImpl
) : ViewModel() {

    private val _shorts = MutableLiveData<NetworkResult<List<Shorts>>>()
    val shorts: LiveData<NetworkResult<List<Shorts>>> = _shorts

    init {
        viewModelScope.launch {
            _shorts.postValue(NetworkResult.Loading)
            val response = youtubeRepositoryImpl.getYoutubeShorts()
            when (response) {
                is NetworkResult.Success -> {
                    if (response.data.isEmpty()) {
                        _shorts.postValue(NetworkResult.Error(message = "No shorts found."))
                    } else {
                        _shorts.postValue(response)
                    }
                }
                is NetworkResult.Error -> _shorts.postValue(response)
                is NetworkResult.Loading -> Unit
            }
        }
    }
}

class ShortsViewModelFactory(
    private val youtubeRepositoryImpl: YoutubeRepositoryImpl
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShortsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShortsViewModel(youtubeRepositoryImpl) as T
        } else {
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
