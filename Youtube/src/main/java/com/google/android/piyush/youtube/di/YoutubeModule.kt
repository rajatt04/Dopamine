package com.google.android.piyush.youtube.di

import com.google.android.piyush.youtube.repository.YoutubeRepository
import com.google.android.piyush.youtube.repository.YoutubeRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object YoutubeModule {

    @Provides
    @Singleton
    fun provideYoutubeRepository(): YoutubeRepository {
        return YoutubeRepositoryImpl()
    }
}
