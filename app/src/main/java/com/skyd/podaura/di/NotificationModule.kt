package com.skyd.podaura.di

import android.content.Intent
import androidx.core.net.toUri
import com.skyd.podaura.R
import com.skyd.podaura.ui.activity.MainActivity
import com.skyd.podaura.ui.component.UuidList
import com.skyd.podaura.ui.notification.ArticleNotificationData
import com.skyd.podaura.ui.screen.article.ArticleRoute
import org.koin.dsl.module

val notificationModule = module {
    single {
        ArticleNotificationData(
            icon = R.drawable.ic_icon_24,
            openActivityIntent = { matchedData ->
                Intent(
                    Intent.ACTION_VIEW,
                    ArticleRoute(articleIds = UuidList(matchedData.map { it.first })).toDeeplink().toUri(),
                    get(),
                    MainActivity::class.java
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            }
        )
    }
}