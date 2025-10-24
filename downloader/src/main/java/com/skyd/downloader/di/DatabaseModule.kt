package com.skyd.downloader.di

import com.skyd.downloader.db.DatabaseInstance
import com.skyd.downloader.db.DownloadDatabase
import org.koin.dsl.module

val databaseModule = module {
    single { DatabaseInstance.getInstance(get()) }
    single { get<DownloadDatabase>().downloadDao() }
}