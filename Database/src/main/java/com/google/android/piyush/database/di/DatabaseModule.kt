package com.google.android.piyush.database.di

import android.content.Context
import androidx.room.Room
import com.google.android.piyush.database.DopamineDatabase
import com.google.android.piyush.database.dao.CustomPlaylistDao
import com.google.android.piyush.database.dao.DopamineDao
import com.google.android.piyush.database.dao.SubscriptionDao
import com.google.android.piyush.database.repository.DopamineDatabaseRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDopamineDatabase(@ApplicationContext context: Context): DopamineDatabase {
        return DopamineDatabase.getDatabase(context)
    }

    @Provides
    fun provideDopamineDao(database: DopamineDatabase): DopamineDao {
        return database.dopamineDao()
    }

    @Provides
    fun provideSubscriptionDao(database: DopamineDatabase): SubscriptionDao {
        return database.subscriptionDao()
    }

    @Provides
    fun provideCustomPlaylistDao(database: DopamineDatabase): CustomPlaylistDao {
        return database.customPlaylistDao()
    }

    @Provides
    @Singleton
    fun provideDopamineDatabaseRepository(
        dopamineDao: DopamineDao,
        subscriptionDao: SubscriptionDao,
        customPlaylistDao: CustomPlaylistDao
    ): DopamineDatabaseRepository {
        return DopamineDatabaseRepository(dopamineDao, subscriptionDao, customPlaylistDao)
    }
}
