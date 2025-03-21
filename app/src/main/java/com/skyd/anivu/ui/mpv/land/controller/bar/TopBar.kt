package com.skyd.anivu.ui.mpv.land.controller.bar

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skyd.anivu.R
import com.skyd.anivu.ext.activity
import com.skyd.anivu.ui.mpv.component.ControllerIconButton
import com.skyd.anivu.ui.mpv.land.controller.ControllerBarGray
import com.skyd.anivu.ui.mpv.pip.manualEnterPictureInPictureMode

data class TopBarCallback(
    val onBack: () -> Unit,
)

@Composable
internal fun TopBar(
    modifier: Modifier = Modifier,
    title: String,
    topBarCallback: TopBarCallback,
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(ControllerBarGray, Color.Transparent)
                )
            )
            .windowInsetsPadding(
                WindowInsets.displayCutout.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                )
            )
            .padding(bottom = 30.dp)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier
                .clip(CircleShape)
                .size(56.dp)
                .clickable(onClick = topBarCallback.onBack)
                .padding(15.dp),
            imageVector = Icons.AutoMirrored.Outlined.ArrowBackIos,
            contentDescription = stringResource(id = R.string.back),
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            modifier = Modifier
                .weight(1f)
                .basicMarquee(iterations = Int.MAX_VALUE),
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            maxLines = 1,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Spacer(modifier = Modifier.width(3.dp))
            ControllerIconButton(
                modifier = Modifier.padding(2.dp),
                onClick = { context.activity.manualEnterPictureInPictureMode() },
                imageVector = Icons.Outlined.PictureInPictureAlt,
                contentDescription = stringResource(id = R.string.player_picture_in_picture),
            )
        }
    }
}