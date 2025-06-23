package com.skyd.podaura.ui.screen.settings

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import com.skyd.compone.ext.setText
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.crash_screen_copy_crash_log
import podaura.shared.generated.resources.crash_screen_crash_log
import podaura.shared.generated.resources.crashed
import podaura.shared.generated.resources.submit_an_issue_on_github

@Composable
fun CrashScreen(
    message: String,
    onReport: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
                text = stringResource(Res.string.crashed),
                style = MaterialTheme.typography.headlineLarge,
            )

            Spacer(modifier = Modifier.height(30.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                val clipboard = LocalClipboard.current
                TextButton(onClick = { scope.launch { clipboard.setText(message) } }) {
                    Text(text = stringResource(Res.string.crash_screen_copy_crash_log))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(onClick = {
                    scope.launch { clipboard.setText(message) }
                    onReport()
                }) {
                    Text(text = stringResource(Res.string.submit_an_issue_on_github))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(Res.string.crash_screen_crash_log),
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(modifier = Modifier.height(10.dp))
            SelectionContainer {
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}