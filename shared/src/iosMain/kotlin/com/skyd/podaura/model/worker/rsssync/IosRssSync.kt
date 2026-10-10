package com.skyd.podaura.model.worker.rsssync

import androidx.datastore.preferences.core.Preferences
import co.touchlab.kermit.Logger
import com.skyd.fundation.di.get
import com.skyd.podaura.ext.get
import com.skyd.podaura.model.db.dao.FeedDao
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.preference.rss.RssSyncBatteryNotLowConstraintPreference
import com.skyd.podaura.model.preference.rss.RssSyncChargingConstraintPreference
import com.skyd.podaura.model.preference.rss.RssSyncFrequencyPreference
import com.skyd.podaura.model.preference.rss.RssSyncWifiConstraintPreference
import com.skyd.podaura.model.repository.article.IArticleRepository
import com.skyd.podaura.ui.notification.ArticleUpdateDelivery
import com.skyd.podaura.ui.notification.ArticleUpdatedManager
import com.skyd.podaura.ui.notification.IosArticleNotificationLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGTask
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSUserDefaults
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970
import platform.Network.nw_interface_type_wifi
import platform.Network.nw_path_get_status
import platform.Network.nw_path_is_expensive
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.Network.nw_path_uses_interface_type
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceBatteryLevelDidChangeNotification
import platform.UIKit.UIDeviceBatteryState.UIDeviceBatteryStateCharging
import platform.UIKit.UIDeviceBatteryState.UIDeviceBatteryStateFull
import platform.UIKit.UIDeviceBatteryStateDidChangeNotification
import platform.darwin.dispatch_get_main_queue
import kotlin.time.Duration.Companion.seconds

internal data class IosRssSyncConfig(
    val frequency: Long,
    val requireWifi: Boolean,
    val requireCharging: Boolean,
    val requireBatteryNotLow: Boolean,
) {
    val enabled get() = frequency > 0

    fun allows(
        connected: Boolean,
        unmeteredWifi: Boolean,
        charging: Boolean,
        batteryLevel: Float
    ): Boolean = enabled && connected && (!requireWifi || unmeteredWifi) &&
            (!requireCharging || charging) &&
            (!requireBatteryNotLow || charging || batteryLevel > 0.15f)
}

private fun Preferences.rssSyncConfig() = IosRssSyncConfig(
    this[RssSyncFrequencyPreference], this[RssSyncWifiConstraintPreference],
    this[RssSyncChargingConstraintPreference], this[RssSyncBatteryNotLowConstraintPreference],
)

internal class IosRssSyncState(private val defaults: NSUserDefaults) {
    fun nextBeginDate(frequency: Long, now: NSDate, consumed: Boolean = false): NSDate? {
        if (frequency <= 0) {
            defaults.removeObjectForKey("rssSync.nextBeginDate")
            defaults.removeObjectForKey("rssSync.frequency")
            return null
        }
        val pending = defaults.objectForKey("rssSync.nextBeginDate") as? NSDate
        if (!consumed && pending != null &&
            defaults.doubleForKey("rssSync.frequency").toLong() == frequency
        ) return pending
        return NSDate.dateWithTimeIntervalSince1970(
            now.timeIntervalSince1970 +
                    frequency.coerceAtLeast(RssSyncFrequencyPreference.EVERY_15_MINUTE) / 1000.0
        ).also {
            defaults.setObject(it, "rssSync.nextBeginDate")
            defaults.setDouble(frequency.toDouble(), "rssSync.frequency")
        }
    }

    fun rotateFeeds(feedUrls: List<String>): List<String> {
        val ordered =
            rotateRssSyncFeeds(feedUrls, defaults.stringForKey("rssSync.lastStartingFeed"))
        // Save before starting requests so expiration or process termination cannot reset progress.
        ordered.firstOrNull()?.let { defaults.setObject(it, "rssSync.lastStartingFeed") }
        return ordered
    }
}

/** All lifecycle state is confined to the main thread; Room/HTTP dispatch their work off it. */
internal object IosRssSync {
    private const val TASK_ID = "com.skyd.podaura.rss-refresh"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val scheduler get() = BGTaskScheduler.sharedScheduler
    private val network = MutableStateFlow<Pair<Boolean, Boolean>?>(null)
    private val monitor = nw_path_monitor_create()
    private val observers = mutableListOf<Any>()
    private val state = IosRssSyncState(NSUserDefaults.standardUserDefaults)
    private var config: IosRssSyncConfig? = null
    private var runningTask: Job? = null

    fun initialize() {
        if (!scheduler.registerForTaskWithIdentifier(
                TASK_ID,
                dispatch_get_main_queue(),
                ::runTask
            )
        ) {
            Logger.e(tag = "IosRssSync") { "RSS background task identifier is not registered in Info.plist" }
        }
        UIDevice.currentDevice.batteryMonitoringEnabled = true
        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_set_update_handler(monitor) { path ->
            if (path != null) {
                network.value = (nw_path_get_status(path) == nw_path_status_satisfied) to
                        (nw_path_uses_interface_type(path, nw_interface_type_wifi) &&
                                !nw_path_is_expensive(path))
                cancelIfConstrained()
            }
        }
        nw_path_monitor_start(monitor)
        observe(UIDeviceBatteryLevelDidChangeNotification, ::cancelIfConstrained)
        observe(UIDeviceBatteryStateDidChangeNotification, ::cancelIfConstrained)
        observe(UIApplicationDidBecomeActiveNotification) {
            config?.let { schedule(it) }
        }
        scope.launch {
            dataStore.data.map { it.rssSyncConfig() }.distinctUntilChanged().catch {
                Logger.e(throwable = it, tag = "IosRssSync") {
                    "Cannot read RSS sync settings"
                }
            }.collect {
                config = it
                schedule(it)
                cancelIfConstrained()
            }
        }
    }

    private fun observe(name: String?, action: () -> Unit) {
        if (name == null) return
        observers += NSNotificationCenter.defaultCenter.addObserverForName(
            name, null, NSOperationQueue.mainQueue,
        ) { action() }
    }

    private fun schedule(config: IosRssSyncConfig, consumed: Boolean = false) {
        val beginDate = state.nextBeginDate(config.frequency, NSDate(), consumed)
        if (beginDate == null) {
            scheduler.cancelTaskRequestWithIdentifier(TASK_ID)
            return
        }
        val request = BGAppRefreshTaskRequest(TASK_ID).apply {
            earliestBeginDate = beginDate
        }
        if (!scheduler.submitTaskRequest(request, null)) {
            Logger.w(tag = "IosRssSync") { "iOS did not accept the RSS background refresh request" }
        }
    }

    private fun allowed(config: IosRssSyncConfig, network: Pair<Boolean, Boolean>): Boolean {
        val device = UIDevice.currentDevice
        val charging = device.batteryState == UIDeviceBatteryStateCharging ||
                device.batteryState == UIDeviceBatteryStateFull
        return config.allows(network.first, network.second, charging, device.batteryLevel)
    }

    private fun cancelIfConstrained() {
        val current = config ?: return
        if (!current.enabled || network.value?.let { !allowed(current, it) } == true) {
            runningTask?.cancel()
        }
    }

    private fun runTask(task: BGTask?) {
        if (task == null) return
        if (runningTask != null) {
            task.setTaskCompletedWithSuccess(false)
            return
        }
        val delivery = ArticleUpdateDelivery()
        val job = scope.launch(context = delivery, start = CoroutineStart.LAZY) {
            var success = false
            try {
                val current = dataStore.data.first().rssSyncConfig()
                config = current
                schedule(current, consumed = true)
                if (!current.enabled) return@launch
                // Reserve time within iOS's short refresh window for committed articles and notifications.
                withTimeout(RSS_SYNC_TIMEOUT) {
                    val connection = network.filterNotNull().first()
                    if (!allowed(current, connection)) return@withTimeout
                    IosArticleNotificationLifecycle.refreshing = true
                    get<IArticleRepository>().refreshArticleList(
                        state.rotateFeeds(get<FeedDao>().getAllUnmutedFeedUrl()), full = false,
                    ).collect()
                    success = true
                }
            } catch (_: CancellationException) {
                // Expiration, changed constraints, or the local deadline stop unfinished requests.
            } catch (error: Exception) {
                Logger.e(throwable = error, tag = "IosRssSync") { "RSS background refresh failed" }
            } finally {
                withContext(NonCancellable) {
                    val submitted = withTimeoutOrNull(5.seconds) {
                        ArticleUpdatedManager.flush(delivery)
                    } == true
                    success = success && submitted
                    IosArticleNotificationLifecycle.refreshing = false
                }
                task.expirationHandler = null
                task.setTaskCompletedWithSuccess(success)
                runningTask = null
            }
        }
        task.expirationHandler = { scope.launch { job.cancel() } }
        runningTask = job
        job.start()
    }

}
