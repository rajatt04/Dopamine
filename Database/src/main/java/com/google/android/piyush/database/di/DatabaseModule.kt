package com.google.android.piyush.database.di

import android.content.Context
import com.google.android.piyush.database.DopamineDatabase
import com.google.android.piyush.database.dao.DopamineDao
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
    @Singleton
    fun provideDopamineDao(database: DopamineDatabase): DopamineDao {
        return database.dopamineDao()
    }

    @Provides
    @Singleton
    fun provideDopamineDatabaseRepository(dao: DopamineDao): DopamineDatabaseRepository {
        return DopamineDatabaseRepository(dao)
    }
}
