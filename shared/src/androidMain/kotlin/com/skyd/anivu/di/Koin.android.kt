package com.skyd.anivu.di

import android.content.Context
import com.skyd.anivu.model.preference.createDataStore
import org.koin.core.module.Module
import org.koin.dsl.module

actual val dataStoreModule: Module
    get() = module { single { createDataStore(get<Context>()) } }