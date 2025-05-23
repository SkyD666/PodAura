package com.skyd.podaura.ui.component

import androidx.annotation.IntRange
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skyd.podaura.ext.alwaysLight
import com.skyd.podaura.ext.thenIf
import java.util.Locale

val LocalUseColorfulIcon = compositionLocalOf { false }
val LocalVerticalPadding = compositionLocalOf { 16.dp }

@Composable
fun BannerItem(content: @Composable () -> Unit) {
    Column {
        Spacer(modifier = Modifier.height(16.dp))
        CompositionLocalProvider(
            LocalContentColor provides (LocalContentColor.current alwaysLight true),
            LocalVerticalPadding provides 21.dp
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(36))
                    .background(MaterialTheme.colorScheme.primaryContainer alwaysLight true)
            ) {
                content()
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SelectedItem(selected: Boolean, content: @Composable () -> Unit) {
    Box(modifier = Modifier.thenIf(selected) {
        background(MaterialTheme.colorScheme.surfaceContainerHighest)
    }) {
        content()
    }
}

@Composable
fun SliderSettingsItem(
    imageVector: ImageVector?,
    text: String,
    value: Float,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @IntRange(from = 0)
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    valueFormat: String = "%.2f",
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit,
) {
    SliderSettingsItem(
        painter = imageVector?.let { rememberVectorPainter(image = it) },
        text = text,
        value = value,
        modifier = modifier,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        valueFormat = valueFormat,
        enabled = enabled,
        onValueChange = onValueChange,
    )
}

@Composable
fun SliderSettingsItem(
    painter: Painter?,
    text: String,
    value: Float,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @IntRange(from = 0)
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    valueFormat: String = "%.2f",
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit,
) {
    BaseSettingsItem(
        icon = painter,
        text = text,
        modifier = modifier,
        enabled = enabled,
        description = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    modifier = Modifier.weight(1f),
                    value = value,
                    enabled = enabled,
                    valueRange = valueRange,
                    steps = steps,
                    onValueChangeFinished = onValueChangeFinished,
                    onValueChange = onValueChange,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = String.format(Locale.getDefault(), valueFormat, value))
            }
        }
    )
}

@Composable
fun SwitchSettingsItem(
    checked: Boolean,
    text: String,
    imageVector: ImageVector?,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit)?,
) {
    SwitchSettingsItem(
        painter = imageVector?.let { rememberVectorPainter(image = it) },
        text = text,
        modifier = modifier,
        description = description,
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
    )
}

@Composable
fun SwitchSettingsItem(
    checked: Boolean,
    text: String,
    painter: Painter?,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit)?,
) {
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    BaseSettingsItem(
        modifier = modifier.toggleable(
            value = checked,
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = { onCheckedChange?.invoke(it) },
        ),
        icon = painter,
        text = text,
        descriptionText = description,
        enabled = enabled,
    ) {
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            interactionSource = interactionSource
        )
    }
}


@Composable
fun SwitchBaseSettingsItem(
    checked: Boolean,
    text: String,
    imageVector: ImageVector?,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    extraContent: (@Composable () -> Unit)? = null,
    onCheckedChange: ((Boolean) -> Unit)?,
) {
    SwitchBaseSettingsItem(
        checked = checked,
        text = text,
        painter = imageVector?.let { rememberVectorPainter(image = it) },
        modifier = modifier,
        description = description,
        enabled = enabled,
        onClick = onClick,
        onLongClick = onLongClick,
        extraContent = extraContent,
        onCheckedChange = onCheckedChange,
    )
}

@Composable
fun SwitchBaseSettingsItem(
    checked: Boolean,
    text: String,
    painter: Painter?,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    extraContent: (@Composable () -> Unit)? = null,
    onCheckedChange: ((Boolean) -> Unit)?,
) {
    BaseSettingsItem(
        modifier = modifier,
        icon = painter,
        text = text,
        descriptionText = description,
        enabled = enabled,
        onClick = onClick,
        onLongClick = onLongClick,
        extraContent = extraContent,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VerticalDivider(modifier = Modifier.height(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
fun RadioSettingsItem(
    imageVector: ImageVector?,
    text: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    RadioSettingsItem(
        painter = imageVector?.let { rememberVectorPainter(image = it) },
        text = text,
        modifier = modifier,
        description = description,
        selected = selected,
        enabled = enabled,
        onClick = onClick
    )
}

@Composable
fun RadioSettingsItem(
    painter: Painter?,
    text: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    BaseSettingsItem(
        modifier = modifier
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = { onClick?.invoke() },
            ),
        icon = painter,
        text = text,
        descriptionText = description,
        enabled = enabled,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled,
            interactionSource = interactionSource
        )
    }
}

@Composable
fun ColorSettingsItem(
    imageVector: ImageVector?,
    text: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    onClick: (() -> Unit)? = null,
    initColor: Color,
) {
    ColorSettingsItem(
        painter = imageVector?.let { rememberVectorPainter(image = it) },
        text = text,
        modifier = modifier,
        description = description,
        onClick = onClick,
        initColor = initColor,
    )
}

@Composable
fun ColorSettingsItem(
    painter: Painter?,
    text: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    onClick: (() -> Unit)? = null,
    initColor: Color,
) {
    BaseSettingsItem(
        icon = painter,
        text = text,
        modifier = modifier,
        descriptionText = description,
        onClick = onClick
    ) {
        IconButton(onClick = { onClick?.invoke() }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = initColor,
                        shape = RoundedCornerShape(50.dp)
                    )
            )
        }
    }
}

@Composable
fun BaseSettingsItem(
    modifier: Modifier = Modifier,
    icon: Painter?,
    text: String,
    descriptionText: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    extraContent: (@Composable () -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    BaseSettingsItem(
        modifier = modifier,
        icon = icon,
        text = text,
        description = if (descriptionText != null) {
            {
                Text(
                    text = descriptionText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else null,
        enabled = enabled,
        onClick = if (enabled) onClick else null,
        onLongClick = if (enabled) onLongClick else null,
        extraContent = extraContent,
        content = content,
    )
}

@Composable
fun BaseSettingsItem(
    modifier: Modifier = Modifier,
    icon: Painter?,
    text: String,
    description: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    extraContent: (@Composable () -> Unit)? = null,
    content: (@Composable () -> Unit)? = null
) {
    CompositionLocalProvider(
        LocalContentColor provides if (enabled) {
            LocalContentColor.current
        } else {
            LocalContentColor.current.copy(alpha = 0.38f)
        },
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .thenIf(onClick != null && enabled) {
                    combinedClickable(onLongClick = onLongClick) { onClick!!() }
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                if (LocalUseColorfulIcon.current) {
                    Image(
                        modifier = Modifier
                            .padding(10.dp)
                            .size(24.dp),
                        painter = icon,
                        contentDescription = null
                    )
                } else {
                    Icon(
                        modifier = Modifier
                            .padding(10.dp)
                            .size(24.dp),
                        painter = icon,
                        contentDescription = null,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = LocalVerticalPadding.current)
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                extraContent?.invoke()
                if (description != null) {
                    Box(modifier = Modifier.padding(top = 4.dp)) {
                        description.invoke()
                    }
                }
            }
            content?.let {
                Box(modifier = Modifier.padding(end = 5.dp)) { it.invoke() }
            }
        }
    }
}

@Composable
fun CategorySettingsItem(text: String) {
    Text(
        modifier = Modifier.padding(
            start = 16.dp + 10.dp,
            end = 20.dp,
            top = 10.dp,
            bottom = 5.dp
        ),
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun TipSettingsItem(text: String) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp + 10.dp, vertical = 10.dp)
    ) {
        Icon(imageVector = Icons.Outlined.Info, contentDescription = null)
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}