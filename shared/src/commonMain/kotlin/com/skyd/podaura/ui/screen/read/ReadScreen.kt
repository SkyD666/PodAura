package com.skyd.podaura.ui.screen.read

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RssFeed
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.skyd.compone.component.ComponeFloatingActionButton
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.component.dialog.WaitingDialog
import com.skyd.compone.ext.setText
import com.skyd.compone.local.LocalNavController
import com.skyd.fundation.ext.format
import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.isPhone
import com.skyd.fundation.util.platform
import com.skyd.mvi.MviEventListener
import com.skyd.mvi.getDispatcher
import com.skyd.podaura.ext.httpDomain
import com.skyd.podaura.ext.ifNullOfBlank
import com.skyd.podaura.ext.safeOpenUri
import com.skyd.podaura.ext.toDateTimeString
import com.skyd.podaura.model.bean.article.ArticleCategoryBean
import com.skyd.podaura.model.preference.appearance.read.ReadContentTonalElevationPreference
import com.skyd.podaura.model.preference.appearance.read.ReadTextSizePreference
import com.skyd.podaura.model.preference.appearance.read.ReadTopBarTonalElevationPreference
import com.skyd.podaura.ui.component.rememberTextSharing
import com.skyd.podaura.ui.component.webview.PodAuraWebView
import com.skyd.podaura.ui.player.jumper.PlayDataMode
import com.skyd.podaura.ui.player.jumper.rememberPlayerJumper
import com.skyd.podaura.ui.screen.article.ArticleRoute
import com.skyd.podaura.ui.screen.article.enclosure.EnclosureBottomSheet
import com.skyd.podaura.ui.screen.article.enclosure.getEnclosuresList
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.article_screen_favorite
import podaura.shared.generated.resources.article_screen_unfavorite
import podaura.shared.generated.resources.bottom_sheet_enclosure_title
import podaura.shared.generated.resources.copy
import podaura.shared.generated.resources.more
import podaura.shared.generated.resources.open_link_in_browser
import podaura.shared.generated.resources.read_screen_download_image
import podaura.shared.generated.resources.read_screen_name
import podaura.shared.generated.resources.read_screen_open_article_screen
import podaura.shared.generated.resources.read_screen_open_image_in_browser
import podaura.shared.generated.resources.read_screen_text_size
import podaura.shared.generated.resources.share


@Serializable
data class ReadRoute(@SerialName("articleId") val articleId: String) {
    fun toDeeplink(): String = "$DEEP_LINK/$articleId"

    companion object {
        private const val DEEP_LINK = "podaura://read.screen"
        const val BASE_PATH = DEEP_LINK

        val deepLinks = listOf(navDeepLink<ReadRoute>(basePath = BASE_PATH))

        @Composable
        fun ReadLauncher(entry: NavBackStackEntry) {
            ReadScreen(articleId = entry.toRoute<ReadRoute>().articleId)
        }
    }
}

@Composable
fun ReadScreen(articleId: String, viewModel: ReadViewModel = koinViewModel()) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val navController = LocalNavController.current
    val uriHandler = LocalUriHandler.current

    val snackbarHostState = remember { SnackbarHostState() }
    var openMoreMenu by rememberSaveable { mutableStateOf(false) }
    var openEnclosureBottomSheet by rememberSaveable { mutableStateOf(false) }
    var openReadTextSizeSliderDialog by rememberSaveable { mutableStateOf(false) }

    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    val dispatcher = viewModel.getDispatcher(startWith = ReadIntent.Init(articleId))

    var fabHeight by remember { mutableStateOf(0.dp) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.Small,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(Res.string.read_screen_name)) },
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                        ReadTopBarTonalElevationPreference.current.dp
                    ),
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                        ReadTopBarTonalElevationPreference.current.dp + 4.dp
                    ),
                ),
                actions = {
                    if (platform.isPhone) {
                        val textSharing = rememberTextSharing()
                        ComponeIconButton(
                            enabled = uiState.articleState is ArticleState.Success,
                            onClick = {
                                val articleState = uiState.articleState
                                if (articleState is ArticleState.Success) {
                                    val article = articleState.article.articleWithEnclosure.article
                                    val link = article.link
                                    val title = article.title
                                    if (!link.isNullOrBlank()) {
                                        textSharing.share(if (title.isNullOrBlank()) link else "[$title] $link")
                                    }
                                }
                            },
                            imageVector = Icons.Outlined.Share,
                            contentDescription = stringResource(Res.string.share),
                        )
                    }
                    val isFavorite = (uiState.articleState as? ArticleState.Success)
                        ?.article?.articleWithEnclosure?.article?.isFavorite == true
                    ComponeIconButton(
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
                            if (isFavorite) Res.string.article_screen_unfavorite
                            else Res.string.article_screen_favorite
                        ),
                    )
                    ComponeIconButton(
                        enabled = uiState.articleState is ArticleState.Success,
                        onClick = { openMoreMenu = true },
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(Res.string.more),
                    )
                    val articleLink = (uiState.articleState as? ArticleState.Success)
                        ?.article?.articleWithEnclosure?.article?.link
                    MoreMenu(
                        expanded = openMoreMenu,
                        onDismissRequest = { openMoreMenu = false },
                        onOpenInBrowserClick = articleLink?.let { { uriHandler.safeOpenUri(it) } },
                        onReadTextSizeClick = { openReadTextSizeSliderDialog = true },
                        onOpenArticleScreen = {
                            val articleState = uiState.articleState
                            if (articleState is ArticleState.Success) {
                                navController.navigate(ArticleRoute(feedUrls = listOf(articleState.article.feed.url)))
                            }
                        },
                    )
                }
            )
        },
        floatingActionButton = {
            ComponeFloatingActionButton(
                onSizeWithSinglePaddingChanged = { _, height -> fabHeight = height },
                onClick = {
                    openEnclosureBottomSheet = uiState.articleState is ArticleState.Success
                },
                contentDescription = stringResource(Res.string.bottom_sheet_enclosure_title),
            ) {
                Icon(
                    imageVector = Icons.Outlined.AttachFile,
                    contentDescription = stringResource(Res.string.bottom_sheet_enclosure_title),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
            LocalAbsoluteTonalElevation.current +
                    ReadContentTonalElevationPreference.current.dp
        ),
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(bottom = fabHeight)
                .testTag("ReadColumn"),
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
                    val clipboard = LocalClipboard.current
                    Content(
                        articleState = articleState,
                        shareImage = { dispatcher(ReadIntent.ShareImage(url = it)) },
                        copyImage = {
                            dispatcher(ReadIntent.CopyImage(url = it, clipboard = clipboard))
                        },
                        downloadImage = {
                            dispatcher(
                                ReadIntent.DownloadImage(
                                    url = it,
                                    title = articleState.article.articleWithEnclosure.article.title,
                                )
                            )
                        },
                    )
                    if (openEnclosureBottomSheet) {
                        EnclosureBottomSheet(
                            onDismissRequest = { openEnclosureBottomSheet = false },
                            dataList = remember(articleState.article) {
                                getEnclosuresList(articleState.article.articleWithEnclosure)
                            },
                            article = articleState.article,
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
private fun CategoryArea(categories: List<ArticleCategoryBean>) {
    if (categories.isNotEmpty()) {
        FlowRow(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val scope = rememberCoroutineScope()
            val clipboard = LocalClipboard.current
            categories.forEach { category ->
                SuggestionChip(
                    onClick = { scope.launch { clipboard.setText(category.category) } },
                    label = { Text(text = category.category) },
                )
            }
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
    val article = articleState.article.articleWithEnclosure
    var openImageSheet by rememberSaveable { mutableStateOf<String?>(null) }
    val playerJumper = rememberPlayerJumper()

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
                            text = date.toDateTimeString(),
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
        playerJumper.jump(
            PlayDataMode.ArticleList(
                articleId = article.article.articleId,
                url = url,
            )
        )
    })
    PodAuraWebView(
        modifier = Modifier.fillMaxWidth(),
        content = article.article.content.ifNullOfBlank {
            article.article.description.orEmpty()
        },
        refererDomain = article.article.link?.httpDomain(),
        horizontalPadding = 16f,
        onImageClick = { imageUrl, alt -> openImageSheet = imageUrl },
    )
    CategoryArea(article.categories)

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
    onOpenInBrowserClick: (() -> Unit)?,
    onReadTextSizeClick: () -> Unit,
    onOpenArticleScreen: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        DropdownMenuItem(
            text = { Text(text = stringResource(Res.string.open_link_in_browser)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.OpenInBrowser,
                    contentDescription = null,
                )
            },
            onClick = {
                onDismissRequest()
                onOpenInBrowserClick?.invoke()
            },
            enabled = onOpenInBrowserClick != null,
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(Res.string.read_screen_text_size)) },
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
        DropdownMenuItem(
            text = { Text(text = stringResource(Res.string.read_screen_open_article_screen)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.RssFeed,
                    contentDescription = null,
                )
            },
            onClick = {
                onDismissRequest()
                onOpenArticleScreen()
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
            val scope = rememberCoroutineScope()
            val textSize = ReadTextSizePreference.current
            Text(
                modifier = Modifier.padding(start = 16.dp),
                text = "${textSize.format(2)} Sp",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Slider(
                modifier = Modifier.padding(horizontal = 16.dp),
                valueRange = 12f..50f,
                value = textSize,
                onValueChange = { ReadTextSizePreference.put(scope = scope, value = it) },
            )
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
    val uriHandler = LocalUriHandler.current
    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
        ) {
            ImageBottomSheetItem(
                icon = Icons.Outlined.Download,
                title = stringResource(Res.string.read_screen_download_image),
                onClick = {
                    downloadImage(imageUrl)
                    onDismissRequest()
                }
            )
            if (platform in arrayOf(Platform.Android, Platform.IOS)) {
                ImageBottomSheetItem(
                    icon = Icons.Outlined.Share,
                    title = stringResource(Res.string.share),
                    onClick = {
                        shareImage(imageUrl)
                        onDismissRequest()
                    }
                )
            }
            ImageBottomSheetItem(
                icon = Icons.Outlined.ContentCopy,
                title = stringResource(Res.string.copy),
                onClick = {
                    copyImage(imageUrl)
                    onDismissRequest()
                }
            )
            ImageBottomSheetItem(
                icon = Icons.Outlined.Public,
                title = stringResource(Res.string.read_screen_open_image_in_browser),
                onClick = {
                    uriHandler.safeOpenUri(imageUrl)
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