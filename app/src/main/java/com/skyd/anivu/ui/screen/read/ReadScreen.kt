package com.skyd.anivu.ui.screen.read

import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import coil3.EventListener
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.video.VideoFrameDecoder
import com.skyd.anivu.R
import com.skyd.anivu.base.mvi.MviEventListener
import com.skyd.anivu.base.mvi.getDispatcher
import com.skyd.anivu.ext.activity
import com.skyd.anivu.ext.copy
import com.skyd.anivu.ext.ifNullOfBlank
import com.skyd.anivu.ext.isWifi
import com.skyd.anivu.ext.openBrowser
import com.skyd.anivu.ext.toDateTimeString
import com.skyd.anivu.ext.toEncodedUrl
import com.skyd.anivu.model.bean.article.ArticleBean
import com.skyd.anivu.model.bean.article.ArticleWithFeed
import com.skyd.anivu.model.bean.article.EnclosureBean
import com.skyd.anivu.model.bean.article.RssMediaBean
import com.skyd.anivu.model.preference.appearance.read.ReadTextSizePreference
import com.skyd.anivu.ui.activity.player.PlayActivity
import com.skyd.anivu.ui.component.PodAuraFloatingActionButton
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.anivu.ui.component.PodAuraImage
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.component.dialog.WaitingDialog
import com.skyd.anivu.ui.component.html.HtmlText
import com.skyd.anivu.ui.component.rememberPodAuraImageLoader
import com.skyd.anivu.ui.local.LocalReadContentTonalElevation
import com.skyd.anivu.ui.local.LocalReadTextSize
import com.skyd.anivu.ui.local.LocalReadTopBarTonalElevation
import com.skyd.anivu.ui.screen.article.enclosure.EnclosureBottomSheet
import com.skyd.anivu.ui.screen.article.enclosure.getEnclosuresList
import com.skyd.anivu.util.ShareUtil
import java.util.Locale


const val READ_SCREEN_ROUTE = "readScreen"
const val ARTICLE_ID_KEY = "articleId"

fun openReadScreen(
    navController: NavController,
    articleId: String,
    navOptions: NavOptions? = null,
) {
    navController.navigate(
        "${READ_SCREEN_ROUTE}/${articleId.toEncodedUrl(allow = null)}",
        navOptions = navOptions,
    )
}

@Composable
fun ReadScreen(articleId: String, viewModel: ReadViewModel = hiltViewModel()) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    var openMoreMenu by rememberSaveable { mutableStateOf(false) }
    var openEnclosureBottomSheet by rememberSaveable { mutableStateOf<List<Any>?>(null) }
    var openReadTextSizeSliderDialog by rememberSaveable { mutableStateOf(false) }

    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    val dispatcher = viewModel.getDispatcher(startWith = ReadIntent.Init(articleId))

    var fabHeight by remember { mutableStateOf(0.dp) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Small,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(R.string.read_screen_name)) },
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                        LocalReadTopBarTonalElevation.current.dp
                    ),
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                        LocalReadTopBarTonalElevation.current.dp + 4.dp
                    ),
                ),
                actions = {
                    PodAuraIconButton(
                        enabled = uiState.articleState is ArticleState.Success,
                        onClick = {
                            val articleState = uiState.articleState
                            if (articleState is ArticleState.Success) {
                                val article = articleState.article.articleWithEnclosure.article
                                val link = article.link
                                val title = article.title
                                if (!link.isNullOrBlank()) {
                                    ShareUtil.shareText(
                                        context = context,
                                        text = if (title.isNullOrBlank()) link else "[$title] $link",
                                    )
                                }
                            }
                        },
                        imageVector = Icons.Outlined.Share,
                        contentDescription = stringResource(R.string.share),
                    )
                    val isFavorite = (uiState.articleState as? ArticleState.Success)
                        ?.article?.articleWithEnclosure?.article?.isFavorite == true
                    PodAuraIconButton(
                        enabled = uiState.articleState is ArticleState.Success,
                        onClick = {
                            val articleState = uiState.articleState
                            if (articleState is ArticleState.Success) {
                                dispatcher(
                                    ReadIntent.Favorite(
                                        articleId = articleId,
                                        favorite = !isFavorite,
                                    )
                                )
                            }
                        },
                        imageVector = if (isFavorite) Icons.Outlined.Favorite
                        else Icons.Outlined.FavoriteBorder,
                        contentDescription = stringResource(
                            if (isFavorite) R.string.article_screen_unfavorite
                            else R.string.article_screen_favorite
                        ),
                    )
                    PodAuraIconButton(
                        enabled = uiState.articleState is ArticleState.Success,
                        onClick = { openMoreMenu = true },
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.more),
                    )
                    MoreMenu(
                        expanded = openMoreMenu,
                        onDismissRequest = { openMoreMenu = false },
                        onOpenInBrowserClick = {
                            val articleState = uiState.articleState
                            if (articleState is ArticleState.Success) {
                                val link = articleState.article.articleWithEnclosure.article.link
                                if (!link.isNullOrBlank()) {
                                    link.openBrowser(context)
                                }
                            }
                        },
                        onReadTextSizeClick = { openReadTextSizeSliderDialog = true },
                    )
                }
            )
        },
        floatingActionButton = {
            PodAuraFloatingActionButton(
                onSizeWithSinglePaddingChanged = { _, height -> fabHeight = height },
                onClick = {
                    val articleState = uiState.articleState
                    if (articleState is ArticleState.Success) {
                        openEnclosureBottomSheet = getEnclosuresList(
                            context, articleState.article.articleWithEnclosure,
                        )
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.AttachFile,
                    contentDescription = stringResource(R.string.bottom_sheet_enclosure_title),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
            LocalAbsoluteTonalElevation.current +
                    LocalReadContentTonalElevation.current.dp
        ),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(bottom = fabHeight),
        ) {
            when (val articleState = uiState.articleState) {
                is ArticleState.Failed -> {
                    Text(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(20.dp),
                        text = articleState.msg,
                    )
                }

                ArticleState.Init,
                ArticleState.Loading -> Unit

                is ArticleState.Success -> {
                    Content(
                        articleState = articleState,
                        shareImage = { dispatcher(ReadIntent.ShareImage(url = it)) },
                        copyImage = { dispatcher(ReadIntent.CopyImage(url = it)) },
                        downloadImage = {
                            dispatcher(
                                ReadIntent.DownloadImage(
                                    url = it,
                                    title = articleState.article.articleWithEnclosure.article.title,
                                )
                            )
                        },
                    )
                    if (openEnclosureBottomSheet != null) {
                        EnclosureBottomSheet(
                            onDismissRequest = { openEnclosureBottomSheet = null },
                            dataList = openEnclosureBottomSheet.orEmpty(),
                            article = articleState.article.articleWithEnclosure,
                        )
                    }
                }
            }
        }

        MviEventListener(viewModel.singleEvent) { event ->
            when (event) {
                is ReadEvent.FavoriteArticleResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is ReadEvent.ReadArticleResultEvent.Failed -> snackbarHostState.showSnackbar(event.msg)
                is ReadEvent.ShareImageResultEvent.Failed -> snackbarHostState.showSnackbar(event.msg)
                is ReadEvent.CopyImageResultEvent.Failed -> snackbarHostState.showSnackbar(event.msg)
                is ReadEvent.DownloadImageResultEvent.Failed -> snackbarHostState.showSnackbar(event.msg)
                is ReadEvent.CopyImageResultEvent.Success,
                is ReadEvent.DownloadImageResultEvent.Success -> Unit
            }
        }

        WaitingDialog(visible = uiState.loadingDialog)

        if (openReadTextSizeSliderDialog) {
            ReadTextSizeSliderDialog(
                onDismissRequest = { openReadTextSizeSliderDialog = false },
            )
        }
    }
}

@Composable
private fun CategoryArea(categories: ArticleBean.Categories) {
    val context = LocalContext.current
    FlowRow(
        modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        categories.categories.forEach { category ->
            SuggestionChip(
                onClick = { category.copy(context) },
                label = { Text(text = category) },
            )
        }
    }
}

@Composable
private fun Content(
    articleState: ArticleState.Success,
    downloadImage: (url: String) -> Unit,
    copyImage: (url: String) -> Unit,
    shareImage: (url: String) -> Unit,
) {
    val context = LocalContext.current
    val article = articleState.article.articleWithEnclosure
    var openImageSheet by rememberSaveable { mutableStateOf<String?>(null) }

    SelectionContainer {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            var expandTitle by rememberSaveable { mutableStateOf(false) }
            article.article.title?.let { title ->
                Text(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .animateContentSize()
                        .clickable { expandTitle = !expandTitle },
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = if (expandTitle) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            val date = article.article.date
            val author = article.article.author
            if (date != null || !author.isNullOrBlank()) {
                Row(modifier = Modifier.padding(vertical = 10.dp)) {
                    if (date != null) {
                        Text(
                            text = date.toDateTimeString(context = context),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (date != null && !author.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    if (!author.isNullOrBlank()) {
                        Text(
                            text = author,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
    MediaRow(articleWithFeed = articleState.article, onPlay = { url ->
        PlayActivity.play(
            activity = context.activity,
            uri = Uri.parse(url),
            title = article.article.title,
            thumbnail = article.media?.image ?: articleState.article.feed.icon,
        )
    })
    HtmlText(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = article.article.content.ifNullOfBlank {
            article.article.description.orEmpty()
        },
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = LocalReadTextSize.current.sp,
        onImageClick = { imageUrl -> openImageSheet = imageUrl }
    )
    article.article.categories?.let { categories ->
        CategoryArea(categories)
    }

    if (openImageSheet != null) {
        ImageBottomSheet(
            imageUrl = openImageSheet!!,
            onDismissRequest = { openImageSheet = null },
            shareImage = shareImage,
            copyImage = copyImage,
            downloadImage = downloadImage,
        )
    }
}

@Composable
private fun MoreMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onOpenInBrowserClick: () -> Unit,
    onReadTextSizeClick: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.read_screen_open_browser)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Public,
                    contentDescription = null,
                )
            },
            onClick = {
                onDismissRequest()
                onOpenInBrowserClick()
            },
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.read_screen_text_size)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.FormatSize,
                    contentDescription = null,
                )
            },
            onClick = {
                onDismissRequest()
                onReadTextSizeClick()
            },
        )
    }
}

@Composable
private fun ReadTextSizeSliderDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = modifier.padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val textSize = LocalReadTextSize.current
            Text(
                modifier = Modifier.padding(start = 16.dp),
                text = String.format(Locale.getDefault(), "%.2f Sp", textSize),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Slider(
                modifier = Modifier.padding(horizontal = 16.dp),
                valueRange = 12f..50f,
                value = textSize,
                onValueChange = {
                    ReadTextSizePreference.put(context = context, scope = scope, value = it)
                },
            )
        }
    }
}

@Composable
private fun RssMediaEpisode(modifier: Modifier = Modifier, rssMedia: RssMediaBean) {
    val episode = rssMedia.episode
    if (episode != null) {
        Text(
            modifier = modifier,
            text = stringResource(id = R.string.read_screen_episode, episode),
            color = Color.White,
        )
    }
}

@Composable
private fun RssMediaDuration(modifier: Modifier = Modifier, rssMedia: RssMediaBean) {
    val duration = rssMedia.duration
    if (duration != null) {
        Text(
            modifier = modifier,
            text = DateUtils.formatElapsedTime(duration / 1000),
            color = Color.White,
        )
    }
}

@Composable
private fun MediaRow(articleWithFeed: ArticleWithFeed, onPlay: (String) -> Unit) {
    val articleWithEnclosure = articleWithFeed.articleWithEnclosure
    val enclosures = articleWithEnclosure.enclosures.filter { it.isMedia }
    val cover = articleWithEnclosure.media?.image ?: articleWithFeed.feed.icon
    if (enclosures.size > 1) {
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(enclosures) { item ->
                MediaCover(
                    modifier = Modifier
                        .height(160.dp)
                        .widthIn(min = 200.dp),
                    cover = cover,
                    enclosure = item,
                    onClick = { onPlay(item.url) },
                )
            }
        }
        articleWithEnclosure.media?.let { media ->
            Spacer(modifier = Modifier.height(12.dp))
            Row {
                RssMediaEpisode(rssMedia = media)
                Spacer(modifier = Modifier.width(12.dp))
                RssMediaDuration(rssMedia = media)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    } else if (enclosures.size == 1) {
        val item = enclosures.first()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 16.dp)
                .padding(horizontal = 16.dp)
        ) {
            MediaCover(
                modifier = Modifier
                    .height(200.dp)
                    .widthIn(max = 350.dp)
                    .align(Alignment.Center),
                cover = cover,
                enclosure = item,
                onClick = { onPlay(item.url) },
            ) {
                articleWithEnclosure.media?.let { media ->
                    RssMediaEpisode(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 15.dp, top = 10.dp),
                        rssMedia = media,
                    )
                    RssMediaDuration(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 15.dp, bottom = 10.dp),
                        rssMedia = media,
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaCover(
    modifier: Modifier = Modifier,
    cover: String?,
    enclosure: EnclosureBean,
    onClick: () -> Unit,
    content: @Composable (BoxScope.() -> Unit) = {},
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.5f)),
    ) {
        Box(
            modifier = Modifier
                .clickable(onClick = onClick)
                .align(Alignment.Center),
            contentAlignment = Alignment.Center,
        ) {
            var realImage by rememberSaveable(enclosure) {
                mutableStateOf(if (context.isWifi() && enclosure.isVideo) enclosure.url else cover)
            }
            PodAuraImage(
                modifier = Modifier.fillMaxHeight(),
                imageLoader = rememberPodAuraImageLoader(
                    listener = object : EventListener() {
                        override fun onError(request: ImageRequest, result: ErrorResult) {
                            if (cover != null && realImage != cover) {
                                realImage = cover
                            }
                        }
                    },
                    components = { add(VideoFrameDecoder.Factory()) },
                ),
                model = realImage,
                contentScale = ContentScale.FillHeight,
                colorFilter = ColorFilter.tint(
                    Color.Black.copy(alpha = 0.5f),
                    blendMode = BlendMode.Darken,
                ),
            )
            Icon(
                modifier = Modifier.size(50.dp),
                imageVector = Icons.Outlined.PlayCircleOutline,
                contentDescription = stringResource(id = R.string.play),
                tint = Color.White,
            )
            content()
        }
    }
}

@Composable
private fun ImageBottomSheet(
    imageUrl: String,
    onDismissRequest: () -> Unit,
    shareImage: (url: String) -> Unit,
    copyImage: (url: String) -> Unit,
    downloadImage: (url: String) -> Unit,
) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
        ) {
            ImageBottomSheetItem(
                icon = Icons.Outlined.Download,
                title = stringResource(id = R.string.read_screen_download_image),
                onClick = {
                    downloadImage(imageUrl)
                    onDismissRequest()
                }
            )
            ImageBottomSheetItem(
                icon = Icons.Outlined.Share,
                title = stringResource(id = R.string.share),
                onClick = {
                    shareImage(imageUrl)
                    onDismissRequest()
                }
            )
            ImageBottomSheetItem(
                icon = Icons.Outlined.ContentCopy,
                title = stringResource(id = android.R.string.copy),
                onClick = {
                    copyImage(imageUrl)
                    onDismissRequest()
                }
            )
            ImageBottomSheetItem(
                icon = Icons.Outlined.Public,
                title = stringResource(id = R.string.read_screen_open_image_in_browser),
                onClick = {
                    imageUrl.openBrowser(context)
                    onDismissRequest()
                }
            )
        }
    }
}

@Composable
private fun ImageBottomSheetItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(modifier = Modifier.width(20.dp))
        Text(text = title)
    }
}