package com.google.android.piyush.youtube.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.piyush.youtube.model.Shorts
import com.google.android.piyush.youtube.repository.YoutubeRepositoryImpl
import com.google.android.piyush.youtube.utilities.YoutubeResource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShortsViewModel @Inject constructor(
    private val youtubeRepositoryImpl: YoutubeRepositoryImpl
) : ViewModel() {

    private val _shorts : MutableLiveData<YoutubeResource<List<Shorts>>> = MutableLiveData()
    val shorts : LiveData<YoutubeResource<List<Shorts>>> = _shorts

    init {
        viewModelScope.launch {
            try {
                _shorts.postValue(YoutubeResource.Loading)
                val response = youtubeRepositoryImpl.getYoutubeShorts()
                if(response.isEmpty()){
                    _shorts.postValue(
                        YoutubeResource.Error(
                            Exception("No Shorts Found")
                        )
                    )
                }else{
                    _shorts.postValue(YoutubeResource.Success(response))
                }
            }catch (e : Exception){
                _shorts.postValue(YoutubeResource.Error(e))
            }
        }
    }
}
