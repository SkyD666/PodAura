package com.skyd.podaura.model.repository.download

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.skyd.podaura.ui.component.showToast
import org.jetbrains.compose.resources.getString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.download_no_notification_permission_tip

class AndroidDownloadStarter(private val context: Context) : DownloadStarter() {
    override suspend fun download(url: String, type: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            )
            if (granted == PermissionChecker.PERMISSION_DENIED) {
                getString(Res.string.download_no_notification_permission_tip).showToast()
                return
            }
        }
        super.download(url, type)
    }
}

@Composable
actual fun rememberDownloadStarter(): DownloadStarter {
    val context = LocalContext.current
    return remember(context) { AndroidDownloadStarter(context) }
}