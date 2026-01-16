package com.skyd.downloader.di

import com.skyd.downloader.Downloader
import org.koin.dsl.module

val downloaderModule = module {
    single { Downloader.instance }
}