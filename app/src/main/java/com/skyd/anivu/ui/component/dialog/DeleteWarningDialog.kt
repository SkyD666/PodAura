package com.skyd.anivu.ui.component.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skyd.anivu.R
import com.skyd.anivu.model.preference.data.delete.KeepFavoriteArticlesPreference
import com.skyd.anivu.model.preference.data.delete.KeepPlaylistArticlesPreference
import com.skyd.anivu.model.preference.data.delete.KeepUnreadArticlesPreference
import com.skyd.anivu.ui.local.LocalKeepFavoriteArticles
import com.skyd.anivu.ui.local.LocalKeepPlaylistArticles
import com.skyd.anivu.ui.local.LocalKeepUnreadArticles

@Composable
fun DeleteWarningDialog(
    visible: Boolean = true,
    title: String = stringResource(R.string.warning),
    text: String? = null,
    confirmText: String = stringResource(R.string.delete),
    dismissText: String = stringResource(R.string.cancel),
    onDismissRequest: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    PodAuraDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        icon = { Icon(imageVector = Icons.Outlined.Warning, contentDescription = null) },
        title = { Text(text = title) },
        text = if (text == null) null else {
            { Text(text = text) }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) { Text(confirmText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissText) } },
    )
}

@Composable
fun DeleteArticleWarningDialog(
    visible: Boolean = true,
    title: String = stringResource(R.string.warning),
    text: String? = null,
    confirmText: String = stringResource(R.string.delete),
    dismissText: String = stringResource(R.string.cancel),
    onDismissRequest: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    PodAuraDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        icon = { Icon(imageVector = Icons.Outlined.Warning, contentDescription = null) },
        title = { Text(text = title) },
        text = {
            val deleteConstraints = listOf<Triple<String, Boolean, (Boolean) -> Unit>>(
                Triple(
                    stringResource(R.string.delete_constraint_screen_keep_unread_articles),
                    LocalKeepUnreadArticles.current
                ) { KeepUnreadArticlesPreference.put(context, scope, it) },
                Triple(
                    stringResource(R.string.delete_constraint_screen_keep_favorite_articles),
                    LocalKeepFavoriteArticles.current
                ) { KeepFavoriteArticlesPreference.put(context, scope, it) },
                Triple(
                    stringResource(R.string.delete_constraint_screen_keep_playlist_articles),
                    LocalKeepPlaylistArticles.current
                ) { KeepPlaylistArticlesPreference.put(context, scope, it) },
            )
            Column {
                if (text != null) {
                    Text(text = text)
                }
                Spacer(modifier = Modifier.height(6.dp))
                deleteConstraints.forEach { (title, checked, onCheckedChange) ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .toggleable(checked, onValueChange = onCheckedChange)
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = title, modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) { Text(confirmText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissText) } },
    )
}