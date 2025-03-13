package com.skyd.anivu.model.worker.rsssync

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.get
import com.skyd.anivu.model.preference.rss.RssSyncBatteryNotLowConstraintPreference
import com.skyd.anivu.model.preference.rss.RssSyncChargingConstraintPreference
import com.skyd.anivu.model.preference.rss.RssSyncFrequencyPreference
import com.skyd.anivu.model.preference.rss.RssSyncWifiConstraintPreference
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

private data class RssSyncConfiguration(
    val rssSyncFrequency: Long,
    val requireWifi: Boolean,
    val requireCharging: Boolean,
    val requireBatteryNotLow: Boolean,
)

fun listenerRssSyncConfig(context: Context) = coroutineScope.launch {
    context.dataStore.data.map {
        RssSyncConfiguration(
            rssSyncFrequency = it[RssSyncFrequencyPreference],
            requireWifi = it[RssSyncWifiConstraintPreference],
            requireCharging = it[RssSyncChargingConstraintPreference],
            requireBatteryNotLow = it[RssSyncBatteryNotLowConstraintPreference],
        )
    }.distinctUntilChanged().combine(
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(RssSyncWorker.UNIQUE_WORK_NAME)
            .distinctUntilChanged(),
    ) { rssSyncConfiguration, workInfos ->
        val workInfo = workInfos.firstOrNull()
        val rssSyncFrequency = rssSyncConfiguration.rssSyncFrequency
        val requireWifi = rssSyncConfiguration.requireWifi
        val requireCharging = rssSyncConfiguration.requireCharging
        val requireBatteryNotLow = rssSyncConfiguration.requireBatteryNotLow

        if (rssSyncFrequency == RssSyncFrequencyPreference.MANUAL) {
            if (workInfo != null && !workInfo.state.isFinished) {
                stopRssSyncWorker(context)
            }
        } else {
            if (workInfo == null || workInfo.state.isFinished) {
                startRssSyncWorker(
                    context = context,
                    rssSyncFrequency = rssSyncFrequency,
                    requireWifi = requireWifi,
                    requireCharging = requireCharging,
                    requireBatteryNotLow = requireBatteryNotLow,
                )
            } else {
                val constraints = workInfo.constraints
                if (workInfo.periodicityInfo?.repeatIntervalMillis != rssSyncFrequency ||
                    constraints.requiresCharging() != requireCharging ||
                    constraints.requiresBatteryNotLow() != requireBatteryNotLow ||
                    constraints.requiredNetworkType != if (requireWifi) NetworkType.UNMETERED
                    else NetworkType.CONNECTED
                ) {
                    updateRssSyncWorker(
                        context = context,
                        rssSyncFrequency = rssSyncFrequency,
                        requireWifi = requireWifi,
                        requireCharging = requireCharging,
                        requireBatteryNotLow = requireBatteryNotLow,
                        id = workInfo.id,
                    )
                }
            }
        }
    }.collect()
}

private fun updateRssSyncWorker(
    context: Context,
    rssSyncFrequency: Long,
    requireWifi: Boolean,
    requireCharging: Boolean,
    requireBatteryNotLow: Boolean,
    id: UUID,
) {
    val future = WorkManager.getInstance(context).updateWork(
        getRssSyncWorkRequest(
            rssSyncFrequency = rssSyncFrequency,
            requireWifi = requireWifi,
            requireCharging = requireCharging,
            requireBatteryNotLow = requireBatteryNotLow,
            id = id,
        )
    )
    Futures.addCallback(
        future,
        object : FutureCallback<WorkManager.UpdateResult> {
            override fun onSuccess(result: WorkManager.UpdateResult) {
                Log.i("updateRssSyncWorker", "Success: $result")
            }

            override fun onFailure(t: Throwable) {
                t.printStackTrace()
            }
        },
        // causes the callbacks to be executed on the main (UI) thread
        ContextCompat.getMainExecutor(context)
    )
}

private fun startRssSyncWorker(
    context: Context,
    rssSyncFrequency: Long,
    requireWifi: Boolean,
    requireCharging: Boolean,
    requireBatteryNotLow: Boolean,
) {
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        RssSyncWorker.UNIQUE_WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP,
        getRssSyncWorkRequest(
            rssSyncFrequency = rssSyncFrequency,
            requireWifi = requireWifi,
            requireCharging = requireCharging,
            requireBatteryNotLow = requireBatteryNotLow,
        ),
    )
}

private fun stopRssSyncWorker(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork(RssSyncWorker.UNIQUE_WORK_NAME)
}

private fun getRssSyncWorkRequest(
    rssSyncFrequency: Long,
    requireWifi: Boolean,
    requireCharging: Boolean,
    requireBatteryNotLow: Boolean,
    id: UUID? = null,
): PeriodicWorkRequest {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(if (requireWifi) NetworkType.UNMETERED else NetworkType.CONNECTED)
        .setRequiresCharging(requireCharging)
        .setRequiresBatteryNotLow(requireBatteryNotLow)
        .build()
    val builder = PeriodicWorkRequestBuilder<RssSyncWorker>(
        rssSyncFrequency, TimeUnit.MILLISECONDS
    ).setConstraints(constraints)
    if (id != null) {
        builder.setId(id)
    }
    return builder.build()
}