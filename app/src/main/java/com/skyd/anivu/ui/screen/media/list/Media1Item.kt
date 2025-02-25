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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.skyd.anivu.ui.component.rememberPodAuraImageLoader
import com.skyd.anivu.ui.local.LocalMediaShowThumbnail
import java.util.Locale

@Composable
fun Media1Item(
    data: MediaBean,
    onPlay: (MediaBean) -> Unit,
    onOpenDir: (MediaBean) -> Unit,
    onRemove: (MediaBean) -> Unit,
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .size(50.dp),
            contentAlignment = Alignment.Center,
        ) {
            var showThumbnail by remember(data) { mutableStateOf(true) }
            val fileIcon = @Composable {
                Icon(
                    modifier = Modifier.size(25.dp),
                    painter = painterResource(id = data.icon),
                    contentDescription = null
                )
            }
            val articleWithEnclosure = data.articleWithEnclosure
            val feed = data.feedBean
            val image = articleWithEnclosure?.media?.image ?: feed?.icon ?: data.file.path
            if (image != null && LocalMediaShowThumbnail.current) {
                if (showThumbnail) {
                    PodAuraImage(
                        modifier = Modifier.fillMaxSize(),
                        model = remember(image) {
                            ImageRequest.Builder(context)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .data(image)
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
        Spacer(modifier = Modifier.width(10.dp))
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
                    TagText(text = remember(fileExtension) { fileExtension.uppercase(Locale.getDefault()) })
                    Spacer(modifier = Modifier.width(12.dp))
                } else if (data.isDir) {
                    TagText(text = stringResource(id = R.string.folder))
                }
                if (!data.isDir) {
                    Text(
                        modifier = Modifier
                            .basicMarquee()
                            .widthIn(min = 40.dp)
                            .alignByBaseline(),
                        text = remember(data) { data.size.fileSize(context) },
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .basicMarquee()
                        .widthIn(min = 40.dp)
                        .alignByBaseline(),
                    text = remember(data) { data.date.toDateTimeString(context = context) },
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
            }

            DropdownMenu(
                expanded = expandMenu,
                onDismissRequest = { expandMenu = false },
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
                        expandMenu = false
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
                        expandMenu = false
                    },
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.remove)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        onRemove(data)
                        expandMenu = false
                    },
                )
            }
        }
    }
}

@Composable
private fun TagText(text: String) {
    Text(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 4.dp),
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontSize = 10.sp,
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