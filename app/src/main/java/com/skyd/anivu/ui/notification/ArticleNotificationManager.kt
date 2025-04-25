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
import com.skyd.anivu.di.get
import com.skyd.anivu.ext.getString
import com.skyd.anivu.ext.onSubList
import com.skyd.anivu.model.bean.ArticleNotificationRuleBean
import com.skyd.anivu.model.db.dao.ArticleDao
import com.skyd.anivu.model.db.dao.ArticleNotificationRuleDao
import com.skyd.anivu.ui.activity.MainActivity
import com.skyd.anivu.ui.component.UuidList
import com.skyd.anivu.ui.screen.article.ArticleRoute
import com.skyd.anivu.util.uniqueInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.article_notification_channel_name
import podaura.shared.generated.resources.article_notification_content_text
import podaura.shared.generated.resources.article_notification_new_articles
import kotlin.time.Duration.Companion.seconds

object ArticleNotificationManager {
    private const val CHANNEL_ID = "articleNotification"

    private val scope = CoroutineScope(Dispatchers.IO)
    private val channel = Channel<List<String>>(capacity = Channel.UNLIMITED)

    init {
        onBatch()
    }

    fun send(articleIds: List<String>) = scope.launch {
        channel.send(articleIds)
    }

    private fun onBatch() = scope.launch {
        val articleNotificationRuleDao = get<ArticleNotificationRuleDao>()
        val articleDao = get<ArticleDao>()
        while (isActive) {
            val articleIds = mutableListOf<List<String>>()

            if (isActive) articleIds += channel.receive()  // Suspend here when no data
            while (isActive) {
                articleIds += withTimeoutOrNull(20.seconds) {
                    channel.receive()
                } ?: break
            }

            val rules = articleNotificationRuleDao.getAllArticleNotificationRules().first()
            val matchedData = mutableListOf<Pair<String, ArticleNotificationRuleBean>>()
            articleIds.flatten().onSubList { subArticleIds ->
                val data = articleDao.getArticleWithEnclosureListByIds(subArticleIds)
                matchedData += data.mapNotNull { item ->
                    val matchedRule = rules.firstOrNull { it.match(item) }
                    matchedRule?.let { item.article.articleId to matchedRule }
                }
            }
            if (matchedData.isNotEmpty()) {
                matchedData.onSubList(step = 5000) { sendNotification(it) }
            }
        }
    }

    private fun sendNotification(matchedData: List<Pair<String, ArticleNotificationRuleBean>>) {
        val content = matchedData.map { it.second }
            .distinctBy { it.id }
            .joinToString(", ") { it.name }
        val intent = Intent(
            Intent.ACTION_VIEW,
            ArticleRoute(articleIds = UuidList(matchedData.map { it.first })).toDeeplink(),
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
            .setContentTitle(appContext.getString(Res.string.article_notification_new_articles))
            .setContentText(
                appContext.getString(Res.string.article_notification_content_text, content)
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
            val name = appContext.getString(Res.string.article_notification_channel_name)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            val notificationManager =
                appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}