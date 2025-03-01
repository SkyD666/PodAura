package com.skyd.anivu.ui.screen.media.sub

import android.os.Bundle
import androidx.compose.foundation.basicMarquee
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.skyd.anivu.R
import com.skyd.anivu.ext.navigate
import com.skyd.anivu.ext.popBackStackWithLifecycle
import com.skyd.anivu.model.bean.MediaBean
import com.skyd.anivu.model.preference.behavior.media.BaseMediaListSortByPreference
import com.skyd.anivu.model.preference.behavior.media.MediaSubListSortAscPreference
import com.skyd.anivu.model.preference.behavior.media.MediaSubListSortByPreference
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.component.dialog.PodAuraDialog
import com.skyd.anivu.ui.component.dialog.SortDialog
import com.skyd.anivu.ui.local.LocalMediaSubListSortAsc
import com.skyd.anivu.ui.local.LocalMediaSubListSortBy
import com.skyd.anivu.ui.local.LocalNavController
import com.skyd.anivu.ui.screen.media.list.MediaList


const val SUB_MEDIA_SCREEN_ROUTE = "subMediaScreen"
const val SUB_MEDIA_SCREEN_MEDIA_KEY = "media"

fun openSubMediaScreen(
    navController: NavController,
    media: MediaBean,
    navOptions: NavOptions? = null,
) {
    navController.navigate(
        SUB_MEDIA_SCREEN_ROUTE,
        Bundle().apply {
            putSerializable(SUB_MEDIA_SCREEN_MEDIA_KEY, media)
        },
        navOptions = navOptions,
    )
}

@Composable
fun SubMediaScreenRoute(media: MediaBean?) {
    val navController = LocalNavController.current
    if (media == null || !media.file.exists() || !media.isDir) {
        PodAuraDialog(
            icon = {
                Icon(imageVector = Icons.Outlined.WarningAmber, contentDescription = null)
            },
            title = { Text(text = stringResource(id = R.string.warning)) },
            text = { Text(text = stringResource(id = R.string.sub_media_screen_path_illegal)) },
            confirmButton = {
                TextButton(onClick = { navController.popBackStackWithLifecycle() }) {
                    Text(text = stringResource(id = R.string.exit))
                }
            },
        )
    } else {
        SubMediaScreen(media)
    }
}

@Composable
private fun SubMediaScreen(media: MediaBean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showSortMediaDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Large,
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                        text = media.displayName ?: media.name,
                        maxLines = 1,
                    )
                },
                actions = {
                    PodAuraIconButton(
                        onClick = { showSortMediaDialog = true },
                        imageVector = Icons.AutoMirrored.Outlined.Sort,
                        contentDescription = stringResource(id = R.string.sort),
                    )
                }
            )
        }
    ) { paddingValues ->
        MediaList(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues,
            path = media.file.path,
            isSubList = true,
        )
    }

    SortDialog(
        visible = showSortMediaDialog,
        onDismissRequest = { showSortMediaDialog = false },
        sortByValues = MediaSubListSortByPreference.values,
        sortBy = LocalMediaSubListSortBy.current,
        sortAsc = LocalMediaSubListSortAsc.current,
        onSortBy = { MediaSubListSortByPreference.put(context, scope, it) },
        onSortAsc = { MediaSubListSortAscPreference.put(context, scope, it) },
        onSortByDisplayName = { BaseMediaListSortByPreference.toDisplayName(context, it) },
        onSortByIcon = { BaseMediaListSortByPreference.toIcon(it) },
    )
}
