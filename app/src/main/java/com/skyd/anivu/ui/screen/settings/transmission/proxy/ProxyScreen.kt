package com.skyd.anivu.ui.screen.settings.transmission.proxy

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Http
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Podcasts
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.VpnKeyOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.skyd.anivu.R
import com.skyd.anivu.model.preference.proxy.ProxyHostnamePreference
import com.skyd.anivu.model.preference.proxy.ProxyModePreference
import com.skyd.anivu.model.preference.proxy.ProxyPasswordPreference
import com.skyd.anivu.model.preference.proxy.ProxyPortPreference
import com.skyd.anivu.model.preference.proxy.ProxyTypePreference
import com.skyd.anivu.model.preference.proxy.ProxyUsernamePreference
import com.skyd.anivu.model.preference.proxy.UseProxyPreference
import com.skyd.anivu.ui.component.BannerItem
import com.skyd.anivu.ui.component.BaseSettingsItem
import com.skyd.anivu.ui.component.CheckableListMenu
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.component.SwitchSettingsItem
import com.skyd.anivu.ui.component.dialog.TextFieldDialog
import com.skyd.anivu.ui.local.LocalProxyHostname
import com.skyd.anivu.ui.local.LocalProxyMode
import com.skyd.anivu.ui.local.LocalProxyPassword
import com.skyd.anivu.ui.local.LocalProxyPort
import com.skyd.anivu.ui.local.LocalProxyType
import com.skyd.anivu.ui.local.LocalProxyUsername
import com.skyd.anivu.ui.local.LocalUseProxy
import kotlinx.serialization.Serializable


@Serializable
data object ProxyRoute

@Composable
fun ProxyScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var expandProxyModeMenu by rememberSaveable { mutableStateOf(false) }
    var expandProxyTypeMenu by rememberSaveable { mutableStateOf(false) }
    var openEditProxyHostnameDialog by rememberSaveable { mutableStateOf(false) }
    var openEditProxyPortDialog by rememberSaveable { mutableStateOf(false) }
    var openEditProxyUsernameDialog by rememberSaveable { mutableStateOf(false) }
    var openEditProxyPasswordDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(R.string.proxy_screen_name)) },
            )
        }
    ) { paddingValues ->
        val useProxy = LocalUseProxy.current
        val proxyModeManual = LocalProxyMode.current == ProxyModePreference.MANUAL_MODE

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues,
        ) {
            item {
                BannerItem {
                    SwitchSettingsItem(
                        imageVector = if (useProxy) Icons.Outlined.VpnKey else Icons.Outlined.VpnKeyOff,
                        text = stringResource(id = R.string.proxy_screen_use_proxy),
                        checked = useProxy,
                        onCheckedChange = { UseProxyPreference.put(context, scope, it) }
                    )
                }
            }
            item {
                BaseSettingsItem(
                    icon = null,
                    text = stringResource(id = R.string.proxy_screen_mode),
                    descriptionText = ProxyModePreference.toDisplayName(
                        context, LocalProxyMode.current,
                    ),
                    enabled = useProxy,
                    extraContent = {
                        ProxyModeMenu(
                            expanded = expandProxyModeMenu,
                            onDismissRequest = { expandProxyModeMenu = false }
                        )
                    },
                    onClick = { expandProxyModeMenu = true },
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(image = Icons.Outlined.Http),
                    text = stringResource(id = R.string.proxy_screen_type),
                    descriptionText = LocalProxyType.current,
                    enabled = useProxy && proxyModeManual,
                    extraContent = {
                        ProxyTypeMenu(
                            expanded = expandProxyTypeMenu,
                            onDismissRequest = { expandProxyTypeMenu = false }
                        )
                    },
                    onClick = { expandProxyTypeMenu = true },
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(image = Icons.Outlined.Devices),
                    text = stringResource(id = R.string.proxy_screen_hostname),
                    descriptionText = LocalProxyHostname.current,
                    enabled = useProxy && proxyModeManual,
                    extraContent = {
                        EditProxyHostnameDialog(
                            visible = openEditProxyHostnameDialog,
                            onDismissRequest = { openEditProxyHostnameDialog = false }
                        )
                    },
                    onClick = { openEditProxyHostnameDialog = true },
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(image = Icons.Outlined.Podcasts),
                    text = stringResource(id = R.string.proxy_screen_port),
                    descriptionText = LocalProxyPort.current.toString(),
                    enabled = useProxy && proxyModeManual,
                    extraContent = {
                        EditProxyPortDialog(
                            visible = openEditProxyPortDialog,
                            onDismissRequest = { openEditProxyPortDialog = false }
                        )
                    },
                    onClick = { openEditProxyPortDialog = true },
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(image = Icons.Outlined.Person),
                    text = stringResource(id = R.string.proxy_screen_username),
                    descriptionText = LocalProxyUsername.current.ifBlank { null },
                    enabled = useProxy && proxyModeManual,
                    extraContent = {
                        EditProxyUsernameDialog(
                            visible = openEditProxyUsernameDialog,
                            onDismissRequest = { openEditProxyUsernameDialog = false }
                        )
                    },
                    onClick = { openEditProxyUsernameDialog = true },
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(image = Icons.Outlined.Password),
                    text = stringResource(id = R.string.proxy_screen_password),
                    descriptionText = if (LocalProxyPassword.current.isBlank()) {
                        stringResource(R.string.not_configure)
                    } else stringResource(R.string.configured),
                    enabled = useProxy && proxyModeManual,
                    extraContent = {
                        EditProxyPasswordDialog(
                            visible = openEditProxyPasswordDialog,
                            onDismissRequest = { openEditProxyPasswordDialog = false }
                        )
                    },
                    onClick = { openEditProxyPasswordDialog = true },
                )
            }
        }
    }
}

@Composable
private fun ProxyModeMenu(expanded: Boolean, onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val proxyMode = LocalProxyMode.current

    CheckableListMenu(
        expanded = expanded,
        current = proxyMode,
        values = ProxyModePreference.values,
        displayName = { ProxyModePreference.toDisplayName(context, it) },
        onChecked = { ProxyModePreference.put(context, scope, it) },
        onDismissRequest = onDismissRequest,
    )
}

@Composable
private fun ProxyTypeMenu(expanded: Boolean, onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val proxyType = LocalProxyType.current

    CheckableListMenu(
        expanded = expanded,
        current = proxyType,
        values = ProxyTypePreference.values,
        displayName = { it },
        onChecked = { ProxyTypePreference.put(context, scope, it) },
        onDismissRequest = onDismissRequest,
    )
}

@Composable
private fun EditProxyHostnameDialog(visible: Boolean, onDismissRequest: () -> Unit) {
    if (visible) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val proxyHostname = LocalProxyHostname.current
        var currentHostname by rememberSaveable { mutableStateOf(proxyHostname) }

        TextFieldDialog(
            icon = { Icon(imageVector = Icons.Outlined.Devices, contentDescription = null) },
            titleText = stringResource(id = R.string.proxy_screen_hostname),
            value = currentHostname,
            onValueChange = { currentHostname = it },
            maxLines = 1,
            onConfirm = {
                ProxyHostnamePreference.put(context, scope, it)
                onDismissRequest()
            },
            onDismissRequest = onDismissRequest,
        )
    }
}

@Composable
private fun EditProxyPortDialog(visible: Boolean, onDismissRequest: () -> Unit) {
    if (visible) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val proxyPort = LocalProxyPort.current
        var currentPort by rememberSaveable { mutableStateOf(proxyPort.toString()) }

        TextFieldDialog(
            icon = { Icon(imageVector = Icons.Outlined.Podcasts, contentDescription = null) },
            titleText = stringResource(id = R.string.proxy_screen_port),
            value = currentPort,
            onValueChange = { currentPort = it },
            maxLines = 1,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Decimal,
            ),
            errorText = if ((currentPort.toIntOrNull() ?: -1) in 0..65535) ""
            else stringResource(R.string.proxy_screen_port_error_message),
            onConfirm = {
                runCatching {
                    val port = it.toInt()
                    check(port in 0..65535)
                    ProxyPortPreference.put(context, scope, port)
                    onDismissRequest()
                }
            },
            onDismissRequest = onDismissRequest,
        )
    }
}

@Composable
private fun EditProxyUsernameDialog(visible: Boolean, onDismissRequest: () -> Unit) {
    if (visible) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val proxyUsername = LocalProxyUsername.current
        var currentUsername by rememberSaveable { mutableStateOf(proxyUsername) }

        TextFieldDialog(
            icon = { Icon(imageVector = Icons.Outlined.Person, contentDescription = null) },
            titleText = stringResource(id = R.string.proxy_screen_username),
            value = currentUsername,
            onValueChange = { currentUsername = it },
            maxLines = 1,
            enableConfirm = { true },
            onConfirm = {
                ProxyUsernamePreference.put(context, scope, it)
                onDismissRequest()
            },
            onDismissRequest = onDismissRequest,
        )
    }
}

@Composable
private fun EditProxyPasswordDialog(visible: Boolean, onDismissRequest: () -> Unit) {
    if (visible) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val proxyPassword = LocalProxyPassword.current
        var currentPassword by rememberSaveable { mutableStateOf(proxyPassword) }

        TextFieldDialog(
            icon = { Icon(imageVector = Icons.Outlined.Password, contentDescription = null) },
            titleText = stringResource(id = R.string.proxy_screen_password),
            isPassword = true,
            value = currentPassword,
            onValueChange = { currentPassword = it },
            maxLines = 1,
            onConfirm = {
                ProxyPasswordPreference.put(context, scope, it)
                onDismissRequest()
            },
            onDismissRequest = onDismissRequest,
        )
    }
}