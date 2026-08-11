package com.skyd.podaura.ui.screen.read

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ChromeReaderMode
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.skyd.compone.component.ComponeFloatingActionButton
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.component.ComponeScaffold
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.component.dialog.WaitingDialog
import com.skyd.compone.component.navigation.LocalNavBackStack
import com.skyd.compone.ext.setText
import com.skyd.fundation.ext.format
import com.skyd.fundation.util.isJvm
import com.skyd.fundation.util.platform
import com.skyd.mvi.MviEventListener
import com.skyd.mvi.getDispatcher
import com.skyd.podaura.ext.httpDomain
import com.skyd.podaura.ext.isHttpOrHttps
import com.skyd.podaura.ext.safeOpenUri
import com.skyd.podaura.ext.toDateTimeString
import com.skyd.podaura.model.bean.article.ArticleCategoryBean
import com.skyd.podaura.model.preference.appearance.read.ReadContentTonalElevationPreference
import com.skyd.podaura.model.preference.appearance.read.ReadTextSizePreference
import com.skyd.podaura.model.preference.appearance.read.ReadTopBarTonalElevationPreference
import com.skyd.podaura.ui.component.AnimatedDismissModalBottomSheet
import com.skyd.podaura.ui.component.navigation.deeplink.DeepLinkPattern
import com.skyd.podaura.ui.component.rememberTextSharing
import com.skyd.podaura.ui.component.webview.PodAuraWebView
import com.skyd.podaura.ui.component.webview.HtmlStyleMode
import com.skyd.podaura.ui.player.jumper.PlayDataMode
import com.skyd.podaura.ui.player.jumper.rememberPlayerJumper
import com.skyd.podaura.ui.screen.article.ArticleRoute
import com.skyd.podaura.ui.screen.article.enclosure.EnclosureBottomSheet
import com.skyd.podaura.ui.screen.article.enclosure.getEnclosuresList
import com.skyd.podaura.ui.screen.image.rememberImagePreviewOpener
import io.ktor.http.Url
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.article_screen_favorite
import podaura.shared.generated.resources.article_screen_unfavorite
import podaura.shared.generated.resources.bottom_sheet_enclosure_title
import podaura.shared.generated.resources.more
import podaura.shared.generated.resources.media_not_exists
import podaura.shared.generated.resources.open_link_in_browser
import podaura.shared.generated.resources.read_screen_name
import podaura.shared.generated.resources.read_screen_get_full_content
import podaura.shared.generated.resources.read_screen_show_feed_content
import podaura.shared.generated.resources.read_screen_open_article_screen
import podaura.shared.generated.resources.read_screen_text_size
import podaura.shared.generated.resources.share

@Serializable
data class ReadRoute(@SerialName("articleId") val articleId: String) : NavKey {

    fun toDeeplink(): String = "$BASE_PATH/$articleId"

    companion object {
        const val BASE_PATH = "podaura://read.screen"

        val deepLinkPattern = DeepLinkPattern(
            serializer(),
            urlPattern = Url("$BASE_PATH/{articleId}")
        )

        @Composable
        fun ReadLauncher(route: ReadRoute, windowInsets: WindowInsets = WindowInsets.safeDrawing) {
            ReadScreen(articleId = route.articleId, windowInsets = windowInsets)
        }
    }
}

@Composable
fun ReadScreen(
    articleId: String,
    viewModel: ReadViewModel = koinViewModel(),
    windowInsets: WindowInsets = WindowInsets.safeDrawing
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val navBackStack = LocalNavBackStack.current
    val uriHandler = LocalUriHandler.current
    val playerJumper = rememberPlayerJumper()
    val mediaNotExistsMessage = stringResource(Res.string.media_not_exists)


    val snackbarHostState = remember { SnackbarHostState() }
    var openMoreMenu by rememberSaveable { mutableStateOf(false) }
    var openEnclosureBottomSheet by rememberSaveable { mutableStateOf(false) }
    var openReadTextSizeSliderDialog by rememberSaveable { mutableStateOf(false) }

    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    val dispatcher = viewModel.getDispatcher(startWith = ReadIntent.Init(articleId))

    var fabHeight by remember { mutableStateOf(0.dp) }

    ComponeScaffold(
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
                    if (!platform.isJvm) {
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
                    val successState = uiState.articleState as? ArticleState.Success
                    MoreMenu(
                        expanded = openMoreMenu,
                        onDismissRequest = { openMoreMenu = false },
                        onOpenInBrowserClick = articleLink?.let { { uriHandler.safeOpenUri(it) } },
                        onReadTextSizeClick = { openReadTextSizeSliderDialog = true },
                        contentSource = successState?.contentSource ?: ReadContentSource.Feed,
                        fullContentActionEnabled = !uiState.fullContentLoading && (
                            successState?.contentSource == ReadContentSource.FullText ||
                                successState?.fullContent != null ||
                                articleLink?.isHttpOrHttps() == true
                            ),
                        onFullContentClick = {
                            when {
                                successState?.contentSource == ReadContentSource.FullText ->
                                    dispatcher(ReadIntent.SelectContentSource(ReadContentSource.Feed))

                                successState?.fullContent != null ->
                                    dispatcher(ReadIntent.SelectContentSource(ReadContentSource.FullText))

                                articleLink?.isHttpOrHttps() == true ->
                                    dispatcher(ReadIntent.FetchFullContent(articleLink))
                            }
                        },
                        onOpenArticleScreen = {
                            val articleState = uiState.articleState
                            if (articleState is ArticleState.Success) {
                                navBackStack.add(ArticleRoute(feedUrls = listOf(articleState.article.feed.url)))
                            }
                        },
                    )
                },
                windowInsets = windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
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
        contentWindowInsets = windowInsets,
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
            LocalAbsoluteTonalElevation.current +
                    ReadContentTonalElevationPreference.current.dp
        ),
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        val contentSource = (uiState.articleState as? ArticleState.Success)?.contentSource
        // Deliberately not saveable: after recreation the tracker starts at the restored source,
        // so the first effect cannot overwrite rememberScrollState's restored offset.
        val contentSourceTransitionTracker = remember(articleId) {
            ReadContentSourceTransitionTracker(contentSource)
        }
        LaunchedEffect(contentSource) {
            if (contentSourceTransitionTracker.shouldResetScroll(contentSource)) {
                scrollState.scrollTo(0)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(scrollState)
                .padding(innerPadding)
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
                    Content(
                        articleState = articleState,
                        onTimestampClick = { mediaUrl, positionSeconds ->
                            dispatcher(
                                ReadIntent.PlayTimestamp(
                                    articleId = articleId,
                                    mediaUrl = mediaUrl,
                                    positionSeconds = positionSeconds,
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

                is ReadEvent.FullContentResultEvent.Failed -> snackbarHostState.showSnackbar(event.msg)

                is ReadEvent.PlayTimestampResultEvent.OpenPlayer -> playerJumper.jump(
                    PlayDataMode.ArticleList(
                        articleId = event.articleId,
                        url = event.mediaUrl,
                        startPositionSeconds = event.positionSeconds,
                    )
                )

                ReadEvent.PlayTimestampResultEvent.MediaNotExists ->
                    snackbarHostState.showSnackbar(mediaNotExistsMessage)
            }
        }

        WaitingDialog(visible = uiState.loadingDialog || uiState.fullContentLoading)

        if (openReadTextSizeSliderDialog) {
            ReadTextSizeSliderDialog(
                onDismissRequest = { openReadTextSizeSliderDialog = false },
            )
        }
    }
}

internal class ReadContentSourceTransitionTracker(
    initialSource: ReadContentSource?,
) {
    private var previousSource = initialSource

    fun shouldResetScroll(currentSource: ReadContentSource?): Boolean {
        if (currentSource == null) return false
        val shouldReset = previousSource != null && previousSource != currentSource
        previousSource = currentSource
        return shouldReset
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
    onTimestampClick: (mediaUrl: String?, positionSeconds: Long) -> Unit,
) {
    val article = articleState.article.articleWithEnclosure
    val playerJumper = rememberPlayerJumper()
    val imagePreviewOpener = rememberImagePreviewOpener()
    val firstMediaUrl = remember(article) {
        article.enclosures.firstOrNull { it.isMedia }?.url
    }

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
        content = articleState.displayedContent,
        baseUrl = articleState.displayedSourceUrl,
        refererDomain = articleState.displayedSourceUrl?.httpDomain(),
        styleMode = if (articleState.contentSource == ReadContentSource.FullText) {
            HtmlStyleMode.HarmonizedSource
        } else {
            HtmlStyleMode.ReaderTheme
        },
        horizontalPadding = 16f,
        onImageClick = { imageUrl, _ ->
            imagePreviewOpener.open(image = imageUrl, title = article.article.title)
        },
        onTimestampClick = { positionSeconds ->
            onTimestampClick(firstMediaUrl, positionSeconds)
        },
    )
    CategoryArea(article.categories)
}

@Composable
private fun MoreMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onOpenInBrowserClick: (() -> Unit)?,
    onReadTextSizeClick: () -> Unit,
    contentSource: ReadContentSource,
    fullContentActionEnabled: Boolean,
    onFullContentClick: () -> Unit,
    onOpenArticleScreen: () -> Unit,
) {
    DropdownMenuPopup(expanded = expanded, onDismissRequest = onDismissRequest) {
        val texts = listOf(
            stringResource(Res.string.open_link_in_browser),
            stringResource(
                if (contentSource == ReadContentSource.FullText) {
                    Res.string.read_screen_show_feed_content
                } else {
                    Res.string.read_screen_get_full_content
                }
            ),
            stringResource(Res.string.read_screen_text_size),
            stringResource(Res.string.read_screen_open_article_screen),
        )
        val leadingIcons = listOf(
            Icons.Outlined.OpenInBrowser,
            if (contentSource == ReadContentSource.FullText) {
                Icons.Outlined.RssFeed
            } else {
                Icons.AutoMirrored.Outlined.ChromeReaderMode
            },
            Icons.Outlined.FormatSize,
            Icons.Outlined.RssFeed,
        )
        val onClicks = listOf<() -> Unit>(
            {
                onDismissRequest()
                onOpenInBrowserClick?.invoke()
            },
            {
                onDismissRequest()
                onFullContentClick()
            },
            {
                onDismissRequest()
                onReadTextSizeClick()
            },
            {
                onDismissRequest()
                onOpenArticleScreen()
            },
        )
        val enables = listOf(
            onOpenInBrowserClick != null,
            fullContentActionEnabled,
            true,
            true,
        )
        DropdownMenuGroup(shapes = MenuDefaults.groupShape(0, 1)) {
            texts.forEachIndexed { index, text ->
                DropdownMenuItem(
                    text = { Text(text = text) },
                    shape = MenuDefaults.itemShape(index, texts.size).shape,
                    leadingIcon = {
                        Icon(imageVector = leadingIcons[index], contentDescription = null)
                    },
                    onClick = onClicks[index],
                    enabled = enables[index],
                )
            }
        }
    }
}

@Composable
private fun ReadTextSizeSliderDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
) {
    AnimatedDismissModalBottomSheet(
        onDismissRequest = onDismissRequest
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
                valueRange = 12f .. 50f,
                value = textSize,
                onValueChange = { ReadTextSizePreference.put(scope = scope, value = it) },
            )
        }
    }
}
