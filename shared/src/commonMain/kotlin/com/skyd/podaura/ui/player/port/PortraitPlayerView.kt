package com.skyd.podaura.ui.player.port

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skyd.compone.component.BackIcon
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.podaura.ext.isExpanded
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.ui.component.AnimatedDismissModalBottomSheet
import com.skyd.podaura.ui.component.isLandscape
import com.skyd.podaura.ui.component.navigation.rememberMainPageOpener
import com.skyd.podaura.ui.local.LocalWindowSizeClass
import com.skyd.podaura.ui.player.PlayerArticleContextState
import com.skyd.podaura.ui.player.PlayerProgress
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.PlayStateCallback
import com.skyd.podaura.ui.player.component.state.dialog.OnDialogVisibilityChanged
import com.skyd.podaura.ui.player.pip.rememberOnEnterPip
import com.skyd.podaura.ui.player.pip.supportPip
import com.skyd.podaura.ui.player.port.controller.Controller
import com.skyd.podaura.ui.player.port.controller.PrimaryControlMode
import com.skyd.podaura.ui.player.port.controller.SmallController
import com.skyd.podaura.ui.screen.article.ArticleRoute
import com.skyd.podaura.ui.screen.playlist.medialist.list.PlaylistMediaList
import com.skyd.podaura.ui.screen.read.ReadRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.article_screen_favorite
import podaura.shared.generated.resources.article_screen_unfavorite
import podaura.shared.generated.resources.feed_screen_name
import podaura.shared.generated.resources.more
import podaura.shared.generated.resources.player_audio_track
import podaura.shared.generated.resources.player_picture_in_picture
import podaura.shared.generated.resources.player_subtitle_track
import podaura.shared.generated.resources.read_screen_name


@Composable
internal fun PortraitPlayerView(
    playState: PlayState,
    articleContextState: PlayerArticleContextState,
    playStateCallback: PlayStateCallback,
    onDialogVisibilityChanged: OnDialogVisibilityChanged,
    onBack: () -> Unit,
    onEnterFullscreen: () -> Unit,
    playerContent: @Composable () -> Unit,
    onSetArticleFavorite: (Boolean) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    presentationState: PlayerPresentationState = PlayerPresentationState.Ready,
    onRetry: () -> Unit = {},
    playerProgress: Flow<PlayerProgress> = emptyFlow(),
    initialProgress: PlayerProgress = PlayerProgress(0L, 0L, 0),
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showMenu by rememberSaveable { mutableStateOf(false) }
    var showPlaylistSheet by remember { mutableStateOf(false) }
    val interactive = presentationState is PlayerPresentationState.Ready

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.Small,
                scrollBehavior = scrollBehavior,
                title = { },
                navigationIcon = { BackIcon(onClick = onBack) },
                actions = {
                    val isFavorite = articleContextState.isFavorite
                    if (isFavorite != null) {
                        ComponeIconButton(
                            enabled = interactive && !articleContextState.isFavoriteUpdating,
                            onClick = { onSetArticleFavorite(!isFavorite) },
                            imageVector = if (isFavorite) {
                                Icons.Outlined.Favorite
                            } else {
                                Icons.Outlined.FavoriteBorder
                            },
                            contentDescription = stringResource(
                                if (isFavorite) Res.string.article_screen_unfavorite
                                else Res.string.article_screen_favorite
                            ),
                        )
                    }
                    if (supportPip) {
                        val onEnterPip = rememberOnEnterPip()
                        ComponeIconButton(
                            enabled = interactive,
                            onClick = onEnterPip::enter,
                            imageVector = Icons.Outlined.PictureInPictureAlt,
                            contentDescription = stringResource(Res.string.player_picture_in_picture),
                        )
                    }
                    ComponeIconButton(
                        enabled = interactive,
                        onClick = { showMenu = true },
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(Res.string.more),
                    )
                    if (interactive) {
                        Menu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            playState = playState,
                            onDialogVisibilityChanged = onDialogVisibilityChanged,
                            media = playState.currentMedia,
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        val windowSizeClass = LocalWindowSizeClass.current
        if (windowSizeClass.isExpanded || isLandscape()) {
            ExpandedContent(
                playerProgress = playerProgress,
                initialProgress = initialProgress,
                playState = playState,
                playStateCallback = playStateCallback,
                onDialogVisibilityChanged = onDialogVisibilityChanged,
                onOpenPlaylistSheet = { showPlaylistSheet = true },
                onEnterFullscreen = onEnterFullscreen,
                contentPadding = paddingValues,
                playerContent = playerContent,
                presentationState = presentationState,
                onRetry = onRetry,
            )
        } else {
            CompactContent(
                playerProgress = playerProgress,
                initialProgress = initialProgress,
                playState = playState,
                playStateCallback = playStateCallback,
                onDialogVisibilityChanged = onDialogVisibilityChanged,
                onOpenPlaylistSheet = { showPlaylistSheet = true },
                onEnterFullscreen = onEnterFullscreen,
                contentPadding = paddingValues,
                playerContent = playerContent,
                presentationState = presentationState,
                onRetry = onRetry,
            )
        }

        if (interactive && showPlaylistSheet) {
            AnimatedDismissModalBottomSheet(
                onDismissRequest = { showPlaylistSheet = false }
            ) {
                PlaylistMediaList(
                    currentPlaylistId = playState.playlistId,
                    currentPlay = playState.currentMedia,
                    playlist = remember(playState) { playState.playlist.values.toList() },
                    onPlay = { playStateCallback.onPlayFileInPlaylist(it.playlistMediaBean.url) },
                    onDelete = { playStateCallback.onRemoveFromPlaylist(it) },
                )
            }
        }
    }
}

@Composable
private fun CompactContent(
    playerProgress: Flow<PlayerProgress>,
    initialProgress: PlayerProgress,
    playState: PlayState,
    playStateCallback: PlayStateCallback,
    onDialogVisibilityChanged: OnDialogVisibilityChanged,
    onOpenPlaylistSheet: () -> Unit,
    onEnterFullscreen: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    playerContent: @Composable () -> Unit,
    presentationState: PlayerPresentationState,
    onRetry: () -> Unit,
) {
    Column(modifier = Modifier.padding(contentPadding)) {
        MediaArea(
            playState = playState,
            modifier = Modifier
                .padding(horizontal = 30.dp)
                .weight(1f),
            playerContent = playerContent,
            presentationState = presentationState,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Titles(
            playState = playState,
            modifier = Modifier.padding(horizontal = 30.dp),
            presentationState = presentationState,
        )
        ControllerArea(
            playerProgress = playerProgress,
            initialProgress = initialProgress,
            isExpanded = false,
            playState = playState,
            playStateCallback = playStateCallback,
            onDialogVisibilityChanged = onDialogVisibilityChanged,
            onOpenPlaylistSheet = onOpenPlaylistSheet,
            onEnterFullscreen = onEnterFullscreen,
            contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp),
            presentationState = presentationState,
            onRetry = onRetry,
        )
    }
}

@Composable
private fun ExpandedContent(
    playerProgress: Flow<PlayerProgress>,
    initialProgress: PlayerProgress,
    playState: PlayState,
    playStateCallback: PlayStateCallback,
    onDialogVisibilityChanged: OnDialogVisibilityChanged,
    onOpenPlaylistSheet: () -> Unit,
    onEnterFullscreen: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    playerContent: @Composable () -> Unit,
    presentationState: PlayerPresentationState,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(contentPadding)
            .fillMaxSize(),
    ) {
        MediaArea(
            playState = playState,
            modifier = Modifier
                .padding(start = 30.dp)
                .weight(0.4f)
                .align(Alignment.CenterVertically),
            playerContent = playerContent,
            presentationState = presentationState,
        )
        Column(
            modifier = Modifier
                .align(Alignment.Bottom)
                .weight(0.6f)
                .verticalScroll(
                    state = rememberScrollState(),
                    enabled = presentationState is PlayerPresentationState.Ready,
                ),
        ) {
            Titles(
                playState = playState,
                modifier = Modifier.padding(horizontal = 30.dp),
                presentationState = presentationState,
            )
            ControllerArea(
                playerProgress = playerProgress,
                initialProgress = initialProgress,
                isExpanded = true,
                playState = playState,
                playStateCallback = playStateCallback,
                onDialogVisibilityChanged = onDialogVisibilityChanged,
                onOpenPlaylistSheet = onOpenPlaylistSheet,
                onEnterFullscreen = onEnterFullscreen,
                contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp),
                presentationState = presentationState,
                onRetry = onRetry,
            )
        }
    }
}

@Composable
private fun ControllerArea(
    playerProgress: Flow<PlayerProgress>,
    initialProgress: PlayerProgress,
    isExpanded: Boolean,
    playState: PlayState,
    playStateCallback: PlayStateCallback,
    onDialogVisibilityChanged: OnDialogVisibilityChanged,
    onOpenPlaylistSheet: () -> Unit,
    onEnterFullscreen: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    presentationState: PlayerPresentationState,
    onRetry: () -> Unit,
) {
    val interactive = presentationState is PlayerPresentationState.Ready
    Column(modifier = Modifier.padding(contentPadding)) {
        val space: @Composable ColumnScope.() -> Unit = {
            Spacer(
                modifier = Modifier.run {
                    if (isExpanded) {
                        weight(1f).heightIn(max = 16.dp)
                    } else {
                        height(16.dp)
                    }
                }
            )
        }
        PlayerProgressBar(
            progress = playerProgress,
            initialProgress = initialProgress,
            isSeeking = playState.isSeeking,
            onSeekTo = playStateCallback.onSeekTo,
            modifier = Modifier.padding(horizontal = 30.dp),
            enabled = interactive,
        )
        space()
        Controller(
            playState = playState,
            playStateCallback = playStateCallback,
            modifier = Modifier
                .padding(horizontal = 22.dp)
                .align(Alignment.CenterHorizontally),
            onDialogVisibilityChanged = onDialogVisibilityChanged,
            enabled = interactive,
            primaryControlMode = presentationState.toPrimaryControlMode(),
            onRetry = onRetry,
        )
        space()
        SmallController(
            playState = playState,
            playStateCallback = playStateCallback,
            onDialogVisibilityChanged = onDialogVisibilityChanged,
            onOpenPlaylist = onOpenPlaylistSheet,
            onEnterFullscreen = onEnterFullscreen,
            modifier = Modifier.padding(horizontal = 30.dp),
            enabled = interactive,
        )
    }
}

private fun PlayerPresentationState.toPrimaryControlMode(): PrimaryControlMode = when (this) {
    PlayerPresentationState.Ready -> PrimaryControlMode.Playback
    PlayerPresentationState.Loading -> PrimaryControlMode.Loading
    is PlayerPresentationState.Failed -> PrimaryControlMode.Retry
}

@Composable
private fun Menu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    playState: PlayState,
    onDialogVisibilityChanged: OnDialogVisibilityChanged,
    media: PlaylistMediaWithArticleBean?,
) {
    val mainPageOpener = rememberMainPageOpener()

    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        val texts = listOf(
            listOf(
                stringResource(Res.string.player_audio_track),
                stringResource(Res.string.player_subtitle_track),
            ),
            listOf(
                stringResource(Res.string.feed_screen_name),
                stringResource(Res.string.read_screen_name),
            )
        )
        val leadingIcons = listOf(
            listOf(
                Icons.Outlined.MusicNote,
                Icons.Outlined.ClosedCaption,
            ),
            listOf(
                Icons.Outlined.RssFeed,
                Icons.AutoMirrored.Outlined.Article,
            )
        )
        val feedUrl = media?.article?.feed?.url
        val articleId = media?.article?.articleWithEnclosure?.article?.articleId
        val onClicks = listOf(
            listOf(
                {
                    onDialogVisibilityChanged.onAudioTrackDialog(true)
                    onDismissRequest()
                },
                {
                    onDialogVisibilityChanged.onSubtitleTrackDialog(true)
                    onDismissRequest()
                },
            ),
            listOf(
                {
                    mainPageOpener.open(ArticleRoute(feedUrls = listOf(feedUrl!!)).toDeeplink())
                    onDismissRequest()
                },
                {
                    mainPageOpener.open(ReadRoute(articleId = articleId!!).toDeeplink())
                    onDismissRequest()
                },
            ),
        )
        val enables = listOf(
            listOf(playState.mediaLoaded, playState.mediaLoaded),
            listOf(feedUrl != null, articleId != null)
        )
        val groupCount = texts.size
        texts.forEachIndexed { groupIndex, subTexts ->
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(groupIndex, groupCount)) {
                subTexts.forEachIndexed { itemIndex, text ->
                    DropdownMenuItem(
                        text = { Text(text = text) },
                        shape = MenuDefaults.itemShape(itemIndex, subTexts.size).shape,
                        leadingIcon = {
                            Icon(
                                imageVector = leadingIcons[groupIndex][itemIndex],
                                contentDescription = null,
                            )
                        },
                        onClick = onClicks[groupIndex][itemIndex],
                        enabled = enables[groupIndex][itemIndex],
                    )
                }
            }
            if (groupIndex != groupCount - 1) {
                Spacer(Modifier.height(MenuDefaults.GroupSpacing))
            }
        }
    }
}
