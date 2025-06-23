package com.skyd.podaura.ui.screen.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.skyd.compone.component.BackInvoker
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.podaura.ext.put
import com.skyd.podaura.model.preference.AcceptTermsPreference
import com.skyd.podaura.model.preference.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.terms_of_service_screen_agree
import podaura.shared.generated.resources.terms_of_service_screen_disagree
import podaura.shared.generated.resources.terms_of_service_screen_name
import podaura.shared.generated.resources.terms_of_service_screen_tip
import podaura.shared.generated.resources.tos
import kotlin.system.exitProcess


@Serializable
data object TermsOfServiceRoute

@Composable
fun TermsOfServiceScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()
    val backInvoker = BackInvoker()

    val onDisagree = {
        scope.launch(Dispatchers.IO) {
            dataStore.put(AcceptTermsPreference.key, false)
            withContext(Dispatchers.Main) {
                exitProcess(0)
            }
        }
    }

    BackHandler {
        onDisagree()
    }

    Scaffold(
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.LargeFlexible,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(Res.string.terms_of_service_screen_name)) },
                navigationIcon = {},
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(Res.string.terms_of_service_screen_tip),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(Res.string.tos).trim(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                TextButton(onClick = { onDisagree() }) {
                    Text(text = stringResource(Res.string.terms_of_service_screen_disagree))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            dataStore.put(AcceptTermsPreference.key, true)
                            withContext(Dispatchers.Main) {
                                backInvoker.invoke()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(Res.string.terms_of_service_screen_agree))
                }
            }
        }
    }
}