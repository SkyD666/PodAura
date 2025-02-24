package com.skyd.anivu.ui.screen.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skyd.anivu.R
import com.skyd.anivu.config.Const
import com.skyd.anivu.ext.getAppVersionName
import com.skyd.anivu.ext.isCompact
import com.skyd.anivu.ext.openBrowser
import com.skyd.anivu.ext.plus
import com.skyd.anivu.model.bean.OtherWorksBean
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.anivu.ui.component.PodAuraImage
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.component.dialog.PodAuraDialog
import com.skyd.anivu.ui.component.shape.CloverShape
import com.skyd.anivu.ui.component.shape.CurlyCornerShape
import com.skyd.anivu.ui.component.shape.SquircleShape
import com.skyd.anivu.ui.local.LocalNavController
import com.skyd.anivu.ui.local.LocalWindowSizeClass
import com.skyd.anivu.ui.screen.about.license.LICENSE_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.about.update.UpdateDialog
import kotlinx.coroutines.launch
import java.util.Calendar


const val ABOUT_SCREEN_ROUTE = "aboutScreen"

@Composable
fun AboutScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val navController = LocalNavController.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var openUpdateDialog by rememberSaveable { mutableStateOf(false) }
    var openSponsorDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(R.string.about_screen_name)) },
                actions = {
                    PodAuraIconButton(
                        imageVector = Icons.Outlined.Balance,
                        contentDescription = stringResource(id = R.string.license_screen_name),
                        onClick = { navController.navigate(LICENSE_SCREEN_ROUTE) }
                    )
                    PodAuraIconButton(
                        onClick = { openUpdateDialog = true },
                        imageVector = Icons.Outlined.Update,
                        contentDescription = stringResource(id = R.string.update_check)
                    )
                },
            )
        }
    ) { paddingValues ->
        val windowSizeClass = LocalWindowSizeClass.current
        val otherWorksList = rememberOtherWorksList()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues + PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (windowSizeClass.isCompact) {
                item { IconArea() }
                item { TextArea() }
                item {
                    HelpArea(
                        openSponsorDialog = openSponsorDialog,
                        onTranslateClick = { Const.TRANSLATION_URL.openBrowser(context) },
                        onSponsorDialogVisibleChange = { openSponsorDialog = it }
                    )
                    ButtonArea()
                }
            } else {
                item {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.weight(0.95f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            IconArea()
                            ButtonArea()
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            TextArea()
                            HelpArea(
                                openSponsorDialog = openSponsorDialog,
                                onTranslateClick = { Const.TRANSLATION_URL.openBrowser(context) },
                                onSponsorDialogVisibleChange = { openSponsorDialog = it }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            item {
                Text(
                    text = stringResource(R.string.about_screen_other_works),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            itemsIndexed(items = otherWorksList) { _, item ->
                OtherWorksItem(data = item)
            }
        }

        var isRetry by rememberSaveable { mutableStateOf(false) }

        if (openUpdateDialog) {
            UpdateDialog(
                isRetry = isRetry,
                onClosed = { openUpdateDialog = false },
                onSuccess = { isRetry = false },
                onError = { msg ->
                    isRetry = true
                    openUpdateDialog = false
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.update_check_failed, msg),
                            withDismissAction = true,
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun IconArea() {
    Box(
        modifier = Modifier
            .padding(16.dp)
            .size(120.dp)
    ) {
        Image(
            modifier = Modifier.aspectRatio(1f),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            painter = painterResource(id = R.drawable.ic_icon_24),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
            contentDescription = null
        )
        val c = remember { Calendar.getInstance() }
        val month = remember(c) { c.get(Calendar.MONTH) }
        val day = remember(c) { c.get(Calendar.DAY_OF_MONTH) }
        if (month == Calendar.DECEMBER && (day in 22..28)) {     // Xmas
            Image(
                modifier = Modifier
                    .fillMaxWidth(0.67f)
                    .aspectRatio(1f)
                    .rotate(15f)
                    .padding(end = 10.dp)
                    .align(Alignment.TopEnd),
                painter = painterResource(R.drawable.ic_santa_hat),
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun TextArea(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .padding(top = 12.dp)
            .fillMaxWidth(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BadgedBox(
            badge = {
                Badge {
                    val badgeNumber = rememberSaveable { context.getAppVersionName() }
                    Text(
                        text = badgeNumber,
                        modifier = Modifier.semantics { contentDescription = badgeNumber }
                    )
                }
            }
        ) {
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        }
        Card(
            modifier = Modifier.padding(top = 16.dp),
            shape = RoundedCornerShape(10)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.app_short_description),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    modifier = Modifier.padding(top = 12.dp),
                    text = stringResource(id = R.string.app_tech_stack_description),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun HelpArea(
    openSponsorDialog: Boolean,
    onTranslateClick: () -> Unit,
    onSponsorDialogVisibleChange: (Boolean) -> Unit,
) {
    Spacer(modifier = Modifier.height(16.dp))
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Button(
            onClick = onTranslateClick,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Icon(imageVector = Icons.Outlined.Translate, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = stringResource(id = R.string.help_translate), textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Button(
            onClick = { onSponsorDialogVisibleChange(true) },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Icon(imageVector = Icons.Outlined.Coffee, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = stringResource(id = R.string.sponsor), textAlign = TextAlign.Center)
        }
    }
    SponsorDialog(visible = openSponsorDialog, onClose = { onSponsorDialogVisibleChange(false) })
}

@Composable
private fun SponsorDialog(visible: Boolean, onClose: () -> Unit) {
    val context = LocalContext.current
    PodAuraDialog(
        visible = visible,
        onDismissRequest = onClose,
        icon = { Icon(imageVector = Icons.Outlined.Coffee, contentDescription = null) },
        title = { Text(text = stringResource(id = R.string.sponsor)) },
        selectable = false,
        text = {
            Column {
                Text(text = stringResource(id = R.string.sponsor_description))
                Spacer(modifier = Modifier.height(6.dp))
                ListItem(
                    modifier = Modifier.clickable {
                        Const.AFADIAN_LINK.openBrowser(context)
                        onClose()
                    },
                    headlineContent = { Text(text = stringResource(R.string.sponsor_afadian)) },
                    leadingContent = {
                        Icon(imageVector = Icons.Outlined.Lightbulb, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                HorizontalDivider()
                ListItem(
                    modifier = Modifier.clickable {
                        Const.BUY_ME_A_COFFEE_LINK.openBrowser(context)
                        onClose()
                    },
                    headlineContent = { Text(text = stringResource(R.string.sponsor_buy_me_a_coffee)) },
                    leadingContent = {
                        Icon(imageVector = Icons.Outlined.Coffee, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text(text = stringResource(R.string.close))
            }
        },
    )
}

@Composable
private fun ButtonArea() {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        val boxModifier = Modifier.padding(vertical = 16.dp, horizontal = 6.dp)
        val context = LocalContext.current
        Box(
            modifier = boxModifier.background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CurlyCornerShape(
                    amp = with(LocalDensity.current) { 1.dp.toPx() },
                    count = 10f
                ),
            ),
            contentAlignment = Alignment.Center
        ) {
            PodAuraIconButton(
                painter = painterResource(id = R.drawable.ic_github_24),
                contentDescription = stringResource(id = R.string.about_screen_visit_github),
                onClick = { Const.GITHUB_REPO.openBrowser(context) }
            )
        }
        Box(
            modifier = boxModifier.background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = SquircleShape,
            ),
            contentAlignment = Alignment.Center
        ) {
            PodAuraIconButton(
                painter = painterResource(id = R.drawable.ic_telegram_24),
                contentDescription = stringResource(id = R.string.about_screen_join_telegram),
                onClick = { Const.TELEGRAM_GROUP.openBrowser(context) }
            )
        }
        Box(
            modifier = boxModifier.background(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = CloverShape,
            ),
            contentAlignment = Alignment.Center
        ) {
            PodAuraIconButton(
                painter = painterResource(id = R.drawable.ic_discord_24),
                contentDescription = stringResource(id = R.string.about_screen_join_discord),
                onClick = { Const.DISCORD_SERVER.openBrowser(context) }
            )
        }
    }
}

@Composable
private fun rememberOtherWorksList(): List<OtherWorksBean> {
    val context = LocalContext.current
    return remember {
        listOf(
            OtherWorksBean(
                name = context.getString(R.string.about_screen_other_works_rays_name),
                icon = R.drawable.ic_rays,
                description = context.getString(R.string.about_screen_other_works_rays_description),
                url = Const.RAYS_ANDROID_URL,
            ),
            OtherWorksBean(
                name = context.getString(R.string.about_screen_other_works_raca_name),
                icon = R.drawable.ic_raca,
                description = context.getString(R.string.about_screen_other_works_raca_description),
                url = Const.RACA_ANDROID_URL,
            ),
            OtherWorksBean(
                name = context.getString(R.string.about_screen_other_works_night_screen_name),
                icon = R.drawable.ic_night_screen,
                description = context.getString(R.string.about_screen_other_works_night_screen_description),
                url = Const.NIGHT_SCREEN_URL,
            ),
        )
    }
}

@Composable
private fun OtherWorksItem(
    modifier: Modifier = Modifier,
    data: OtherWorksBean,
) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .padding(vertical = 10.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .clickable { data.url.openBrowser(context) }
                .padding(15.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PodAuraImage(
                    modifier = Modifier
                        .size(30.dp)
                        .aspectRatio(1f),
                    model = data.icon,
                    contentDescription = data.name
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = data.name,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = data.description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}