package com.skyd.podaura.di

import com.skyd.downloader.di.downloaderDatabaseModule
import com.skyd.downloader.di.downloaderModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(
        ioModule,
        databaseModule,
        dataStoreModule,
        pagingModule,
        repositoryModule,
        viewModelModule,
        downloaderModule,
        downloaderDatabaseModule,
    )
}

expect val dataStoreModule: Module
