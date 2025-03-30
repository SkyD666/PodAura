package com.skyd.anivu.ui.screen.feed.item

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.skyd.anivu.R
import com.skyd.anivu.model.bean.group.GroupVo
import com.skyd.anivu.model.bean.group.GroupVo.Companion.isDefaultGroup
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.generated.preference.LocalFeedDefaultGroupExpand


val SHAPE_CORNER_DP = 26.dp

@Composable
fun Group1Item(
    index: Int,
    data: GroupVo,
    onExpandChange: (GroupVo, Boolean) -> Unit,
    isEmpty: (index: Int) -> Boolean,
    onShowAllArticles: (GroupVo) -> Unit,
    onEdit: ((GroupVo) -> Unit)? = null,
) {
    val isExpanded =
        if (data.isDefaultGroup()) LocalFeedDefaultGroupExpand.current else data.isExpanded
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

    Row(
        modifier = Modifier
            .padding(top = 16.dp)
            .background(
                shape = shape,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
            )
            .clip(shape)
            .combinedClickable(
                onLongClick = if (onEdit == null) null else {
                    { onEdit(data) }
                },
                onClick = { onShowAllArticles(data) },
            )
            .padding(start = 20.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = data.name,
            style = MaterialTheme.typography.titleMedium,
        )

        val expandIconRotate by animateFloatAsState(
            targetValue = if (isExpanded) 0f else 180f,
            label = "expand icon rotate",
        )

        PodAuraIconButton(
            onClick = { onExpandChange(data, !isExpanded) },
            imageVector = Icons.Outlined.KeyboardArrowUp,
            contentDescription = stringResource(if (isExpanded) R.string.collapse else R.string.expend),
            rotate = expandIconRotate,
        )
    }
}