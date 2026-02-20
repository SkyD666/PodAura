package com.skyd.podaura.ui.screen.article

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Drafts
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ImportContacts
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.skyd.compone.component.blockString
import com.skyd.compone.component.menu.DropdownMenuDeleteItem
import com.skyd.compone.component.navigation.LocalNavBackStack
import com.skyd.compone.ext.thenIf
import com.skyd.podaura.ext.onRightClickIfSupported
import com.skyd.podaura.ext.readable
import com.skyd.podaura.ext.safeOpenUri
import com.skyd.podaura.ext.toDateTimeString
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.article.ArticleWithEnclosureBean
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.model.bean.playlist.MediaUrlWithArticleIdBean.Companion.toMediaUrlWithArticleIdBean
import com.skyd.podaura.model.preference.appearance.article.ArticleItemTonalElevationPreference
import com.skyd.podaura.model.preference.behavior.article.ArticleSwipeActionPreference
import com.skyd.podaura.model.preference.behavior.article.ArticleSwipeLeftActionPreference
import com.skyd.podaura.model.preference.behavior.article.ArticleSwipeRightActionPreference
import com.skyd.podaura.model.preference.behavior.article.ArticleTapActionPreference
import com.skyd.podaura.model.preference.behavior.article.DeduplicateTitleInDescPreference
import com.skyd.podaura.ui.component.PodAuraImage
import com.skyd.podaura.ui.component.dialog.DeleteArticleWarningDialog
import com.skyd.podaura.ui.component.swipe.SwipeAction
import com.skyd.podaura.ui.component.swipe.SwipeableActionsBox
import com.skyd.podaura.ui.screen.article.enclosure.EnclosureBottomSheet
import com.skyd.podaura.ui.screen.article.enclosure.getEnclosuresList
import com.skyd.podaura.ui.screen.feed.FeedIcon
import com.skyd.podaura.ui.screen.playlist.addto.AddToPlaylistSheet
import com.skyd.podaura.ui.screen.read.ReadRoute
import com.skyd.settings.suspendString
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.add_to_playlist
import podaura.shared.generated.resources.article_item_delete_warning
import podaura.shared.generated.resources.article_screen_favorite
import podaura.shared.generated.resources.article_screen_mark_as_read
import podaura.shared.generated.resources.article_screen_mark_as_unread
import podaura.shared.generated.resources.article_screen_no_link_tip
import podaura.shared.generated.resources.article_screen_read
import podaura.shared.generated.resources.article_screen_unfavorite
import podaura.shared.generated.resources.bottom_sheet_enclosure_title
import podaura.shared.generated.resources.open_link_in_browser


@Composable
fun Article1Item(
    data: ArticleWithFeed,
    onFavorite: (ArticleWithFeed, Boolean) -> Unit,
    onRead: (ArticleWithFeed, Boolean) -> Unit,
    onDelete: (ArticleWithFeed) -> Unit,
    onMessage: (String) -> Unit,
) {
    val navBackStack = LocalNavBackStack.current
    val uriHandler = LocalUriHandler.current
    var expandMenu by rememberSaveable { mutableStateOf(false) }
    val currentData by rememberUpdatedState(newValue = data)
    var openEnclosureBottomSheet by rememberSaveable { mutableStateOf(false) }
    var openAddToPlaylistSheet by rememberSaveable { mutableStateOf(false) }

    val articleWithEnclosure = currentData.articleWithEnclosure

    fun onAction(articleSwipeAction: String) {
        swipeAction(
            articleSwipeAction = articleSwipeAction,
            navBackStack = navBackStack,
            data = articleWithEnclosure,
            onMarkAsRead = {
                onRead(currentData, !articleWithEnclosure.article.isRead)
            },
            onMarkAsFavorite = {
                onFavorite(currentData, !articleWithEnclosure.article.isFavorite)
            },
            onShowEnclosureBottomSheet = { openEnclosureBottomSheet = true },
            onOpenLink = { uriHandler.safeOpenUri(it) },
            onOpenAddToPlaylistSheet = { openAddToPlaylistSheet = true },
            onMessage = onMessage,
        )
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .testTag("ArticleItem"),
    ) {
        val enableDismissFromStartToEnd =
            ArticleSwipeRightActionPreference.current != ArticleSwipeActionPreference.NONE
        val enableDismissFromEndToStart =
            ArticleSwipeLeftActionPreference.current != ArticleSwipeActionPreference.NONE

        SwipeableActionsBox(
            startActions = if (enableDismissFromStartToEnd) {
                rememberSwipeActions(
                    isStart = true,
                    article = articleWithEnclosure.article,
                    onSwipe = ::onAction,
                )
            } else {
                emptyList()
            },
            endActions = if (enableDismissFromEndToStart) {
                rememberSwipeActions(
                    isStart = false,
                    article = articleWithEnclosure.article,
                    onSwipe = ::onAction,
                )
            } else {
                emptyList()
            },
            backgroundUntilSwipeThreshold = MaterialTheme.colorScheme.background,
        ) {
            Article1ItemContent(
                data = data,
                onLongClick = { expandMenu = true },
                onFavorite = onFavorite,
                onRead = onRead,
                onShowEnclosureBottomSheet = { openEnclosureBottomSheet = true }
            )
            ArticleMenu(
                expanded = expandMenu,
                onDismissRequest = { expandMenu = false },
                data = data,
                onFavorite = onFavorite,
                onRead = onRead,
                onDelete = onDelete,
                onShowEnclosureBottomSheet = { openEnclosureBottomSheet = true },
                onOpenAddToPlaylistSheet = { openAddToPlaylistSheet = true },
            )
        }
    }

    if (openEnclosureBottomSheet) {
        EnclosureBottomSheet(
            onDismissRequest = { openEnclosureBottomSheet = false },
            dataList = remember(data) { getEnclosuresList(data.articleWithEnclosure) },
            article = data,
        )
    }
    if (openAddToPlaylistSheet) {
        val enclosures = data.articleWithEnclosure.enclosures
        AddToPlaylistSheet(
            onDismissRequest = { openAddToPlaylistSheet = false },
            currentPlaylistId = null,
            selectedMediaList = remember(enclosures) {
                enclosures.map { it.toMediaUrlWithArticleIdBean() }
            },
        )
    }
}

@Composable
private fun Article1ItemContent(
    data: ArticleWithFeed,
    onLongClick: () -> Unit,
    onFavorite: (ArticleWithFeed, Boolean) -> Unit,
    onRead: (ArticleWithFeed, Boolean) -> Unit,
    onShowEnclosureBottomSheet: () -> Unit,
) {
    val navBackStack = LocalNavBackStack.current
    val articleTapAction = ArticleTapActionPreference.current
    val articleWithEnclosure = data.articleWithEnclosure
    val article = articleWithEnclosure.article
    val colorAlpha = if (data.articleWithEnclosure.article.isRead) 0.5f else 1f

    CompositionLocalProvider(
        LocalContentColor provides LocalContentColor.current.copy(alpha = colorAlpha)
    ) {
        Column(
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceColorAtElevation(
                        LocalAbsoluteTonalElevation.current +
                                ArticleItemTonalElevationPreference.current.dp
                    )
                )
                .fillMaxWidth()
                .thenIf(!article.image.isNullOrBlank()) { height(IntrinsicSize.Max) }
                .combinedClickable(
                    onLongClick = onLongClick,
                    onClick = {
                        tapAction(
                            articleTapAction,
                            navBackStack,
                            articleWithEnclosure,
                            onShowEnclosureBottomSheet,
                        )
                    },
                )
                .onRightClickIfSupported(onClick = onLongClick),
        ) {
            val title = article.title?.readable().orEmpty()

            Row(modifier = Modifier.height(IntrinsicSize.Max)) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 12.dp)
                        .padding(horizontal = 15.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    Row {
                        val author = article.author
                        if (!author.isNullOrBlank()) {
                            Text(
                                text = author,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        val date = article.date?.toDateTimeString()
                        if (!date.isNullOrBlank()) {
                            if (!author.isNullOrBlank()) {
                                Text(
                                    modifier = Modifier.padding(horizontal = 3.dp),
                                    text = "·",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                            Text(
                                text = date,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = colorAlpha),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))

                    val description = article.description?.readable()?.let { desc ->
                        if (DeduplicateTitleInDescPreference.current) desc.replace(title, "")
                        else desc
                    }?.trim()
                    if (!description.isNullOrBlank()) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (!article.image.isNullOrBlank()) {
                    OutlinedCard(
                        modifier = Modifier
                            .padding(top = 12.dp, end = 12.dp)
                            .align(Alignment.CenterVertically),
                    ) {
                        PodAuraImage(
                            modifier = Modifier
                                .width(100.dp)
                                .fillMaxHeight()
                                .heightIn(min = 70.dp, max = 120.dp)
                                .layout { measurable, constraints ->
                                    if (constraints.maxHeight == Constraints.Infinity) {
                                        layout(0, 0) {}
                                    } else {
                                        val placeable = measurable.measure(constraints)
                                        layout(placeable.width, placeable.height) {
                                            placeable.place(0, 0)
                                        }
                                    }
                                },
                            model = articleWithEnclosure.media?.image.orEmpty()
                                .ifBlank { article.image },
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }

            // Bottom row
            Row(
                modifier = Modifier.padding(start = 11.dp, end = 9.dp, top = 3.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArticleItemFeedInfo(data = data, colorAlpha = colorAlpha)
                val isFavorite = articleWithEnclosure.article.isFavorite
                val isRead = articleWithEnclosure.article.isRead
                ArticleItemIconButton(
                    onClick = { onFavorite(data, !isFavorite) },
                    imageVector = if (isFavorite) {
                        Icons.Outlined.Favorite
                    } else {
                        Icons.Outlined.FavoriteBorder
                    },
                    contentDescription = if (isFavorite) {
                        stringResource(Res.string.article_screen_favorite)
                    } else {
                        stringResource(Res.string.article_screen_unfavorite)
                    },
                )
                Spacer(modifier = Modifier.width(3.dp))
                ArticleItemIconButton(
                    onClick = { onRead(data, !isRead) },
                    imageVector = if (isRead) {
                        Icons.Outlined.Drafts
                    } else {
                        Icons.Outlined.MarkEmailUnread
                    },
                    contentDescription = if (isRead) {
                        stringResource(Res.string.article_screen_mark_as_unread)
                    } else {
                        stringResource(Res.string.article_screen_mark_as_read)
                    },
                )
            }
        }
    }
}

@Composable
fun ArticleItemIconButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String?,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(6.dp),
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint,
    )
}

@Composable
fun RowScope.ArticleItemFeedInfo(data: ArticleWithFeed, colorAlpha: Float = 1f) {
    val navBackStack = LocalNavBackStack.current
    Box(modifier = Modifier.weight(1f)) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .clickable { navBackStack.add(ArticleRoute(feedUrls = listOf(data.feed.url))) }
                .padding(horizontal = 4.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FeedIcon(
                data = data.feed,
                size = 22.dp
            )
            val feedName =
                data.feed.nickname.orEmpty().ifBlank { data.feed.title.orEmpty() }
            if (feedName.isNotBlank()) {
                Text(
                    modifier = Modifier.padding(start = 6.dp, end = 2.dp),
                    text = feedName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = colorAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ArticleMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    data: ArticleWithFeed,
    onFavorite: (ArticleWithFeed, Boolean) -> Unit,
    onRead: (ArticleWithFeed, Boolean) -> Unit,
    onDelete: (ArticleWithFeed) -> Unit,
    onShowEnclosureBottomSheet: () -> Unit,
    onOpenAddToPlaylistSheet: () -> Unit,
) {
    val navBackStack = LocalNavBackStack.current
    val uriHandler = LocalUriHandler.current
    val articleWithEnclosure = data.articleWithEnclosure
    val isFavorite = articleWithEnclosure.article.isFavorite
    val isRead = articleWithEnclosure.article.isRead
    val articleLink = articleWithEnclosure.article.link
    var openDeleteWarningDialog by rememberSaveable { mutableStateOf(false) }

    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        val texts = listOf(
            listOf(
                stringResource(
                    if (isFavorite) Res.string.article_screen_unfavorite
                    else Res.string.article_screen_favorite
                ),
                stringResource(
                    if (isRead) Res.string.article_screen_mark_as_unread
                    else Res.string.article_screen_mark_as_read
                )
            ),
            listOf(
                stringResource(Res.string.article_screen_read),
                stringResource(Res.string.bottom_sheet_enclosure_title),
                stringResource(Res.string.add_to_playlist),
                stringResource(Res.string.open_link_in_browser),
            ),
        )
        val leadingIcons = listOf(
            listOf(
                if (isFavorite) Icons.Outlined.FavoriteBorder else Icons.Outlined.Favorite,
                if (isRead) Icons.Outlined.MarkEmailUnread else Icons.Outlined.MarkEmailRead,
            ),
            listOf(
                Icons.Outlined.ImportContacts,
                Icons.Outlined.AttachFile,
                Icons.AutoMirrored.Outlined.PlaylistAdd,
                Icons.Outlined.OpenInBrowser,
            ),
        )
        val onClicks = listOf(
            listOf(
                {
                    onFavorite(data, !isFavorite)
                    onDismissRequest()
                },
                {
                    onRead(data, !isRead)
                    onDismissRequest()
                },
            ),
            listOf(
                {
                    navigateToReadScreen(
                        navBackStack = navBackStack,
                        data = articleWithEnclosure,
                    )
                    onDismissRequest()
                },
                {
                    onShowEnclosureBottomSheet()
                    onDismissRequest()
                },
                {
                    onOpenAddToPlaylistSheet()
                    onDismissRequest()
                },
                {
                    articleLink?.let { uriHandler.safeOpenUri(it) }
                    onDismissRequest()
                },
            ),
        )
        val enables = listOf(
            listOf(true, true),
            listOf(true, true, true, articleLink != null),
        )
        val groupCount = texts.size
        texts.forEachIndexed { groupIndex, subTexts ->
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(groupIndex, groupCount + 1)) {
                subTexts.forEachIndexed { itemIndex, text ->
                    DropdownMenuItem(
                        text = { Text(text = text) },
                        shape = MenuDefaults.itemShape(itemIndex, subTexts.size).shape,
                        leadingIcon = {
                            Icon(
                                imageVector = leadingIcons[groupIndex][itemIndex],
                                contentDescription = null,
                            )
                        },
                        onClick = onClicks[groupIndex][itemIndex],
                        enabled = enables[groupIndex][itemIndex],
                    )
                }
            }
            Spacer(Modifier.height(MenuDefaults.GroupSpacing))
        }
        DropdownMenuGroup(shapes = MenuDefaults.groupShape(groupCount, groupCount + 1)) {
            DropdownMenuDeleteItem(
                shape = MenuDefaults.itemShape(0, 1).shape,
                onClick = {
                    openDeleteWarningDialog = true
                    onDismissRequest()
                },
            )
        }
    }
    if (openDeleteWarningDialog) {
        DeleteArticleWarningDialog(
            text = stringResource(Res.string.article_item_delete_warning),
            onDismissRequest = { openDeleteWarningDialog = false },
            onDismiss = { openDeleteWarningDialog = false },
            onConfirm = {
                onDelete(data)
                openDeleteWarningDialog = false
            },
        )
    }
}

@Composable
private fun rememberSwipeActions(
    isStart: Boolean,
    article: ArticleBean,
    onSwipe: (String) -> Unit,
): List<SwipeAction> {
    val articleSwipeAction = if (isStart) {
        ArticleSwipeRightActionPreference.current
    } else {
        ArticleSwipeLeftActionPreference.current
    }
    val background = MaterialTheme.colorScheme.tertiaryContainer
    val icon = rememberVectorPainter(
        when (articleSwipeAction) {
            ArticleSwipeActionPreference.READ -> Icons.Outlined.ImportContacts
            ArticleSwipeActionPreference.SHOW_ENCLOSURES -> Icons.Outlined.AttachFile
            ArticleSwipeActionPreference.SWITCH_READ_STATE ->
                if (article.isRead) Icons.Outlined.MarkEmailUnread
                else Icons.Outlined.Drafts

            ArticleSwipeActionPreference.SWITCH_FAVORITE_STATE ->
                if (article.isFavorite) Icons.Outlined.FavoriteBorder
                else Icons.Outlined.Favorite

            ArticleSwipeActionPreference.OPEN_LINK_IN_BROWSER -> Icons.Outlined.OpenInBrowser
            ArticleSwipeActionPreference.ADD_TO_PLAYLIST -> Icons.AutoMirrored.Outlined.PlaylistAdd

            else -> Icons.Outlined.ImportContacts
        }
    )
    val contentDescription = suspendString {
        ArticleSwipeActionPreference.toDisplayName(articleSwipeAction)
    }
    return remember(onSwipe, articleSwipeAction, background, icon, contentDescription) {
        listOf(
            SwipeAction(
                onSwipe = { onSwipe(articleSwipeAction) },
                icon = {
                    Icon(
                        painter = icon,
                        contentDescription = contentDescription,
                        modifier = Modifier.padding(20.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                },
                background = background,
            )
        )
    }
}

@Composable
fun Article1ItemPlaceholder() {
    val color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                MaterialTheme.colorScheme.surfaceColorAtElevation(
                    LocalAbsoluteTonalElevation.current +
                            ArticleItemTonalElevationPreference.current.dp
                )
            )
            .fillMaxWidth(),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Max)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 12.dp)
                    .padding(horizontal = 15.dp),
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .fillMaxWidth(0.7f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
                Spacer(modifier = Modifier.height(7.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color)
                )
            }
        }

        // Bottom row
        Row(
            modifier = Modifier.padding(start = 15.dp, end = 9.dp, top = 3.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(color)
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .fillMaxWidth(0.3f)
                    .height(15.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
            )

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .padding(6.dp),
                imageVector = Icons.Outlined.Favorite,
                tint = color,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(3.dp))
            Icon(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .padding(6.dp),
                imageVector = Icons.Outlined.Drafts,
                tint = color,
                contentDescription = null,
            )
        }
    }
}

private fun swipeAction(
    articleSwipeAction: String,
    navBackStack: MutableList<NavKey>,
    data: ArticleWithEnclosureBean,
    onMarkAsRead: () -> Unit,
    onMarkAsFavorite: () -> Unit,
    onShowEnclosureBottomSheet: () -> Unit,
    onOpenLink: (String) -> Unit,
    onOpenAddToPlaylistSheet: () -> Unit,
    onMessage: (String) -> Unit,
) {
    when (articleSwipeAction) {
        ArticleSwipeActionPreference.READ ->
            navigateToReadScreen(navBackStack = navBackStack, data = data)

        ArticleSwipeActionPreference.SHOW_ENCLOSURES -> onShowEnclosureBottomSheet()
        ArticleSwipeActionPreference.OPEN_LINK_IN_BROWSER -> data.article.link?.let { onOpenLink(it) }
            ?: onMessage(blockString(Res.string.article_screen_no_link_tip))

        ArticleSwipeActionPreference.SWITCH_READ_STATE -> onMarkAsRead()
        ArticleSwipeActionPreference.SWITCH_FAVORITE_STATE -> onMarkAsFavorite()
        ArticleSwipeActionPreference.ADD_TO_PLAYLIST -> onOpenAddToPlaylistSheet()
        else -> navigateToReadScreen(navBackStack = navBackStack, data = data)
    }
}

private fun tapAction(
    articleTapAction: String,
    navBackStack: MutableList<NavKey>,
    data: ArticleWithEnclosureBean,
    onShowEnclosureBottomSheet: () -> Unit,
) {
    when (articleTapAction) {
        ArticleTapActionPreference.READ ->
            navigateToReadScreen(navBackStack = navBackStack, data = data)

        ArticleTapActionPreference.SHOW_ENCLOSURES -> onShowEnclosureBottomSheet()
        else -> navigateToReadScreen(navBackStack = navBackStack, data = data)
    }
}

fun navigateToReadScreen(navBackStack: MutableList<NavKey>, data: ArticleWithEnclosureBean) {
    navBackStack.add(ReadRoute(articleId = data.article.articleId))
}