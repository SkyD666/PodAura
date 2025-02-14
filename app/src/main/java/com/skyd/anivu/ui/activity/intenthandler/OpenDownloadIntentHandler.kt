package com.skyd.anivu.ui.activity.intenthandler

import android.content.Intent
import com.skyd.anivu.ui.screen.download.DOWNLOAD_SCREEN_DEEP_LINK_DATA

class OpenDownloadIntentHandler(private val onHandle: () -> Unit) : IntentHandler {
    override fun handle(intent: Intent) {
        if (intent.data.toString() == DOWNLOAD_SCREEN_DEEP_LINK_DATA.deepLink) {
            onHandle()
        }
    }
}