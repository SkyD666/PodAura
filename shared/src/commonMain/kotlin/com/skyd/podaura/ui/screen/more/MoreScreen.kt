package com.skyd.podaura.ui.screen.more

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.component.navigation.LocalNavBackStack
import com.skyd.compone.ext.plus
import com.skyd.podaura.ext.isCompact
import com.skyd.podaura.model.bean.MoreBean
import com.skyd.podaura.ui.local.LocalWindowSizeClass
import com.skyd.podaura.ui.screen.about.AboutRoute
import com.skyd.podaura.ui.screen.download.DownloadRoute
import com.skyd.podaura.ui.screen.history.HistoryRoute
import com.skyd.podaura.ui.screen.settings.SettingsRoute
import com.skyd.podaura.ui.screen.settings.data.importexport.ImportExportRoute
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.about_screen_name
import podaura.shared.generated.resources.download_screen_name
import podaura.shared.generated.resources.history_screen_name
import podaura.shared.generated.resources.import_export_screen_name
import podaura.shared.generated.resources.more_screen_name
import podaura.shared.generated.resources.settings


@Serializable
data object MoreRoute : NavKey

@Composable
fun MoreScreen() {
    val navBackStack = LocalNavBackStack.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val windowSizeClass = LocalWindowSizeClass.current

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.Small,
                title = { Text(text = stringResource(Res.string.more_screen_name)) },
                navigationIcon = {},
                windowInsets =
                    if (windowSizeClass.isCompact)
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                    else
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.End),
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets =
            if (windowSizeClass.isCompact)
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            else
                WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
    ) { innerPadding ->
        val colorScheme: ColorScheme = MaterialTheme.colorScheme
        var dataList by remember { mutableStateOf(emptyList<MoreBean>()) }

        LaunchedEffect(Unit) {
            dataList = getMoreBeanList(colorScheme, navBackStack)
        }

        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = innerPadding + PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            columns = GridCells.Adaptive(135.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(dataList) { item ->
                MoreItem(
                    data = item,
                    onClickListener = { data -> data.action.invoke() }
                )
            }
        }
    }
}

@Composable
fun MoreItem(
    data: MoreBean,
    onClickListener: ((data: MoreBean) -> Unit)? = null
) {
    OutlinedCard(shape = RoundedCornerShape(16)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        onClickListener?.invoke(data)
                    }
                )
                .padding(25.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .padding(5.dp)
                    .background(
                        color = data.shapeColor,
                        shape = data.shape.toShape(),
                    )
                    .padding(16.dp)
            ) {
                Icon(
                    modifier = Modifier.size(35.dp),
                    imageVector = data.icon,
                    contentDescription = null,
                    tint = data.iconTint
                )
            }
            Text(
                modifier = Modifier
                    .padding(top = 15.dp)
                    .basicMarquee(iterations = Int.MAX_VALUE),
                text = data.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

private suspend fun getMoreBeanList(
    colorScheme: ColorScheme,
    navBackStack: MutableList<NavKey>,
): MutableList<MoreBean> {
    return mutableListOf(
        MoreBean(
            title = getString(Res.string.history_screen_name),
            icon = Icons.Outlined.History,
            iconTint = colorScheme.onPrimary,
            shape = MaterialShapes.Pill,
            shapeColor = colorScheme.primary,
            action = { navBackStack.add(HistoryRoute) },
        ),
        MoreBean(
            title = getString(Res.string.download_screen_name),
            icon = Icons.Outlined.Download,
            iconTint = colorScheme.onSecondary,
            shape = MaterialShapes.Clover8Leaf,
            shapeColor = colorScheme.secondary,
            action = { navBackStack.add(DownloadRoute()) },
        ),
        MoreBean(
            title = getString(Res.string.import_export_screen_name),
            icon = Icons.Outlined.SwapVert,
            iconTint = colorScheme.onTertiary,
            shape = MaterialShapes.Clover4Leaf,
            shapeColor = colorScheme.tertiary,
            action = { navBackStack.add(ImportExportRoute) },
        ),
        MoreBean(
            title = getString(Res.string.settings),
            icon = Icons.Outlined.Settings,
            iconTint = colorScheme.onPrimary,
            shape = MaterialShapes.Slanted,
            shapeColor = colorScheme.primary,
            action = { navBackStack.add(SettingsRoute) },
        ),
        MoreBean(
            title = getString(Res.string.about_screen_name),
            icon = Icons.Outlined.Info,
            iconTint = colorScheme.onSecondary,
            shape = MaterialShapes.Cookie12Sided,
            shapeColor = colorScheme.secondary,
            action = { navBackStack.add(AboutRoute) }
        ),
    )
}