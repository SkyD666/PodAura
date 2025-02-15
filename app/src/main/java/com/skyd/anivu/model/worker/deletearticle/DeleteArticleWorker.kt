package com.skyd.anivu.model.worker.deletearticle

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.getOrDefault
import com.skyd.anivu.model.db.dao.ArticleDao
import com.skyd.anivu.model.preference.data.autodelete.AutoDeleteArticleBeforePreference
import com.skyd.anivu.model.preference.data.autodelete.AutoDeleteArticleKeepFavoritePreference
import com.skyd.anivu.model.preference.data.autodelete.AutoDeleteArticleKeepUnreadPreference
import com.skyd.anivu.model.preference.data.autodelete.AutoDeleteArticleMaxCountPreference
import com.skyd.anivu.model.preference.data.autodelete.AutoDeleteArticleUseBeforePreference
import com.skyd.anivu.model.preference.data.autodelete.AutoDeleteArticleUseMaxCountPreference
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class DeleteArticleWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        val articleDao: ArticleDao
    }

    private val hiltEntryPoint = EntryPointAccessors.fromApplication(
        context, WorkerEntryPoint::class.java
    )

    override suspend fun doWork(): Result {
        runCatching {
            val dataStore = applicationContext.dataStore
            val keepUnread = dataStore.getOrDefault(AutoDeleteArticleKeepUnreadPreference)
            val keepFavorite = dataStore.getOrDefault(AutoDeleteArticleKeepFavoritePreference)
            val useBefore = dataStore.getOrDefault(AutoDeleteArticleUseBeforePreference)
            if (useBefore) {
                hiltEntryPoint.articleDao.deleteArticleBefore(
                    timestamp = System.currentTimeMillis() - dataStore.getOrDefault(
                        AutoDeleteArticleBeforePreference
                    ),
                    keepUnread = keepUnread,
                    keepFavorite = keepFavorite,
                )
            }
            val useMaxCount = dataStore.getOrDefault(AutoDeleteArticleUseMaxCountPreference)
            val maxCount = dataStore.getOrDefault(AutoDeleteArticleMaxCountPreference)
            if (useMaxCount && maxCount > 1) {
                hiltEntryPoint.articleDao.deleteArticleExceed(
                    count = maxCount,
                    keepUnread = keepUnread,
                    keepFavorite = keepFavorite,
                )
            }
        }.onFailure { return Result.failure() }
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "deleteArticleWorker"
    }
}