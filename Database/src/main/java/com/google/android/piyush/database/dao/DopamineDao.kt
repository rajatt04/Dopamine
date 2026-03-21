package com.google.android.piyush.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.google.android.piyush.database.entities.EntityDownload
import com.google.android.piyush.database.entities.EntityFavouritePlaylist
import com.google.android.piyush.database.entities.EntityRecentVideos
import com.google.android.piyush.database.entities.EntityVideoSearch

@Dao
interface DopamineDao {
    @Insert
    suspend fun insertSearchVideos(vararg searchVideo: EntityVideoSearch)
    @Query("SELECT * FROM search_table")
    suspend fun getSearchVideoList(): List<EntityVideoSearch>
    @Query("DELETE FROM search_table")
    suspend fun deleteSearchVideoList()
    @Insert
    suspend fun insertFavouriteVideos(vararg favouriteVideo : EntityFavouritePlaylist)
    @Query("SELECT videoId FROM favorite_playlist WHERE videoId = :videoId")
    suspend fun isFavouriteVideo (videoId : String) : String

    @Query("DELETE FROM favorite_playlist WHERE videoId = :videoId")
    suspend fun deleteFavouriteVideo(videoId : String)
    @Query("Select * FROM favorite_playlist")
    suspend fun getFavouritePlayList(): List<EntityFavouritePlaylist>
    @Insert
    suspend fun insertRecentVideos(vararg fav: EntityRecentVideos)
    @Query("Select * FROM recent_videos")
    suspend fun getRecentVideos(): List<EntityRecentVideos>
    @Query("SELECT videoId FROM recent_videos WHERE videoId = :videoId")
    suspend fun  isRecentVideo(videoId : String) : String
    @Query("Update recent_videos SET timing = :time WHERE videoId = :videoId")
    suspend fun updateRecentVideo(videoId: String, time: String)
    @Query("DELETE FROM recent_videos")
    suspend fun deleteRecentVideo()

    // Download methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: EntityDownload)

    @Update
    suspend fun updateDownload(download: EntityDownload)

    @Query("SELECT * FROM entity_downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): LiveData<List<EntityDownload>>

    @Query("SELECT * FROM entity_downloads ORDER BY createdAt DESC")
    suspend fun getAllDownloadsList(): List<EntityDownload>

    @Query("SELECT * FROM entity_downloads WHERE videoId = :videoId")
    suspend fun getDownload(videoId: String): EntityDownload?

    @Query("SELECT * FROM entity_downloads WHERE videoId = :videoId")
    fun getDownloadLive(videoId: String): LiveData<EntityDownload?>

    @Query("UPDATE entity_downloads SET status = :status WHERE videoId = :videoId")
    suspend fun updateDownloadStatus(videoId: String, status: Int)

    @Query("UPDATE entity_downloads SET progress = :progress, downloadedBytes = :downloadedBytes WHERE videoId = :videoId")
    suspend fun updateDownloadProgress(videoId: String, progress: Int, downloadedBytes: Long)

    @Query("UPDATE entity_downloads SET filePath = :filePath, status = :status WHERE videoId = :videoId")
    suspend fun updateDownloadPath(videoId: String, filePath: String, status: Int)

    @Query("DELETE FROM entity_downloads WHERE videoId = :videoId")
    suspend fun deleteDownload(videoId: String)

    @Query("DELETE FROM entity_downloads WHERE status = :status")
    suspend fun deleteDownloadsByStatus(status: Int)

    @Query("SELECT COUNT(*) FROM entity_downloads WHERE status = :status")
    suspend fun getDownloadCountByStatus(status: Int): Int

    @Query("SELECT * FROM entity_downloads WHERE status = :status")
    suspend fun getDownloadsByStatus(status: Int): List<EntityDownload>
}