package com.skyd.podaura.di

import com.skyd.fundation.credential.CredentialStore
import com.skyd.fundation.credential.DesktopCredentialStore
import org.koin.core.context.stopKoin
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame

class CredentialStoreModuleTest {
    @Test
    fun sharedProvidesCredentialsWithoutPodnya() {
        val app = initKoin()
        try {
            val store = app.koin.get<CredentialStore>()
            assertIs<DesktopCredentialStore>(store)
            assertSame(store, app.koin.get<CredentialStore>())
        } finally {
            stopKoin()
        }
    }

}
