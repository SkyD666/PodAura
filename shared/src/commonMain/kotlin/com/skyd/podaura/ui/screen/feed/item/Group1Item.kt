package com.skyd.podaura.ui.screen.feed.item

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.ext.thenIfNotNull
import com.skyd.podaura.ext.onRightClickIfSupported
import com.skyd.podaura.model.bean.group.GroupVo
import com.skyd.podaura.model.bean.group.GroupVo.Companion.isDefaultGroup
import com.skyd.podaura.model.bean.group.GroupWithFeedCount
import com.skyd.podaura.model.preference.appearance.feed.FeedDefaultGroupExpandPreference
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.collapse
import podaura.shared.generated.resources.expend
import podaura.shared.generated.resources.feed_group_all_muted
import podaura.shared.generated.resources.feed_group_muted_count


val SHAPE_CORNER_DP = 26.dp

@Composable
fun Group1Item(
    index: Int,
    data: GroupWithFeedCount,
    onExpandChange: (GroupVo, Boolean) -> Unit,
    isEmpty: (index: Int) -> Boolean,
    onShowAllArticles: (GroupVo) -> Unit,
    onEdit: ((GroupVo) -> Unit)? = null,
) {
    val group = data.group
    val isExpanded =
        if (group.isDefaultGroup()) FeedDefaultGroupExpandPreference.current else group.isExpanded
    val backgroundShapeCorner: Dp by animateDpAsState(
        targetValue = if (isExpanded && !isEmpty(index)) 0.dp else SHAPE_CORNER_DP,
        label = "background shape corner",
    )
    val shape = RoundedCornerShape(
        SHAPE_CORNER_DP,
        SHAPE_CORNER_DP,
        backgroundShapeCorner,
        backgroundShapeCorner,
    )
    val onEditBlock = if (onEdit == null) null else {
        { onEdit(group) }
    }

    Row(
        modifier = Modifier
            .padding(top = 16.dp)
            .background(
                shape = shape,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
            )
            .clip(shape)
            .combinedClickable(
                onLongClick = onEditBlock,
                onClick = { onShowAllArticles(group) },
            )
            .thenIfNotNull(onEditBlock) { onRightClickIfSupported(onClick = it) }
            .padding(start = 20.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = group.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (data.mutedFeedCount > 0) {
            val allMuted = data.mutedFeedCount == data.feedCount
            val description = if (allMuted) {
                stringResource(Res.string.feed_group_all_muted)
            } else {
                pluralStringResource(
                    Res.plurals.feed_group_muted_count,
                    data.mutedFeedCount,
                    data.mutedFeedCount,
                )
            }
            Row(
                modifier = Modifier
                    .semantics { contentDescription = description }
                    .padding(start = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!allMuted) {
                    Text(
                        text = data.mutedFeedCount.toString(),
                        modifier = Modifier.clearAndSetSemantics {},
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.VolumeOff,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val expandIconRotate by animateFloatAsState(
            targetValue = if (isExpanded) 0f else 180f,
            label = "expand icon rotate",
        )

        ComponeIconButton(
            onClick = { onExpandChange(group, !isExpanded) },
            imageVector = Icons.Outlined.KeyboardArrowUp,
            contentDescription = stringResource(if (isExpanded) Res.string.collapse else Res.string.expend),
            rotate = expandIconRotate,
        )
    }
}
