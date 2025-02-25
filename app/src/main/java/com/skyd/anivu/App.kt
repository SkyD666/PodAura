package com.skyd.anivu

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.getOrDefault
import com.skyd.anivu.model.preference.appearance.DarkModePreference
import com.skyd.anivu.model.repository.download.DownloadManager
import com.skyd.anivu.model.worker.deletearticle.listenerDeleteArticleFrequency
import com.skyd.anivu.model.worker.rsssync.listenerRssSyncConfig
import com.skyd.anivu.util.CrashHandler
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = this
        AppCompatDelegate.setDefaultNightMode(dataStore.getOrDefault(DarkModePreference))

        CrashHandler.init(this)

        listenerRssSyncConfig(this)
        listenerDeleteArticleFrequency(this)
        DownloadManager.listenDownloadEvent()
    }
}

lateinit var appContext: Context