package com.skyd.podaura.ui.screen.article

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.Drafts
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.FilterAltOff
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.Markunread
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.skyd.compone.component.blockString
import com.skyd.compone.component.menu.ListMenu
import com.skyd.podaura.model.bean.feed.FeedBean
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.article_screen_filter_all
import podaura.shared.generated.resources.article_screen_filter_clear_all_filter
import podaura.shared.generated.resources.article_screen_filter_favorite
import podaura.shared.generated.resources.article_screen_filter_mute
import podaura.shared.generated.resources.article_screen_filter_read
import podaura.shared.generated.resources.article_screen_filter_unfavorite
import podaura.shared.generated.resources.article_screen_filter_unmute
import podaura.shared.generated.resources.article_screen_filter_unread
import podaura.shared.generated.resources.article_screen_hide_filter_bar
import podaura.shared.generated.resources.article_screen_show_filter_bar
import podaura.shared.generated.resources.article_screen_sort_date_asc
import podaura.shared.generated.resources.article_screen_sort_date_desc
import podaura.shared.generated.resources.article_screen_sort_title_asc
import podaura.shared.generated.resources.article_screen_sort_title_desc

@Composable
fun FilterIcon(
    hasFilter: Boolean,
    showFilterBar: Boolean,
    onFilterBarVisibilityChanged: (Boolean) -> Unit,
    onFilterMaskChanged: (Int) -> Unit,
) {
    var expandMenu by rememberSaveable { mutableStateOf(false) }

    val icon: @Composable () -> Unit = {
        IconToggleButton(
            checked = showFilterBar,
            onCheckedChange = {
                if (hasFilter) expandMenu = true
                else onFilterBarVisibilityChanged(it)
            },
        ) {
            Icon(
                imageVector = Icons.Outlined.FilterAlt,
                contentDescription = stringResource(Res.string.article_screen_show_filter_bar),
            )
        }
    }

    if (hasFilter) {
        BadgedBox(badge = { Badge() }) {
            icon()
        }
    } else {
        icon()
    }

    DropdownMenuPopup(
        expanded = expandMenu,
        onDismissRequest = { expandMenu = false },
    ) {
        val texts = listOf(
            stringResource(Res.string.article_screen_filter_clear_all_filter),
            stringResource(
                if (showFilterBar) Res.string.article_screen_hide_filter_bar
                else Res.string.article_screen_show_filter_bar
            ),
        )
        val leadingIcons = listOf(
            Icons.Outlined.ClearAll,
            if (showFilterBar) Icons.Outlined.FilterAltOff
            else Icons.Outlined.FilterAlt,
        )
        val onClicks = listOf(
            {
                onFilterMaskChanged(FeedBean.DEFAULT_FILTER_MASK)
                expandMenu = false
            },
            {
                onFilterBarVisibilityChanged(!showFilterBar)
                expandMenu = false
            },
        )
        DropdownMenuGroup(shapes = MenuDefaults.groupShape(0, 1)) {
            texts.forEachIndexed { index, text ->
                DropdownMenuItem(
                    text = { Text(text = text) },
                    shape = MenuDefaults.itemShape(index, texts.size).shape,
                    leadingIcon = {
                        Icon(imageVector = leadingIcons[index], contentDescription = null)
                    },
                    onClick = onClicks[index],
                )
            }
        }
    }
}

@Composable
internal fun FilterRow(
    modifier: Modifier = Modifier,
    articleFilterMask: Int,
    onFilterMaskChanged: (Int) -> Unit,
) {

    Row(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FavoriteFilter(
                current = FeedBean.parseFilterMaskToFavorite(filterMask = articleFilterMask),
                onFilterFavorite = {
                    onFilterMaskChanged(
                        FeedBean.newFilterMask(filterMask = articleFilterMask, filterFavorite = it)
                    )
                },
            )
            ReadFilter(
                current = FeedBean.parseFilterMaskToRead(filterMask = articleFilterMask),
                onFilterRead = {
                    onFilterMaskChanged(
                        FeedBean.newFilterMask(filterMask = articleFilterMask, filterRead = it)
                    )
                },
            )
            MuteFilter(
                current = FeedBean.parseFilterMaskToMute(filterMask = articleFilterMask),
                onFilterMute = {
                    onFilterMaskChanged(
                        FeedBean.newFilterMask(filterMask = articleFilterMask, filterMute = it)
                    )
                },
            )
            SortSetting(
                current = FeedBean.parseFilterMaskToSort(filterMask = articleFilterMask),
                onSort = {
                    onFilterMaskChanged(
                        FeedBean.newFilterMask(filterMask = articleFilterMask, sort = it)
                    )
                },
            )
        }
    }
}

@Composable
internal fun SortSetting(
    current: FeedBean.SortBy,
    onSort: (FeedBean.SortBy) -> Unit,
) {
    var expandMenu by rememberSaveable { mutableStateOf(false) }
    val items = remember {
        mapOf(
            FeedBean.SortBy.default to Pair(
                Res.string.article_screen_sort_date_desc,
                Icons.Outlined.CalendarMonth,
            ),
            FeedBean.SortBy.Date(true) to Pair(
                Res.string.article_screen_sort_date_asc,
                Icons.Outlined.CalendarMonth,
            ),
            FeedBean.SortBy.Title(true) to Pair(
                Res.string.article_screen_sort_title_asc,
                Icons.Outlined.Title,
            ),
            FeedBean.SortBy.Title(false) to Pair(
                Res.string.article_screen_sort_title_desc,
                Icons.Outlined.Title,
            ),
        )
    }

    Box {
        FilterChip(
            onClick = { expandMenu = !expandMenu },
            label = {
                Text(
                    modifier = Modifier.animateContentSize(),
                    text = stringResource(items[current]!!.first),
                )
            },
            selected = current != FeedBean.SortBy.default,
            leadingIcon = {
                Icon(
                    imageVector = items[current]!!.second,
                    contentDescription = null,
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = if (expandMenu) Icons.Default.ArrowDropUp
                    else Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        )
        ListMenu(
            expanded = expandMenu,
            values = remember(items) { items.keys },
            displayName = { blockString(items[it]!!.first) },
            leadingIcon = { Icon(imageVector = items[it]!!.second, contentDescription = null) },
            onClick = onSort,
            onDismissRequest = { expandMenu = false },
        )
    }
}

@Composable
internal fun FavoriteFilter(
    current: Boolean?,
    onFilterFavorite: (Boolean?) -> Unit,
) {
    val items = remember {
        mapOf(
            null to Pair(
                Res.string.article_screen_filter_all,
                Icons.Outlined.FavoriteBorder,
            ),
            true to Pair(
                Res.string.article_screen_filter_favorite,
                Icons.Outlined.Favorite,
            ),
            false to Pair(
                Res.string.article_screen_filter_unfavorite,
                Icons.Outlined.FavoriteBorder,
            ),
        )
    }
    FavoriteReadMuteFilter(
        current = current,
        items = items,
        onFilter = onFilterFavorite,
    )
}

@Composable
internal fun ReadFilter(
    current: Boolean?,
    onFilterRead: (Boolean?) -> Unit,
) {
    val items = remember {
        mapOf(
            null to Pair(
                Res.string.article_screen_filter_all,
                Icons.Outlined.Markunread,
            ),
            true to Pair(
                Res.string.article_screen_filter_read,
                Icons.Outlined.Drafts,
            ),
            false to Pair(
                Res.string.article_screen_filter_unread,
                Icons.Outlined.MarkEmailUnread,
            ),
        )
    }
    FavoriteReadMuteFilter(
        current = current,
        items = items,
        onFilter = onFilterRead,
    )
}

@Composable
internal fun MuteFilter(
    current: Boolean?,
    onFilterMute: (Boolean?) -> Unit,
) {
    val items = remember {
        mapOf(
            null to Pair(
                Res.string.article_screen_filter_all,
                Icons.AutoMirrored.Outlined.VolumeDown,
            ),
            true to Pair(
                Res.string.article_screen_filter_mute,
                Icons.AutoMirrored.Outlined.VolumeOff,
            ),
            false to Pair(
                Res.string.article_screen_filter_unmute,
                Icons.AutoMirrored.Outlined.VolumeUp,
            ),
        )
    }
    FavoriteReadMuteFilter(
        current = current,
        items = items,
        onFilter = onFilterMute,
    )
}

@Composable
private fun FavoriteReadMuteFilter(
    current: Boolean?,
    items: Map<Boolean?, Pair<StringResource, ImageVector>>,
    onFilter: (Boolean?) -> Unit,
) {
    var expandMenu by rememberSaveable { mutableStateOf(false) }

    Box {
        FilterChip(
            onClick = { expandMenu = !expandMenu },
            label = {
                Text(
                    modifier = Modifier.animateContentSize(),
                    text = stringResource(items[current]!!.first),
                )
            },
            selected = current != null,
            leadingIcon = {
                Icon(
                    imageVector = items[current]!!.second,
                    contentDescription = null,
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = if (expandMenu) Icons.Default.ArrowDropUp
                    else Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        )
        ListMenu(
            expanded = expandMenu,
            values = remember(items) { items.keys },
            displayName = { blockString(items[it]!!.first) },
            leadingIcon = { Icon(imageVector = items[it]!!.second, contentDescription = null) },
            onClick = onFilter,
            onDismissRequest = { expandMenu = false },
        )
    }
}