package com.skyd.podaura.di

import com.skyd.podaura.model.preference.createDataStore
import com.skyd.podaura.model.preference.dataStoreFileName
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

actual val dataStoreModule: Module
    get() = module {
        single {
            createDataStore(
                dirPath = {
                    File(System.getProperty("java.io.tmpdir"), dataStoreFileName).absolutePath
                }
            )
        }
    }