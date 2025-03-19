package com.skyd.anivu.ui.screen.article

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skyd.anivu.R
import com.skyd.anivu.model.repository.article.ArticleSort
import com.skyd.anivu.ui.component.ListMenu

@Composable
fun FilterIcon(
    filterCount: Int,
    showFilterBar: Boolean,
    onFilterBarVisibilityChanged: (Boolean) -> Unit,
    onFilterFavorite: (Boolean?) -> Unit,
    onFilterRead: (Boolean?) -> Unit,
    onSort: (ArticleSort) -> Unit,
) {
    var expandMenu by rememberSaveable { mutableStateOf(false) }

    val icon: @Composable () -> Unit = {
        IconToggleButton(
            checked = showFilterBar,
            onCheckedChange = {
                if (filterCount == 0) onFilterBarVisibilityChanged(it)
                else expandMenu = true
            },
        ) {
            Icon(
                imageVector = Icons.Outlined.FilterAlt,
                contentDescription = stringResource(id = R.string.article_screen_show_filter_bar),
            )
        }
    }

    if (filterCount == 0) {
        icon()
    } else {
        BadgedBox(
            badge = {
                Badge {
                    Text(text = filterCount.toString())
                }
            }
        ) { icon() }
    }

    DropdownMenu(
        expanded = expandMenu,
        onDismissRequest = { expandMenu = false },
    ) {
        DropdownMenuItem(
            text = { Text(text = stringResource(id = R.string.article_screen_filter_clear_all_filter)) },
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.ClearAll, contentDescription = null)
            },
            onClick = {
                onFilterFavorite(null)
                onFilterRead(null)
                onSort(ArticleSort.default)
                expandMenu = false
            },
        )
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(
                        if (showFilterBar) R.string.article_screen_hide_filter_bar
                        else R.string.article_screen_show_filter_bar
                    )
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = if (showFilterBar) Icons.Outlined.FilterAltOff
                    else Icons.Outlined.FilterAlt,
                    contentDescription = null,
                )
            },
            onClick = {
                onFilterBarVisibilityChanged(!showFilterBar)
                expandMenu = false
            },
        )
    }
}

@Composable
internal fun FilterRow(
    modifier: Modifier = Modifier,
    articleFilterState: ArticleFilterState,
    onFilterFavorite: (Boolean?) -> Unit,
    onFilterRead: (Boolean?) -> Unit,
    onSort: (ArticleSort) -> Unit,
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
                current = articleFilterState.favoriteFilter,
                onFilterFavorite = onFilterFavorite,
            )
            ReadFilter(
                current = articleFilterState.readFilter,
                onFilterRead = onFilterRead,
            )
            SortSetting(
                current = articleFilterState.sortFilter,
                onSort = onSort,
            )
        }
    }
}

@Composable
internal fun SortSetting(
    current: ArticleSort,
    onSort: (ArticleSort) -> Unit,
) {
    val context = LocalContext.current
    var expandMenu by rememberSaveable { mutableStateOf(false) }
    val items = remember {
        mapOf(
            ArticleSort.default to Pair(
                context.getString(R.string.article_screen_sort_date_desc),
                Icons.Outlined.CalendarMonth,
            ),
            ArticleSort.Date(true) to Pair(
                context.getString(R.string.article_screen_sort_date_asc),
                Icons.Outlined.CalendarMonth,
            ),
            ArticleSort.Title(true) to Pair(
                context.getString(R.string.article_screen_sort_title_asc),
                Icons.Outlined.Title,
            ),
            ArticleSort.Title(false) to Pair(
                context.getString(R.string.article_screen_sort_title_desc),
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
                    text = items[current]!!.first,
                )
            },
            selected = current != ArticleSort.default,
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
            displayName = { items[it]?.first.orEmpty() },
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
    val context = LocalContext.current
    val items = remember {
        mapOf(
            null to Pair(
                context.getString(R.string.article_screen_filter_all),
                Icons.Outlined.FavoriteBorder,
            ),
            true to Pair(
                context.getString(R.string.article_screen_filter_favorite),
                Icons.Outlined.Favorite,
            ),
            false to Pair(
                context.getString(R.string.article_screen_filter_unfavorite),
                Icons.Outlined.FavoriteBorder,
            ),
        )
    }
    FavoriteReadFilter(
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
    val context = LocalContext.current
    val items = remember {
        mapOf(
            null to Pair(
                context.getString(R.string.article_screen_filter_all),
                Icons.Outlined.Markunread,
            ),
            true to Pair(
                context.getString(R.string.article_screen_filter_read),
                Icons.Outlined.Drafts,
            ),
            false to Pair(
                context.getString(R.string.article_screen_filter_unread),
                Icons.Outlined.MarkEmailUnread,
            ),
        )
    }
    FavoriteReadFilter(
        current = current,
        items = items,
        onFilter = onFilterRead,
    )
}

@Composable
private fun FavoriteReadFilter(
    current: Boolean?,
    items: Map<Boolean?, Pair<String, ImageVector>>,
    onFilter: (Boolean?) -> Unit,
) {
    var expandMenu by rememberSaveable { mutableStateOf(false) }

    Box {
        FilterChip(
            onClick = { expandMenu = !expandMenu },
            label = {
                Text(
                    modifier = Modifier.animateContentSize(),
                    text = items[current]!!.first,
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
            displayName = { items[it]?.first.orEmpty() },
            leadingIcon = { Icon(imageVector = items[it]!!.second, contentDescription = null) },
            onClick = onFilter,
            onDismissRequest = { expandMenu = false },
        )
    }
}