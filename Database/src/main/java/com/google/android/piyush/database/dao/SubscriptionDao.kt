package com.google.android.piyush.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.google.android.piyush.database.entities.SubscriptionEntity

@Dao
interface SubscriptionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscription: SubscriptionEntity)

    @Query("DELETE FROM subscription_table WHERE channelId = :channelId")
    suspend fun delete(channelId: String)

    @Query("SELECT * FROM subscription_table")
    suspend fun getAllSubscriptions(): List<SubscriptionEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM subscription_table WHERE channelId = :channelId)")
    suspend fun isSubscribed(channelId: String): Boolean
}
