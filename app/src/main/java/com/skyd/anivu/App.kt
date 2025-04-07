package com.skyd.anivu

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.getOrDefault
import com.skyd.anivu.model.preference.appearance.DarkModePreference
import com.skyd.anivu.model.repository.download.DownloadManager
import com.skyd.anivu.model.worker.deletearticle.listenDeleteArticleFrequency
import com.skyd.anivu.model.worker.rsssync.listenRssSyncConfig
import com.skyd.anivu.util.CrashHandler
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = this
        AppCompatDelegate.setDefaultNightMode(dataStore.getOrDefault(DarkModePreference))

        CrashHandler.init(this)

        listenRssSyncConfig(this)
        listenDeleteArticleFrequency(this)
        DownloadManager.listenDownloadEvent()
    }
}

lateinit var appContext: Context