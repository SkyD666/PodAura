package com.skyd.podaura.ui.screen.article

import androidx.compose.animation.Animatable
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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.navigation.NavController
import com.skyd.compone.component.menu.DropdownMenuDeleteItem
import com.skyd.compone.ext.thenIf
import com.skyd.compone.local.LocalGlobalNavController
import com.skyd.compone.local.LocalNavController
import com.skyd.podaura.ext.getOrDefault
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
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.ui.component.PodAuraImage
import com.skyd.podaura.ui.component.dialog.DeleteArticleWarningDialog
import com.skyd.podaura.ui.screen.article.enclosure.EnclosureBottomSheet
import com.skyd.podaura.ui.screen.article.enclosure.getEnclosuresList
import com.skyd.podaura.ui.screen.feed.FeedIcon
import com.skyd.podaura.ui.screen.playlist.addto.AddToPlaylistSheet
import com.skyd.podaura.ui.screen.read.ReadRoute
import com.skyd.settings.suspendString
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
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
    val globalNavController = LocalGlobalNavController.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var expandMenu by rememberSaveable { mutableStateOf(false) }
    val currentData by rememberUpdatedState(newValue = data)
    var openEnclosureBottomSheet by rememberSaveable { mutableStateOf(false) }
    var openAddToPlaylistSheet by rememberSaveable { mutableStateOf(false) }

    val swipeToDismissBoxState = rememberSwipeToDismissBoxState(positionalThreshold = { it * .15f })
    LaunchedEffect(data) { swipeToDismissBoxState.reset() }
    var isSwipeToDismissActive by remember(data) { mutableStateOf(false) }

    LaunchedEffect(swipeToDismissBoxState.progress > 0.15f) {
        isSwipeToDismissActive = swipeToDismissBoxState.progress > 0.15f &&
                swipeToDismissBoxState.targetValue != SwipeToDismissBoxValue.Settled
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
        SwipeToDismissBox(
            state = swipeToDismissBoxState,
            backgroundContent = {
                SwipeBackgroundContent(
                    article = data.articleWithEnclosure.article,
                    direction = swipeToDismissBoxState.dismissDirection,
                    isActive = isSwipeToDismissActive,
                )
            },
            enableDismissFromStartToEnd = enableDismissFromStartToEnd,
            enableDismissFromEndToStart = enableDismissFromEndToStart,
            gesturesEnabled = enableDismissFromStartToEnd || enableDismissFromEndToStart,
            onDismiss = { dismissValue ->
                val articleSwipeAction = dataStore.getOrDefault(
                    if (dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                        ArticleSwipeRightActionPreference
                    } else {
                        ArticleSwipeLeftActionPreference
                    }
                )
                when (dismissValue) {
                    SwipeToDismissBoxValue.EndToStart, SwipeToDismissBoxValue.StartToEnd -> scope.launch {
                        val articleWithEnclosure = currentData.articleWithEnclosure
                        swipeAction(
                            articleSwipeAction = articleSwipeAction,
                            navController = globalNavController,
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

                    SwipeToDismissBoxValue.Settled -> Unit
                }
                false
            },
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
    val globalNavController = LocalGlobalNavController.current
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
                            globalNavController,
                            articleWithEnclosure,
                            onShowEnclosureBottomSheet,
                        )
                    },
                ),
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
    val navController = LocalNavController.current
    Box(modifier = Modifier.weight(1f)) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .clickable { navController.navigate(ArticleRoute(feedUrls = listOf(data.feed.url))) }
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
    val globalNavController = LocalGlobalNavController.current
    val uriHandler = LocalUriHandler.current
    val articleWithEnclosure = data.articleWithEnclosure
    val isFavorite = articleWithEnclosure.article.isFavorite
    val isRead = articleWithEnclosure.article.isRead
    val articleLink = articleWithEnclosure.article.link
    var openDeleteWarningDialog by rememberSaveable { mutableStateOf(false) }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(
                        if (isFavorite) Res.string.article_screen_unfavorite
                        else Res.string.article_screen_favorite
                    )
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = if (isFavorite) Icons.Outlined.FavoriteBorder
                    else Icons.Outlined.Favorite,
                    contentDescription = null,
                )
            },
            onClick = {
                onFavorite(data, !isFavorite)
                onDismissRequest()
            },
        )
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(
                        if (isRead) Res.string.article_screen_mark_as_unread
                        else Res.string.article_screen_mark_as_read
                    )
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = if (isRead) Icons.Outlined.MarkEmailUnread
                    else Icons.Outlined.MarkEmailRead,
                    contentDescription = null,
                )
            },
            onClick = {
                onRead(data, !isRead)
                onDismissRequest()
            },
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(text = stringResource(Res.string.article_screen_read)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.ImportContacts,
                    contentDescription = null,
                )
            },
            onClick = {
                navigateToReadScreen(
                    navController = globalNavController,
                    data = articleWithEnclosure,
                )
                onDismissRequest()
            },
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(Res.string.bottom_sheet_enclosure_title)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.AttachFile,
                    contentDescription = null,
                )
            },
            onClick = {
                onShowEnclosureBottomSheet()
                onDismissRequest()
            },
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(Res.string.add_to_playlist)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.PlaylistAdd,
                    contentDescription = null,
                )
            },
            onClick = {
                onOpenAddToPlaylistSheet()
                onDismissRequest()
            },
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(Res.string.open_link_in_browser)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.OpenInBrowser,
                    contentDescription = null,
                )
            },
            onClick = {
                articleLink?.let { uriHandler.safeOpenUri(it) }
                onDismissRequest()
            },
            enabled = articleLink != null
        )
        HorizontalDivider()
        DropdownMenuDeleteItem(
            onClick = {
                openDeleteWarningDialog = true
                onDismissRequest()
            }
        )
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
private fun SwipeBackgroundContent(
    article: ArticleBean,
    direction: SwipeToDismissBoxValue,
    isActive: Boolean,
) {
    val containerColor = MaterialTheme.colorScheme.background
    val containerColorElevated = MaterialTheme.colorScheme.tertiaryContainer
    val backgroundColor = remember(isActive) { Animatable(containerColor) }
    val articleSwipeAction = if (direction == SwipeToDismissBoxValue.StartToEnd) {
        ArticleSwipeRightActionPreference.current
    } else {
        ArticleSwipeLeftActionPreference.current
    }

    LaunchedEffect(isActive) {
        backgroundColor.animateTo(if (isActive) containerColorElevated else containerColor)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(backgroundColor.value)
            .padding(horizontal = 20.dp),
        contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) {
            Alignment.CenterStart
        } else {
            Alignment.CenterEnd
        },
    ) {
        val painter = when (direction) {
            SwipeToDismissBoxValue.StartToEnd,
            SwipeToDismissBoxValue.EndToStart -> rememberVectorPainter(
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

            SwipeToDismissBoxValue.Settled -> null
        }
        val contentDescription = when (direction) {
            SwipeToDismissBoxValue.StartToEnd,
            SwipeToDismissBoxValue.EndToStart -> suspendString {
                ArticleSwipeActionPreference.toDisplayName(articleSwipeAction)
            }

            SwipeToDismissBoxValue.Settled -> null
        }

        if (painter != null) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
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

private suspend fun swipeAction(
    articleSwipeAction: String,
    navController: NavController,
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
            navigateToReadScreen(navController = navController, data = data)

        ArticleSwipeActionPreference.SHOW_ENCLOSURES -> onShowEnclosureBottomSheet()
        ArticleSwipeActionPreference.OPEN_LINK_IN_BROWSER -> data.article.link?.let { onOpenLink(it) }
            ?: onMessage(getString(Res.string.article_screen_no_link_tip))

        ArticleSwipeActionPreference.SWITCH_READ_STATE -> onMarkAsRead()
        ArticleSwipeActionPreference.SWITCH_FAVORITE_STATE -> onMarkAsFavorite()
        ArticleSwipeActionPreference.ADD_TO_PLAYLIST -> onOpenAddToPlaylistSheet()
        else -> navigateToReadScreen(navController = navController, data = data)
    }
}

private fun tapAction(
    articleTapAction: String,
    navController: NavController,
    data: ArticleWithEnclosureBean,
    onShowEnclosureBottomSheet: () -> Unit,
) {
    when (articleTapAction) {
        ArticleTapActionPreference.READ ->
            navigateToReadScreen(navController = navController, data = data)

        ArticleTapActionPreference.SHOW_ENCLOSURES -> onShowEnclosureBottomSheet()
        else -> navigateToReadScreen(navController = navController, data = data)
    }
}

fun navigateToReadScreen(navController: NavController, data: ArticleWithEnclosureBean) {
    navController.navigate(ReadRoute(articleId = data.article.articleId))
}