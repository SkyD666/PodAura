package com.skyd.podaura.di

import org.koin.core.context.GlobalContext.startKoin
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
    )
}

expect val dataStoreModule: Module