package com.skyd.anivu.ui.screen.settings.appearance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.material.color.DynamicColors
import com.materialkolor.ktx.from
import com.materialkolor.palettes.TonalPalette
import com.skyd.anivu.R
import com.skyd.anivu.ext.activity
import com.skyd.anivu.model.preference.appearance.AmoledDarkModePreference
import com.skyd.anivu.model.preference.appearance.DarkModePreference
import com.skyd.anivu.model.preference.appearance.DateStylePreference
import com.skyd.anivu.model.preference.appearance.NavigationBarLabelPreference
import com.skyd.anivu.model.preference.appearance.TextFieldStylePreference
import com.skyd.anivu.model.preference.appearance.ThemePreference
import com.skyd.anivu.ui.component.BaseSettingsItem
import com.skyd.anivu.ui.component.CategorySettingsItem
import com.skyd.anivu.ui.component.CheckableListMenu
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.component.SwitchSettingsItem
import com.skyd.anivu.ui.local.LocalAmoledDarkMode
import com.skyd.anivu.ui.local.LocalDarkMode
import com.skyd.anivu.ui.local.LocalDateStyle
import com.skyd.anivu.ui.local.LocalNavController
import com.skyd.anivu.ui.local.LocalNavigationBarLabel
import com.skyd.anivu.ui.local.LocalTextFieldStyle
import com.skyd.anivu.ui.local.LocalTheme
import com.skyd.anivu.ui.screen.settings.appearance.article.ArticleStyleRoute
import com.skyd.anivu.ui.screen.settings.appearance.feed.FeedStyleRoute
import com.skyd.anivu.ui.screen.settings.appearance.media.MediaStyleRoute
import com.skyd.anivu.ui.screen.settings.appearance.read.ReadStyleRoute
import com.skyd.anivu.ui.screen.settings.appearance.search.SearchStyleRoute
import com.skyd.anivu.ui.theme.extractColors
import kotlinx.serialization.Serializable


@Serializable
data object AppearanceRoute

@Composable
fun AppearanceScreen() {
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
                style = PodAuraTopBarStyle.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(R.string.appearance_screen_name)) },
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
                CategorySettingsItem(text = stringResource(id = R.string.appearance_screen_theme_category))
            }
            item {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                ) {
                    DarkModePreference.values.forEachIndexed { index, darkModeValue ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = DarkModePreference.values.size,
                            ),
                            onClick = { DarkModePreference.put(context, scope, darkModeValue) },
                            selected = index == DarkModePreference.values.indexOf(LocalDarkMode.current)
                        ) {
                            Text(DarkModePreference.toDisplayName(context, darkModeValue))
                        }
                    }
                }
            }
            item {
                Palettes(colors = extractColors(darkTheme = false))
            }
            if (DynamicColors.isDynamicColorAvailable()) {
                item {
                    SwitchSettingsItem(
                        imageVector = Icons.Outlined.Colorize,
                        text = stringResource(id = R.string.appearance_screen_use_dynamic_theme),
                        description = stringResource(id = R.string.appearance_screen_use_dynamic_theme_description),
                        checked = LocalTheme.current == ThemePreference.DYNAMIC,
                        onCheckedChange = {
                            ThemePreference.put(
                                context = context,
                                scope = scope,
                                value = if (it) ThemePreference.DYNAMIC
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
                    text = stringResource(id = R.string.appearance_screen_amoled_dark),
                    checked = LocalAmoledDarkMode.current,
                    onCheckedChange = {
                        AmoledDarkModePreference.put(
                            context = context,
                            scope = scope,
                            value = it,
                        )
                    }
                )
            }
            item {
                CategorySettingsItem(text = stringResource(id = R.string.appearance_screen_style_category))
            }
            item {
                BaseSettingsItem(
                    icon = null,
                    text = stringResource(id = R.string.appearance_screen_text_field_style),
                    descriptionText = TextFieldStylePreference.toDisplayName(
                        context, LocalTextFieldStyle.current,
                    ),
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
                    text = stringResource(id = R.string.appearance_screen_date_style),
                    descriptionText = DateStylePreference.toDisplayName(
                        context, LocalDateStyle.current,
                    ),
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
                    text = stringResource(id = R.string.appearance_screen_navigation_bar_label),
                    descriptionText = NavigationBarLabelPreference.toDisplayName(
                        context, LocalNavigationBarLabel.current,
                    ),
                    extraContent = {
                        NavigationBarLabelStyleMenu(
                            expanded = expandNavigationBarLabelMenu,
                            onDismissRequest = { expandNavigationBarLabelMenu = false }
                        )
                    },
                    onClick = { expandNavigationBarLabelMenu = true },
                )
            }
            item {
                CategorySettingsItem(text = stringResource(id = R.string.appearance_screen_screen_style_category))
            }
            item {
                BaseSettingsItem(
                    icon = null,
                    text = stringResource(id = R.string.feed_style_screen_name),
                    description = null,
                    onClick = { navController.navigate(FeedStyleRoute) },
                )
            }
            item {
                BaseSettingsItem(
                    icon = null,
                    text = stringResource(id = R.string.article_style_screen_name),
                    description = null,
                    onClick = { navController.navigate(ArticleStyleRoute) },
                )
            }
            item {
                BaseSettingsItem(
                    icon = null,
                    text = stringResource(id = R.string.read_style_screen_name),
                    description = null,
                    onClick = { navController.navigate(ReadStyleRoute) },
                )
            }
            item {
                BaseSettingsItem(
                    icon = null,
                    text = stringResource(id = R.string.search_style_screen_name),
                    description = null,
                    onClick = { navController.navigate(SearchStyleRoute) },
                )
            }
            item {
                BaseSettingsItem(
                    icon = null,
                    text = stringResource(id = R.string.media_style_screen_name),
                    description = null,
                    onClick = { navController.navigate(MediaStyleRoute) },
                )
            }
        }
    }
}

@Composable
private fun TextFieldStyleMenu(expanded: Boolean, onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val textFieldStyle = LocalTextFieldStyle.current

    CheckableListMenu(
        expanded = expanded,
        current = textFieldStyle,
        values = TextFieldStylePreference.values,
        displayName = { TextFieldStylePreference.toDisplayName(context, it) },
        onChecked = { TextFieldStylePreference.put(context, scope, it) },
        onDismissRequest = onDismissRequest,
    )
}

@Composable
private fun DateStyleStyleMenu(expanded: Boolean, onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dateStyle = LocalDateStyle.current

    CheckableListMenu(
        expanded = expanded,
        current = dateStyle,
        values = remember { DateStylePreference.values.toList() },
        displayName = { DateStylePreference.toDisplayName(context, it) },
        onChecked = { DateStylePreference.put(context, scope, it) },
        onDismissRequest = onDismissRequest,
    )
}

@Composable
private fun NavigationBarLabelStyleMenu(expanded: Boolean, onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navigationBarLabel = LocalNavigationBarLabel.current

    CheckableListMenu(
        expanded = expanded,
        current = navigationBarLabel,
        values = remember { NavigationBarLabelPreference.values.toList() },
        displayName = { NavigationBarLabelPreference.toDisplayName(context, it) },
        onChecked = { NavigationBarLabelPreference.put(context, scope, it) },
        onDismissRequest = onDismissRequest,
    )
}

@Composable
fun Palettes(
    colors: Map<String, ColorScheme>,
    themeName: String = LocalTheme.current,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        colors.forEach { (t, u) ->
            SelectableMiniPalette(
                selected = t == themeName,
                onClick = {
                    ThemePreference.put(context, scope, t) {
                        context.activity.recreate()
                    }
                },
                contentDescription = { ThemePreference.toDisplayName(context, t) },
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
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: () -> Unit,
    contentDescription: () -> String,
    accents: List<TonalPalette>,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.inverseOnSurface,
    ) {
        TooltipBox(
            modifier = modifier,
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
            tooltip = {
                PlainTooltip {
                    Text(contentDescription())
                }
            },
            state = rememberTooltipState()
        ) {
            Surface(
                modifier = Modifier
                    .clickable(onClick = onClick)
                    .padding(12.dp)
                    .size(50.dp),
                shape = CircleShape,
                color = Color(accents[0].tone(60)),
            ) {
                Box {
                    Surface(
                        modifier = Modifier
                            .size(50.dp)
                            .offset((-25).dp, 25.dp),
                        color = Color(accents[1].tone(85)),
                    ) {}
                    Surface(
                        modifier = Modifier
                            .size(50.dp)
                            .offset(25.dp, 25.dp),
                        color = Color(accents[2].tone(75)),
                    ) {}
                    val animationSpec = spring<Float>(stiffness = Spring.StiffnessMedium)
                    AnimatedVisibility(
                        visible = selected,
                        enter = scaleIn(animationSpec) + fadeIn(animationSpec),
                        exit = scaleOut(animationSpec) + fadeOut(animationSpec),
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(10.dp)
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Checked",
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(16.dp),
                                tint = MaterialTheme.colorScheme.surface
                            )
                        }
                    }
                }
            }
        }
    }
}