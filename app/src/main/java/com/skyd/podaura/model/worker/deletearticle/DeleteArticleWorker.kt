package com.skyd.podaura.model.worker.deletearticle

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.skyd.podaura.di.get
import com.skyd.podaura.ext.getOrDefaultSuspend
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.preference.data.delete.autodelete.AutoDeleteArticleBeforePreference
import com.skyd.podaura.model.preference.data.delete.autodelete.AutoDeleteArticleKeepFavoritePreference
import com.skyd.podaura.model.preference.data.delete.autodelete.AutoDeleteArticleKeepPlaylistPreference
import com.skyd.podaura.model.preference.data.delete.autodelete.AutoDeleteArticleKeepUnreadPreference
import com.skyd.podaura.model.preference.data.delete.autodelete.AutoDeleteArticleMaxCountPreference
import com.skyd.podaura.model.preference.data.delete.autodelete.AutoDeleteArticleUseBeforePreference
import com.skyd.podaura.model.preference.data.delete.autodelete.AutoDeleteArticleUseMaxCountPreference
import com.skyd.podaura.model.preference.dataStore

class DeleteArticleWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        runCatching {
            val articleDao = get<ArticleDao>()
            val keepPlaylistArticles =
                dataStore.getOrDefaultSuspend(AutoDeleteArticleKeepPlaylistPreference)
            val keepUnread = dataStore.getOrDefaultSuspend(AutoDeleteArticleKeepUnreadPreference)
            val keepFavorite =
                dataStore.getOrDefaultSuspend(AutoDeleteArticleKeepFavoritePreference)
            val useBefore = dataStore.getOrDefaultSuspend(AutoDeleteArticleUseBeforePreference)
            if (useBefore) {
                articleDao.deleteArticleBefore(
                    timestamp = System.currentTimeMillis() - dataStore.getOrDefaultSuspend(
                        AutoDeleteArticleBeforePreference
                    ),
                    keepPlaylistArticles = keepPlaylistArticles,
                    keepUnread = keepUnread,
                    keepFavorite = keepFavorite,
                )
            }
            val useMaxCount = dataStore.getOrDefaultSuspend(AutoDeleteArticleUseMaxCountPreference)
            val maxCount = dataStore.getOrDefaultSuspend(AutoDeleteArticleMaxCountPreference)
            if (useMaxCount && maxCount > 1) {
                articleDao.deleteArticleExceed(
                    count = maxCount,
                    keepPlaylistArticles = keepPlaylistArticles,
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