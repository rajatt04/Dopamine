package com.google.android.piyush.dopamine.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.piyush.youtube.model.SearchTube
import com.google.android.piyush.youtube.repository.YoutubeRepositoryImpl
import com.google.android.piyush.youtube.utilities.YoutubeResource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val youtubeRepositoryImpl: YoutubeRepositoryImpl
) : ViewModel() {

    private val _searchVideos : MutableLiveData<YoutubeResource<SearchTube>> = MutableLiveData()
    val searchVideos : MutableLiveData<YoutubeResource<SearchTube>> = _searchVideos

    private val _reGetSearchVideos : MutableLiveData<YoutubeResource<SearchTube>> = MutableLiveData()
    val reGetSearchVideos : MutableLiveData<YoutubeResource<SearchTube>> = _reGetSearchVideos

    private val _searchSuggestions : MutableLiveData<List<String>> = MutableLiveData()
    val searchSuggestions : LiveData<List<String>> = _searchSuggestions

    fun fetchSearchSuggestions(query: String) {
        if (query.isBlank()) {
            _searchSuggestions.postValue(emptyList())
            return
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = java.net.URL("http://suggestqueries.google.com/complete/search?client=firefox&ds=yt&q=${java.net.URLEncoder.encode(query, "UTF-8")}")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = org.json.JSONArray(response)
                    if (jsonArray.length() > 1) {
                        val suggestionsArray = jsonArray.getJSONArray(1)
                        val suggestions = mutableListOf<String>()
                        for (i in 0 until suggestionsArray.length()) {
                            suggestions.add(suggestionsArray.getString(i))
                        }
                        _searchSuggestions.postValue(suggestions)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun searchVideos(query : String) {
        viewModelScope.launch {
            try {
                _searchVideos.postValue(YoutubeResource.Loading)
                val videos = youtubeRepositoryImpl.getSearchVideos(query)
                if(videos.items.isNullOrEmpty()){
                    _searchVideos.postValue(
                        YoutubeResource.Error(
                            Exception(
                                "The request cannot be completed because you have exceeded your quota."
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
                val videos = youtubeRepositoryImpl.reGetSearchVideos(query)
                if(videos.items.isNullOrEmpty()){
                    _reGetSearchVideos.postValue(
                        YoutubeResource.Error(
                            Exception(
                                "The request cannot be completed because you have exceeded your quota."
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