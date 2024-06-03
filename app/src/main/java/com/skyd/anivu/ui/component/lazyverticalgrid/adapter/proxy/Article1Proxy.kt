package com.skyd.anivu.ui.component.lazyverticalgrid.adapter.proxy

import android.content.Context
import android.os.Bundle
import androidx.compose.animation.Animatable
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ImportContacts
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import coil.EventListener
import coil.request.ErrorResult
import coil.request.ImageRequest
import com.skyd.anivu.R
import com.skyd.anivu.ext.activity
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.getOrDefault
import com.skyd.anivu.ext.readable
import com.skyd.anivu.ext.toDateTimeString
import com.skyd.anivu.model.bean.ArticleWithEnclosureBean
import com.skyd.anivu.model.bean.ArticleWithFeed
import com.skyd.anivu.model.bean.FeedBean
import com.skyd.anivu.model.preference.behavior.article.ArticleSwipeLeftActionPreference
import com.skyd.anivu.model.preference.behavior.article.ArticleTapActionPreference
import com.skyd.anivu.ui.component.AniVuImage
import com.skyd.anivu.ui.component.lazyverticalgrid.adapter.LazyGridAdapter
import com.skyd.anivu.ui.component.rememberAniVuImageLoader
import com.skyd.anivu.ui.fragment.read.EnclosureBottomSheet
import com.skyd.anivu.ui.fragment.read.ReadFragment
import com.skyd.anivu.ui.local.LocalArticleSwipeLeftAction
import com.skyd.anivu.ui.local.LocalArticleTapAction
import com.skyd.anivu.ui.local.LocalDeduplicateTitleInDesc
import com.skyd.anivu.ui.local.LocalNavController

class Article1Proxy(
    private val onFavorite: (ArticleWithFeed, Boolean) -> Unit,
    private val onRead: (ArticleWithFeed, Boolean) -> Unit,
) : LazyGridAdapter.Proxy<ArticleWithFeed>() {
    @Composable
    override fun Draw(index: Int, data: ArticleWithFeed) {
        Article1Item(data = data, onFavorite = onFavorite, onRead = onRead)
    }
}

@Composable
fun Article1Item(
    data: ArticleWithFeed,
    onFavorite: (ArticleWithFeed, Boolean) -> Unit,
    onRead: (ArticleWithFeed, Boolean) -> Unit,
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val articleTapAction = LocalArticleTapAction.current
    val articleSwipeLeftAction = LocalArticleSwipeLeftAction.current
    val articleWithEnclosure = data.articleWithEnclosure
    val article = articleWithEnclosure.article
    var expandMenu by rememberSaveable { mutableStateOf(false) }

    val swipeToDismissBoxState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                swipeLeftAction(
                    // rememberSwipeToDismissBoxState does not update the variable
                    // when the outer recompose, so don't use the outer articleSwipeLeftAction
                    context.dataStore.getOrDefault(ArticleSwipeLeftActionPreference),
                    context,
                    navController,
                    articleWithEnclosure,
                )
            }
            false
        },
        positionalThreshold = { it * 0.15f },
    )
    var isSwipeToDismissActive by remember(data) { mutableStateOf(false) }

    LaunchedEffect(swipeToDismissBoxState.progress > 0.15f) {
        isSwipeToDismissActive = swipeToDismissBoxState.progress > 0.15f &&
                swipeToDismissBoxState.targetValue != SwipeToDismissBoxValue.Settled
    }

    SwipeToDismissBox(
        state = swipeToDismissBoxState,
        backgroundContent = {
            SwipeBackgroundContent(
                direction = swipeToDismissBoxState.dismissDirection,
                isActive = isSwipeToDismissActive,
                articleSwipeLeftAction = articleSwipeLeftAction,
                context = context,
            )
        },
        enableDismissFromStartToEnd = false,
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxWidth()
                .run { if (article.image.isNullOrBlank()) this else height(IntrinsicSize.Max) }
                .combinedClickable(
                    onLongClick = { expandMenu = true },
                    onClick = {
                        tapAction(
                            articleTapAction,
                            context,
                            navController,
                            articleWithEnclosure,
                        )
                    },
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            val colorAlpha = if (data.articleWithEnclosure.article.isRead) 0.5f else 1f
            CompositionLocalProvider(
                LocalContentColor provides LocalContentColor.current.copy(alpha = colorAlpha)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FeedIcon(data = data.feed)
                    val feedName =
                        data.feed.nickname.orEmpty().ifBlank { data.feed.title.orEmpty() }
                    if (feedName.isNotBlank()) {
                        Text(
                            modifier = Modifier
                                .padding(horizontal = 10.dp)
                                .weight(1f),
                            text = feedName,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = colorAlpha),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    val date = article.date?.toDateTimeString(context = context)
                    if (!date.isNullOrBlank()) {
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
                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        val title = article.title?.readable().orEmpty()
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val author = article.author
                        if (!author.isNullOrBlank()) {
                            Text(
                                text = author,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        val description = article.description?.readable()?.let { desc ->
                            if (LocalDeduplicateTitleInDesc.current) desc.replace(
                                title,
                                ""
                            ) else desc
                        }
                        if (!description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (!article.image.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(15.dp))
                        OutlinedCard(
                            modifier = Modifier
                                .width(90.dp)
                                .fillMaxHeight(),
                        ) {
                            AniVuImage(
                                modifier = Modifier.fillMaxSize(),
                                model = article.image,
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }
            }

            ArticleMenu(
                expanded = expandMenu,
                onDismissRequest = { expandMenu = false },
                data = data,
                onFavorite = onFavorite,
                onRead = onRead,
            )
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
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val articleWithEnclosure = data.articleWithEnclosure
    val isFavorite = articleWithEnclosure.article.isFavorite
    val isRead = articleWithEnclosure.article.isRead

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
                    painter = painterResource(id = R.drawable.ic_home_storage_24),
                    contentDescription = null,
                )
            },
            onClick = {
                showEnclosureBottomSheet(
                    context = context,
                    data = articleWithEnclosure
                )
                onDismissRequest()
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
                text = (data.nickname.orEmpty().ifBlank { data.title }?.firstOrNull() ?: "")
                    .toString(),
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
        AniVuImage(
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            model = icon,
            imageLoader = rememberAniVuImageLoader(listener = object : EventListener {
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
    direction: SwipeToDismissBoxValue,
    isActive: Boolean,
    articleSwipeLeftAction: String,
    context: Context,
) {
    val containerColor = MaterialTheme.colorScheme.background
    val containerColorElevated = MaterialTheme.colorScheme.tertiaryContainer
    val backgroundColor = remember(isActive) { Animatable(containerColor) }

    LaunchedEffect(isActive) {
        backgroundColor.animateTo(if (isActive) containerColorElevated else containerColor)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(backgroundColor.value)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        val painter = when (direction) {
            SwipeToDismissBoxValue.EndToStart -> when (articleSwipeLeftAction) {
                ArticleSwipeLeftActionPreference.READ -> {
                    rememberVectorPainter(image = Icons.Outlined.ImportContacts)
                }

                ArticleSwipeLeftActionPreference.SHOW_ENCLOSURES -> {
                    painterResource(id = R.drawable.ic_home_storage_24)
                }

                else -> rememberVectorPainter(image = Icons.Outlined.ImportContacts)
            }

            SwipeToDismissBoxValue.StartToEnd -> null
            SwipeToDismissBoxValue.Settled -> null
        }
        val contentDescription = when (direction) {
            SwipeToDismissBoxValue.EndToStart -> ArticleSwipeLeftActionPreference
                .toDisplayName(context, articleSwipeLeftAction)

            SwipeToDismissBoxValue.StartToEnd -> null
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

private fun swipeLeftAction(
    articleSwipeLeftAction: String,
    context: Context,
    navController: NavController,
    data: ArticleWithEnclosureBean,
) {
    when (articleSwipeLeftAction) {
        ArticleSwipeLeftActionPreference.READ -> {
            navigateToReadScreen(navController = navController, data = data)
        }

        ArticleSwipeLeftActionPreference.SHOW_ENCLOSURES -> {
            showEnclosureBottomSheet(context = context, data = data)
        }
    }
}

private fun tapAction(
    articleTapAction: String,
    context: Context,
    navController: NavController,
    data: ArticleWithEnclosureBean,
) {
    when (articleTapAction) {
        ArticleTapActionPreference.READ -> {
            navigateToReadScreen(navController = navController, data = data)
        }

        ArticleTapActionPreference.SHOW_ENCLOSURES -> {
            showEnclosureBottomSheet(context = context, data = data)
        }
    }
}

private fun navigateToReadScreen(navController: NavController, data: ArticleWithEnclosureBean) {
    val bundle = Bundle().apply {
        putString(ReadFragment.ARTICLE_ID_KEY, data.article.articleId)
    }
    navController.navigate(R.id.action_to_read_fragment, bundle)
}

private fun showEnclosureBottomSheet(context: Context, data: ArticleWithEnclosureBean) {
    EnclosureBottomSheet().apply {
        show(
            (context.activity as FragmentActivity).supportFragmentManager,
            EnclosureBottomSheet.TAG,
        )
        updateData(ReadFragment.getEnclosuresList(context, data))
    }
}