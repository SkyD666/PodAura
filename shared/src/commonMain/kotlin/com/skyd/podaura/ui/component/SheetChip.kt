package com.skyd.podaura.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skyd.compone.ext.thenIfNotNull
import com.skyd.podaura.ext.onRightClickIfSupported

@Composable
fun SheetChip(
    modifier: Modifier = Modifier,
    icon: ImageVector?,
    iconBackgroundColor: Color = MaterialTheme.colorScheme.primary,
    iconTint: Color = contentColorFor(iconBackgroundColor),
    text: String?,
    contentDescription: String? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onRightClick: (() -> Unit)? = null,
    onIconClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .height(35.dp)
            .combinedClickable(onLongClick = onLongClick, onClick = onClick)
            .thenIfNotNull(onRightClick) { onRightClickIfSupported(onClick = it) }
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(iconBackgroundColor)
                    .thenIfNotNull(onIconClick) { clickable(onClick = it) }
                    .padding(3.dp)
                    .fillMaxHeight()
                    .aspectRatio(1f),
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
            )
            if (text != null) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
        if (text != null) {
            Text(
                modifier = Modifier.padding(horizontal = 6.dp),
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}