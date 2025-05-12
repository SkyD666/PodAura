package com.skyd.podaura.di

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.module.Module
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
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

inline fun <reified T : Any> get(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null,
): T {
    return object : KoinComponent {
        val value: T = get(qualifier, parameters)
    }.value
}

inline fun <reified T : Any> inject(): Lazy<T> = lazy { get<T>() }