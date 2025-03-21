package com.skyd.anivu.ui.activity.intenthandler

import android.content.Intent
import android.net.Uri
import com.skyd.anivu.ext.type

class ImportOpmlIntentHandler(private val onHandle: (String) -> Unit) : IntentHandler {
    override fun handle(intent: Intent) {
        val data = intent.data
        val clipData = intent.clipData
        if (intent.action == Intent.ACTION_VIEW && data != null) {
            val scheme = data.scheme
            when (scheme) {
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
        return path.endsWith(".xml") ||
                path.endsWith(".opml") ||
                data.type == "text/xml" ||
                data.type == "application/xml" ||
                data.type == "text/x-opml" ||
                data.type == "application/octet-stream"
    }
}