package com.skyd.podaura.di

import android.content.Context
import com.skyd.podaura.model.preference.createDataStore
import com.skyd.podaura.model.repository.fullcontent.AndroidRenderedPageProvider
import com.skyd.podaura.model.repository.fullcontent.RenderedPageProvider
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

actual val dataStoreModule: Module
    get() = module { single { createDataStore(get<Context>()) } }

actual val fullContentPlatformModule: Module
    get() = module {
        single { AndroidRenderedPageProvider(get(), get()) } bind RenderedPageProvider::class
    }
