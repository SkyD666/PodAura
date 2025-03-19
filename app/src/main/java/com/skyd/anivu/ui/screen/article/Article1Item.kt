package com.skyd.anivu.ui.screen.article

import android.content.Context
import androidx.compose.animation.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.material3.MenuDefaults
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.EventListener
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import com.skyd.anivu.R
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.firstCodePointOrNull
import com.skyd.anivu.ext.getOrDefault
import com.skyd.anivu.ext.openBrowser
import com.skyd.anivu.ext.readable
import com.skyd.anivu.ext.thenIf
import com.skyd.anivu.ext.toDateTimeString
import com.skyd.anivu.model.bean.article.ArticleBean
import com.skyd.anivu.model.bean.article.ArticleWithEnclosureBean
import com.skyd.anivu.model.bean.article.ArticleWithFeed
import com.skyd.anivu.model.bean.feed.FeedBean
import com.skyd.anivu.model.preference.behavior.article.ArticleSwipeActionPreference
import com.skyd.anivu.model.preference.behavior.article.ArticleSwipeLeftActionPreference
import com.skyd.anivu.model.preference.behavior.article.ArticleSwipeRightActionPreference
import com.skyd.anivu.model.preference.behavior.article.ArticleTapActionPreference
import com.skyd.anivu.ui.component.PodAuraImage
import com.skyd.anivu.ui.component.dialog.DeleteArticleWarningDialog
import com.skyd.anivu.ui.component.rememberPodAuraImageLoader
import com.skyd.anivu.ui.component.showToast
import com.skyd.anivu.ui.local.LocalArticleItemTonalElevation
import com.skyd.anivu.ui.local.LocalArticleSwipeLeftAction
import com.skyd.anivu.ui.local.LocalArticleSwipeRightAction
import com.skyd.anivu.ui.local.LocalArticleTapAction
import com.skyd.anivu.ui.local.LocalDeduplicateTitleInDesc
import com.skyd.anivu.ui.local.LocalNavController
import com.skyd.anivu.ui.screen.article.enclosure.EnclosureBottomSheet
import com.skyd.anivu.ui.screen.article.enclosure.getEnclosuresList
import com.skyd.anivu.ui.screen.read.openReadScreen


@Composable
fun Article1Item(
    data: ArticleWithFeed,
    onFavorite: (ArticleWithFeed, Boolean) -> Unit,
    onRead: (ArticleWithFeed, Boolean) -> Unit,
    onDelete: (ArticleWithFeed) -> Unit,
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    var expandMenu by rememberSaveable { mutableStateOf(false) }
    val dataWrapper by rememberUpdatedState(newValue = data)
    var openEnclosureBottomSheet by rememberSaveable { mutableStateOf<List<Any>?>(null) }

    val swipeToDismissBoxState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            val articleSwipeAction = context.dataStore.getOrDefault(
                if (dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                    ArticleSwipeRightActionPreference
                } else {
                    ArticleSwipeLeftActionPreference
                }
            )
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart, SwipeToDismissBoxValue.StartToEnd -> {
                    val articleWithEnclosure = dataWrapper.articleWithEnclosure
                    swipeAction(
                        articleSwipeAction = articleSwipeAction,
                        context = context,
                        navController = navController,
                        data = articleWithEnclosure,
                        onMarkAsRead = {
                            onRead(dataWrapper, !articleWithEnclosure.article.isRead)
                        },
                        onMarkAsFavorite = {
                            onFavorite(dataWrapper, !articleWithEnclosure.article.isFavorite)
                        },
                        onShowEnclosureBottomSheet = {
                            openEnclosureBottomSheet = getEnclosuresList(context, it)
                        }
                    )
                }

                SwipeToDismissBoxValue.Settled -> Unit
            }
            false
        },
        positionalThreshold = { it * 0.15f },
    )
    LaunchedEffect(data) { swipeToDismissBoxState.reset() }
    var isSwipeToDismissActive by remember(data) { mutableStateOf(false) }

    LaunchedEffect(swipeToDismissBoxState.progress > 0.15f) {
        isSwipeToDismissActive = swipeToDismissBoxState.progress > 0.15f &&
                swipeToDismissBoxState.targetValue != SwipeToDismissBoxValue.Settled
    }

    Box(modifier = Modifier.clip(RoundedCornerShape(12.dp))) {
        val enableDismissFromStartToEnd =
            LocalArticleSwipeRightAction.current != ArticleSwipeActionPreference.NONE
        val enableDismissFromEndToStart =
            LocalArticleSwipeLeftAction.current != ArticleSwipeActionPreference.NONE
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
        ) {
            Article1ItemContent(
                data = data,
                onLongClick = { expandMenu = true },
                onFavorite = onFavorite,
                onRead = onRead,
                onShowEnclosureBottomSheet = {
                    openEnclosureBottomSheet = getEnclosuresList(context, it)
                }
            )
            ArticleMenu(
                expanded = expandMenu,
                onDismissRequest = { expandMenu = false },
                data = data,
                onFavorite = onFavorite,
                onRead = onRead,
                onDelete = onDelete,
                onShowEnclosureBottomSheet = {
                    openEnclosureBottomSheet = getEnclosuresList(context, it)
                }
            )
        }
    }

    if (openEnclosureBottomSheet != null) {
        EnclosureBottomSheet(
            onDismissRequest = { openEnclosureBottomSheet = null },
            dataList = openEnclosureBottomSheet.orEmpty(),
            article = data,
        )
    }
}

@Composable
private fun Article1ItemContent(
    data: ArticleWithFeed,
    onLongClick: () -> Unit,
    onFavorite: (ArticleWithFeed, Boolean) -> Unit,
    onRead: (ArticleWithFeed, Boolean) -> Unit,
    onShowEnclosureBottomSheet: (ArticleWithEnclosureBean) -> Unit,
) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val articleTapAction = LocalArticleTapAction.current
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
                                LocalArticleItemTonalElevation.current.dp
                    )
                )
                .fillMaxWidth()
                .thenIf(!article.image.isNullOrBlank()) { height(IntrinsicSize.Max) }
                .combinedClickable(
                    onLongClick = onLongClick,
                    onClick = {
                        tapAction(
                            articleTapAction,
                            navController,
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
                        val date = article.date?.toDateTimeString(context = context)
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
                        if (LocalDeduplicateTitleInDesc.current) desc.replace(title, "") else desc
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
                Box(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .clickable {
                                openArticleScreen(
                                    navController = navController,
                                    feedUrls = listOf(data.feed.url),
                                )
                            }
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
                val isFavorite = articleWithEnclosure.article.isFavorite
                val isRead = articleWithEnclosure.article.isRead
                Icon(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onFavorite(data, !isFavorite) }
                        .padding(6.dp),
                    imageVector = if (isFavorite) {
                        Icons.Outlined.Favorite
                    } else {
                        Icons.Outlined.FavoriteBorder
                    },
                    contentDescription = if (isFavorite) {
                        stringResource(id = R.string.article_screen_favorite)
                    } else {
                        stringResource(id = R.string.article_screen_unfavorite)
                    },
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onRead(data, !isRead) }
                        .padding(6.dp),
                    imageVector = if (isRead) {
                        Icons.Outlined.Drafts
                    } else {
                        Icons.Outlined.MarkEmailUnread
                    },
                    contentDescription = if (isRead) {
                        stringResource(id = R.string.article_screen_mark_as_unread)
                    } else {
                        stringResource(id = R.string.article_screen_mark_as_read)
                    },
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
    onShowEnclosureBottomSheet: (ArticleWithEnclosureBean) -> Unit,
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val articleWithEnclosure = data.articleWithEnclosure
    val isFavorite = articleWithEnclosure.article.isFavorite
    val isRead = articleWithEnclosure.article.isRead
    var openDeleteWarningDialog by rememberSaveable { mutableStateOf(false) }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(
                        if (isFavorite) R.string.article_screen_unfavorite
                        else R.string.article_screen_favorite
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
                        if (isRead) R.string.article_screen_mark_as_unread
                        else R.string.article_screen_mark_as_read
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
            text = { Text(text = stringResource(id = R.string.article_screen_read)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.ImportContacts,
                    contentDescription = null,
                )
            },
            onClick = {
                navigateToReadScreen(
                    navController = navController,
                    data = articleWithEnclosure,
                )
                onDismissRequest()
            },
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(id = R.string.bottom_sheet_enclosure_title)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.AttachFile,
                    contentDescription = null,
                )
            },
            onClick = {
                onShowEnclosureBottomSheet(articleWithEnclosure)
                onDismissRequest()
            },
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(id = R.string.open_link_in_browser)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.OpenInBrowser,
                    contentDescription = null,
                )
            },
            onClick = {
                data.articleWithEnclosure.article.openLinkInBrowser(context)
                onDismissRequest()
            },
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(text = stringResource(id = R.string.delete)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                )
            },
            onClick = {
                openDeleteWarningDialog = true
                onDismissRequest()
            },
            colors = MenuDefaults.itemColors(
                textColor = MaterialTheme.colorScheme.error,
                leadingIconColor = MaterialTheme.colorScheme.error,
                trailingIconColor = MaterialTheme.colorScheme.error,
            ),
        )
    }
    if (openDeleteWarningDialog) {
        DeleteArticleWarningDialog(
            text = stringResource(id = R.string.article_item_delete_warning),
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
fun FeedIcon(modifier: Modifier = Modifier, data: FeedBean, size: Dp = 22.dp) {
    val defaultIcon: @Composable () -> Unit = {
        Box(
            modifier = modifier
                .size(size)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = (data.nickname?.takeIf { it.isNotEmpty() } ?: data.title)
                    ?.firstCodePointOrNull().orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
    var imageLoadError by rememberSaveable(data) { mutableStateOf(false) }

    var icon by remember(data) { mutableStateOf(data.customIcon.orEmpty().ifBlank { data.icon }) }
    if (icon.isNullOrBlank() || imageLoadError) {
        defaultIcon()
    } else {
        PodAuraImage(
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            model = icon,
            imageLoader = rememberPodAuraImageLoader(listener = object : EventListener() {
                override fun onError(request: ImageRequest, result: ErrorResult) {
                    if (icon == data.customIcon) {
                        icon = data.icon
                    } else {
                        imageLoadError = true
                    }
                }
            }),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun SwipeBackgroundContent(
    article: ArticleBean,
    direction: SwipeToDismissBoxValue,
    isActive: Boolean,
) {
    val context = LocalContext.current
    val containerColor = MaterialTheme.colorScheme.background
    val containerColorElevated = MaterialTheme.colorScheme.tertiaryContainer
    val backgroundColor = remember(isActive) { Animatable(containerColor) }
    val articleSwipeAction = if (direction == SwipeToDismissBoxValue.StartToEnd) {
        LocalArticleSwipeRightAction.current
    } else {
        LocalArticleSwipeLeftAction.current
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

                    ArticleSwipeActionPreference.OPEN_LINK_IN_BROWSER ->
                        Icons.Outlined.OpenInBrowser

                    else -> Icons.Outlined.ImportContacts
                }
            )

            SwipeToDismissBoxValue.Settled -> null
        }
        val contentDescription = when (direction) {
            SwipeToDismissBoxValue.StartToEnd,
            SwipeToDismissBoxValue.EndToStart -> ArticleSwipeActionPreference
                .toDisplayName(context, articleSwipeAction)

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
                            LocalArticleItemTonalElevation.current.dp
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
    context: Context,
    navController: NavController,
    data: ArticleWithEnclosureBean,
    onMarkAsRead: () -> Unit,
    onMarkAsFavorite: () -> Unit,
    onShowEnclosureBottomSheet: (ArticleWithEnclosureBean) -> Unit,
) {
    when (articleSwipeAction) {
        ArticleSwipeActionPreference.READ ->
            navigateToReadScreen(navController = navController, data = data)

        ArticleSwipeActionPreference.SHOW_ENCLOSURES -> onShowEnclosureBottomSheet(data)
        ArticleSwipeActionPreference.OPEN_LINK_IN_BROWSER -> data.article.openLinkInBrowser(context)
        ArticleSwipeActionPreference.SWITCH_READ_STATE -> onMarkAsRead()
        ArticleSwipeActionPreference.SWITCH_FAVORITE_STATE -> onMarkAsFavorite()
        else -> navigateToReadScreen(navController = navController, data = data)
    }
}

private fun tapAction(
    articleTapAction: String,
    navController: NavController,
    data: ArticleWithEnclosureBean,
    onShowEnclosureBottomSheet: (ArticleWithEnclosureBean) -> Unit,
) {
    when (articleTapAction) {
        ArticleTapActionPreference.READ ->
            navigateToReadScreen(navController = navController, data = data)

        ArticleTapActionPreference.SHOW_ENCLOSURES -> onShowEnclosureBottomSheet(data)
        else -> navigateToReadScreen(navController = navController, data = data)
    }
}

fun ArticleBean.openLinkInBrowser(context: Context) {
    link?.openBrowser(context)
        ?: context.getString(R.string.article_screen_no_link_tip).showToast()
}

fun navigateToReadScreen(navController: NavController, data: ArticleWithEnclosureBean) {
    openReadScreen(
        navController = navController,
        articleId = data.article.articleId,
    )
}