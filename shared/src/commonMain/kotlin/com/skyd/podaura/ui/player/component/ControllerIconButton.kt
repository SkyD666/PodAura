package com.skyd.podaura.ui.player.component

import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButtonColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.component.ComponeIconToggleButton

@Composable
/*internal*/ fun ControllerIconButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String?,
) {
    ComponeIconButton(
        modifier = modifier,
        imageVector = imageVector,
        onClick = onClick,
        contentDescription = contentDescription,
        enabled = enabled,
    )
}

@Composable
/*internal*/ fun ControllerIconToggleButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    imageVector: ImageVector,
    contentDescription: String?,
    colors: IconToggleButtonColors = IconButtonDefaults.iconToggleButtonColors(
        contentColor = LocalContentColor.current.copy(alpha = 0.4f),
        checkedContentColor = LocalContentColor.current,
    ),
) {
    ComponeIconToggleButton(
        modifier = modifier,
        checked = checked,
        onCheckedChange = onCheckedChange,
        imageVector = imageVector,
        contentDescription = contentDescription,
        enabled = enabled,
        colors = colors,
    )
}