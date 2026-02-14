package com.skyd.podaura.ui.screen

import androidx.compose.runtime.Composable

@Composable
internal actual fun PermissionChecker(onMainContent: @Composable (() -> Unit)) = onMainContent()
