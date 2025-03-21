package com.skyd.anivu.ui.activity.intenthandler

import android.content.Intent
import android.net.Uri
import com.skyd.anivu.ext.type

class UrlDownloadIntentHandler(private val onHandle: (String) -> Unit) : IntentHandler {
    override fun handle(intent: Intent) {
        val data = intent.data
        val clipData = intent.clipData
        if (intent.action == Intent.ACTION_VIEW && data != null) {
            val scheme = data.scheme
            when (scheme) {
                "magnet" -> onHandle(data.toString())
                "http", "https", "file", "content" -> {
                    if (check(data)) {
                        onHandle(data.toString())
                    }
                }
            }
        } else if (intent.action == Intent.ACTION_SEND && clipData != null) {
            for (i in 0..<clipData.itemCount) {
                val uri = clipData.getItemAt(i).uri
                if (check(uri)) {
                    onHandle(uri.toString())
                    break
                }
            }
        }
    }

    private fun check(data: Uri): Boolean {
        val path = data.toString()
        return path.endsWith(".torrent") ||
                data.type == "application/x-bittorrent" ||
                data.type == "applications/x-bittorrent"
    }
}