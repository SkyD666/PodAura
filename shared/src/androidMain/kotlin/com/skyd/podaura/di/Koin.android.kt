package com.skyd.podaura.di

import android.content.Context
import com.skyd.podaura.model.preference.createDataStore
import com.skyd.podaura.model.repository.fullcontent.AndroidRenderedPageProvider
import com.skyd.podaura.model.repository.fullcontent.RenderedPageProvider
import com.skyd.podaura.model.repository.translation.AndroidCredentialStore
import com.skyd.podaura.model.repository.translation.CredentialStore
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

actual val dataStoreModule: Module
    get() = module { single { createDataStore(get<Context>()) } }

actual val credentialStoreModule: Module
    get() = module { single<CredentialStore> { AndroidCredentialStore(get()) } }

actual val fullContentPlatformModule: Module
    get() = module {
        single { AndroidRenderedPageProvider(get(), get()) } bind RenderedPageProvider::class
    }
