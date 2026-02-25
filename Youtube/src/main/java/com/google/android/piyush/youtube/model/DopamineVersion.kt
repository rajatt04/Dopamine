package com.google.android.piyush.youtube.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.piyush.youtube.utilities.YoutubeClient
import com.google.android.piyush.youtube.utilities.YoutubeResource
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class DopamineVersion(
    val versionName : String? = null,
    val url : String? = null,
    val changelog : String? = null
)

class DopamineVersionViewModel : ViewModel() {
    private val _update : MutableLiveData<YoutubeResource<DopamineVersion>> = MutableLiveData()
    val update : MutableLiveData<YoutubeResource<DopamineVersion>> = _update

    private val _preRelease : MutableLiveData<YoutubeResource<DopamineVersion>> = MutableLiveData()
    val preRelease : MutableLiveData<YoutubeResource<DopamineVersion>> = _preRelease

    init{
        try {
            viewModelScope.launch {
                _update.postValue(YoutubeResource.Loading)
                _update.postValue(
                    YoutubeResource.Success(
                        YoutubeClient.CLIENT.get(
                            YoutubeClient.DOPAMINE_UPDATE
                        ).body()
                    )
                )
            }
        }catch (e : Exception){
            _update.postValue(YoutubeResource.Error(e))
        }
    }

    fun preReleaseUpdate() {
        viewModelScope.launch {
            try {
                _preRelease.postValue(YoutubeResource.Loading)
                _preRelease.postValue(
                    YoutubeResource.Success(
                        YoutubeClient.CLIENT.get(
                            YoutubeClient.PRE_RELEASE
                        ).body()
                    )
                )
            }catch (e : Exception){
                _preRelease.postValue(YoutubeResource.Error(e))
            }
        }
    }
}
