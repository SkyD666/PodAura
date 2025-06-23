package com.skyd.podaura.ui.screen.media.list.item

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.EventListener
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.skyd.compone.component.TagText
import com.skyd.compone.component.dialog.DeleteWarningDialog
import com.skyd.compone.component.menu.DropdownMenuDeleteItem
import com.skyd.podaura.ext.fileSize
import com.skyd.podaura.ext.openWith
import com.skyd.podaura.ext.toDateTimeString
import com.skyd.podaura.ext.toUri
import com.skyd.podaura.model.bean.MediaBean
import com.skyd.podaura.model.preference.appearance.media.MediaShowThumbnailPreference
import com.skyd.podaura.ui.component.PodAuraImage
import com.skyd.podaura.ui.component.rememberPodAuraImageLoader
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.rememberShareFileLauncher
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
import java.io.File
import java.util.Locale

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
    val context = LocalContext.current
    var expandMenu by rememberSaveable { mutableStateOf(false) }

    remember(
        context,
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

            override fun Modifier.itemClickable(): Modifier = combinedClickable(
                onLongClick = {
                    if (onLongClick == null) {
                        expandMenu = true
                    } else {
                        expandMenu = false
                        onLongClick(data)
                    }
                },
                onClick = {
                    if (data.isDir) {
                        onOpenDir(data)
                    } else if (data.isMedia) {
                        onPlay(data)
                    } else {
                        File(data.filePath.toString())
                            .toUri(context)
                            .openWith(context)
                    }
                },
            )
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
            text = remember(fileExtension) { fileExtension.uppercase(Locale.getDefault()) },
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
    val context = LocalContext.current
    Text(
        modifier = modifier,
        text = remember(data) { data.size.fileSize(context) },
        style = style,
        maxLines = 1,
    )
}

@Composable
fun MediaItemScope.GridFileSize(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    TagText(
        modifier = modifier,
        text = remember(data) { data.size.fileSize(context) },
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
    val context = LocalContext.current
    var openDeleteWarningDialog by rememberSaveable { mutableStateOf(false) }
    val shareLauncher = rememberShareFileLauncher()

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        DropdownMenuItem(
            text = { Text(text = stringResource(Res.string.open_with)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = null,
                )
            },
            onClick = {
                File(data.filePath.toString())
                    .toUri(context)
                    .openWith(context)
                onDismissRequest()
            },
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(Res.string.share)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = null,
                )
            },
            onClick = {
                shareLauncher.launch(PlatformFile(data.path))
                onDismissRequest()
            },
        )
        if (onOpenAddToPlaylistSheet != null && data.isMedia) {
            DropdownMenuItem(
                text = { Text(text = stringResource(Res.string.add_to_playlist)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.PlaylistAdd,
                        contentDescription = null,
                    )
                },
                onClick = {
                    onOpenAddToPlaylistSheet(data)
                    onDismissRequest()
                },
            )
        }
        if (onOpenFeed != null) {
            DropdownMenuItem(
                text = { Text(text = stringResource(Res.string.feed_screen_name)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.RssFeed,
                        contentDescription = null,
                    )
                },
                onClick = {
                    onOpenFeed(data)
                    onDismissRequest()
                },
            )
        }
        if (onOpenArticle != null) {
            DropdownMenuItem(
                text = { Text(text = stringResource(Res.string.read_screen_name)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Article,
                        contentDescription = null,
                    )
                },
                onClick = {
                    onOpenArticle(data)
                    onDismissRequest()
                },
            )
        }
        HorizontalDivider()
        DropdownMenuDeleteItem(
            onClick = {
                openDeleteWarningDialog = true
                onDismissRequest()
            }
        )
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
    val context = LocalContext.current
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

    if (data.cover != null && MediaShowThumbnailPreference.current) {
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