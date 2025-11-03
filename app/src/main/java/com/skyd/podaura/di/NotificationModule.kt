package com.skyd.podaura.di

import android.content.Intent
import androidx.core.net.toUri
import com.skyd.compone.component.blockString
import com.skyd.downloader.notification.NotificationConfig
import com.skyd.podaura.R
import com.skyd.podaura.ui.activity.MainActivity
import com.skyd.podaura.ui.component.UuidList
import com.skyd.podaura.ui.notification.ArticleNotificationData
import com.skyd.podaura.ui.screen.article.ArticleRoute
import com.skyd.podaura.ui.screen.download.DownloadRoute
import org.koin.dsl.module
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.download_channel_description
import podaura.shared.generated.resources.download_channel_name

val notificationModule = module {
    single {
        ArticleNotificationData(
            icon = R.drawable.ic_icon_24,
            openActivityIntent = { matchedData ->
                Intent(
                    Intent.ACTION_VIEW,
                    ArticleRoute(articleIds = UuidList(matchedData.map { it.first }))
                        .toDeeplink()
                        .toUri(),
                    get(),
                    MainActivity::class.java
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            }
        )
    }

    single {
        NotificationConfig(
            channelName = blockString(Res.string.download_channel_name),
            channelDescription = blockString(Res.string.download_channel_description),
            smallIcon = R.drawable.ic_icon_24,
            intentContentActivity = MainActivity::class.qualifiedName,
            intentContentBasePath = DownloadRoute.BASE_PATH,
        )
    }
}