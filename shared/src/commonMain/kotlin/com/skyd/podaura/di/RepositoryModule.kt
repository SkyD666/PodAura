package com.skyd.podaura.di

import com.skyd.podaura.model.repository.DataRepository
import com.skyd.podaura.model.repository.FilePickerRepository
import com.skyd.podaura.model.repository.HistoryRepository
import com.skyd.podaura.model.repository.ReadRepository
import com.skyd.podaura.model.repository.SearchRepository
import com.skyd.podaura.model.repository.UpdateNotificationRepository
import com.skyd.podaura.model.repository.UpdateRepository
import com.skyd.podaura.model.repository.article.ArticleRepository
import com.skyd.podaura.model.repository.article.IArticleRepository
import com.skyd.podaura.model.repository.calendar.CalendarRepository
import com.skyd.podaura.model.repository.download.AutoDownloadRuleRepository
import com.skyd.podaura.model.repository.download.DownloadManager
import com.skyd.podaura.model.repository.download.DownloadRepository
import com.skyd.podaura.model.repository.download.IDownloadManager
import com.skyd.podaura.model.repository.feed.FeedRepository
import com.skyd.podaura.model.repository.feed.IFeedRepository
import com.skyd.podaura.model.repository.feed.ReorderFeedRepository
import com.skyd.podaura.model.repository.feed.ReorderGroupRepository
import com.skyd.podaura.model.repository.feed.RequestHeadersRepository
import com.skyd.podaura.model.repository.feed.RssHelper
import com.skyd.podaura.model.repository.importexport.ImportExportRepository
import com.skyd.podaura.model.repository.importexport.opml.IExportOpmlRepository
import com.skyd.podaura.model.repository.importexport.opml.IImportOpmlRepository
import com.skyd.podaura.model.repository.importexport.opml.ImportExportOpmlRepository
import com.skyd.podaura.model.repository.media.IMediaRepository
import com.skyd.podaura.model.repository.media.MediaRepository
import com.skyd.podaura.model.repository.player.IPlayerRepository
import com.skyd.podaura.model.repository.player.PlayerRepository
import com.skyd.podaura.model.repository.playlist.AddToPlaylistRepository
import com.skyd.podaura.model.repository.playlist.IAddToPlaylistRepository
import com.skyd.podaura.model.repository.playlist.IPlaylistMediaRepository
import com.skyd.podaura.model.repository.playlist.IPlaylistRepository
import com.skyd.podaura.model.repository.playlist.PlaylistMediaRepository
import com.skyd.podaura.model.repository.playlist.PlaylistRepository
import com.skyd.podaura.util.favicon.FaviconExtractor
import com.skyd.podaura.util.favicon.extractor.BaseUrlIconTagExtractor
import com.skyd.podaura.util.favicon.extractor.HardCodedExtractor
import com.skyd.podaura.util.favicon.extractor.IconTagExtractor
import org.koin.dsl.binds
import org.koin.dsl.module

val repositoryModule = module {
    factory { ReorderGroupRepository(get(), get()) }
    factory { ReorderFeedRepository(get(), get()) }
    factory { DataRepository(get(), get(), get()) }
    factory { HistoryRepository(get(), get(), get()) }
    factory { ReadRepository(get(), get()) }
    factory { SearchRepository(get(), get(), get(), get()) }
    factory { UpdateNotificationRepository(get()) }
    factory { RequestHeadersRepository(get()) }
    factory { FaviconExtractor() }
    factory { BaseUrlIconTagExtractor(get()) }
    factory { HardCodedExtractor(get()) }
    factory { IconTagExtractor(get()) }
    factory { FilePickerRepository() }
    factory { UpdateRepository(get()) }
    factory { ImportExportRepository() }
    factory { AutoDownloadRuleRepository(get(), get()) }
    factory { CalendarRepository(get(), get()) }
    factory { MediaRepository(get(), get(), get()) } binds arrayOf(IMediaRepository::class)
    factory {
        AddToPlaylistRepository(get(), get(), get())
    } binds arrayOf(IAddToPlaylistRepository::class)

    factory {
        PlaylistMediaRepository(get(), get(), get(), get())
    } binds arrayOf(IPlaylistMediaRepository::class)

    factory {
        PlaylistRepository(get(), get(), get())
    } binds arrayOf(IPlaylistRepository::class)

    factory {
        ImportExportOpmlRepository(get(), get())
    } binds arrayOf(
        ImportExportOpmlRepository::class,
        IImportOpmlRepository::class,
        IExportOpmlRepository::class,
    )

    factory {
        PlayerRepository(get(), get(), get())
    } binds arrayOf(IPlayerRepository::class)

    factory { RssHelper(get()) }

    factory {
        FeedRepository(get(), get(), get(), get(), get())
    } binds arrayOf(IFeedRepository::class)

    factory {
        ArticleRepository(get(), get(), get(), get())
    } binds arrayOf(IArticleRepository::class)

    factory { DownloadRepository() }

    factory { DownloadManager.instance } binds arrayOf(IDownloadManager::class)
}