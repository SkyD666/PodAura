package com.skyd.podaura.ui.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.skyd.podaura.di.get
import com.skyd.podaura.model.bean.ArticleNotificationRuleBean
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.article.ArticleWithEnclosureBean
import com.skyd.podaura.model.repository.download.AutoDownloadStarter
import com.skyd.podaura.ui.component.blockString
import com.skyd.podaura.ui.notification.ArticleUpdatedManager.CHANNEL_ID
import com.skyd.podaura.util.uniqueInt
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.article_notification_channel_name
import podaura.shared.generated.resources.article_notification_content_text
import podaura.shared.generated.resources.article_notification_new_articles

class ArticleNotificationData(
    @DrawableRes
    val icon: Int,
    val openActivityIntent: (List<Pair<String, ArticleNotificationRuleBean>>) -> Intent,
)

actual object PlatformArticleNotification {
    actual fun sendNotification(matchedData: List<Pair<String, ArticleNotificationRuleBean>>) {
        val content = matchedData.map { it.second }
            .distinctBy { it.id }
            .joinToString(", ") { it.name }
        val appContext = get<Context>()
        val articleNotificationData = get<ArticleNotificationData>()
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            articleNotificationData.openActivityIntent(matchedData),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(articleNotificationData.icon)
            .setContentTitle(blockString(Res.string.article_notification_new_articles))
            .setContentText(blockString(Res.string.article_notification_content_text, content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(appContext)) {
            if (ActivityCompat.checkSelfPermission(
                    appContext, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                createNotificationChannel()
                notify(uniqueInt(), builder.build())
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = blockString(Res.string.article_notification_channel_name)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            val notificationManager =
                get<Context>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

actual object PlatformAutoDownload {
    actual suspend fun autoDownload(data: Map<String, List<ArticleBean>>) {
        get<AutoDownloadStarter>().start(data)
    }
}