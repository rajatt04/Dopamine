package com.google.android.piyush.database.viewModel

import com.google.android.piyush.database.repository.DopamineDatabaseRepository
import android.content.ContentValues
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.piyush.database.DopamineDatabase
import com.google.android.piyush.database.entities.EntityFavouritePlaylist
import com.google.android.piyush.database.entities.EntityRecentVideos
import com.google.android.piyush.database.entities.EntityVideoSearch
import com.google.android.piyush.database.model.CustomPlaylistView
import com.google.android.piyush.database.model.CustomPlaylists
import kotlinx.coroutines.launch

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class DatabaseViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val dopamineDatabaseRepository : DopamineDatabaseRepository
    private val database = DopamineDatabase.getDatabase(application).openHelper

    private val _searchVideoHistory = MutableLiveData<List<EntityVideoSearch>>()
    val searchVideoHistory : LiveData<List<EntityVideoSearch>> = _searchVideoHistory

    private val _favouritePlayList = MutableLiveData<List<EntityFavouritePlaylist>>()
    val favouritePlayList : LiveData<List<EntityFavouritePlaylist>> = _favouritePlayList

    private val _isFavourite : MutableLiveData<String> = MutableLiveData()
    val isFavourite : LiveData<String> = _isFavourite

    private val _recentVideos : MutableLiveData<List<EntityRecentVideos>> = MutableLiveData()
    val recentVideos : LiveData<List<EntityRecentVideos>> = _recentVideos

    private val _isRecent : MutableLiveData<String> = MutableLiveData()
    val isRecent : LiveData<String> = _isRecent

    private val _subscriptions : MutableLiveData<List<com.google.android.piyush.database.entities.SubscriptionEntity>> = MutableLiveData()
    val subscriptions : LiveData<List<com.google.android.piyush.database.entities.SubscriptionEntity>> = _subscriptions

    private val _isSubscribed : MutableLiveData<Boolean> = MutableLiveData()
    val isSubscribed : LiveData<Boolean> = _isSubscribed


    init {
        val database = DopamineDatabase.getDatabase(getApplication())
        val dopamineDao = database.dopamineDao()
        val subscriptionDao = database.subscriptionDao()
        dopamineDatabaseRepository = DopamineDatabaseRepository(dopamineDao, subscriptionDao)
    }

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

    fun updateRecentVideo(videoId: String,time : String) {
        viewModelScope.launch {
            dopamineDatabaseRepository.updateRecentVideo(videoId,time)
        }
    }

    fun deleteRecentVideo() {
        viewModelScope.launch {
            dopamineDatabaseRepository.deleteRecentVideo()
        }
    }

    // Subscription Functions
    fun insertSubscription(subscription: com.google.android.piyush.database.entities.SubscriptionEntity) {
        viewModelScope.launch {
            dopamineDatabaseRepository.insertSubscription(subscription)
            // Refresh list
            getAllSubscriptions()
        }
    }

    fun deleteSubscription(channelId: String) {
        viewModelScope.launch {
            dopamineDatabaseRepository.deleteSubscription(channelId)
            // Refresh list
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

    //Custom Playlist
    val defaultMasterDev = database.writableDatabase.execSQL("CREATE TABLE IF NOT EXISTS DopamineMastersDev (playlistName TEXT PRIMARY KEY, playlistDescription TEXT)")
    fun createCustomPlaylist(playlistsData: CustomPlaylistView) {
        val writableDatabase = database.writableDatabase
        val newPlaylistName = sanitizeTableName(playlistsData.playListName)
        writableDatabase.execSQL("CREATE TABLE IF NOT EXISTS DopamineMastersDev (playlistName TEXT PRIMARY KEY, playlistDescription TEXT)")
        val values = ContentValues().apply {
            put("playlistName", sanitizeTableName(playlistsData.playListName))
            put("playlistDescription", playlistsData.playListDescription)
        }
        writableDatabase.insert("DopamineMastersDev", 1, values)
        val query = "CREATE TABLE IF NOT EXISTS $newPlaylistName (videoId TEXT PRIMARY KEY, title TEXT, thumbnail TEXT, channelId TEXT, publishedAt TEXT, viewCount TEXT, channelTitle TEXT, duration TEXT)"
        writableDatabase.execSQL(query)
    }

    fun addItemsInCustomPlaylist(playlistName: String, playlistsData: CustomPlaylists) {
        val writableDatabase = database.writableDatabase
        val newPlaylistName = sanitizeTableName(playlistName)
        val values = ContentValues().apply {
            put("videoId", playlistsData.videoId)
            put("title", playlistsData.title?.replace('"', ' '))
            put("thumbnail", playlistsData.thumbnail)
            put("channelId", playlistsData.channelId)
            put("publishedAt", playlistsData.publishedAt)
            put("viewCount", playlistsData.viewCount)
            put("channelTitle", playlistsData.channelTitle)
            put("duration", playlistsData.duration)
        }
        writableDatabase.insert(newPlaylistName, 1, values)
    }

    fun userFromPhoneAuth() {
        val usersFavoritePlayListDescription = "You can store your favorite videos here"
        val writableDatabase = database.writableDatabase
        writableDatabase.execSQL("CREATE TABLE IF NOT EXISTS dopaminePlaylist (videoId TEXT PRIMARY KEY, title TEXT, thumbnail TEXT, channelId TEXT, publishedAt TEXT, viewCount TEXT, channelTitle TEXT, duration TEXT)")
        val values = ContentValues().apply {
            put("playlistName", "dopaminePlaylist")
            put("playlistDescription", usersFavoritePlayListDescription)
        }
        writableDatabase.insert("DopamineMastersDev", 1, values)
    }

    fun getPlaylist(): List<CustomPlaylistView> {
        val writableDatabase = database.writableDatabase
        val list = mutableListOf<CustomPlaylistView>()
        writableDatabase.query("SELECT * FROM DopamineMastersDev ORDER BY playlistName ASC").use { data ->
            while (data.moveToNext()) {
                list.add(
                    CustomPlaylistView(
                        displayName(data.getString(0)),
                        data.getString(1)
                    )
                )
            }
        }
        Log.d(ContentValues.TAG, " -> viewModel : Database || GetPlaylist : $list")
        return list
    }

    fun getPlaylistsFromDatabase(): List<String> {
        val writableDatabase = database.writableDatabase
        val list = mutableListOf<String>()
        val excludedTables = setOf(
            "android_metadata", "recent_videos", "room_master_table",
            "sqlite_sequence", "search_table", "favorite_playlist",
            "DopamineMastersDev", "subscription_table"
        )
        writableDatabase.query("SELECT name FROM sqlite_master WHERE type='table'").use { data ->
            while (data.moveToNext()) {
                val tableName = data.getString(0)
                if (tableName !in excludedTables) {
                    list.add(displayName(tableName))
                }
            }
        }
        Log.d(ContentValues.TAG, " -> viewModel : Database || GetAllPlaylist : $list")
        return list
    }

    fun isExistsDataInPlaylist(playlistName: String, videoId: String): Boolean {
        val writableDatabase = database.writableDatabase
        val newPlaylistName = sanitizeTableName(playlistName)
        writableDatabase.query("SELECT videoId FROM $newPlaylistName WHERE videoId = ?", arrayOf(videoId)).use { data ->
            if (data.moveToFirst()) {
                Log.d(ContentValues.TAG, " -> viewModel : Database || isExistsDataInPlaylist : $newPlaylistName || True")
                return true
            }
        }
        Log.d(ContentValues.TAG, " -> viewModel : Database || isExistsDataInPlaylist : $newPlaylistName || False")
        return false
    }

    fun countTheNumberOfCustomPlaylist(): Int {
        val writableDatabase = database.writableDatabase
        var count = 0
        writableDatabase.query("SELECT COUNT(*) FROM DopamineMastersDev").use { data ->
            if (data.moveToFirst()) {
                count = data.getInt(0)
            }
        }
        Log.d(ContentValues.TAG, " -> viewModel : Database || countTheNumberOfCustomPlaylist : $count")
        return count
    }

    fun deleteVideoFromPlaylist(playlistName: String, videoId: String) {
        val writableDatabase = database.writableDatabase
        val newPlaylistName = sanitizeTableName(playlistName)
        writableDatabase.execSQL("DELETE FROM $newPlaylistName WHERE videoId = ?", arrayOf(videoId))
    }

    fun isPlaylistExist(playlistName: String): Boolean {
        val writableDatabase = database.writableDatabase
        val sanitized = sanitizeTableName(playlistName)
        writableDatabase.query("SELECT playlistName FROM DopamineMastersDev WHERE playlistName = ?", arrayOf(sanitized)).use { cursor ->
            if (cursor.moveToFirst()) {
                Log.d(ContentValues.TAG, " -> viewModel : Database || isPlaylistExist : $playlistName || True")
                return true
            }
        }
        Log.d(ContentValues.TAG, " -> viewModel : Database || isPlaylistExist : $playlistName || False")
        return false
    }

    fun getPlaylistData(playlistName: String): List<CustomPlaylists> {
        val writableDatabase = database.writableDatabase
        val newPlaylistName = sanitizeTableName(playlistName)
        val list = mutableListOf<CustomPlaylists>()
        writableDatabase.query("SELECT * FROM $newPlaylistName").use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    CustomPlaylists(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getString(5),
                        cursor.getString(6),
                        cursor.getString(7)
                    )
                )
            }
        }
        Log.d(ContentValues.TAG, " -> viewModel : Database || getPlaylistData : $list")
        return list
    }

    /**
     * Converts a display name into a safe SQLite table name.
     * Replaces spaces with underscores, strips unsafe characters.
     */
    private fun sanitizeTableName(name: String): String {
        if (name.isBlank()) return "Null"
        return name
            .replace(" ", "_")
            .replace("(", "_")
            .replace(")", "_")
            .replace(Regex("[^a-zA-Z0-9_]"), "")
    }

    /**
     * Converts a sanitized table name back to a display name.
     * Replaces underscores with spaces for UI display.
     */
    private fun displayName(tableName: String): String {
        if (tableName.isBlank()) return "Null"
        return tableName.replace("_", " ")
    }
}