package com.google.android.piyush.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.android.piyush.database.dao.DopamineDao
import com.google.android.piyush.database.entities.EntityDownload
import com.google.android.piyush.database.entities.EntityFavouritePlaylist
import com.google.android.piyush.database.entities.EntityRecentVideos
import com.google.android.piyush.database.entities.EntityVideoSearch

@Database(
    entities = [
        EntityVideoSearch::class,
        EntityRecentVideos::class,
        EntityFavouritePlaylist::class,
        EntityDownload::class
    ],
    version = 2,
    exportSchema = false
)
abstract class DopamineDatabase : RoomDatabase() {
    abstract fun dopamineDao(): DopamineDao
    companion object {
        @Volatile
        private var INSTANCE: DopamineDatabase? = null
        private const val DATABASE_NAME = "dopamine_database"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `entity_downloads` (
                        `videoId` TEXT NOT NULL,
                        `title` TEXT,
                        `thumbnail` TEXT,
                        `channelId` TEXT,
                        `channelTitle` TEXT,
                        `filePath` TEXT,
                        `downloadId` INTEGER NOT NULL DEFAULT -1,
                        `status` INTEGER NOT NULL DEFAULT 0,
                        `progress` INTEGER NOT NULL DEFAULT 0,
                        `fileSize` INTEGER NOT NULL DEFAULT 0,
                        `downloadedBytes` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`videoId`)
                    )"""
                )
            }
        }

        fun getDatabase(context: Context): DopamineDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DopamineDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}