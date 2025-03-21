package com.skyd.anivu.ui.screen.settings.data.deleteconstraint

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.skyd.anivu.R
import com.skyd.anivu.model.preference.data.delete.KeepFavoriteArticlesPreference
import com.skyd.anivu.model.preference.data.delete.KeepPlaylistArticlesPreference
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.component.SwitchSettingsItem
import com.skyd.anivu.ui.component.TipSettingsItem
import com.skyd.anivu.ui.local.LocalKeepFavoriteArticles
import com.skyd.anivu.ui.local.LocalKeepPlaylistArticles
import com.skyd.anivu.ui.local.LocalKeepUnreadArticles


const val DELETE_CONSTRAINT_SCREEN_ROUTE = "deleteConstraintScreen"

@Composable
fun DeleteConstraintScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(R.string.delete_constraint_screen_name)) },
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues,
        ) {
            item {
                SwitchSettingsItem(
                    checked = LocalKeepUnreadArticles.current,
                    imageVector = Icons.Outlined.MarkEmailUnread,
                    text = stringResource(id = R.string.delete_constraint_screen_keep_unread_articles),
                    onCheckedChange = { KeepPlaylistArticlesPreference.put(context, scope, it) },
                )
            }
            item {
                SwitchSettingsItem(
                    checked = LocalKeepFavoriteArticles.current,
                    imageVector = Icons.Outlined.Favorite,
                    text = stringResource(id = R.string.delete_constraint_screen_keep_favorite_articles),
                    onCheckedChange = { KeepFavoriteArticlesPreference.put(context, scope, it) },
                )
            }
            item {
                SwitchSettingsItem(
                    checked = LocalKeepPlaylistArticles.current,
                    imageVector = Icons.AutoMirrored.Outlined.PlaylistPlay,
                    text = stringResource(id = R.string.delete_constraint_screen_keep_playlist_articles),
                    description = stringResource(id = R.string.delete_constraint_screen_keep_playlist_articles_description),
                    onCheckedChange = { KeepPlaylistArticlesPreference.put(context, scope, it) },
                )
            }
            item {
                TipSettingsItem(stringResource(R.string.delete_constraint_screen_options_tip))
            }
        }
    }
}