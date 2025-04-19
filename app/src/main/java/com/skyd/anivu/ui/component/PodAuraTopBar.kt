package com.skyd.anivu.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.skyd.anivu.R
import com.skyd.anivu.ext.activity
import com.skyd.anivu.ext.popBackStackWithLifecycle
import com.skyd.anivu.ui.local.LocalGlobalNavController
import com.skyd.anivu.ui.local.LocalNavController

enum class PodAuraTopBarStyle {
    Small, Large, CenterAligned
}

@Composable
fun PodAuraTopBar(
    style: PodAuraTopBarStyle = PodAuraTopBarStyle.Small,
    title: @Composable () -> Unit,
    contentPadding: @Composable () -> PaddingValues = { PaddingValues() },
    navigationIcon: @Composable () -> Unit = { BackIcon() },
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val topBarModifier = Modifier.padding(contentPadding())
    when (style) {
        PodAuraTopBarStyle.Small -> {
            TopAppBar(
                title = title,
                modifier = topBarModifier,
                navigationIcon = navigationIcon,
                actions = actions,
                windowInsets = windowInsets,
                colors = colors,
                scrollBehavior = scrollBehavior
            )
        }

        PodAuraTopBarStyle.Large -> {
            LargeTopAppBar(
                modifier = topBarModifier,
                title = title,
                navigationIcon = navigationIcon,
                actions = actions,
                windowInsets = windowInsets,
                colors = colors,
                scrollBehavior = scrollBehavior
            )
        }

        PodAuraTopBarStyle.CenterAligned -> {
            CenterAlignedTopAppBar(
                modifier = topBarModifier,
                title = title,
                navigationIcon = navigationIcon,
                actions = actions,
                windowInsets = windowInsets,
                colors = colors,
                scrollBehavior = scrollBehavior
            )
        }
    }
}

@Composable
fun BackIcon() {
    val navController = LocalNavController.current
    val globalNavController = LocalGlobalNavController.current
    val context = LocalContext.current
    BackIcon {
        if (!navController.popBackStackWithLifecycle() && globalNavController == navController) {
            context.activity.finish()
        }
    }
}

val DefaultBackClick = { }

@Composable
fun BackIcon(onClick: () -> Unit = {}) {
    PodAuraIconButton(
        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
        contentDescription = stringResource(id = R.string.back),
        onClick = onClick
    )
}
