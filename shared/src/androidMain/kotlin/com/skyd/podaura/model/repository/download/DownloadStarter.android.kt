package com.skyd.podaura.model.repository.download

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.skyd.downloader.download.DownloadConstraints
import com.skyd.podaura.model.download.ArticleDownloadSource
import com.skyd.podaura.model.preference.dataStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidDownloadStarter private constructor(
    private val context: Context,
    private val permissionCoordinator: NotificationPermissionCoordinator? = null,
    private val onNotificationPermissionDenied: () -> Unit = {},
) : DownloadStarter() {
    constructor(context: Context) : this(
        context = context,
        permissionCoordinator = null,
    )

    override suspend fun download(
        url: String,
        type: String?,
        articleDownloadSource: ArticleDownloadSource?,
        automatic: Boolean,
        constraints: DownloadConstraints,
    ) {
        val coordinator = permissionCoordinator
        val notificationsAllowed = if (automatic || coordinator == null) {
            true
        } else {
            runCatching { coordinator.awaitPermission() }.getOrDefault(false)
        }
        val showSilentDownloadNotice = !notificationsAllowed &&
                coordinator != null &&
                runCatching { coordinator.consumeSilentDownloadNotice() }
                    .getOrDefault(false)
        super.download(
            url = url,
            type = type,
            articleDownloadSource = articleDownloadSource,
            automatic = automatic,
            constraints = constraints,
        )
        if (showSilentDownloadNotice) {
            withContext(Dispatchers.Main.immediate) { onNotificationPermissionDenied() }
        }
    }

    override fun openNotificationSettings() {
        val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val notificationSettingsIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            null
        }
        if (notificationSettingsIntent == null ||
            runCatching { context.startActivity(notificationSettingsIntent) }.isFailure
        ) {
            runCatching { context.startActivity(appDetailsIntent) }
        }
    }

    companion object {
        internal fun withPermissionCoordinator(
            context: Context,
            permissionCoordinator: NotificationPermissionCoordinator,
            onNotificationPermissionDenied: () -> Unit,
        ) = AndroidDownloadStarter(
            context = context,
            permissionCoordinator = permissionCoordinator,
            onNotificationPermissionDenied = onNotificationPermissionDenied,
        )
    }
}

internal class NotificationPermissionCoordinator(
    context: Context,
    private val launchPermissionRequest: () -> Unit,
) {
    private val applicationContext = context.applicationContext

    suspend fun awaitPermission(): Boolean = withContext(Dispatchers.Main.immediate) {
        if (notificationsEnabled()) return@withContext true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return@withContext false
        }
        if (applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext false
        }
        val result = pendingRequest ?: CompletableDeferred<Boolean>().also { deferred ->
            pendingRequest = deferred
            if (claimOnce(PERMISSION_REQUESTED_KEY)) {
                runCatching { launchPermissionRequest() }
                    .onFailure { complete(false) }
            } else {
                complete(false)
            }
        }
        result.await()
    }

    fun complete(granted: Boolean) {
        pendingRequest?.complete(granted && notificationsEnabled())
        pendingRequest = null
    }

    suspend fun consumeSilentDownloadNotice(): Boolean = claimOnce(SILENT_NOTICE_SHOWN_KEY)

    private suspend fun claimOnce(key: Preferences.Key<Boolean>): Boolean {
        var claimed = false
        dataStore.edit { preferences ->
            if (preferences[key] != true) {
                preferences[key] = true
                claimed = true
            }
        }
        return claimed
    }

    private fun notificationsEnabled(): Boolean {
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = applicationContext.getSystemService(NotificationManager::class.java)
                .getNotificationChannel(DOWNLOAD_NOTIFICATION_CHANNEL_ID)
            if (channel?.importance == NotificationManager.IMPORTANCE_NONE) return false
        }
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        var pendingRequest: CompletableDeferred<Boolean>? = null
        val PERMISSION_REQUESTED_KEY = booleanPreferencesKey(
            "downloadNotificationPermissionRequested"
        )
        val SILENT_NOTICE_SHOWN_KEY = booleanPreferencesKey(
            "downloadSilentNotificationNoticeShown"
        )
        const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "downloadChannel"
    }
}

private class PermissionCoordinatorHolder {
    var coordinator: NotificationPermissionCoordinator? = null
}

@Composable
actual fun rememberDownloadStarter(
    onNotificationPermissionDenied: () -> Unit,
): DownloadStarter {
    val context = LocalContext.current
    val latestDeniedCallback by rememberUpdatedState(onNotificationPermissionDenied)
    val holder = remember { PermissionCoordinatorHolder() }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        holder.coordinator?.complete(granted)
    }
    val coordinator = remember(context, launcher) {
        NotificationPermissionCoordinator(context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    SideEffect { holder.coordinator = coordinator }
    return remember(context, coordinator) {
        AndroidDownloadStarter.withPermissionCoordinator(
            context = context.applicationContext,
            permissionCoordinator = coordinator,
            onNotificationPermissionDenied = { latestDeniedCallback() },
        )
    }
}
