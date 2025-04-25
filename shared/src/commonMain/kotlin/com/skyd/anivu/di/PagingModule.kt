package com.skyd.anivu.di

import androidx.paging.PagingConfig
import org.koin.dsl.module

val pagingModule = module {
    single { PagingConfig(pageSize = 20, enablePlaceholders = true) }
}