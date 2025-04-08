package com.skyd.anivu.di

import com.skyd.anivu.model.repository.article.ArticleRepository
import com.skyd.anivu.model.repository.article.IArticleRepository
import com.skyd.anivu.model.repository.feed.FeedRepository
import com.skyd.anivu.model.repository.feed.IFeedRepository
import com.skyd.anivu.model.repository.importexport.opml.IExportOpmlRepository
import com.skyd.anivu.model.repository.importexport.opml.IImportOpmlRepository
import com.skyd.anivu.model.repository.importexport.opml.ImportExportOpmlRepository
import com.skyd.anivu.model.repository.media.IMediaRepository
import com.skyd.anivu.model.repository.media.MediaRepository
import com.skyd.anivu.model.repository.player.IPlayerRepository
import com.skyd.anivu.model.repository.player.PlayerRepository
import com.skyd.anivu.model.repository.playlist.AddToPlaylistRepository
import com.skyd.anivu.model.repository.playlist.IAddToPlaylistRepository
import com.skyd.anivu.model.repository.playlist.IPlaylistMediaRepository
import com.skyd.anivu.model.repository.playlist.IPlaylistRepository
import com.skyd.anivu.model.repository.playlist.PlaylistMediaRepository
import com.skyd.anivu.model.repository.playlist.PlaylistRepository
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
    abstract fun provideIArticleRepository(repo: ArticleRepository): IArticleRepository

    @Binds
    @Singleton
    abstract fun provideIFeedRepository(repo: FeedRepository): IFeedRepository

    @Binds
    @Singleton
    abstract fun provideIImportRepository(repo: ImportExportOpmlRepository): IImportOpmlRepository

    @Binds
    @Singleton
    abstract fun provideIExportRepository(repo: ImportExportOpmlRepository): IExportOpmlRepository

    @Binds
    @Singleton
    abstract fun provideIPlayerRepository(repo: PlayerRepository): IPlayerRepository

    @Binds
    @Singleton
    abstract fun provideIPlaylistMediaRepository(repo: PlaylistMediaRepository): IPlaylistMediaRepository

    @Binds
    @Singleton
    abstract fun provideIPlaylistRepository(repo: PlaylistRepository): IPlaylistRepository

    @Binds
    @Singleton
    abstract fun provideIAddToPlaylistRepository(repo: AddToPlaylistRepository): IAddToPlaylistRepository

    @Binds
    @Singleton
    abstract fun provideIMediaRepository(repo: MediaRepository): IMediaRepository
}