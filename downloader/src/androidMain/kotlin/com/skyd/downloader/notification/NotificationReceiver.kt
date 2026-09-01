package com.skyd.downloader.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.skyd.downloader.Downloader
import com.skyd.fundation.di.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(NotificationConst.KEY_DOWNLOAD_TASK_ID) ?: return
        val notificationId = intent.getIntExtra(NotificationConst.KEY_NOTIFICATION_ID, -1)
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (notificationId >= 0) {
                    NotificationManagerCompat.from(context).cancel(notificationId)
                }
                val downloader: Downloader = get()
                when (intent.action) {
                    NotificationConst.ACTION_NOTIFICATION_PAUSE_CLICK -> downloader.pause(taskId)
                    NotificationConst.ACTION_NOTIFICATION_RESUME_CLICK -> downloader.resume(taskId)
                    NotificationConst.ACTION_NOTIFICATION_RETRY_CLICK -> downloader.retry(taskId)
                    NotificationConst.ACTION_NOTIFICATION_CANCEL_CLICK -> downloader.cancel(taskId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
