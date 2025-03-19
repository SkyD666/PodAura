package com.skyd.anivu.model.worker.rsssync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.skyd.anivu.model.db.dao.FeedDao
import com.skyd.anivu.model.repository.article.IArticleRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect

class RssSyncWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        val feedDao: FeedDao
        val articleRepo: IArticleRepository
    }

    private val hiltEntryPoint = EntryPointAccessors.fromApplication(
        context, WorkerEntryPoint::class.java
    )

    override suspend fun doWork(): Result {
        var hasError = false
        hiltEntryPoint.articleRepo
            .refreshArticleList(hiltEntryPoint.feedDao.getAllUnmutedFeedUrl())
            .catch {
                hasError = true
                it.printStackTrace()
            }.collect()
        return if (hasError) Result.failure() else Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "rssSyncWorker"
    }
}