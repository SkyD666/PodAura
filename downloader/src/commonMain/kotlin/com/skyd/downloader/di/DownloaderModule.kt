package com.skyd.downloader.di

import com.skyd.downloader.Downloader
import com.skyd.downloader.download.DownloadExecutor
import com.skyd.downloader.download.DownloadManager
import com.skyd.downloader.download.DownloadTransferEngine
import org.koin.dsl.module

val downloaderModule = module {
    single { DownloadTransferEngine() }
    single { DownloadExecutor(get(), get()) }
    single { DownloadManager() }
    single { Downloader(get(), get()) }
}
