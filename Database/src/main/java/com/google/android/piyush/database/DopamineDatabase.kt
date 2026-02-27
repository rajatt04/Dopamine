package com.google.android.piyush.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.google.android.piyush.database.dao.DopamineDao
import com.google.android.piyush.database.dao.SubscriptionDao
import com.google.android.piyush.database.dao.CustomPlaylistDao
import com.google.android.piyush.database.entities.CustomPlaylistEntity
import com.google.android.piyush.database.entities.CustomPlaylistVideoEntity
import com.google.android.piyush.database.entities.EntityFavouritePlaylist
import com.google.android.piyush.database.entities.EntityRecentVideos
import com.google.android.piyush.database.entities.EntityVideoSearch
import com.google.android.piyush.database.entities.SubscriptionEntity

@Database(
    entities = [
        EntityVideoSearch::class,
        EntityRecentVideos::class,
        EntityFavouritePlaylist::class,
        SubscriptionEntity::class,
        CustomPlaylistEntity::class,
        CustomPlaylistVideoEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class DopamineDatabase : RoomDatabase() {
    abstract fun dopamineDao(): DopamineDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun customPlaylistDao(): CustomPlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: DopamineDatabase? = null
        private const val DATABASE_NAME = "dopamine_database"

        fun getDatabase(context: Context): DopamineDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DopamineDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}