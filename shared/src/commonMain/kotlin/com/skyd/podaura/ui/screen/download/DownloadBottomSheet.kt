package com.skyd.podaura.ui.screen.download

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import com.skyd.podaura.model.download.DownloadInfoBean
import com.skyd.podaura.ui.component.AnimatedDismissModalBottomSheet
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.close
import podaura.shared.generated.resources.download_screen_add_download
import podaura.shared.generated.resources.download_screen_name

data class DownloadListFocus(
    val queued: Set<String> = emptySet(),
    val existing: Set<String> = emptySet(),
) {
    internal fun initialIndex(items: List<DownloadInfoBean>): Int {
        if (queued.isEmpty() && existing.isEmpty()) return 0
        val added = items.indexOfFirst { it.id in queued }
        if (added >= 0) return added
        return items.indexOfFirst { it.id in existing }.coerceAtLeast(0)
    }
}

@Composable
fun DownloadBottomSheet(
    loadFocus: suspend () -> DownloadListFocus,
    onDismissRequest: () -> Unit,
) {
    val focus by produceState<DownloadListFocus?>(initialValue = null, key1 = loadFocus) {
        value = loadFocus()
    }
    var link by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbarHost = remember { SnackbarHostState() }
    val viewModel =
        koinViewModel<DownloadViewModel>(viewModelStoreOwner = rememberViewModelStoreOwner())

    DownloadSheetLayout(
        onDismissRequest = onDismissRequest,
        onAdd = { link = "" },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            DownloadContent(
                viewModel = viewModel,
                snackbarHostState = snackbarHost,
                contentPadding = PaddingValues(bottom = 16.dp),
                focus = focus,
            )
            SnackbarHost(
                hostState = snackbarHost,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
    AddDownloadDialog(
        link = link,
        onLinkChange = { link = it },
        snackbarHostState = snackbarHost,
    )
}

@Composable
internal fun DownloadSheetLayout(
    onDismissRequest: () -> Unit,
    onAdd: () -> Unit,
    windowInsets: @Composable () -> WindowInsets = { WindowInsets.safeDrawing },
    content: @Composable () -> Unit,
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.PartiallyExpanded, SheetValue.Expanded),
    )
    AnimatedDismissModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        contentWindowInsets = { windowInsets().only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top) },
    ) { dismiss ->
        val density = LocalDensity.current
        var visibleHeight by remember { mutableStateOf(0.dp) }
        // The partial anchor moves the bottom of the sheet outside the window.
        // Size the list to the visible viewport so its final items can be brought into view.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    visibleHeight = with(density) { coordinates.boundsInWindow().height.toDp() }
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(visibleHeight)
                    .windowInsetsPadding(windowInsets().only(WindowInsetsSides.Bottom)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.download_screen_name),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    IconButton(onClick = onAdd) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = stringResource(Res.string.download_screen_add_download),
                        )
                    }
                    IconButton(onClick = dismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(Res.string.close),
                        )
                    }
                }
                content()
            }
        }
    }
}
