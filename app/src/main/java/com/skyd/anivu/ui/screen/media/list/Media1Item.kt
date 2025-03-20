package com.skyd.anivu.ui.screen.media.list

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.EventListener
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.skyd.anivu.R
import com.skyd.anivu.ext.fileSize
import com.skyd.anivu.ext.openWith
import com.skyd.anivu.ext.share
import com.skyd.anivu.ext.toDateTimeString
import com.skyd.anivu.ext.toUri
import com.skyd.anivu.model.bean.MediaBean
import com.skyd.anivu.ui.component.PodAuraImage
import com.skyd.anivu.ui.component.TagText
import com.skyd.anivu.ui.component.dialog.DeleteWarningDialog
import com.skyd.anivu.ui.component.menu.DropdownMenuDeleteItem
import com.skyd.anivu.ui.component.rememberPodAuraImageLoader
import com.skyd.anivu.ui.local.LocalMediaShowThumbnail
import java.util.Locale

@Composable
fun Media1Item(
    data: MediaBean,
    onPlay: (MediaBean) -> Unit,
    onOpenDir: (MediaBean) -> Unit,
    onRemove: (MediaBean) -> Unit,
    onOpenFeed: ((MediaBean) -> Unit)?,
    onOpenArticle: ((MediaBean) -> Unit)?,
    onLongClick: ((MediaBean) -> Unit)? = null,
) {
    val context = LocalContext.current
    var expandMenu by rememberSaveable { mutableStateOf(false) }

    val fileNameWithoutExtension = rememberSaveable(data) {
        if (data.isDir) data.name else data.name.substringBeforeLast(".")
    }
    val fileExtension = rememberSaveable(data) {
        if (data.isDir) "" else data.name.substringAfterLast(".", "")
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondary.copy(0.1f))
            .combinedClickable(
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
                        data.file
                            .toUri(context)
                            .openWith(context)
                    }
                },
            )
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MediaCover(
            data = data,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .size(50.dp),
        )
        Spacer(modifier = Modifier.width(11.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (data.displayName.isNullOrBlank()) {
                        fileNameWithoutExtension
                    } else {
                        data.displayName
                    },
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    style = MaterialTheme.typography.titleSmall,
                )
                MediaFolderNumberBadge(mediaBean = data)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (fileExtension.isNotBlank()) {
                    TagText(
                        text = remember(fileExtension) { fileExtension.uppercase(Locale.getDefault()) },
                        fontSize = 10.sp,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                } else if (data.isDir) {
                    TagText(text = stringResource(id = R.string.folder), fontSize = 10.sp)
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .basicMarquee(),
                ) {
                    if (!data.isDir) {
                        Text(
                            modifier = Modifier.alignByBaseline(),
                            text = remember(data) { data.size.fileSize(context) },
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .alignByBaseline(),
                        text = remember(data) { data.date.toDateTimeString(context = context) },
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }
            }
            Menu(
                expanded = expandMenu,
                onDismissRequest = { expandMenu = false },
                data = data,
                onRemove = onRemove,
                onOpenFeed = onOpenFeed,
                onOpenArticle = onOpenArticle,
            )
        }
    }
}

@Composable
private fun Menu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    data: MediaBean,
    onRemove: (MediaBean) -> Unit,
    onOpenFeed: ((MediaBean) -> Unit)?,
    onOpenArticle: ((MediaBean) -> Unit)?,
) {
    val context = LocalContext.current
    var openDeleteWarningDialog by rememberSaveable { mutableStateOf(false) }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        DropdownMenuItem(
            text = { Text(text = stringResource(id = R.string.open_with)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = null,
                )
            },
            onClick = {
                data.file
                    .toUri(context)
                    .openWith(context)
                onDismissRequest()
            },
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(id = R.string.share)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = null,
                )
            },
            onClick = {
                data.file.toUri(context).share(context)
                onDismissRequest()
            },
        )
        if (onOpenFeed != null) {
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.feed_screen_name)) },
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
                text = { Text(text = stringResource(id = R.string.read_screen_name)) },
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
            if (data.isFile) R.string.media_screen_delete_file_warning
            else R.string.media_screen_delete_directory_warning
        ),
        onDismissRequest = { openDeleteWarningDialog = false },
        onDismiss = { openDeleteWarningDialog = false },
        onConfirm = { onRemove(data) },
    )
}

@Composable
private fun MediaFolderNumberBadge(mediaBean: MediaBean) {
    if (mediaBean.fileCount > 0) {
        Row {
            Spacer(modifier = Modifier.width(6.dp))
            Row(modifier = Modifier.clip(RoundedCornerShape(20.dp))) {
                Text(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = 3.dp),
                    text = mediaBean.fileCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
fun MediaCover(data: MediaBean, modifier: Modifier = Modifier, iconSize: Dp = 25.dp) {
    val context = LocalContext.current
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        var showThumbnail by remember(data) { mutableStateOf(true) }
        val fileIcon = @Composable {
            Icon(
                modifier = Modifier.size(iconSize),
                painter = painterResource(id = data.icon),
                contentDescription = null
            )
        }
        if (data.cover != null && LocalMediaShowThumbnail.current) {
            if (showThumbnail) {
                PodAuraImage(
                    modifier = Modifier.fillMaxSize(),
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
                fileIcon()
            }
        } else {
            fileIcon()
        }
    }
}