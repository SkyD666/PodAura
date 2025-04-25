package com.skyd.anivu.ui.mpv.port

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.skyd.anivu.ext.activity
import com.skyd.anivu.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.anivu.ui.activity.MainActivity
import com.skyd.anivu.ui.component.BackIcon
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.mpv.component.state.PlayState
import com.skyd.anivu.ui.mpv.component.state.PlayStateCallback
import com.skyd.anivu.ui.mpv.component.state.dialog.OnDialogVisibilityChanged
import com.skyd.anivu.ui.mpv.pip.manualEnterPictureInPictureMode
import com.skyd.anivu.ui.mpv.port.controller.Controller
import com.skyd.anivu.ui.mpv.port.controller.SmallController
import com.skyd.anivu.ui.screen.article.ArticleRoute
import com.skyd.anivu.ui.screen.playlist.medialist.list.PlaylistMediaList
import com.skyd.anivu.ui.screen.read.ReadRoute
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.feed_screen_name
import podaura.shared.generated.resources.more
import podaura.shared.generated.resources.player_picture_in_picture
import podaura.shared.generated.resources.read_screen_name


@Composable
internal fun PortraitPlayerView(
    playState: PlayState,
    playStateCallback: PlayStateCallback,
    onDialogVisibilityChanged: OnDialogVisibilityChanged,
    onBack: () -> Unit,
    playerContent: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val playlistSheetState = rememberModalBottomSheetState()
    var showMenu by rememberSaveable { mutableStateOf(false) }
    var showPlaylistSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Small,
                scrollBehavior = scrollBehavior,
                title = { },
                navigationIcon = { BackIcon(onClick = onBack) },
                actions = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        PodAuraIconButton(
                            onClick = { context.activity.manualEnterPictureInPictureMode() },
                            imageVector = Icons.Outlined.PictureInPictureAlt,
                            contentDescription = stringResource(Res.string.player_picture_in_picture),
                        )
                    }
                    PodAuraIconButton(
                        onClick = { showMenu = true },
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(Res.string.more),
                    )
                    Menu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        media = playState.currentMedia,
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(paddingValues),
        ) {
            MediaArea(
                playState = playState,
                modifier = Modifier.padding(horizontal = 30.dp),
                playerContent = playerContent,
            )

            Spacer(modifier = Modifier.height(12.dp))
            Titles(
                playState = playState,
                modifier = Modifier.padding(horizontal = 30.dp),
            )

            Spacer(modifier = Modifier.height(10.dp))
            ProgressBar(
                playState = playState,
                playStateCallback = playStateCallback,
                modifier = Modifier.padding(horizontal = 30.dp),
            )

            Spacer(modifier = Modifier.height(3.dp))
            Controller(
                playState = playState,
                playStateCallback = playStateCallback,
                modifier = Modifier.padding(horizontal = 22.dp),
            )

            Spacer(modifier = Modifier.height(20.dp))
            SmallController(
                playState = playState,
                onDialogVisibilityChanged = onDialogVisibilityChanged,
                onOpenPlaylist = { showPlaylistSheet = true },
                modifier = Modifier.padding(horizontal = 30.dp),
            )
            Spacer(modifier = Modifier.height(20.dp))
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
private fun Menu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    media: PlaylistMediaWithArticleBean?,
) {
    val context = LocalContext.current

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        val feedUrl = media?.article?.feed?.url
        DropdownMenuItem(
            text = { Text(text = stringResource(Res.string.feed_screen_name)) },
            leadingIcon = { Icon(imageVector = Icons.Outlined.RssFeed, contentDescription = null) },
            onClick = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    ArticleRoute(feedUrls = listOf(feedUrl!!)).toDeeplink(),
                    context,
                    MainActivity::class.java
                )
                context.startActivity(intent)
                onDismissRequest()
            },
            enabled = feedUrl != null
        )
        val articleId = media?.article?.articleWithEnclosure?.article?.articleId
        DropdownMenuItem(
            text = { Text(text = stringResource(Res.string.read_screen_name)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Article,
                    contentDescription = null,
                )
            },
            onClick = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    ReadRoute(articleId = articleId!!).toDeeplink(),
                    context,
                    MainActivity::class.java
                )
                context.startActivity(intent)
                onDismissRequest()
            },
            enabled = articleId != null
        )
    }
}