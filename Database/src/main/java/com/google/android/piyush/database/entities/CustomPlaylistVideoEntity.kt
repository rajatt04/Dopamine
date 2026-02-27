package com.google.android.piyush.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "custom_playlist_videos",
    primaryKeys = ["playlistName", "videoId"],
    foreignKeys = [
        ForeignKey(
            entity = CustomPlaylistEntity::class,
            parentColumns = ["playlistName"],
            childColumns = ["playlistName"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["playlistName"])]
)
data class CustomPlaylistVideoEntity(
    val playlistName: String,
    val videoId: String,
    val title: String?,
    val thumbnail: String?,
    val channelId: String?,
    val publishedAt: String?,
    val viewCount: String?,
    val channelTitle: String?,
    val duration: String?
)
