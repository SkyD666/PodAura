package com.skyd.podaura

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.skyd.mvi.mviConfig
import com.skyd.podaura.di.initKoin
import com.skyd.podaura.di.notificationModule
import com.skyd.podaura.di.repositoryModule
import com.skyd.podaura.di.viewModelModule
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.preference.appearance.DarkModePreference
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.repository.download.DownloadManager
import com.skyd.podaura.model.worker.deletearticle.listenDeleteArticleFrequency
import com.skyd.podaura.model.worker.rsssync.listenRssSyncConfig
import com.skyd.podaura.util.CrashHandler
import com.skyd.podaura.util.isDebug
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.loadKoinModules


class App : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = this
        initKoin {
            androidLogger()
            androidContext(this@App)
        }
        loadKoinModules(listOf(notificationModule, repositoryModule, viewModelModule))
        AppCompatDelegate.setDefaultNightMode(dataStore.getOrDefault(DarkModePreference))

        mviConfig { printLog = isDebug }

        CrashHandler.init(this)

        listenRssSyncConfig(this)
        listenDeleteArticleFrequency(this)
        DownloadManager.listenDownloadEvent()
    }
}

lateinit var appContext: Context