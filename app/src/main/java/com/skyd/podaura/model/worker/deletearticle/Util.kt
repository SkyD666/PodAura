package com.skyd.podaura.model.worker.deletearticle

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.skyd.podaura.ext.get
import com.skyd.podaura.model.preference.data.delete.autodelete.AutoDeleteArticleFrequencyPreference
import com.skyd.podaura.model.preference.data.delete.autodelete.UseAutoDeletePreference
import com.skyd.podaura.model.preference.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

private val coroutineScope = CoroutineScope(Dispatchers.IO)

private data class DeleteArticleConfiguration(
    val useDeleteArticle: Boolean,
    val deleteArticleFrequency: Long,
)

fun listenDeleteArticleFrequency(context: Context) = coroutineScope.launch {
    dataStore.data.map {
        DeleteArticleConfiguration(
            useDeleteArticle = it[UseAutoDeletePreference],
            deleteArticleFrequency = it[AutoDeleteArticleFrequencyPreference],
        )
    }.distinctUntilChanged().combine(
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(DeleteArticleWorker.UNIQUE_WORK_NAME)
            .distinctUntilChanged(),
    ) { deleteArticleConfiguration, workInfos ->
        val workInfo = workInfos.firstOrNull()
        val useDeleteArticle = deleteArticleConfiguration.useDeleteArticle
        val deleteArticleFrequency = deleteArticleConfiguration.deleteArticleFrequency

        if (!useDeleteArticle) {
            if (workInfo == null || !workInfo.state.isFinished) {
                stopDeleteArticleWorker(context)
            }
        } else {
            if (workInfo == null || workInfo.state.isFinished) {
                startRssSyncWorker(
                    context = context,
                    deleteArticleFrequency = deleteArticleFrequency,
                )
            } else {
                if (workInfo.periodicityInfo?.repeatIntervalMillis != deleteArticleFrequency) {
                    updateDeleteArticleWorker(
                        context = context,
                        deleteArticleFrequency = deleteArticleFrequency,
                        id = workInfo.id,
                    )
                }
            }
        }
    }.collect()
}

fun updateDeleteArticleWorker(
    context: Context,
    deleteArticleFrequency: Long,
    id: UUID,
) {
    val future = WorkManager.getInstance(context).updateWork(
        getDeleteArticleWorkRequest(
            deleteArticleFrequency = deleteArticleFrequency,
            id = id,
        )
    )
    Futures.addCallback(
        future,
        object : FutureCallback<WorkManager.UpdateResult> {
            override fun onSuccess(result: WorkManager.UpdateResult) {
                Log.i("updateDeleteArticleWorker", "Success: $result")
            }

            override fun onFailure(t: Throwable) {
                t.printStackTrace()
            }
        },
        // causes the callbacks to be executed on the main (UI) thread
        ContextCompat.getMainExecutor(context)
    )
}

fun startRssSyncWorker(
    context: Context,
    deleteArticleFrequency: Long,
) {
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        DeleteArticleWorker.UNIQUE_WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP,
        getDeleteArticleWorkRequest(
            deleteArticleFrequency = deleteArticleFrequency,
        ),
    )
}

fun stopDeleteArticleWorker(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork(DeleteArticleWorker.UNIQUE_WORK_NAME)
}

fun getDeleteArticleWorkRequest(
    deleteArticleFrequency: Long,
    id: UUID? = null,
): PeriodicWorkRequest {
    val builder = PeriodicWorkRequestBuilder<DeleteArticleWorker>(
        deleteArticleFrequency, TimeUnit.MILLISECONDS
    )
    if (id != null) {
        builder.setId(id)
    }
    return builder.build()
}