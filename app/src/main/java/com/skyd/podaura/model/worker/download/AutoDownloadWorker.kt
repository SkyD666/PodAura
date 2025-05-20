package com.skyd.podaura.model.worker.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.skyd.podaura.di.get
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.db.dao.EnclosureDao
import com.skyd.podaura.model.db.dao.download.AutoDownloadRuleDao
import com.skyd.podaura.model.repository.download.AutoDownloadStarter
import com.skyd.podaura.model.repository.download.DownloadStarter
import kotlinx.coroutines.flow.first

class AutoDownloadWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val articleId = inputData.getString("articleId") ?: return Result.failure()
        val enclosureDao = get<EnclosureDao>()
        val enclosure = enclosureDao.getEnclosureList(articleId).first().firstOrNull()
            ?: return Result.success()
        DownloadStarter.download(
            context = applicationContext,
            url = enclosure.url,
            type = enclosure.type,
        )
        return Result.success()
    }
}

class AutoDownloadStarterImpl : AutoDownloadStarter {
    override suspend fun start(data: Map<String, List<ArticleBean>>) {
        val autoDownloadRuleDao = get<AutoDownloadRuleDao>()
        val works = data.mapNotNull { (k, v) ->
            val rule = autoDownloadRuleDao.getRuleByFeedUrl(k).first() ?: return@mapNotNull null
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (rule.requireWifi) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .setRequiresCharging(rule.requireCharging)
                .setRequiresBatteryNotLow(rule.requireBatteryNotLow)
                .build()
            v.map { article ->
                OneTimeWorkRequestBuilder<AutoDownloadWorker>()
                    .setConstraints(constraints)
                    .setInputData(workDataOf("articleId" to article.articleId))
                    .build()
            }
        }.flatten()
        if (works.isNotEmpty()) {
            WorkManager.getInstance(get()).enqueue(works)
        }
    }
}
