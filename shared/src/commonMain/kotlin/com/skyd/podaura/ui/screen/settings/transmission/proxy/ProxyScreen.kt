package com.skyd.podaura.ui.screen.settings.transmission.proxy

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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.skyd.podaura.model.preference.proxy.ProxyHostnamePreference
import com.skyd.podaura.model.preference.proxy.ProxyModePreference
import com.skyd.podaura.model.preference.proxy.ProxyPasswordPreference
import com.skyd.podaura.model.preference.proxy.ProxyPortPreference
import com.skyd.podaura.model.preference.proxy.ProxyTypePreference
import com.skyd.podaura.model.preference.proxy.ProxyUsernamePreference
import com.skyd.podaura.model.preference.proxy.UseProxyPreference
import com.skyd.podaura.ui.component.BannerItem
import com.skyd.podaura.ui.component.BaseSettingsItem
import com.skyd.podaura.ui.component.CheckableListMenu
import com.skyd.podaura.ui.component.PodAuraTopBar
import com.skyd.podaura.ui.component.PodAuraTopBarStyle
import com.skyd.podaura.ui.component.SwitchSettingsItem
import com.skyd.podaura.ui.component.dialog.TextFieldDialog
import com.skyd.podaura.ui.component.suspendString
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.configured
import podaura.shared.generated.resources.not_configure
import podaura.shared.generated.resources.proxy_screen_hostname
import podaura.shared.generated.resources.proxy_screen_mode
import podaura.shared.generated.resources.proxy_screen_name
import podaura.shared.generated.resources.proxy_screen_password
import podaura.shared.generated.resources.proxy_screen_port
import podaura.shared.generated.resources.proxy_screen_port_error_message
import podaura.shared.generated.resources.proxy_screen_type
import podaura.shared.generated.resources.proxy_screen_use_proxy
import podaura.shared.generated.resources.proxy_screen_username


@Serializable
data object ProxyRoute

@Composable
fun ProxyScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
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
                title = { Text(text = stringResource(Res.string.proxy_screen_name)) },
            )
        }
    ) { paddingValues ->
        val useProxy = UseProxyPreference.current
        val proxyModeManual = ProxyModePreference.current == ProxyModePreference.MANUAL_MODE

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
                        text = stringResource(Res.string.proxy_screen_use_proxy),
                        checked = useProxy,
                        onCheckedChange = { UseProxyPreference.put(scope, it) }
                    )
                }
            }
            item {
                BaseSettingsItem(
                    icon = null,
                    text = stringResource(Res.string.proxy_screen_mode),
                    descriptionText = suspendString(ProxyModePreference.current) {
                        ProxyModePreference.toDisplayName(it)
                    },
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
                    text = stringResource(Res.string.proxy_screen_type),
                    descriptionText = ProxyTypePreference.current,
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
                    text = stringResource(Res.string.proxy_screen_hostname),
                    descriptionText = ProxyHostnamePreference.current,
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
                    text = stringResource(Res.string.proxy_screen_port),
                    descriptionText = ProxyPortPreference.current.toString(),
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
                    text = stringResource(Res.string.proxy_screen_username),
                    descriptionText = ProxyUsernamePreference.current.ifBlank { null },
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
                    text = stringResource(Res.string.proxy_screen_password),
                    descriptionText = if (ProxyPasswordPreference.current.isBlank()) {
                        stringResource(Res.string.not_configure)
                    } else stringResource(Res.string.configured),
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
    val scope = rememberCoroutineScope()
    val proxyMode = ProxyModePreference.current

    CheckableListMenu(
        expanded = expanded,
        current = proxyMode,
        values = ProxyModePreference.values,
        displayName = { ProxyModePreference.toDisplayName(it) },
        onChecked = { ProxyModePreference.put(scope, it) },
        onDismissRequest = onDismissRequest,
    )
}

@Composable
private fun ProxyTypeMenu(expanded: Boolean, onDismissRequest: () -> Unit) {
    val scope = rememberCoroutineScope()
    val proxyType = ProxyTypePreference.current

    CheckableListMenu(
        expanded = expanded,
        current = proxyType,
        values = ProxyTypePreference.values,
        displayName = { it },
        onChecked = { ProxyTypePreference.put(scope, it) },
        onDismissRequest = onDismissRequest,
    )
}

@Composable
private fun EditProxyHostnameDialog(visible: Boolean, onDismissRequest: () -> Unit) {
    if (visible) {
        val scope = rememberCoroutineScope()
        val proxyHostname = ProxyHostnamePreference.current
        var currentHostname by rememberSaveable { mutableStateOf(proxyHostname) }

        TextFieldDialog(
            icon = { Icon(imageVector = Icons.Outlined.Devices, contentDescription = null) },
            titleText = stringResource(Res.string.proxy_screen_hostname),
            value = currentHostname,
            onValueChange = { currentHostname = it },
            maxLines = 1,
            onConfirm = {
                ProxyHostnamePreference.put(scope, it)
                onDismissRequest()
            },
            onDismissRequest = onDismissRequest,
        )
    }
}

@Composable
private fun EditProxyPortDialog(visible: Boolean, onDismissRequest: () -> Unit) {
    if (visible) {
        val scope = rememberCoroutineScope()
        val proxyPort = ProxyPortPreference.current
        var currentPort by rememberSaveable { mutableStateOf(proxyPort.toString()) }

        TextFieldDialog(
            icon = { Icon(imageVector = Icons.Outlined.Podcasts, contentDescription = null) },
            titleText = stringResource(Res.string.proxy_screen_port),
            value = currentPort,
            onValueChange = { currentPort = it },
            maxLines = 1,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Decimal,
            ),
            errorText = if ((currentPort.toIntOrNull() ?: -1) in 0..65535) ""
            else stringResource(Res.string.proxy_screen_port_error_message),
            onConfirm = {
                runCatching {
                    val port = it.toInt()
                    check(port in 0..65535)
                    ProxyPortPreference.put(scope, port)
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
        val scope = rememberCoroutineScope()
        val proxyUsername = ProxyUsernamePreference.current
        var currentUsername by rememberSaveable { mutableStateOf(proxyUsername) }

        TextFieldDialog(
            icon = { Icon(imageVector = Icons.Outlined.Person, contentDescription = null) },
            titleText = stringResource(Res.string.proxy_screen_username),
            value = currentUsername,
            onValueChange = { currentUsername = it },
            maxLines = 1,
            enableConfirm = { true },
            onConfirm = {
                ProxyUsernamePreference.put(scope, it)
                onDismissRequest()
            },
            onDismissRequest = onDismissRequest,
        )
    }
}

@Composable
private fun EditProxyPasswordDialog(visible: Boolean, onDismissRequest: () -> Unit) {
    if (visible) {
        val scope = rememberCoroutineScope()
        val proxyPassword = ProxyPasswordPreference.current
        var currentPassword by rememberSaveable { mutableStateOf(proxyPassword) }

        TextFieldDialog(
            icon = { Icon(imageVector = Icons.Outlined.Password, contentDescription = null) },
            titleText = stringResource(Res.string.proxy_screen_password),
            isPassword = true,
            value = currentPassword,
            onValueChange = { currentPassword = it },
            maxLines = 1,
            onConfirm = {
                ProxyPasswordPreference.put(scope, it)
                onDismissRequest()
            },
            onDismissRequest = onDismissRequest,
        )
    }
}