package com.skyd.downloader.di

import com.skyd.downloader.Downloader
import com.skyd.downloader.db.DownloadDatabase
import com.skyd.downloader.db.builder
import com.skyd.downloader.db.instance
import org.koin.dsl.module

val downloaderModule = module {
    single { Downloader.instance }
}