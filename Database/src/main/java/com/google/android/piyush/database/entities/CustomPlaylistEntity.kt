package com.google.android.piyush.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_playlists")
data class CustomPlaylistEntity(
    @PrimaryKey val playlistName: String,
    val playlistDescription: String
)
