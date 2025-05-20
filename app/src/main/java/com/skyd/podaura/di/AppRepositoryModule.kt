package com.skyd.podaura.di

import com.skyd.podaura.model.repository.RssHelper
import com.skyd.podaura.model.repository.article.ArticleRepository
import com.skyd.podaura.model.repository.article.IArticleRepository
import com.skyd.podaura.model.repository.download.AutoDownloadStarter
import com.skyd.podaura.model.repository.download.DownloadRepository
import com.skyd.podaura.model.repository.feed.FeedRepository
import com.skyd.podaura.model.repository.feed.IFeedRepository
import com.skyd.podaura.model.repository.player.IPlayerRepository
import com.skyd.podaura.model.repository.player.PlayerRepository
import com.skyd.podaura.model.worker.download.AutoDownloadStarterImpl
import org.koin.dsl.binds
import org.koin.dsl.module

val repositoryModule = module {
    factory { AutoDownloadStarterImpl() } binds arrayOf(AutoDownloadStarter::class)
    factory { RssHelper(get(), get()) }
    factory { DownloadRepository() }
    factory {
        ArticleRepository(get(), get(), get(), get())
    } binds arrayOf(IArticleRepository::class)

    factory {
        FeedRepository(get(), get(), get(), get(), get())
    } binds arrayOf(IFeedRepository::class)

    factory {
        PlayerRepository(get(), get(), get())
    } binds arrayOf(IPlayerRepository::class)
}