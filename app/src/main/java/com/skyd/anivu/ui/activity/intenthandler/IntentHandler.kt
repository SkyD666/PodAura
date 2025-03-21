package com.skyd.anivu.ui.activity.intenthandler

import android.content.Intent

fun interface IntentHandler {
    fun handle(intent: Intent)
}