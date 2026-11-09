package com.skyd.podaura.di

import com.skyd.fundation.config.Const
import com.skyd.fundation.config.DATA_STORE_DIR
import com.skyd.fundation.util.joinPath
import com.skyd.podaura.model.preference.createDataStore
import com.skyd.podaura.model.preference.dataStoreFileName
import com.skyd.podaura.model.repository.fullcontent.AppleRenderedPageProvider
import com.skyd.podaura.model.repository.fullcontent.RenderedPageProvider
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

actual val dataStoreModule: Module
    get() = module {
        single { createDataStore { joinPath(Const.DATA_STORE_DIR, dataStoreFileName) } }
    }

actual val fullContentPlatformModule: Module
    get() = module {
        single { AppleRenderedPageProvider(get()) } bind RenderedPageProvider::class
    }
