package com.google.android.piyush.dopamine.viewModels

import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    private val _currentVideo = androidx.lifecycle.MutableLiveData<SelectedVideo?>()
    val currentVideo: androidx.lifecycle.LiveData<SelectedVideo?> = _currentVideo

    fun selectVideo(video: SelectedVideo) {
        _currentVideo.value = video
    }

    // Helper to clear video selection or collapse
    fun closePlayer() {
        _currentVideo.value = null
    }
}

data class SelectedVideo(
    val videoId: String,
    val channelId: String,
    val title: String? = null,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val channelTitle: String? = null
)