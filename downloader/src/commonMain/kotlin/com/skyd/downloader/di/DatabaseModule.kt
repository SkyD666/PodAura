package com.skyd.downloader.di

import com.skyd.downloader.db.DownloadDatabase
import com.skyd.downloader.db.builder
import com.skyd.downloader.db.instance
import org.koin.dsl.module

val downloaderDatabaseModule = module {
    single { DownloadDatabase.instance(DownloadDatabase.builder()) }
    single { get<DownloadDatabase>().downloadDao() }
}