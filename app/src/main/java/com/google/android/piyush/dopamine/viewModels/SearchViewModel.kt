package com.google.android.piyush.dopamine.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.android.piyush.youtube.model.SearchTube
import com.google.android.piyush.youtube.model.SearchTubeItems
import com.google.android.piyush.youtube.paging.SearchPagingSource
import com.google.android.piyush.youtube.repository.YoutubeRepositoryImpl
import com.google.android.piyush.youtube.utilities.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SearchViewModel(
    private val youtubeRepositoryImpl: YoutubeRepositoryImpl
) : ViewModel() {

    private val _searchVideos = MutableLiveData<NetworkResult<SearchTube>>()
    val searchVideos: MutableLiveData<NetworkResult<SearchTube>> = _searchVideos

    private val _reGetSearchVideos = MutableLiveData<NetworkResult<SearchTube>>()
    val reGetSearchVideos: MutableLiveData<NetworkResult<SearchTube>> = _reGetSearchVideos

    private var currentQuery: String? = null
    private var currentPagingFlow: Flow<PagingData<SearchTubeItems>>? = null

    fun searchVideosPaged(
        query: String,
        order: String = "relevance",
        type: String? = null,
        videoDuration: String? = null
    ): Flow<PagingData<SearchTubeItems>> {
        val lastResult = currentPagingFlow
        if (query == currentQuery && lastResult != null) {
            return lastResult
        }
        currentQuery = query
        val newResult = Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 5
            ),
            pagingSourceFactory = {
                SearchPagingSource(
                    repository = youtubeRepositoryImpl,
                    query = query,
                    order = order,
                    type = type,
                    videoDuration = videoDuration
                )
            }
        ).flow.cachedIn(viewModelScope)
        currentPagingFlow = newResult
        return newResult
    }

    fun searchVideos(
        query: String,
        order: String = "relevance",
        type: String? = null,
        videoDuration: String? = null
    ) {
        viewModelScope.launch {
            _searchVideos.postValue(NetworkResult.Loading)
            val response = youtubeRepositoryImpl.getSearchVideos(
                query = query,
                order = order,
                type = type,
                videoDuration = videoDuration
            )
            when (response) {
                is NetworkResult.Success -> {
                    if (response.data.items.isNullOrEmpty()) {
                        _searchVideos.postValue(NetworkResult.Error(message = "No results found for \"$query\"."))
                    } else {
                        _searchVideos.postValue(response)
                    }
                }
                is NetworkResult.Error -> _searchVideos.postValue(response)
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun reSearchVideos(query: String) {
        viewModelScope.launch {
            try {
                _reGetSearchVideos.postValue(NetworkResult.Loading)
                val videos = youtubeRepositoryImpl.reGetSearchVideos(query)
                if (videos.items.isNullOrEmpty()) {
                    _reGetSearchVideos.postValue(NetworkResult.Error(message = "API quota exceeded. Please try again later."))
                } else {
                    _reGetSearchVideos.postValue(NetworkResult.Success(videos))
                }
            } catch (e: Exception) {
                _reGetSearchVideos.postValue(
                    NetworkResult.Error(message = e.message ?: "Search failed.", exception = e)
                )
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
class SearchViewModelFactory(
    private val youtubeRepositoryImpl: YoutubeRepositoryImpl
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            return SearchViewModel(youtubeRepositoryImpl) as T
        } else {
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
