package com.skyd.anivu

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.skyd.anivu.di.databaseModule
import com.skyd.anivu.di.initKoin
import com.skyd.anivu.ext.getOrDefault
import com.skyd.anivu.model.preference.appearance.DarkModePreference
import com.skyd.anivu.model.preference.dataStore
import com.skyd.anivu.model.repository.download.DownloadManager
import com.skyd.anivu.model.worker.deletearticle.listenDeleteArticleFrequency
import com.skyd.anivu.model.worker.rsssync.listenRssSyncConfig
import com.skyd.anivu.util.CrashHandler
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.loadKoinModules
import org.koin.ksp.generated.defaultModule


class App : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = this
        initKoin {
            androidLogger()
            androidContext(this@App)
        }
        loadKoinModules(listOf(databaseModule, defaultModule))
        AppCompatDelegate.setDefaultNightMode(dataStore.getOrDefault(DarkModePreference))

        CrashHandler.init(this)

        listenRssSyncConfig(this)
        listenDeleteArticleFrequency(this)
        DownloadManager.listenDownloadEvent()
    }
}

lateinit var appContext: Context