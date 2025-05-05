package com.skyd.podaura.ui.screen.media.sub

import androidx.compose.foundation.basicMarquee
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Search
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
import androidx.navigation.NavBackStackEntry
import androidx.navigation.toRoute
import com.skyd.podaura.ext.exists
import com.skyd.podaura.ext.popBackStackWithLifecycle
import com.skyd.podaura.model.bean.MediaBean
import com.skyd.podaura.model.preference.behavior.media.BaseMediaListSortByPreference
import com.skyd.podaura.model.preference.behavior.media.MediaSubListSortAscPreference
import com.skyd.podaura.model.preference.behavior.media.MediaSubListSortByPreference
import com.skyd.podaura.ui.component.PodAuraIconButton
import com.skyd.podaura.ui.component.PodAuraTopBar
import com.skyd.podaura.ui.component.PodAuraTopBarStyle
import com.skyd.podaura.ui.component.dialog.PodAuraDialog
import com.skyd.podaura.ui.component.dialog.SortDialog
import com.skyd.podaura.ui.component.serializableType
import com.skyd.podaura.ui.local.LocalNavController
import com.skyd.podaura.ui.screen.media.list.MediaList
import com.skyd.podaura.ui.screen.media.search.MediaSearchRoute
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.exit
import podaura.shared.generated.resources.media_screen_search_hint
import podaura.shared.generated.resources.sort
import podaura.shared.generated.resources.sub_media_screen_path_illegal
import podaura.shared.generated.resources.warning
import kotlin.reflect.typeOf


@Serializable
data class SubMediaRoute(val media: MediaBean) {
    companion object {
        val typeMap = mapOf(typeOf<MediaBean>() to serializableType<MediaBean>())

        @Composable
        fun SubMediaLauncher(entry: NavBackStackEntry) {
            SubMediaScreenRoute(media = entry.toRoute<SubMediaRoute>().media)
        }
    }
}

@Composable
fun SubMediaScreenRoute(media: MediaBean?) {
    val navController = LocalNavController.current
    if (media == null || !media.path.exists() || !media.isDir) {
        PodAuraDialog(
            icon = {
                Icon(imageVector = Icons.Outlined.WarningAmber, contentDescription = null)
            },
            title = { Text(text = stringResource(Res.string.warning)) },
            text = { Text(text = stringResource(Res.string.sub_media_screen_path_illegal)) },
            confirmButton = {
                TextButton(onClick = { navController.popBackStackWithLifecycle() }) {
                    Text(text = stringResource(Res.string.exit))
                }
            },
        )
    } else {
        SubMediaScreen(media)
    }
}

@Composable
private fun SubMediaScreen(media: MediaBean) {
    val scope = rememberCoroutineScope()
    val navController = LocalNavController.current
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
                        onClick = {
                            navController.navigate(
                                MediaSearchRoute(path = media.filePath, isSubList = true)
                            )
                        },
                        imageVector = Icons.Outlined.Search,
                        contentDescription = stringResource(Res.string.media_screen_search_hint),
                    )
                    PodAuraIconButton(
                        onClick = { showSortMediaDialog = true },
                        imageVector = Icons.AutoMirrored.Outlined.Sort,
                        contentDescription = stringResource(Res.string.sort),
                    )
                }
            )
        }
    ) { paddingValues ->
        MediaList(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues,
            path = media.filePath,
            isSubList = true,
        )
    }

    SortDialog(
        visible = showSortMediaDialog,
        onDismissRequest = { showSortMediaDialog = false },
        sortByValues = MediaSubListSortByPreference.values,
        sortBy = MediaSubListSortByPreference.current,
        sortAsc = MediaSubListSortAscPreference.current,
        onSortBy = { MediaSubListSortByPreference.put(scope, it) },
        onSortAsc = { MediaSubListSortAscPreference.put(scope, it) },
        onSortByDisplayName = { BaseMediaListSortByPreference.toDisplayName(it) },
        onSortByIcon = { BaseMediaListSortByPreference.toIcon(it) },
    )
}
