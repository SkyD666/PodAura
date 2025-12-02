package com.skyd.podaura.di

import com.skyd.fundation.config.Const
import com.skyd.fundation.config.DATA_STORE_DIR
import com.skyd.podaura.model.preference.createDataStore
import com.skyd.podaura.model.preference.dataStoreFileName
import org.koin.core.module.Module
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