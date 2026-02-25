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
data class Developer(
    val userId : String? = null,
    val userName : String? = null,
    val userDesignation : String? = null,
    val userImage : String? = null,
    val userBanner : String? = null,
    val userEmail : String? = null,
    val userAbout : String? = null,
    val userLocation : String? = null,
    val userPhotos : List<Photos>? = null
)

@Serializable
data class Photos(
    val photo : String? = null
)

class DevelopersViewModel : ViewModel() {

    private val _devModel : MutableLiveData<YoutubeResource<List<Developer>>> = MutableLiveData()
    val devModel : MutableLiveData<YoutubeResource<List<Developer>>> = _devModel

    init {
        viewModelScope.launch {
            try {
                _devModel.postValue(YoutubeResource.Loading)
                val response = YoutubeClient.CLIENT.get(
                    YoutubeClient.DEVELOPER
                ).body<List<Developer>>()
                if(response.isNotEmpty()){
                    _devModel.postValue(YoutubeResource.Success(response))
                }else{
                    _devModel.postValue(YoutubeResource.Error(Exception("Code 521 : Web server is down")))
                }
            }catch (exception : Exception){
                _devModel.postValue(YoutubeResource.Error(exception))
            }
        }
    }
}
