package com.skyd.podaura.model.worker.rsssync

import android.content.Context
import android.widget.Toast
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import co.touchlab.kermit.Logger
import com.skyd.podaura.di.get
import com.skyd.podaura.model.db.dao.FeedDao
import com.skyd.podaura.model.repository.article.ArticleRepository
import com.skyd.podaura.model.repository.article.IArticleRepository
import com.skyd.podaura.ui.component.showToast
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect

class RssSyncWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        var hasError = false
        get<IArticleRepository>().refreshArticleList(
            feedUrls = get<FeedDao>().getAllUnmutedFeedUrl(),
            full = false,
        ).catch { e ->
            if (e is ArticleRepository.RefreshFeedsException) {
                e.message?.showToast(Toast.LENGTH_LONG)
            } else {
                hasError = true
                Logger.e("RssSyncWorker", e)
            }
        }.collect()
        return if (hasError) Result.failure() else Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "rssSyncWorker"
    }
}