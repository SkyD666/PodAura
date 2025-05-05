package com.skyd.podaura.ui.screen.article.enclosure

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skyd.podaura.ext.activity
import com.skyd.podaura.ext.fileSize
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.ext.setText
import com.skyd.podaura.model.bean.LinkEnclosureBean
import com.skyd.podaura.model.bean.article.ArticleWithEnclosureBean
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.model.bean.article.EnclosureBean
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.preference.rss.ParseLinkTagAsEnclosurePreference
import com.skyd.podaura.model.repository.download.DownloadStarter
import com.skyd.podaura.ui.activity.player.PlayActivity
import com.skyd.podaura.ui.component.PodAuraIconButton
import com.skyd.podaura.ui.component.TagText
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.bottom_sheet_enclosure_title
import podaura.shared.generated.resources.download
import podaura.shared.generated.resources.enclosure_item_link_tag
import podaura.shared.generated.resources.play

fun getEnclosuresList(articleWithEnclosureBean: ArticleWithEnclosureBean): List<Any> {
    val dataList: MutableList<Any> = articleWithEnclosureBean.enclosures.toMutableList()
    if (dataStore.getOrDefault(ParseLinkTagAsEnclosurePreference)) {
        articleWithEnclosureBean.article.link?.let { link ->
            dataList += LinkEnclosureBean(link = link)
        }
    }
    return dataList
}

@Composable
fun EnclosureBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
    dataList: List<Any>,
    article: ArticleWithFeed,
) {
    val context = LocalContext.current
    val onDownload: (Any) -> Unit = remember {
        {
            val url = when (it) {
                is EnclosureBean -> it.url
                is LinkEnclosureBean -> it.link
                else -> null
            }
            if (!url.isNullOrBlank()) {
                DownloadStarter.download(
                    context = context,
                    url = url,
                    type = (it as? EnclosureBean)?.type,
                )
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.bottom_sheet_enclosure_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyColumn {
                itemsIndexed(dataList) { index, item ->
                    if (index > 0) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    if (item is EnclosureBean) {
                        EnclosureItem(
                            enclosure = item,
                            article = article,
                            onDownload = onDownload,
                        )
                    } else if (item is LinkEnclosureBean) {
                        LinkEnclosureItem(
                            enclosure = item,
                            article = article,
                            onDownload = onDownload,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnclosureItem(
    enclosure: EnclosureBean,
    article: ArticleWithFeed,
    onDownload: (EnclosureBean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val articleWithEnclosure = article.articleWithEnclosure

    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            val clipboard = LocalClipboard.current
            Text(
                modifier = Modifier.clickable { scope.launch { clipboard.setText(enclosure.url) } },
                text = enclosure.url,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
            )
            Row(modifier = Modifier.padding(top = 6.dp)) {
                Text(
                    text = enclosure.length.fileSize(context),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
                enclosure.type.takeIf { !it.isNullOrBlank() }?.let { enclosureType ->
                    Text(
                        modifier = Modifier.padding(start = 16.dp),
                        text = enclosureType,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        if (enclosure.isMedia) {
            PodAuraIconButton(
                onClick = {
                    try {
                        PlayActivity.playArticleList(
                            context.activity,
                            articleId = articleWithEnclosure.article.articleId,
                            url = enclosure.url
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = stringResource(Res.string.play),
            )
        }
        PodAuraIconButton(
            onClick = { onDownload(enclosure) },
            imageVector = Icons.Outlined.Download,
            contentDescription = stringResource(Res.string.download),
        )
    }
}

@Composable
private fun LinkEnclosureItem(
    enclosure: LinkEnclosureBean,
    article: ArticleWithFeed,
    onDownload: (LinkEnclosureBean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val articleWithEnclosure = article.articleWithEnclosure
    val isMagnetOrTorrent = rememberSaveable {
        enclosure.link.startsWith("magnet:") ||
                Regex("^(http|https)://.*\\.torrent$").matches(enclosure.link)
    }
    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            val clipboard = LocalClipboard.current
            Text(
                modifier = Modifier.clickable { scope.launch { clipboard.setText(enclosure.link) } },
                text = enclosure.link,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 5,
            )
            Spacer(modifier = Modifier.height(4.dp))
            TagText(text = stringResource(Res.string.enclosure_item_link_tag), fontSize = 10.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        if (enclosure.isMedia) {
            PodAuraIconButton(
                onClick = {
                    try {
                        PlayActivity.playArticleList(
                            context.activity,
                            articleId = articleWithEnclosure.article.articleId,
                            url = enclosure.link,
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = stringResource(Res.string.play),
            )
        }
        if (isMagnetOrTorrent) {
            PodAuraIconButton(
                onClick = { onDownload(enclosure) },
                imageVector = Icons.Outlined.Download,
                contentDescription = stringResource(Res.string.download),
            )
        }
    }
}