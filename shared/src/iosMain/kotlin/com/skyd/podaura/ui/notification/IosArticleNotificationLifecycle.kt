package com.skyd.podaura.ui.notification

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationState.UIApplicationStateBackground
import platform.UIKit.UIBackgroundTaskInvalid
import kotlin.time.Duration.Companion.seconds

/** App-wide notification delivery state, confined to the main thread. */
internal object IosArticleNotificationLifecycle {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val observers = mutableListOf<Any>()
    private var background =
        UIApplication.sharedApplication.applicationState == UIApplicationStateBackground

    var refreshing = false
        set(value) {
            field = value
            updateDeliveryMode()
        }

    fun initialize() {
        observe(UIApplicationDidEnterBackgroundNotification) {
            background = true
            updateDeliveryMode()
            flushBeforeSuspension()
        }
        observe(UIApplicationDidBecomeActiveNotification) {
            background = false
            updateDeliveryMode()
        }
        updateDeliveryMode()
    }

    private fun updateDeliveryMode() {
        ArticleUpdatedManager.setImmediateDelivery(background && !refreshing)
    }

    private fun observe(name: String?, action: () -> Unit) {
        if (name == null) return
        observers += NSNotificationCenter.defaultCenter.addObserverForName(
            name, null, NSOperationQueue.mainQueue,
        ) { action() }
    }

    private fun flushBeforeSuspension() {
        val app = UIApplication.sharedApplication
        var identifier = UIBackgroundTaskInvalid
        var job: Job? = null
        fun finish() {
            if (identifier != UIBackgroundTaskInvalid) {
                app.endBackgroundTask(identifier)
                identifier = UIBackgroundTaskInvalid
            }
        }
        identifier = app.beginBackgroundTaskWithName("Finish article notifications") {
            job?.cancel()
            finish()
        }
        job = scope.launch {
            try {
                withTimeoutOrNull(5.seconds) { ArticleUpdatedManager.flush() }
            } finally {
                finish()
            }
        }
    }
}
