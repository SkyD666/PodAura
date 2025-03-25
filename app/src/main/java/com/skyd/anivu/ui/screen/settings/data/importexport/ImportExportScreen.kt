package com.skyd.anivu.ui.screen.settings.data.importexport

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import com.skyd.anivu.R
import com.skyd.anivu.ui.component.BaseSettingsItem
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.local.LocalNavController
import com.skyd.anivu.ui.screen.settings.data.importexport.exportopml.ExportOpmlRoute
import com.skyd.anivu.ui.screen.settings.data.importexport.importopml.ImportOpmlRoute
import kotlinx.serialization.Serializable


@Serializable
data object ImportExportRoute

@Composable
fun ImportExportScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val navController = LocalNavController.current
    Scaffold(
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(R.string.import_export_screen_name)) },
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
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.FileDownload),
                    text = stringResource(id = R.string.import_opml_screen_name),
                    descriptionText = null,
                    onClick = { navController.navigate(ImportOpmlRoute()) }
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.FileUpload),
                    text = stringResource(id = R.string.export_opml_screen_name),
                    descriptionText = null,
                    onClick = { navController.navigate(ExportOpmlRoute) }
                )
            }
        }
    }
}
