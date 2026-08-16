package com.skyd.podaura.di

import com.skyd.fundation.config.Const
import com.skyd.fundation.config.DATA_STORE_DIR
import com.skyd.podaura.model.preference.createDataStore
import com.skyd.podaura.model.preference.dataStoreFileName
import com.skyd.podaura.model.repository.fullcontent.RenderedPageException
import com.skyd.podaura.model.repository.fullcontent.RenderedPageProvider
import com.skyd.podaura.model.repository.translation.CredentialStore
import com.skyd.podaura.model.repository.translation.DesktopCredentialStore
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import java.io.File

actual val dataStoreModule: Module
    get() = module {
        single {
            createDataStore(
                dirPath = { File(Const.DATA_STORE_DIR, dataStoreFileName).absolutePath }
            )
        }
    }

actual val credentialStoreModule: Module
    get() = module { single<CredentialStore> { DesktopCredentialStore() } }

actual val fullContentPlatformModule: Module
    get() = module {
        single {
            object : RenderedPageProvider {
                override suspend fun render(url: String) =
                    throw RenderedPageException("Rendered pages are not supported on JVM Desktop")
            }
        } bind RenderedPageProvider::class
    }
