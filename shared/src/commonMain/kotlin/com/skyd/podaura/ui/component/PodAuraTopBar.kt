package com.skyd.podaura.ui.component

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
import com.skyd.podaura.ext.popBackStackWithLifecycle
import com.skyd.podaura.ui.local.LocalGlobalNavController
import com.skyd.podaura.ui.local.LocalNavController
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.back

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
expect fun onEmptyPopBackStack(): () -> Unit

@Composable
fun BackInvoker(): () -> Unit {
    val navController = LocalNavController.current
    val globalNavController = LocalGlobalNavController.current
    val onEmptyPopBackStack = onEmptyPopBackStack()
    return {
        if (!navController.popBackStackWithLifecycle() && globalNavController == navController) {
            onEmptyPopBackStack.invoke()
        }
    }
}

@Composable
fun BackIcon() {
    val backInvoker = BackInvoker()
    BackIcon { backInvoker.invoke() }
}

val DefaultBackClick = { }

@Composable
fun BackIcon(onClick: () -> Unit = {}) {
    PodAuraIconButton(
        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
        contentDescription = stringResource(Res.string.back),
        onClick = onClick
    )
}
