package com.skyd.anivu.ui.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.skyd.anivu.R
import com.skyd.anivu.appContext
import com.skyd.anivu.model.bean.ArticleNotificationRuleBean
import com.skyd.anivu.model.bean.article.ArticleWithEnclosureBean
import com.skyd.anivu.model.db.dao.ArticleNotificationRuleDao
import com.skyd.anivu.ui.activity.MainActivity
import com.skyd.anivu.ui.screen.article.ArticleRoute
import com.skyd.anivu.util.uniqueInt
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object ArticleNotificationManager {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ArticleNotificationManagerEntryPoint {
        val articleNotificationRuleDao: ArticleNotificationRuleDao
    }

    private const val CHANNEL_ID = "articleNotification"

    private val scope = CoroutineScope(Dispatchers.IO)

    fun onNewData(data: List<ArticleWithEnclosureBean>) {
        scope.launch {
            val hiltEntryPoint = EntryPointAccessors.fromApplication(
                appContext, ArticleNotificationManagerEntryPoint::class.java
            )
            val rules =
                hiltEntryPoint.articleNotificationRuleDao.getAllArticleNotificationRules().first()
            val matchedData = data.mapNotNull { item ->
                val matchedRule = rules.firstOrNull { it.match(item) }
                if (matchedRule != null) {
                    item to matchedRule
                } else null
            }
            if (matchedData.isNotEmpty()) {
                sendNotification(matchedData)
            }
        }
    }

    private fun sendNotification(
        matchedData: List<Pair<ArticleWithEnclosureBean, ArticleNotificationRuleBean>>,
    ) {
        val content = matchedData.map { it.second }
            .distinctBy { it.id }
            .joinToString(", ") { it.name }
        val intent = Intent(
            Intent.ACTION_VIEW,
            ArticleRoute(
                feedUrls = emptyList(),
                groupIds = emptyList(),
                articleIds = matchedData.map { it.first.article.articleId },
            ).toDeeplink(),
            appContext,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_icon_24)
            .setContentTitle(appContext.getString(R.string.article_notification_new_articles))
            .setContentText(
                appContext.getString(R.string.article_notification_content_text, content)
            )
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
            val name = appContext.getString(R.string.article_notification_channel_name)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            val notificationManager =
                appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}