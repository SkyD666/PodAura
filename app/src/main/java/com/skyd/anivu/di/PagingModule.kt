package com.skyd.anivu.di

import androidx.paging.PagingConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PagingModule {
    @Provides
    @Singleton
    // enablePlaceholders must be true
    fun providePagingConfig(): PagingConfig = PagingConfig(
        pageSize = 20, enablePlaceholders = true
    )
}