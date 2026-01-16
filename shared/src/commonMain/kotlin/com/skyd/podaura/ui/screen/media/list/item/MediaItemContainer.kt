package com.skyd.podaura.ui.screen.media.list.item

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.EventListener
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.skyd.compone.component.TagText
import com.skyd.compone.component.dialog.DeleteWarningDialog
import com.skyd.compone.component.menu.DropdownMenuDeleteItem
import com.skyd.fundation.util.isPhone
import com.skyd.fundation.util.platform
import com.skyd.podaura.ext.fileSize
import com.skyd.podaura.ext.onRightClickIfSupported
import com.skyd.podaura.ext.share
import com.skyd.podaura.ext.toDateTimeString
import com.skyd.podaura.model.bean.MediaBean
import com.skyd.podaura.model.preference.appearance.media.MediaShowThumbnailPreference
import com.skyd.podaura.ui.component.PodAuraImage
import com.skyd.podaura.ui.component.rememberPodAuraImageLoader
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.add_to_playlist
import podaura.shared.generated.resources.feed_screen_name
import podaura.shared.generated.resources.folder
import podaura.shared.generated.resources.media_screen_delete_directory_warning
import podaura.shared.generated.resources.media_screen_delete_file_warning
import podaura.shared.generated.resources.open_with
import podaura.shared.generated.resources.read_screen_name
import podaura.shared.generated.resources.share

@Composable
fun MediaItemContainer(
    data: MediaBean,
    onPlay: (MediaBean) -> Unit,
    onOpenDir: (MediaBean) -> Unit,
    onRemove: (MediaBean) -> Unit,
    onOpenFeed: ((MediaBean) -> Unit)?,
    onOpenArticle: ((MediaBean) -> Unit)?,
    onOpenAddToPlaylistSheet: ((MediaBean) -> Unit)?,
    onLongClick: ((MediaBean) -> Unit)? = null,
    content: @Composable MediaItemScope.() -> Unit,
) {
    var expandMenu by rememberSaveable { mutableStateOf(false) }

    remember(
        data,
        onPlay,
        onOpenDir,
        onRemove,
        onOpenFeed,
        onOpenArticle,
        onOpenAddToPlaylistSheet,
        onLongClick,
    ) {
        val fileNameWithoutExtension =
            if (data.isDir) data.name else data.name.substringBeforeLast(".")
        val fileExtension = if (data.isDir) "" else data.name.substringAfterLast(".", "")

        object : MediaItemScopeDefaultImpl() {
            override val data = data
            override val onPlay = onPlay
            override val onOpenDir = onOpenDir
            override val onRemove = onRemove
            override val onOpenFeed = onOpenFeed
            override val onOpenArticle = onOpenArticle
            override val onOpenAddToPlaylistSheet = onOpenAddToPlaylistSheet
            override val onLongClick = onLongClick
            override val fileNameWithoutExtension: String = fileNameWithoutExtension
            override val fileExtension: String = fileExtension
            override var expandMenu: Boolean
                get() = expandMenu
                set(value) {
                    expandMenu = value
                }

            override fun Modifier.itemClickable(): Modifier {
                val onShowMenu = {
                    if (onLongClick == null) {
                        expandMenu = true
                    } else {
                        expandMenu = false
                        onLongClick(data)
                    }
                }
                return combinedClickable(
                    onLongClick = onShowMenu,
                    onClick = {
                        if (data.isDir) {
                            onOpenDir(data)
                        } else if (data.isMedia) {
                            onPlay(data)
                        } else {
                            FileKit.openFileWithDefaultApplication(PlatformFile(data.filePath))
                        }
                    },
                ).onRightClickIfSupported(onClick = onShowMenu)
            }
        }
    }.content()
}


@Composable
fun MediaItemScope.Title(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = 3,
    style: TextStyle = MaterialTheme.typography.titleSmall,
) {
    val displayName = data.displayName
    Text(
        text = if (displayName.isNullOrBlank()) fileNameWithoutExtension else displayName,
        modifier = modifier,
        color = color,
        overflow = overflow,
        maxLines = maxLines,
        style = style,
    )
}

@Composable
fun MediaItemScope.Tag(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor: Color = Color.Unspecified,
) {
    if (fileExtension.isNotBlank()) {
        TagText(
            modifier = modifier,
            text = remember(fileExtension) { fileExtension.uppercase() },
            fontSize = 10.sp,
            containerColor = containerColor,
            contentColor = contentColor,
        )
    } else if (data.isDir) {
        TagText(
            modifier = modifier,
            text = stringResource(Res.string.folder),
            fontSize = 10.sp,
            containerColor = containerColor,
            contentColor = contentColor,
        )
    }
}


@Composable
fun MediaItemScope.GridTag(modifier: Modifier = Modifier) {
    Tag(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface.copy(0.65f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
fun MediaItemScope.FileSize(
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelMedium,
) {
    Text(
        modifier = modifier,
        text = remember(data) { data.size.fileSize() },
        style = style,
        maxLines = 1,
    )
}

@Composable
fun MediaItemScope.GridFileSize(modifier: Modifier = Modifier) {
    TagText(
        modifier = modifier,
        text = remember(data) { data.size.fileSize() },
        fontSize = 10.sp,
        containerColor = MaterialTheme.colorScheme.surface.copy(0.65f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
fun MediaItemScope.FolderNumberBadge(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor: Color = MaterialTheme.colorScheme.outline,
) {
    MediaFolderNumberBadge(
        modifier = modifier,
        mediaBean = data,
        containerColor = containerColor,
        contentColor = contentColor,
    )
}

@Composable
fun MediaItemScope.GridFolderNumberBadge(modifier: Modifier = Modifier) {
    MediaFolderNumberBadge(
        modifier = modifier,
        mediaBean = data,
        containerColor = MaterialTheme.colorScheme.surface.copy(0.65f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
fun MediaItemScope.Date(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = remember(data) { data.date.toDateTimeString() },
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
    )
}

@Composable
fun MediaItemScope.Menu() {
    Menu(
        expanded = expandMenu,
        onDismissRequest = { expandMenu = false },
        data = data,
        onRemove = onRemove,
        onOpenFeed = onOpenFeed,
        onOpenArticle = onOpenArticle,
        onOpenAddToPlaylistSheet = onOpenAddToPlaylistSheet,
    )
}

@Composable
private fun Menu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    data: MediaBean,
    onRemove: (MediaBean) -> Unit,
    onOpenFeed: ((MediaBean) -> Unit)?,
    onOpenArticle: ((MediaBean) -> Unit)?,
    onOpenAddToPlaylistSheet: ((MediaBean) -> Unit)?,
) {
    var openDeleteWarningDialog by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        val texts = listOf(
            stringResource(Res.string.open_with),
            stringResource(Res.string.share),
            stringResource(Res.string.add_to_playlist),
            stringResource(Res.string.feed_screen_name),
            stringResource(Res.string.read_screen_name),
        )
        val visible = listOf(
            true,
            platform.isPhone,
            onOpenAddToPlaylistSheet != null && data.isMedia,
            onOpenFeed != null,
            onOpenArticle != null
        )
        val leadingIcons = listOf(
            Icons.AutoMirrored.Outlined.OpenInNew,
            Icons.Outlined.Share,
            Icons.AutoMirrored.Outlined.PlaylistAdd,
            Icons.Outlined.RssFeed,
            Icons.AutoMirrored.Outlined.Article
        )
        val onClicks = listOf(
            {
                FileKit.openFileWithDefaultApplication(PlatformFile(data.filePath))
                onDismissRequest()
            },
            {
                scope.launch { PlatformFile(data.path).share() }
                onDismissRequest()
            },
            {
                onOpenAddToPlaylistSheet?.invoke(data)
                onDismissRequest()
            },
            {
                onOpenFeed?.invoke(data)
                onDismissRequest()
            },
            {
                onOpenArticle?.invoke(data)
                onDismissRequest()
            },
        )
        DropdownMenuGroup(shapes = MenuDefaults.groupShape(0, 2)) {
            var invisibleItemCount = 0
            val visibleCount = visible.count { it }
            texts.forEachIndexed { itemIndex, text ->
                if (visible[itemIndex]) {
                    DropdownMenuItem(
                        text = { Text(text = text) },
                        shape = MenuDefaults.itemShape(
                            itemIndex - invisibleItemCount,
                            visibleCount,
                        ).shape,
                        leadingIcon = {
                            Icon(
                                imageVector = leadingIcons[itemIndex],
                                contentDescription = null,
                            )
                        },
                        onClick = onClicks[itemIndex],
                    )
                } else {
                    invisibleItemCount++
                }
            }
        }
        Spacer(Modifier.height(MenuDefaults.GroupSpacing))
        DropdownMenuGroup(shapes = MenuDefaults.groupShape(1, 2)) {
            DropdownMenuDeleteItem(
                shape = MenuDefaults.itemShape(0, 1).shape,
                onClick = {
                    openDeleteWarningDialog = true
                    onDismissRequest()
                }
            )
        }
    }

    DeleteWarningDialog(
        visible = openDeleteWarningDialog,
        text = stringResource(
            if (data.isFile) Res.string.media_screen_delete_file_warning
            else Res.string.media_screen_delete_directory_warning
        ),
        onDismissRequest = { openDeleteWarningDialog = false },
        onDismiss = { openDeleteWarningDialog = false },
        onConfirm = { onRemove(data) },
    )
}

@Composable
private fun MediaFolderNumberBadge(
    modifier: Modifier,
    mediaBean: MediaBean,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor: Color = MaterialTheme.colorScheme.outline,
) {
    if (mediaBean.fileCount > 0) {
        Row(modifier = modifier.clip(RoundedCornerShape(20.dp))) {
            Text(
                modifier = Modifier
                    .background(containerColor)
                    .padding(horizontal = 3.dp),
                text = mediaBean.fileCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
            )
        }
    }
}

@Composable
fun MediaCover(data: MediaBean, modifier: Modifier = Modifier, iconSize: Dp = 25.dp) {
    val context = LocalPlatformContext.current
    var showThumbnail by remember(data) { mutableStateOf(true) }

    @Composable
    fun FileIcon(modifier: Modifier) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(iconSize),
                painter = painterResource(data.icon),
                contentDescription = null
            )
        }
    }

    if (MediaShowThumbnailPreference.current) {
        if (showThumbnail) {
            PodAuraImage(
                modifier = modifier.fillMaxSize(),
                model = remember(data.cover) {
                    ImageRequest.Builder(context)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .data(data.cover)
                        .crossfade(true)
                        .build()
                },
                imageLoader = rememberPodAuraImageLoader(listener = object :
                    EventListener() {
                    override fun onError(request: ImageRequest, result: ErrorResult) {
                        showThumbnail = false
                    }
                }),
                contentScale = ContentScale.Crop,
            )
        } else {
            FileIcon(modifier)
        }
    } else {
        FileIcon(modifier)
    }
}