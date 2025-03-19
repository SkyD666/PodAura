package com.skyd.anivu.di

import com.skyd.anivu.model.repository.article.ArticleRepository
import com.skyd.anivu.model.repository.article.IArticleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepoModule {
    @Binds
    @Singleton
    abstract fun provideIArticleRepository(
        articleRepository: ArticleRepository,
    ): IArticleRepository
}