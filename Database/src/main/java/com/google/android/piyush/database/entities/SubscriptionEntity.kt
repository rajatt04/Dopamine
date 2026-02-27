package com.google.android.piyush.database.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "subscription_table")
data class SubscriptionEntity(
    @PrimaryKey
    val channelId: String,
    val title: String,
    val description: String?,
    val thumbnail: String?,
    val channelTitle: String?,
) : Parcelable
