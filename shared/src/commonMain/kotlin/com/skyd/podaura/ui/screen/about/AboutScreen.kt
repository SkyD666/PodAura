package com.skyd.podaura.ui.screen.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.component.dialog.ComponeDialog
import com.skyd.compone.local.LocalNavController
import com.skyd.podaura.BuildKonfig
import com.skyd.podaura.config.Const
import com.skyd.podaura.ext.isCompact
import com.skyd.podaura.ext.safeOpenUri
import com.skyd.podaura.model.bean.OtherWorksBean
import com.skyd.podaura.ui.local.LocalWindowSizeClass
import com.skyd.podaura.ui.screen.about.license.LicenseRoute
import com.skyd.podaura.ui.screen.about.update.UpdateDialog
import kotlinx.coroutines.launch
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.about_screen_join_discord
import podaura.shared.generated.resources.about_screen_join_telegram
import podaura.shared.generated.resources.about_screen_name
import podaura.shared.generated.resources.about_screen_other_works
import podaura.shared.generated.resources.about_screen_other_works_night_screen_description
import podaura.shared.generated.resources.about_screen_other_works_night_screen_name
import podaura.shared.generated.resources.about_screen_other_works_raca_description
import podaura.shared.generated.resources.about_screen_other_works_raca_name
import podaura.shared.generated.resources.about_screen_other_works_rays_description
import podaura.shared.generated.resources.about_screen_other_works_rays_name
import podaura.shared.generated.resources.about_screen_visit_github
import podaura.shared.generated.resources.app_name
import podaura.shared.generated.resources.app_short_description
import podaura.shared.generated.resources.app_tech_stack_description
import podaura.shared.generated.resources.close
import podaura.shared.generated.resources.help_translate
import podaura.shared.generated.resources.ic_discord_24
import podaura.shared.generated.resources.ic_github_24
import podaura.shared.generated.resources.ic_icon_24
import podaura.shared.generated.resources.ic_night_screen
import podaura.shared.generated.resources.ic_raca
import podaura.shared.generated.resources.ic_rays
import podaura.shared.generated.resources.ic_santa_hat
import podaura.shared.generated.resources.ic_telegram_24
import podaura.shared.generated.resources.license_screen_name
import podaura.shared.generated.resources.more
import podaura.shared.generated.resources.sponsor
import podaura.shared.generated.resources.sponsor_afadian
import podaura.shared.generated.resources.sponsor_buy_me_a_coffee
import podaura.shared.generated.resources.sponsor_description
import podaura.shared.generated.resources.terms_of_service_screen_name
import podaura.shared.generated.resources.update_check
import podaura.shared.generated.resources.update_check_failed
import kotlin.time.Clock


@Serializable
data object AboutRoute

@Composable
fun AboutScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var openUpdateDialog by rememberSaveable { mutableStateOf(false) }
    var openSponsorDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.LargeFlexible,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(Res.string.about_screen_name)) },
                actions = {
                    ComponeIconButton(
                        imageVector = Icons.Outlined.Gavel,
                        contentDescription = stringResource(Res.string.terms_of_service_screen_name),
                        onClick = { navController.navigate(TermsOfServiceRoute) }
                    )
                    ComponeIconButton(
                        imageVector = Icons.Outlined.Balance,
                        contentDescription = stringResource(Res.string.license_screen_name),
                        onClick = { navController.navigate(LicenseRoute) }
                    )
                    ComponeIconButton(
                        onClick = { openUpdateDialog = true },
                        imageVector = Icons.Outlined.Update,
                        contentDescription = stringResource(Res.string.update_check)
                    )
                },
            )
        }
    ) { paddingValues ->
        val windowSizeClass = LocalWindowSizeClass.current
        val otherWorksList = rememberOtherWorksList()
        val uriHandler = LocalUriHandler.current

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
                        onTranslateClick = { uriHandler.safeOpenUri(Const.TRANSLATION_URL) },
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
                                onTranslateClick = { uriHandler.safeOpenUri(Const.TRANSLATION_URL) },
                                onSponsorDialogVisibleChange = { openSponsorDialog = it }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            item {
                Text(
                    text = stringResource(Res.string.about_screen_other_works),
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
                            message = getString(Res.string.update_check_failed, msg),
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
            painter = painterResource(Res.drawable.ic_icon_24),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
            contentDescription = null
        )
        val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
        val month = remember(today) { today.month }
        val day = remember(today) { today.day }
        if (month == Month.DECEMBER && (day in 22..28)) {     // Xmas
            Image(
                modifier = Modifier
                    .fillMaxWidth(0.67f)
                    .aspectRatio(1f)
                    .rotate(15f)
                    .padding(end = 10.dp)
                    .align(Alignment.TopEnd),
                painter = painterResource(Res.drawable.ic_santa_hat),
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun TextArea(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(top = 12.dp)
            .fillMaxWidth(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BadgedBox(
            badge = {
                Badge {
                    val badgeNumber = BuildKonfig.versionName
                    Text(
                        text = badgeNumber,
                        modifier = Modifier.semantics { contentDescription = badgeNumber }
                    )
                }
            }
        ) {
            Text(
                text = stringResource(Res.string.app_name),
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
                    text = stringResource(Res.string.app_short_description),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    modifier = Modifier.padding(top = 12.dp),
                    text = stringResource(Res.string.app_tech_stack_description),
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
    val translateLabel = stringResource(Res.string.help_translate)
    val sponsorLabel = stringResource(Res.string.sponsor)
    ButtonGroup(
        overflowIndicator = { menuState ->
            FilledIconButton(
                onClick = { if (menuState.isShowing) menuState.dismiss() else menuState.show() }
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = stringResource(Res.string.more),
                )
            }
        },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        clickableItem(
            onClick = onTranslateClick,
            label = translateLabel,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Translate,
                    contentDescription = null,
                    modifier = Modifier.padding(3.dp),
                )
            },
            weight = 1f,
        )
        clickableItem(
            onClick = { onSponsorDialogVisibleChange(true) },
            label = sponsorLabel,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Coffee,
                    contentDescription = null,
                    modifier = Modifier.padding(3.dp),
                )
            },
            weight = 1f,
        )
    }
    SponsorDialog(visible = openSponsorDialog, onClose = { onSponsorDialogVisibleChange(false) })
}

@Composable
private fun SponsorDialog(visible: Boolean, onClose: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    ComponeDialog(
        visible = visible,
        onDismissRequest = onClose,
        icon = { Icon(imageVector = Icons.Outlined.Coffee, contentDescription = null) },
        title = { Text(text = stringResource(Res.string.sponsor)) },
        selectable = false,
        text = {
            Column {
                Text(text = stringResource(Res.string.sponsor_description))
                Spacer(modifier = Modifier.height(6.dp))
                ListItem(
                    modifier = Modifier.clickable {
                        uriHandler.safeOpenUri(Const.AFADIAN_LINK)
                        onClose()
                    },
                    headlineContent = { Text(text = stringResource(Res.string.sponsor_afadian)) },
                    leadingContent = {
                        Icon(imageVector = Icons.Outlined.Lightbulb, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                HorizontalDivider()
                ListItem(
                    modifier = Modifier.clickable {
                        uriHandler.safeOpenUri(Const.BUY_ME_A_COFFEE_LINK)
                        onClose()
                    },
                    headlineContent = { Text(text = stringResource(Res.string.sponsor_buy_me_a_coffee)) },
                    leadingContent = {
                        Icon(imageVector = Icons.Outlined.Coffee, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text(text = stringResource(Res.string.close))
            }
        },
    )
}

@Composable
private fun ButtonArea() {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        val boxModifier = Modifier.padding(vertical = 16.dp, horizontal = 6.dp)
        Box(
            modifier = boxModifier.background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialShapes.Cookie9Sided.toShape(),
            ),
            contentAlignment = Alignment.Center
        ) {
            ComponeIconButton(
                painter = painterResource(Res.drawable.ic_github_24),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                contentDescription = stringResource(Res.string.about_screen_visit_github),
                onClick = { uriHandler.safeOpenUri(Const.GITHUB_REPO) }
            )
        }
        Box(
            modifier = boxModifier.background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialShapes.Pill.toShape(),
            ),
            contentAlignment = Alignment.Center
        ) {
            ComponeIconButton(
                painter = painterResource(Res.drawable.ic_telegram_24),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                contentDescription = stringResource(Res.string.about_screen_join_telegram),
                onClick = { uriHandler.safeOpenUri(Const.TELEGRAM_GROUP) }
            )
        }
        Box(
            modifier = boxModifier.background(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = MaterialShapes.Clover4Leaf.toShape(),
            ),
            contentAlignment = Alignment.Center
        ) {
            ComponeIconButton(
                painter = painterResource(Res.drawable.ic_discord_24),
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                contentDescription = stringResource(Res.string.about_screen_join_discord),
                onClick = { uriHandler.safeOpenUri(Const.DISCORD_SERVER) }
            )
        }
    }
}

@Composable
private fun rememberOtherWorksList(): List<OtherWorksBean> {
    var result by remember { mutableStateOf<List<OtherWorksBean>>(emptyList()) }
    LaunchedEffect(Unit) {
        result = listOf(
            OtherWorksBean(
                name = getString(Res.string.about_screen_other_works_rays_name),
                icon = Res.drawable.ic_rays,
                description = getString(Res.string.about_screen_other_works_rays_description),
                url = Const.RAYS_ANDROID_URL,
            ),
            OtherWorksBean(
                name = getString(Res.string.about_screen_other_works_raca_name),
                icon = Res.drawable.ic_raca,
                description = getString(Res.string.about_screen_other_works_raca_description),
                url = Const.RACA_ANDROID_URL,
            ),
            OtherWorksBean(
                name = getString(Res.string.about_screen_other_works_night_screen_name),
                icon = Res.drawable.ic_night_screen,
                description = getString(Res.string.about_screen_other_works_night_screen_description),
                url = Const.NIGHT_SCREEN_URL,
            ),
        )
    }
    return result
}

@Composable
private fun OtherWorksItem(
    modifier: Modifier = Modifier,
    data: OtherWorksBean,
) {
    val uriHandler = LocalUriHandler.current
    Card(
        modifier = modifier
            .padding(vertical = 10.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .clickable { uriHandler.safeOpenUri(data.url) }
                .padding(15.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    modifier = Modifier
                        .size(30.dp)
                        .aspectRatio(1f),
                    painter = painterResource(data.icon),
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