package com.skyd.anivu.model.worker.deletearticle

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.skyd.anivu.di.get
import com.skyd.anivu.ext.getOrDefault
import com.skyd.anivu.model.db.dao.ArticleDao
import com.skyd.anivu.model.preference.data.delete.autodelete.AutoDeleteArticleBeforePreference
import com.skyd.anivu.model.preference.data.delete.autodelete.AutoDeleteArticleKeepFavoritePreference
import com.skyd.anivu.model.preference.data.delete.autodelete.AutoDeleteArticleKeepPlaylistPreference
import com.skyd.anivu.model.preference.data.delete.autodelete.AutoDeleteArticleKeepUnreadPreference
import com.skyd.anivu.model.preference.data.delete.autodelete.AutoDeleteArticleMaxCountPreference
import com.skyd.anivu.model.preference.data.delete.autodelete.AutoDeleteArticleUseBeforePreference
import com.skyd.anivu.model.preference.data.delete.autodelete.AutoDeleteArticleUseMaxCountPreference
import com.skyd.anivu.model.preference.dataStore

class DeleteArticleWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        runCatching {
            val articleDao = get<ArticleDao>()
            val keepPlaylistArticles =
                dataStore.getOrDefault(AutoDeleteArticleKeepPlaylistPreference)
            val keepUnread = dataStore.getOrDefault(AutoDeleteArticleKeepUnreadPreference)
            val keepFavorite = dataStore.getOrDefault(AutoDeleteArticleKeepFavoritePreference)
            val useBefore = dataStore.getOrDefault(AutoDeleteArticleUseBeforePreference)
            if (useBefore) {
                articleDao.deleteArticleBefore(
                    timestamp = System.currentTimeMillis() - dataStore.getOrDefault(
                        AutoDeleteArticleBeforePreference
                    ),
                    keepPlaylistArticles = keepPlaylistArticles,
                    keepUnread = keepUnread,
                    keepFavorite = keepFavorite,
                )
            }
            val useMaxCount = dataStore.getOrDefault(AutoDeleteArticleUseMaxCountPreference)
            val maxCount = dataStore.getOrDefault(AutoDeleteArticleMaxCountPreference)
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