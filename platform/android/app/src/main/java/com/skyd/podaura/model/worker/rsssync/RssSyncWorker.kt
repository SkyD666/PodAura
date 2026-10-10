package com.skyd.podaura.model.worker.rsssync

import android.content.Context
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import co.touchlab.kermit.Logger
import com.skyd.fundation.di.get
import com.skyd.podaura.model.db.dao.FeedDao
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.repository.article.ArticleRepository
import com.skyd.podaura.model.repository.article.IArticleRepository
import com.skyd.podaura.ui.component.showToast
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

class RssSyncWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result = withTimeoutOrNull(RSS_SYNC_TIMEOUT) {
        var hasError = false
        val feeds = rotateRssSyncFeeds(
            get<FeedDao>().getAllUnmutedFeedUrl(),
            dataStore.data.first()[LAST_STARTING_FEED],
        )
        // Persist before requests begin, including when this run is later cancelled or killed.
        feeds.firstOrNull()?.let { first -> dataStore.edit { it[LAST_STARTING_FEED] = first } }
        get<IArticleRepository>().refreshArticleList(
            feedUrls = feeds,
            full = false,
        ).catch { e ->
            if (e is ArticleRepository.RefreshFeedsException) {
                e.message?.showToast(Toast.LENGTH_LONG)
            } else {
                hasError = true
                Logger.e("RssSyncWorker", e)
            }
        }.collect()
        if (hasError) Result.failure() else Result.success()
    } ?: Result.failure()

    companion object {
        const val UNIQUE_WORK_NAME = "rssSyncWorker"
        private val LAST_STARTING_FEED = stringPreferencesKey("rssSync.lastStartingFeed")
    }
}
