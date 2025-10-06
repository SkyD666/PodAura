package com.skyd.podaura.di

import com.skyd.podaura.model.repository.article.ArticleRepository
import com.skyd.podaura.model.repository.article.IArticleRepository
import com.skyd.podaura.model.repository.download.AutoDownloadStarter
import com.skyd.podaura.model.repository.download.DownloadManager
import com.skyd.podaura.model.repository.download.DownloadRepository
import com.skyd.podaura.model.repository.download.IDownloadManager
import com.skyd.podaura.model.worker.download.AutoDownloadStarterImpl
import org.koin.dsl.binds
import org.koin.dsl.module

val repositoryModule = module {
    factory { AutoDownloadStarterImpl() } binds arrayOf(AutoDownloadStarter::class)
    factory { DownloadRepository() }
    factory {
        ArticleRepository(get(), get(), get(), get())
    } binds arrayOf(IArticleRepository::class)

    factory {
        DownloadManager.getInstance(get())
    } binds arrayOf(IDownloadManager::class)
}