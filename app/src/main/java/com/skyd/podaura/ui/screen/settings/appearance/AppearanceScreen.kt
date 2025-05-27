package com.skyd.podaura.ui.screen.settings.appearance

import android.os.Parcelable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.google.android.material.color.DynamicColors
import com.materialkolor.ktx.from
import com.materialkolor.ktx.toneColor
import com.materialkolor.palettes.TonalPalette
import com.skyd.podaura.ext.activity
import com.skyd.podaura.model.preference.appearance.AmoledDarkModePreference
import com.skyd.podaura.model.preference.appearance.BaseDarkModePreference
import com.skyd.podaura.model.preference.appearance.BaseThemePreference
import com.skyd.podaura.model.preference.appearance.DarkModePreference
import com.skyd.podaura.model.preference.appearance.DateStylePreference
import com.skyd.podaura.model.preference.appearance.NavigationBarLabelPreference
import com.skyd.podaura.model.preference.appearance.TextFieldStylePreference
import com.skyd.podaura.model.preference.appearance.ThemePreference
import com.skyd.podaura.ui.component.BackIcon
import com.skyd.podaura.ui.component.CheckableListMenu
import com.skyd.podaura.ui.component.DefaultBackClick
import com.skyd.podaura.ui.component.PodAuraTopBar
import com.skyd.podaura.ui.component.PodAuraTopBarStyle
import com.skyd.podaura.ui.component.connectedButtonShapes
import com.skyd.podaura.ui.component.settings.BaseSettingsItem
import com.skyd.podaura.ui.component.settings.SettingsDefaults
import com.skyd.podaura.ui.component.settings.SettingsLazyColumn
import com.skyd.podaura.ui.component.settings.SwitchSettingsItem
import com.skyd.podaura.ui.component.suspendString
import com.skyd.podaura.ui.local.LocalNavController
import com.skyd.podaura.ui.screen.settings.appearance.article.ArticleStyleRoute
import com.skyd.podaura.ui.screen.settings.appearance.feed.FeedStyleRoute
import com.skyd.podaura.ui.screen.settings.appearance.media.MediaStyleRoute
import com.skyd.podaura.ui.screen.settings.appearance.read.ReadStyleRoute
import com.skyd.podaura.ui.screen.settings.appearance.search.SearchStyleRoute
import com.skyd.podaura.ui.theme.extractAllColors
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.appearance_screen_amoled_dark
import podaura.shared.generated.resources.appearance_screen_date_style
import podaura.shared.generated.resources.appearance_screen_name
import podaura.shared.generated.resources.appearance_screen_navigation_bar_label
import podaura.shared.generated.resources.appearance_screen_screen_style_category
import podaura.shared.generated.resources.appearance_screen_style_category
import podaura.shared.generated.resources.appearance_screen_text_field_style
import podaura.shared.generated.resources.appearance_screen_theme_category
import podaura.shared.generated.resources.appearance_screen_use_dynamic_theme
import podaura.shared.generated.resources.appearance_screen_use_dynamic_theme_description
import podaura.shared.generated.resources.article_style_screen_name
import podaura.shared.generated.resources.feed_style_screen_name
import podaura.shared.generated.resources.media_style_screen_name
import podaura.shared.generated.resources.read_style_screen_name
import podaura.shared.generated.resources.search_style_screen_name


@Serializable
@Parcelize
data object AppearanceRoute : Parcelable

@Composable
fun AppearanceScreen(onBack: (() -> Unit)? = DefaultBackClick) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()
    var expandTextFieldStyleMenu by rememberSaveable { mutableStateOf(false) }
    var expandDateStyleMenu by rememberSaveable { mutableStateOf(false) }
    var expandNavigationBarLabelMenu by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.LargeFlexible,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(Res.string.appearance_screen_name)) },
                navigationIcon = { if (onBack != null) BackIcon(onClick = onBack) },
            )
        }
    ) { paddingValues ->
        SettingsLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues,
        ) {
            group(text = { getString(Res.string.appearance_screen_theme_category) }) {
                otherItem {
                    DarkModeButtonGroup()
                }
                otherItem {
                    Palettes(colors = extractAllColors(darkTheme = false))
                }
                if (DynamicColors.isDynamicColorAvailable()) {
                    item {
                        SwitchSettingsItem(
                            imageVector = Icons.Outlined.Colorize,
                            text = stringResource(Res.string.appearance_screen_use_dynamic_theme),
                            description = stringResource(Res.string.appearance_screen_use_dynamic_theme_description),
                            checked = ThemePreference.current == BaseThemePreference.DYNAMIC,
                            onCheckedChange = {
                                ThemePreference.put(
                                    scope = scope,
                                    value = if (it) BaseThemePreference.DYNAMIC
                                    else ThemePreference.basicValues.first(),
                                ) {
                                    context.activity.recreate()
                                }
                            }
                        )
                    }
                }
                item {
                    SwitchSettingsItem(
                        imageVector = null,
                        text = stringResource(Res.string.appearance_screen_amoled_dark),
                        checked = AmoledDarkModePreference.current,
                        onCheckedChange = {
                            AmoledDarkModePreference.put(
                                scope = scope,
                                value = it
                            )
                        }
                    )
                }
            }
            group(text = { getString(Res.string.appearance_screen_style_category) }) {
                item {
                    BaseSettingsItem(
                        icon = null,
                        text = stringResource(Res.string.appearance_screen_text_field_style),
                        descriptionText = suspendString(TextFieldStylePreference.current) {
                            TextFieldStylePreference.toDisplayName(it)
                        },
                        extraContent = {
                            TextFieldStyleMenu(
                                expanded = expandTextFieldStyleMenu,
                                onDismissRequest = { expandTextFieldStyleMenu = false }
                            )
                        },
                        onClick = { expandTextFieldStyleMenu = true },
                    )
                }
                item {
                    BaseSettingsItem(
                        icon = null,
                        text = stringResource(Res.string.appearance_screen_date_style),
                        descriptionText = suspendString(DateStylePreference.current) {
                            DateStylePreference.toDisplayName(it)
                        },
                        extraContent = {
                            DateStyleStyleMenu(
                                expanded = expandDateStyleMenu,
                                onDismissRequest = { expandDateStyleMenu = false }
                            )
                        },
                        onClick = { expandDateStyleMenu = true },
                    )
                }
                item {
                    BaseSettingsItem(
                        icon = null,
                        text = stringResource(Res.string.appearance_screen_navigation_bar_label),
                        descriptionText = suspendString(NavigationBarLabelPreference.current) {
                            NavigationBarLabelPreference.toDisplayName(it)
                        },
                        extraContent = {
                            NavigationBarLabelStyleMenu(
                                expanded = expandNavigationBarLabelMenu,
                                onDismissRequest = { expandNavigationBarLabelMenu = false }
                            )
                        },
                        onClick = { expandNavigationBarLabelMenu = true },
                    )
                }
            }
            group(text = { getString(Res.string.appearance_screen_screen_style_category) }) {
                item {
                    BaseSettingsItem(
                        icon = null,
                        text = stringResource(Res.string.feed_style_screen_name),
                        description = null,
                        onClick = { navController.navigate(FeedStyleRoute) },
                    )
                }
                item {
                    BaseSettingsItem(
                        icon = null,
                        text = stringResource(Res.string.article_style_screen_name),
                        description = null,
                        onClick = { navController.navigate(ArticleStyleRoute) },
                    )
                }
                item {
                    BaseSettingsItem(
                        icon = null,
                        text = stringResource(Res.string.read_style_screen_name),
                        description = null,
                        onClick = { navController.navigate(ReadStyleRoute) },
                    )
                }
                item {
                    BaseSettingsItem(
                        icon = null,
                        text = stringResource(Res.string.search_style_screen_name),
                        description = null,
                        onClick = { navController.navigate(SearchStyleRoute) },
                    )
                }
                item {
                    BaseSettingsItem(
                        icon = null,
                        text = stringResource(Res.string.media_style_screen_name),
                        description = null,
                        onClick = { navController.navigate(MediaStyleRoute) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DarkModeButtonGroup() {
    val scope = rememberCoroutineScope()
    val darkMode = DarkModePreference.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(
            space = ButtonGroupDefaults.ConnectedSpaceBetween,
            alignment = Alignment.CenterHorizontally,
        ),
    ) {
        DarkModePreference.values.forEachIndexed { index, darkModeValue ->
            val checked = index == DarkModePreference.values.indexOf(darkMode)
            ToggleButton(
                checked = checked,
                onCheckedChange = { if (it) DarkModePreference.put(scope, darkModeValue) },
                modifier = Modifier.semantics { role = Role.RadioButton },
                shapes = ButtonGroupDefaults.connectedButtonShapes(
                    list = DarkModePreference.values,
                    index = index,
                ),
            ) {
                Text(
                    text = suspendString { BaseDarkModePreference.toDisplayName(darkModeValue) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun TextFieldStyleMenu(expanded: Boolean, onDismissRequest: () -> Unit) {
    val scope = rememberCoroutineScope()
    val textFieldStyle = TextFieldStylePreference.current

    CheckableListMenu(
        expanded = expanded,
        current = textFieldStyle,
        values = TextFieldStylePreference.values,
        displayName = { TextFieldStylePreference.toDisplayName(it) },
        onChecked = { TextFieldStylePreference.put(scope, it) },
        onDismissRequest = onDismissRequest,
    )
}

@Composable
private fun DateStyleStyleMenu(expanded: Boolean, onDismissRequest: () -> Unit) {
    val scope = rememberCoroutineScope()
    val dateStyle = DateStylePreference.current

    CheckableListMenu(
        expanded = expanded,
        current = dateStyle,
        values = remember { DateStylePreference.values.toList() },
        displayName = { DateStylePreference.toDisplayName(it) },
        onChecked = { DateStylePreference.put(scope, it) },
        onDismissRequest = onDismissRequest,
    )
}

@Composable
private fun NavigationBarLabelStyleMenu(expanded: Boolean, onDismissRequest: () -> Unit) {
    val scope = rememberCoroutineScope()
    val navigationBarLabel = NavigationBarLabelPreference.current

    CheckableListMenu(
        expanded = expanded,
        current = navigationBarLabel,
        values = remember { NavigationBarLabelPreference.values.toList() },
        displayName = { NavigationBarLabelPreference.toDisplayName(it) },
        onChecked = { NavigationBarLabelPreference.put(scope, it) },
        onDismissRequest = onDismissRequest,
    )
}

@Composable
fun Palettes(
    colors: Map<String, ColorScheme>,
    themeName: String = ThemePreference.current,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = SettingsDefaults.itemHorizontalSpace, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        colors.forEach { (t, u) ->
            SelectableMiniPalette(
                selected = t == themeName,
                onClick = {
                    ThemePreference.put(scope, t) {
                        context.activity.recreate()
                    }
                },
                contentDescription = { ThemePreference.toDisplayName(t) },
                accents = remember(u) {
                    listOf(
                        TonalPalette.from(u.primary),
                        TonalPalette.from(u.secondary),
                        TonalPalette.from(u.tertiary)
                    )
                }
            )
        }
    }
}

@Composable
fun SelectableMiniPalette(
    selected: Boolean,
    onClick: () -> Unit,
    contentDescription: suspend () -> String,
    accents: List<TonalPalette>,
) {
    TooltipBox(
        modifier = Modifier,
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(suspendString { contentDescription() })
            }
        },
        state = rememberTooltipState()
    ) {
        Box(
            modifier = Modifier
                .size(74.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick),
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.inverseOnSurface)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.size(50.dp),
                    shape = CircleShape,
                    color = accents[0].toneColor(36),
                ) {
                    Box {
                        Surface(
                            modifier = Modifier
                                .size(50.dp)
                                .offset((-25).dp, 25.dp),
                            color = accents[1].toneColor(80),
                        ) {}
                        Surface(
                            modifier = Modifier
                                .size(50.dp)
                                .offset(25.dp, 25.dp),
                            color = accents[2].toneColor(65),
                        ) {}
                    }
                }
            }
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 2.dp,
                            color = accents[0].toneColor(50),
                            shape = RoundedCornerShape(16.dp),
                        )
                        .padding(2.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(15.dp),
                            ),
                    )
                }
            }
        }
    }
}