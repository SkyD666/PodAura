package com.skyd.podaura.di

import android.content.Context
import com.skyd.podaura.model.preference.createDataStore
import org.koin.core.module.Module
import org.koin.dsl.module

actual val dataStoreModule: Module
    get() = module { single { createDataStore(get<Context>()) } }