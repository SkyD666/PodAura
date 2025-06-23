package com.skyd.podaura.ui.screen.settings.data.deleteconstraint

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.podaura.model.preference.data.delete.KeepFavoriteArticlesPreference
import com.skyd.podaura.model.preference.data.delete.KeepPlaylistArticlesPreference
import com.skyd.podaura.model.preference.data.delete.KeepUnreadArticlesPreference
import com.skyd.settings.SettingsLazyColumn
import com.skyd.settings.SwitchSettingsItem
import com.skyd.settings.TipSettingsItem
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.delete_constraint_screen_keep_favorite_articles
import podaura.shared.generated.resources.delete_constraint_screen_keep_playlist_articles
import podaura.shared.generated.resources.delete_constraint_screen_keep_playlist_articles_description
import podaura.shared.generated.resources.delete_constraint_screen_keep_unread_articles
import podaura.shared.generated.resources.delete_constraint_screen_name
import podaura.shared.generated.resources.delete_constraint_screen_options_tip


@Serializable
data object DeleteConstraintRoute

@Composable
fun DeleteConstraintScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.LargeFlexible,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(Res.string.delete_constraint_screen_name)) },
            )
        }
    ) { paddingValues ->
        SettingsLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues,
        ) {
            group {
                item {
                    SwitchSettingsItem(
                        checked = KeepUnreadArticlesPreference.current,
                        imageVector = Icons.Outlined.MarkEmailUnread,
                        text = stringResource(Res.string.delete_constraint_screen_keep_unread_articles),
                        onCheckedChange = { KeepUnreadArticlesPreference.put(scope, it) },
                    )
                }
                item {
                    SwitchSettingsItem(
                        checked = KeepFavoriteArticlesPreference.current,
                        imageVector = Icons.Outlined.Favorite,
                        text = stringResource(Res.string.delete_constraint_screen_keep_favorite_articles),
                        onCheckedChange = { KeepFavoriteArticlesPreference.put(scope, it) },
                    )
                }
                item {
                    SwitchSettingsItem(
                        checked = KeepPlaylistArticlesPreference.current,
                        imageVector = Icons.AutoMirrored.Outlined.PlaylistPlay,
                        text = stringResource(Res.string.delete_constraint_screen_keep_playlist_articles),
                        description = stringResource(Res.string.delete_constraint_screen_keep_playlist_articles_description),
                        onCheckedChange = { KeepPlaylistArticlesPreference.put(scope, it) },
                    )
                }
                otherItem {
                    TipSettingsItem(stringResource(Res.string.delete_constraint_screen_options_tip))
                }
            }
        }
    }
}