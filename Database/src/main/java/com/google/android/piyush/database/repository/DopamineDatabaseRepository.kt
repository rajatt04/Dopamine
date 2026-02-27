package com.google.android.piyush.database.repository

import com.google.android.piyush.database.dao.CustomPlaylistDao
import com.google.android.piyush.database.dao.DopamineDao
import com.google.android.piyush.database.dao.SubscriptionDao
import com.google.android.piyush.database.entities.CustomPlaylistEntity
import com.google.android.piyush.database.entities.CustomPlaylistVideoEntity
import com.google.android.piyush.database.entities.EntityFavouritePlaylist
import com.google.android.piyush.database.entities.EntityRecentVideos
import com.google.android.piyush.database.entities.EntityVideoSearch
import com.google.android.piyush.database.entities.SubscriptionEntity

class DopamineDatabaseRepository(
    private val dopamineDao: DopamineDao,
    private val subscriptionDao: SubscriptionDao,
    private val customPlaylistDao: CustomPlaylistDao
) {
    suspend fun insertSearchVideos(vararg searchVideo: EntityVideoSearch) {
        dopamineDao.insertSearchVideos(*searchVideo)
    }

    suspend fun getSearchVideoList(): List<EntityVideoSearch> {
        return dopamineDao.getSearchVideoList()
    }

    suspend fun deleteSearchVideoList() {
        dopamineDao.deleteSearchVideoList()
    }

    suspend fun insertFavouriteVideos(vararg favouriteVideo: EntityFavouritePlaylist) {
        dopamineDao.insertFavouriteVideos(*favouriteVideo)
    }

    suspend fun isFavouriteVideo(videoId: String): String {
        return dopamineDao.isFavouriteVideo(videoId)
    }

    suspend fun deleteFavouriteVideo(videoId: String) {
        dopamineDao.deleteFavouriteVideo(videoId)
    }

    suspend fun getFavouritePlayList(): List<EntityFavouritePlaylist> {
        return dopamineDao.getFavouritePlayList()
    }

    suspend fun insertRecentVideos(vararg recentVideos: EntityRecentVideos) {
        dopamineDao.insertRecentVideos(*recentVideos)
    }

    suspend fun getRecentVideos(): List<EntityRecentVideos> {
        return dopamineDao.getRecentVideos()
    }

    suspend fun isRecentVideo(videoId: String): String {
        return dopamineDao.isRecentVideo(videoId)
    }

    suspend fun updateRecentVideo(videoId: String, time: String) {
        dopamineDao.updateRecentVideo(videoId, time)
    }
    suspend fun deleteRecentVideo() {
        dopamineDao.deleteRecentVideo()
    }

    // Subscription Methods
    suspend fun insertSubscription(subscription: SubscriptionEntity) {
        subscriptionDao.insert(subscription)
    }

    suspend fun deleteSubscription(channelId: String) {
        subscriptionDao.delete(channelId)
    }

    suspend fun getAllSubscriptions(): List<SubscriptionEntity> {
        return subscriptionDao.getAllSubscriptions()
    }

    suspend fun isSubscribed(channelId: String): Boolean {
        return subscriptionDao.isSubscribed(channelId)
    }

    // Custom Playlist Methods
    suspend fun createPlaylist(playlist: CustomPlaylistEntity) {
        customPlaylistDao.createPlaylist(playlist)
    }

    suspend fun getAllPlaylists(): List<CustomPlaylistEntity> {
        return customPlaylistDao.getAllPlaylists()
    }

    suspend fun getPlaylistCount(): Int {
        return customPlaylistDao.getPlaylistCount()
    }

    suspend fun isPlaylistExist(name: String): Boolean {
        return customPlaylistDao.isPlaylistExist(name)
    }

    suspend fun deletePlaylist(name: String) {
        customPlaylistDao.deletePlaylist(name)
    }

    suspend fun addVideoToPlaylist(video: CustomPlaylistVideoEntity) {
        customPlaylistDao.addVideoToPlaylist(video)
    }

    suspend fun getPlaylistVideos(playlistName: String): List<CustomPlaylistVideoEntity> {
        return customPlaylistDao.getPlaylistVideos(playlistName)
    }

    suspend fun isVideoInPlaylist(playlistName: String, videoId: String): Boolean {
        return customPlaylistDao.isVideoInPlaylist(playlistName, videoId)
    }

    suspend fun deleteVideoFromPlaylist(playlistName: String, videoId: String) {
        customPlaylistDao.deleteVideoFromPlaylist(playlistName, videoId)
    }
}
