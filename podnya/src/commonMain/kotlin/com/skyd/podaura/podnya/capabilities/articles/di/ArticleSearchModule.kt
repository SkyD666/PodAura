package com.skyd.podaura.podnya.capabilities.articles.di

import com.skyd.podaura.podnya.capabilities.articles.model.repository.LocalArticleSearchRepository
import org.koin.dsl.module

val articleSearchModule = module {
    single { LocalArticleSearchRepository(get()) }
}
