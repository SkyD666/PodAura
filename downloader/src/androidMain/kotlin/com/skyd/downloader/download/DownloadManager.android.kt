package com.skyd.downloader.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.skyd.downloader.Status
import com.skyd.downloader.db.DownloadDao
import com.skyd.downloader.db.DownloadEntity
import com.skyd.downloader.util.NotificationUtil.removeNotification
import com.skyd.fundation.di.inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent

internal actual class DownloadManager actual constructor() : KoinComponent {
    private val context by inject<Context>()
    private val downloadDao by inject<DownloadDao>()
    private val workManager by lazy { WorkManager.getInstance(context) }

    actual suspend fun schedule(entity: DownloadEntity) {
        removeNotification(context, entity.id)
        enqueue(entity, ExistingWorkPolicy.REPLACE)
    }

    actual suspend fun cancel(id: String) {
        withContext(Dispatchers.IO) {
            workManager.cancelUniqueWork(workName(id)).result.get()
        }
        removeNotification(context, id)
    }

    actual suspend fun reconcile() {
        downloadDao.findAllInStatuses(listOf(Status.Queued.name)).forEach { entity ->
            if (entity.attemptId.isNotBlank()) enqueue(entity, ExistingWorkPolicy.KEEP)
        }
    }

    private fun enqueue(entity: DownloadEntity, policy: ExistingWorkPolicy) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                if (entity.requireUnmetered) NetworkType.UNMETERED else NetworkType.CONNECTED
            )
            .setRequiresCharging(entity.requiresCharging)
            .setRequiresBatteryNotLow(entity.requiresBatteryNotLow)
            .build()
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    DownloadWorker.INPUT_DATA_ID_KEY to entity.id,
                    DownloadWorker.INPUT_DATA_ATTEMPT_ID_KEY to entity.attemptId,
                )
            )
            .addTag(DOWNLOAD_WORK_TAG)
            .build()
        workManager.enqueueUniqueWork(workName(entity.id), policy, request)
    }

    private fun workName(id: String): String = "download:$id"

    companion object {
        private const val DOWNLOAD_WORK_TAG = "podaura-download"
    }
}
