package com.skyd.podaura.ui.player

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.skyd.podaura.di.initKoin
import com.skyd.podaura.model.db.AppDatabase
import com.skyd.podaura.model.db.instance
import com.skyd.podaura.model.preference.AcceptTermsPreference
import com.skyd.podaura.model.preference.player.PlayerSeekOptionPreference
import com.skyd.podaura.model.preference.player.BackgroundPlayPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import platform.AppKit.*
import platform.Foundation.*
import kotlin.test.*

/** Real player sessions with isolated preferences and playback history for native smoke tests. */
internal fun withMacosPlayerSession(backgroundPlay: Boolean = false, block: (MacosPlayerSession) -> Unit) {
    val directory = NSTemporaryDirectory() + "podaura-pip-" + NSUUID().UUIDString
    NSFileManager.defaultManager.createDirectoryAtPath(directory, true, null, null)
    val prefsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val prefs = PreferenceDataStoreFactory.createWithPath(scope = prefsScope) {
        "$directory/test.preferences_pb".toPath()
    }
    runBlocking {
        prefs.edit {
            it[AcceptTermsPreference.key] = true
            it[BackgroundPlayPreference.key] = backgroundPlay
            it[PlayerSeekOptionPreference.key] = PlayerSeekOptionPreference.EXACT
        }
    }
    val database = AppDatabase.instance(
        Room.databaseBuilder<AppDatabase>(name = "$directory/playback.db")
            .setDriver(BundledSQLiteDriver())
    )
    initKoin().koin.loadModules(listOf(module {
        single<DataStore<Preferences>> { prefs }
        single<AppDatabase> { database }
    }))
    val app = NSApplication.sharedApplication()
    app.setActivationPolicy(NSApplicationActivationPolicy.NSApplicationActivationPolicyRegular)
    app.finishLaunching()
    val session = MacosPlayerSession { null }
    try {
        block(session)
    } finally {
        var stopped = false
        session.shutdown { stopped = true }
        awaitPlayer("session shutdown") { stopped }
        prefsScope.cancel()
        database.close()
        stopKoin()
        NSFileManager.defaultManager.removeItemAtPath(directory, null)
    }
}

internal fun awaitPlayer(description: String, timeoutSeconds: Double = 15.0, condition: () -> Boolean) {
    val deadline = NSDate.dateWithTimeIntervalSinceNow(timeoutSeconds)
    while (!condition() && deadline.timeIntervalSinceNow > 0) pumpPlayer(0.02)
    assertTrue(condition(), description)
}

internal fun pumpPlayer(seconds: Double) {
    val deadline = NSDate.dateWithTimeIntervalSinceNow(seconds)
    val app = NSApplication.sharedApplication()
    while (deadline.timeIntervalSinceNow > 0) {
        NSRunLoop.mainRunLoop.runUntilDate(NSDate.dateWithTimeIntervalSinceNow(0.01))
        while (true) {
            val event = app.nextEventMatchingMask(ULong.MAX_VALUE, NSDate.date(), NSDefaultRunLoopMode, true) ?: break
            app.sendEvent(event)
        }
    }
}
