package com.google.android.piyush.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "subscription_table")
data class SubscriptionEntity(
    @PrimaryKey
    val channelId: String,
    val title: String,
    val description: String?,
    val thumbnail: String?,
    val channelTitle: String?,
    // Add other relevant fields if needed
) : Serializable
