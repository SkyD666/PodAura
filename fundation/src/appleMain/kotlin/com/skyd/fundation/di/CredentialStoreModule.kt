package com.skyd.fundation.di

import com.skyd.fundation.credential.CredentialStore
import com.skyd.fundation.credential.AppleCredentialStore
import org.koin.core.module.Module
import org.koin.dsl.module

actual val credentialStoreModule: Module = module {
    single<CredentialStore> { AppleCredentialStore() }
}
