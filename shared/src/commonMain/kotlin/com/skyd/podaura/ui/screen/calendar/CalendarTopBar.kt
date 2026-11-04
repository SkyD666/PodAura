package com.skyd.podaura.ui.screen.calendar

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.calendar_screen_name
import podaura.shared.generated.resources.feed_screen_refresh_all_subscriptions

@Composable
internal fun CalendarTopBar(
    windowInsets: WindowInsets,
    scrollBehavior: TopAppBarScrollBehavior,
    refreshing: Boolean,
    onRefreshAllFeeds: () -> Unit,
) {
    ComponeTopBar(
        style = ComponeTopBarStyle.Small,
        title = { Text(stringResource(Res.string.calendar_screen_name)) },
        navigationIcon = {},
        actions = {
            ComponeIconButton(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = stringResource(Res.string.feed_screen_refresh_all_subscriptions),
                enabled = !refreshing,
                onClick = onRefreshAllFeeds,
            )
        },
        windowInsets = windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        scrollBehavior = scrollBehavior,
    )
}
