package com.google.android.piyush.database.viewModel

import com.google.android.piyush.database.repository.DopamineDatabaseRepository
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.piyush.database.entities.CustomPlaylistEntity
import com.google.android.piyush.database.entities.CustomPlaylistVideoEntity
import com.google.android.piyush.database.entities.EntityFavouritePlaylist
import com.google.android.piyush.database.entities.EntityRecentVideos
import com.google.android.piyush.database.entities.EntityVideoSearch
import com.google.android.piyush.database.model.CustomPlaylistView
import com.google.android.piyush.database.model.CustomPlaylists
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DatabaseViewModel @Inject constructor(
    private val dopamineDatabaseRepository: DopamineDatabaseRepository
) : ViewModel() {

    private val _searchVideoHistory = MutableLiveData<List<EntityVideoSearch>>()
    val searchVideoHistory: LiveData<List<EntityVideoSearch>> = _searchVideoHistory

    private val _favouritePlayList = MutableLiveData<List<EntityFavouritePlaylist>>()
    val favouritePlayList: LiveData<List<EntityFavouritePlaylist>> = _favouritePlayList

    private val _isFavourite: MutableLiveData<String> = MutableLiveData()
    val isFavourite: LiveData<String> = _isFavourite

    private val _recentVideos: MutableLiveData<List<EntityRecentVideos>> = MutableLiveData()
    val recentVideos: LiveData<List<EntityRecentVideos>> = _recentVideos

    private val _isRecent: MutableLiveData<String> = MutableLiveData()
    val isRecent: LiveData<String> = _isRecent

    private val _subscriptions: MutableLiveData<List<com.google.android.piyush.database.entities.SubscriptionEntity>> = MutableLiveData()
    val subscriptions: LiveData<List<com.google.android.piyush.database.entities.SubscriptionEntity>> = _subscriptions

    private val _isSubscribed: MutableLiveData<Boolean> = MutableLiveData()
    val isSubscribed: LiveData<Boolean> = _isSubscribed

    // ── Search History ──

    fun insertSearchVideos(searchVideos: EntityVideoSearch) {
        viewModelScope.launch {
            dopamineDatabaseRepository.insertSearchVideos(searchVideos)
        }
    }

    fun deleteSearchVideoList() {
        viewModelScope.launch {
            dopamineDatabaseRepository.deleteSearchVideoList()
        }
    }

    fun getSearchVideoList() {
        viewModelScope.launch {
            _searchVideoHistory.value = dopamineDatabaseRepository.getSearchVideoList()
        }
    }

    // ── Favourite Videos ──

    fun insertFavouriteVideos(favouritePlaylist: EntityFavouritePlaylist) {
        viewModelScope.launch {
            dopamineDatabaseRepository.insertFavouriteVideos(favouritePlaylist)
        }
    }

    fun isFavouriteVideo(videoId: String) {
        viewModelScope.launch {
            _isFavourite.value = dopamineDatabaseRepository.isFavouriteVideo(videoId)
        }
    }

    fun deleteFavouriteVideo(videoId: String) {
        viewModelScope.launch {
            dopamineDatabaseRepository.deleteFavouriteVideo(videoId)
        }
    }

    fun getFavouritePlayList() {
        viewModelScope.launch {
            _favouritePlayList.value = dopamineDatabaseRepository.getFavouritePlayList()
        }
    }

    // ── Recent Videos ──

    fun insertRecentVideos(recentVideos: EntityRecentVideos) {
        viewModelScope.launch {
            dopamineDatabaseRepository.insertRecentVideos(recentVideos)
        }
    }

    fun getRecentVideos() {
        viewModelScope.launch {
            _recentVideos.value = dopamineDatabaseRepository.getRecentVideos()
        }
    }

    fun isRecentVideo(videoId: String) {
        viewModelScope.launch {
            _isRecent.value = dopamineDatabaseRepository.isRecentVideo(videoId)
        }
    }

    fun updateRecentVideo(videoId: String, time: String) {
        viewModelScope.launch {
            dopamineDatabaseRepository.updateRecentVideo(videoId, time)
        }
    }

    fun deleteRecentVideo() {
        viewModelScope.launch {
            dopamineDatabaseRepository.deleteRecentVideo()
        }
    }

    // ── Subscriptions ──

    fun insertSubscription(subscription: com.google.android.piyush.database.entities.SubscriptionEntity) {
        viewModelScope.launch {
            dopamineDatabaseRepository.insertSubscription(subscription)
            getAllSubscriptions()
        }
    }

    fun deleteSubscription(channelId: String) {
        viewModelScope.launch {
            dopamineDatabaseRepository.deleteSubscription(channelId)
            getAllSubscriptions()
        }
    }

    fun getAllSubscriptions() {
        viewModelScope.launch {
            _subscriptions.value = dopamineDatabaseRepository.getAllSubscriptions()
        }
    }

    fun checkIsSubscribed(channelId: String) {
        viewModelScope.launch {
            _isSubscribed.value = dopamineDatabaseRepository.isSubscribed(channelId)
        }
    }

    // ── Custom Playlists (Room DAO) ──

    suspend fun createCustomPlaylist(playlistsData: CustomPlaylistView) {
        dopamineDatabaseRepository.createPlaylist(
            CustomPlaylistEntity(
                playlistName = playlistsData.playListName,
                playlistDescription = playlistsData.playListDescription
            )
        )
    }

    suspend fun addItemsInCustomPlaylist(playlistName: String, playlistsData: CustomPlaylists) {
        dopamineDatabaseRepository.addVideoToPlaylist(
            CustomPlaylistVideoEntity(
                playlistName = playlistName,
                videoId = playlistsData.videoId,
                title = playlistsData.title,
                thumbnail = playlistsData.thumbnail,
                channelId = playlistsData.channelId,
                publishedAt = playlistsData.publishedAt,
                viewCount = playlistsData.viewCount,
                channelTitle = playlistsData.channelTitle,
                duration = playlistsData.duration
            )
        )
    }

    suspend fun userFromPhoneAuth() {
        dopamineDatabaseRepository.createPlaylist(
            CustomPlaylistEntity(
                playlistName = "dopaminePlaylist",
                playlistDescription = "You can store your favorite videos here"
            )
        )
    }

    suspend fun getPlaylist(): List<CustomPlaylistView> {
        return dopamineDatabaseRepository.getAllPlaylists().map { entity ->
            CustomPlaylistView(
                playListName = entity.playlistName,
                playListDescription = entity.playlistDescription
            )
        }
    }

    suspend fun getPlaylistsFromDatabase(): List<String> {
        return dopamineDatabaseRepository.getAllPlaylists().map { it.playlistName }
    }

    suspend fun isExistsDataInPlaylist(playlistName: String, videoId: String): Boolean {
        return dopamineDatabaseRepository.isVideoInPlaylist(playlistName, videoId)
    }

    suspend fun countTheNumberOfCustomPlaylist(): Int {
        return dopamineDatabaseRepository.getPlaylistCount()
    }

    suspend fun deleteVideoFromPlaylist(playlistName: String, videoId: String) {
        dopamineDatabaseRepository.deleteVideoFromPlaylist(playlistName, videoId)
    }

    suspend fun isPlaylistExist(playlistName: String): Boolean {
        return dopamineDatabaseRepository.isPlaylistExist(playlistName)
    }

    suspend fun getPlaylistData(playlistName: String): List<CustomPlaylists> {
        return dopamineDatabaseRepository.getPlaylistVideos(playlistName).map { entity ->
            CustomPlaylists(
                videoId = entity.videoId,
                title = entity.title,
                thumbnail = entity.thumbnail,
                channelId = entity.channelId,
                publishedAt = entity.publishedAt,
                viewCount = entity.viewCount,
                channelTitle = entity.channelTitle,
                duration = entity.duration
            )
        }
    }
}