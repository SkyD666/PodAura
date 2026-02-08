package com.skyd.podaura.ui.player.port

import android.content.Intent
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
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.skyd.compone.component.BackIcon
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.podaura.ext.isExpanded
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.ui.activity.MainActivity
import com.skyd.podaura.ui.component.isLandscape
import com.skyd.podaura.ui.local.LocalWindowSizeClass
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.PlayStateCallback
import com.skyd.podaura.ui.player.component.state.dialog.OnDialogVisibilityChanged
import com.skyd.podaura.ui.player.pip.rememberOnEnterPip
import com.skyd.podaura.ui.player.pip.supportPip
import com.skyd.podaura.ui.player.port.controller.Controller
import com.skyd.podaura.ui.player.port.controller.SmallController
import com.skyd.podaura.ui.screen.article.ArticleRoute
import com.skyd.podaura.ui.screen.playlist.medialist.list.PlaylistMediaList
import com.skyd.podaura.ui.screen.read.ReadRoute
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.feed_screen_name
import podaura.shared.generated.resources.more
import podaura.shared.generated.resources.player_audio_track
import podaura.shared.generated.resources.player_picture_in_picture
import podaura.shared.generated.resources.player_subtitle_track
import podaura.shared.generated.resources.read_screen_name


@Composable
internal fun PortraitPlayerView(
    playState: PlayState,
    playStateCallback: PlayStateCallback,
    onDialogVisibilityChanged: OnDialogVisibilityChanged,
    onBack: () -> Unit,
    onEnterFullscreen: () -> Unit,
    playerContent: @Composable () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val playlistSheetState = rememberModalBottomSheetState()
    var showMenu by rememberSaveable { mutableStateOf(false) }
    var showPlaylistSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.Small,
                scrollBehavior = scrollBehavior,
                title = { },
                navigationIcon = { BackIcon(onClick = onBack) },
                actions = {
                    if (supportPip) {
                        val onEnterPip = rememberOnEnterPip()
                        ComponeIconButton(
                            onClick = onEnterPip::enter,
                            imageVector = Icons.Outlined.PictureInPictureAlt,
                            contentDescription = stringResource(Res.string.player_picture_in_picture),
                        )
                    }
                    ComponeIconButton(
                        onClick = { showMenu = true },
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(Res.string.more),
                    )
                    Menu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        playState = playState,
                        onDialogVisibilityChanged = onDialogVisibilityChanged,
                        media = playState.currentMedia,
                    )
                }
            )
        }
    ) { paddingValues ->
        val windowSizeClass = LocalWindowSizeClass.current
        if (windowSizeClass.isExpanded || isLandscape()) {
            ExpandedContent(
                playState = playState,
                playStateCallback = playStateCallback,
                onDialogVisibilityChanged = onDialogVisibilityChanged,
                onOpenPlaylistSheet = { showPlaylistSheet = true },
                onEnterFullscreen = onEnterFullscreen,
                contentPadding = paddingValues,
                playerContent = playerContent,
            )
        } else {
            CompactContent(
                playState = playState,
                playStateCallback = playStateCallback,
                onDialogVisibilityChanged = onDialogVisibilityChanged,
                onOpenPlaylistSheet = { showPlaylistSheet = true },
                onEnterFullscreen = onEnterFullscreen,
                contentPadding = paddingValues,
                playerContent = playerContent,
            )
        }

        if (showPlaylistSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPlaylistSheet = false },
                sheetState = playlistSheetState
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
    playState: PlayState,
    playStateCallback: PlayStateCallback,
    onDialogVisibilityChanged: OnDialogVisibilityChanged,
    onOpenPlaylistSheet: () -> Unit,
    onEnterFullscreen: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    playerContent: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(contentPadding)) {
        MediaArea(
            playState = playState,
            modifier = Modifier
                .padding(horizontal = 30.dp)
                .weight(1f),
            playerContent = playerContent,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Titles(
            playState = playState,
            modifier = Modifier.padding(horizontal = 30.dp),
        )
        ControllerArea(
            isExpanded = false,
            playState = playState,
            playStateCallback = playStateCallback,
            onDialogVisibilityChanged = onDialogVisibilityChanged,
            onOpenPlaylistSheet = onOpenPlaylistSheet,
            onEnterFullscreen = onEnterFullscreen,
            contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp)
        )
    }
}

@Composable
private fun ExpandedContent(
    playState: PlayState,
    playStateCallback: PlayStateCallback,
    onDialogVisibilityChanged: OnDialogVisibilityChanged,
    onOpenPlaylistSheet: () -> Unit,
    onEnterFullscreen: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    playerContent: @Composable () -> Unit,
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
        )
        Column(
            modifier = Modifier
                .align(Alignment.Bottom)
                .weight(0.6f)
                .verticalScroll(rememberScrollState()),
        ) {
            Titles(
                playState = playState,
                modifier = Modifier.padding(horizontal = 30.dp),
            )
            ControllerArea(
                isExpanded = true,
                playState = playState,
                playStateCallback = playStateCallback,
                onDialogVisibilityChanged = onDialogVisibilityChanged,
                onOpenPlaylistSheet = onOpenPlaylistSheet,
                onEnterFullscreen = onEnterFullscreen,
                contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp)
            )
        }
    }
}

@Composable
private fun ControllerArea(
    isExpanded: Boolean,
    playState: PlayState,
    playStateCallback: PlayStateCallback,
    onDialogVisibilityChanged: OnDialogVisibilityChanged,
    onOpenPlaylistSheet: () -> Unit,
    onEnterFullscreen: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
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
        ProgressBar(
            playState = playState,
            playStateCallback = playStateCallback,
            modifier = Modifier.padding(horizontal = 30.dp),
        )
        space()
        Controller(
            playState = playState,
            playStateCallback = playStateCallback,
            modifier = Modifier
                .padding(horizontal = 22.dp)
                .align(Alignment.CenterHorizontally),
            onDialogVisibilityChanged = onDialogVisibilityChanged,
        )
        space()
        SmallController(
            playState = playState,
            playStateCallback = playStateCallback,
            onDialogVisibilityChanged = onDialogVisibilityChanged,
            onOpenPlaylist = onOpenPlaylistSheet,
            onEnterFullscreen = onEnterFullscreen,
            modifier = Modifier.padding(horizontal = 30.dp),
        )
    }
}

@Composable
private fun Menu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    playState: PlayState,
    onDialogVisibilityChanged: OnDialogVisibilityChanged,
    media: PlaylistMediaWithArticleBean?,
) {
    val context = LocalContext.current

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
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        ArticleRoute(feedUrls = listOf(feedUrl!!)).toDeeplink().toUri(),
                        context,
                        MainActivity::class.java
                    )
                    context.startActivity(intent)
                    onDismissRequest()
                },
                {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        ReadRoute(articleId = articleId!!).toDeeplink().toUri(),
                        context,
                        MainActivity::class.java
                    )
                    context.startActivity(intent)
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