package com.skyd.podaura.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import com.skyd.podaura.config.Const
import com.skyd.podaura.ext.getAppVersionCode
import com.skyd.podaura.ext.getAppVersionName
import com.skyd.podaura.ext.safeOpenUri
import com.skyd.podaura.model.preference.appearance.DarkModePreference
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.ui.component.SettingsProvider
import com.skyd.podaura.ui.local.LocalWindowSizeClass
import com.skyd.podaura.ui.screen.settings.CrashScreen
import com.skyd.podaura.ui.theme.PodAuraTheme


/**
 * CrashActivity, do not extends BaseActivity
 */
class CrashActivity : ComponentActivity() {
    companion object {
        const val CRASH_INFO = "crashInfo"

        fun start(context: Context, crashInfo: String) {
            val intent = Intent(context, CrashActivity::class.java)
            intent.putExtra(CRASH_INFO, crashInfo)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val crashInfo = intent.getStringExtra(CRASH_INFO)
        val message = buildString {
            append("VersionName: ").append(getAppVersionName()).append("\n")
            append("VersionCode: ").append(getAppVersionCode()).append("\n")
            append("Brand: ").append(Build.BRAND).append("\n")
            append("Model: ").append(Build.MODEL).append("\n")
            append("SDK Version: ").append(Build.VERSION.SDK_INT).append("\n")
            append("ABI: ").append(Build.SUPPORTED_ABIS.firstOrNull().orEmpty()).append("\n\n")
            append("Crash Info: \n")
            append(crashInfo)
        }

        setContent {
            CompositionLocalProvider(LocalWindowSizeClass provides calculateWindowSizeClass(this)) {
                val dataStore = remember { dataStore }
                SettingsProvider(dataStore) {
                    PodAuraTheme(darkTheme = DarkModePreference.current) {
                        val uriHandler = LocalUriHandler.current
                        CrashScreen(
                            message = message,
                            onReport = { uriHandler.safeOpenUri(Const.GITHUB_NEW_ISSUE_URL) },
                        )
                    }
                }
            }
        }
    }
}