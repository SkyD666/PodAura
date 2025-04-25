package com.skyd.anivu.ext

import android.Manifest
import android.app.Notification
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationManagerCompat

@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
fun Notification.notify(context: Context, id: Int) {
    NotificationManagerCompat.from(context).notify(id, this)
}