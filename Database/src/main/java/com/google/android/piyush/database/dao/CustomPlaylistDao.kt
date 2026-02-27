package com.google.android.piyush.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.google.android.piyush.database.entities.CustomPlaylistEntity
import com.google.android.piyush.database.entities.CustomPlaylistVideoEntity

@Dao
interface CustomPlaylistDao {

    // ── Master playlist operations ──

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun createPlaylist(playlist: CustomPlaylistEntity)

    @Query("SELECT * FROM custom_playlists ORDER BY playlistName ASC")
    suspend fun getAllPlaylists(): List<CustomPlaylistEntity>

    @Query("SELECT COUNT(*) FROM custom_playlists")
    suspend fun getPlaylistCount(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM custom_playlists WHERE playlistName = :name)")
    suspend fun isPlaylistExist(name: String): Boolean

    @Query("DELETE FROM custom_playlists WHERE playlistName = :name")
    suspend fun deletePlaylist(name: String)

    // ── Playlist video operations ──

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addVideoToPlaylist(video: CustomPlaylistVideoEntity)

    @Query("SELECT * FROM custom_playlist_videos WHERE playlistName = :playlistName")
    suspend fun getPlaylistVideos(playlistName: String): List<CustomPlaylistVideoEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM custom_playlist_videos WHERE playlistName = :playlistName AND videoId = :videoId)")
    suspend fun isVideoInPlaylist(playlistName: String, videoId: String): Boolean

    @Query("DELETE FROM custom_playlist_videos WHERE playlistName = :playlistName AND videoId = :videoId")
    suspend fun deleteVideoFromPlaylist(playlistName: String, videoId: String)

    @Query("SELECT DISTINCT playlistName FROM custom_playlist_videos")
    suspend fun getPlaylistNamesWithVideos(): List<String>
}
