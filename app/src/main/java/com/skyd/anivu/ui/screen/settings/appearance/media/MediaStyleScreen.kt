package com.skyd.anivu.ui.screen.settings.appearance.media

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Toc
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.skyd.anivu.R
import com.skyd.anivu.model.preference.appearance.media.MediaShowGroupTabPreference
import com.skyd.anivu.model.preference.appearance.media.MediaShowThumbnailPreference
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.component.CategorySettingsItem
import com.skyd.anivu.ui.component.SwitchSettingsItem
import com.skyd.anivu.ui.local.LocalMediaShowGroupTab
import com.skyd.anivu.ui.local.LocalMediaShowThumbnail


const val MEDIA_STYLE_SCREEN_ROUTE = "mediaStyleScreen"

@Composable
fun MediaStyleScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(R.string.media_style_screen_name)) },
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
                CategorySettingsItem(text = stringResource(id = R.string.media_style_screen_media_list_category))
            }
            item {
                val mediaShowThumbnail = LocalMediaShowThumbnail.current
                SwitchSettingsItem(
                    imageVector = if (mediaShowThumbnail) Icons.Outlined.Image else Icons.Outlined.HideImage,
                    text = stringResource(id = R.string.media_style_screen_media_list_show_thumbnail),
                    checked = mediaShowThumbnail,
                    onCheckedChange = {
                        MediaShowThumbnailPreference.put(
                            context = context,
                            scope = scope,
                            value = it,
                        )
                    }
                )
            }
            item {
                val mediaShowGroupTab = LocalMediaShowGroupTab.current
                SwitchSettingsItem(
                    imageVector = Icons.AutoMirrored.Outlined.Toc,
                    text = stringResource(id = R.string.media_style_screen_media_list_show_group_tab),
                    checked = mediaShowGroupTab,
                    onCheckedChange = {
                        MediaShowGroupTabPreference.put(
                            context = context,
                            scope = scope,
                            value = it,
                        )
                    }
                )
            }
        }
    }
}