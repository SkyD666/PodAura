package com.skyd.podaura

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.skyd.mvi.mviConfig
import com.skyd.podaura.di.initKoin
import com.skyd.podaura.di.notificationModule
import com.skyd.podaura.di.repositoryModule
import com.skyd.podaura.di.viewModelModule
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.preference.appearance.DarkModePreference
import com.skyd.podaura.model.preference.appearance.DarkModePreference.toAndroidNightMode
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.worker.deletearticle.listenDeleteArticleFrequency
import com.skyd.podaura.model.worker.rsssync.listenRssSyncConfig
import com.skyd.podaura.util.CrashHandler
import com.skyd.podaura.util.isDebug
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidLogger()
            androidContext(this@App)
            modules(
                notificationModule,
                repositoryModule,
                viewModelModule
            )
        }

        AppCompatDelegate.setDefaultNightMode(
            dataStore.getOrDefault(DarkModePreference).toAndroidNightMode()
        )

        mviConfig { printLog = isDebug }

        CrashHandler.init(this)

        listenRssSyncConfig(this)
        listenDeleteArticleFrequency(this)

        onAppStart()
    }
}
