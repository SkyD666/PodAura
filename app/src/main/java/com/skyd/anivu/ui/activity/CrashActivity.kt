package com.skyd.anivu.ui.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skyd.anivu.R
import com.skyd.anivu.config.Const.GITHUB_NEW_ISSUE_URL
import com.skyd.anivu.ext.copy
import com.skyd.anivu.ext.getAppVersionCode
import com.skyd.anivu.ext.getAppVersionName
import com.skyd.anivu.ext.openBrowser
import com.skyd.anivu.model.preference.SettingsProvider
import com.skyd.anivu.ui.local.LocalDarkMode
import com.skyd.anivu.ui.local.LocalWindowSizeClass
import com.skyd.anivu.ui.theme.PodAuraTheme


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
            CompositionLocalProvider(
                LocalWindowSizeClass provides calculateWindowSizeClass(this)
            ) {
                SettingsProvider {
                    PodAuraTheme(darkTheme = LocalDarkMode.current) {
                        CrashScreen(
                            message = message,
                            onReport = {
                                Uri.parse(GITHUB_NEW_ISSUE_URL).openBrowser(this)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CrashScreen(
    message: String,
    onReport: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(it)
                .padding(20.dp)
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            Icon(
                modifier = Modifier.size(40.dp),
                imageVector = Icons.Outlined.BugReport,
                contentDescription = null,
            )

            Spacer(modifier = Modifier.height(30.dp))
            Text(
                text = stringResource(id = R.string.crashed),
                style = MaterialTheme.typography.headlineLarge,
            )

            Spacer(modifier = Modifier.height(30.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { message.copy(context) }) {
                    Text(text = stringResource(id = R.string.crash_screen_copy_crash_log))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(onClick = {
                    message.copy(context)
                    onReport()
                }) {
                    Text(text = stringResource(id = R.string.submit_an_issue_on_github))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.crash_screen_crash_log),
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(modifier = Modifier.height(10.dp))
            SelectionContainer {
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}