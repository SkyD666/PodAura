package com.skyd.podaura.ui.component.frame.windows

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.FontLoadResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinUser
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.ic_fluent_dismiss_48_regular
import podaura.shared.generated.resources.ic_fluent_square_48_regular
import podaura.shared.generated.resources.ic_fluent_square_multiple_48_regular
import podaura.shared.generated.resources.ic_fluent_subtract_48_filled

@Composable
fun CaptionButtonRow(
    windowHandle: HWND,
    isMaximize: Boolean,
    isActive: Boolean,
    accentColor: Color,
    frameColorEnabled: Boolean,
    onCloseRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onMinimizeButtonRectUpdate: (Rect) -> Unit = {},
    onMaximizeButtonRectUpdate: (Rect) -> Unit = {},
    onCloseButtonRectUpdate: (Rect) -> Unit = {}
) {
    Row(
        modifier = modifier.zIndex(1f)
    ) {
        val isSystemInDarkTheme = isSystemInDarkTheme()
        val windowsColorScheme by remember {
            derivedStateOf {
                if (isSystemInDarkTheme) darkWindowsColorScheme() else lightWindowsColorScheme()
            }
        }
        val defaultColorScheme by remember {
            derivedStateOf {
                if (frameColorEnabled && accentColor != Color.Unspecified) {
                    windowsColorScheme.toAccentCaptionButtonColorScheme(accentColor = accentColor)
                } else {
                    windowsColorScheme.toDefaultCaptionButtonColorScheme()
                }
            }
        }
        val closeColorScheme by remember {
            derivedStateOf {
                windowsColorScheme.toAccentCaptionButtonColorScheme()
            }
        }
        CaptionButton(
            onClick = {
                User32.INSTANCE.ShowWindow(windowHandle, WinUser.SW_MINIMIZE)
            },
            icon = CaptionButtonIcon.Minimize,
            isActive = isActive,
            colorScheme = defaultColorScheme,
            modifier = Modifier.onGloballyPositioned {
                onMinimizeButtonRectUpdate(it.boundsInWindow())
            }
        )
        CaptionButton(
            onClick = {
                if (isMaximize) {
                    User32.INSTANCE.ShowWindow(
                        windowHandle,
                        WinUser.SW_RESTORE
                    )
                } else {
                    User32.INSTANCE.ShowWindow(
                        windowHandle,
                        WinUser.SW_MAXIMIZE
                    )
                }
            },
            icon = if (isMaximize) {
                CaptionButtonIcon.Restore
            } else {
                CaptionButtonIcon.Maximize
            },
            isActive = isActive,
            colorScheme = defaultColorScheme,
            modifier = Modifier.onGloballyPositioned {
                onMaximizeButtonRectUpdate(it.boundsInWindow())
            }
        )
        CaptionButton(
            icon = CaptionButtonIcon.Close,
            onClick = onCloseRequest,
            isActive = isActive,
            colorScheme = closeColorScheme,
            modifier = Modifier.onGloballyPositioned {
                onCloseButtonRectUpdate(it.boundsInWindow())
            }
        )
    }
}

@Composable
private fun CaptionButton(
    onClick: () -> Unit,
    isActive: Boolean,
    icon: CaptionButtonIcon,
    colorScheme: CaptionButtonColorScheme,
    modifier: Modifier = Modifier,
    interaction: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isHovered by interaction.collectIsHoveredAsState()
    val isPressed by interaction.collectIsPressedAsState()

    val stateColors by remember {
        derivedStateOf {
            when {
                isPressed -> colorScheme.pressed
                isHovered -> colorScheme.hovered
                else -> colorScheme.default
            }
        }
    }
    val backgroundColor by remember(isActive) {
        derivedStateOf { if (isActive) stateColors.activeBackground else stateColors.inactiveBackground }
    }
    val foregroundColor by remember(isActive) {
        derivedStateOf { if (isActive) stateColors.activeForeground else stateColors.inactiveForeground }
    }

    Surface(
        color = backgroundColor,
        contentColor = foregroundColor,
        modifier = modifier
            .size(46.dp, 32.dp)
            .clickable(
                onClick = onClick,
                interactionSource = interaction,
                indication = null,
            ),
        shape = RectangleShape,
    ) {
        val fontFamily by rememberFontIconFamily()
        if (fontFamily != null) {
            Text(
                text = icon.glyph.toString(),
                fontFamily = fontFamily,
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
            )
        } else {
            Icon(
                painter = painterResource(icon.imageVector),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center).size(13.dp),
            )
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun rememberFontIconFamily(): State<FontFamily?> {
    val fontFamily = remember { mutableStateOf<FontFamily?>(null) }
    // Get Windows system font icon, fallback to fluent svg icon if failed.
    val fontFamilyResolver = LocalFontFamilyResolver.current
    LaunchedEffect(fontFamilyResolver) {
        fontFamily.value = sequenceOf("Segoe Fluent Icons", "Segoe MDL2 Assets")
            .firstNotNullOfOrNull {
                val fontFamily = FontFamily(it)
                runCatching {
                    val result = fontFamilyResolver.resolve(fontFamily).value as FontLoadResult
                    if (result.typeface == null || result.typeface?.familyName != it) {
                        null
                    } else {
                        fontFamily
                    }
                }.getOrNull()
            }
    }
    return fontFamily
}

private data class WindowsColorScheme(
    val textPrimaryColor: Color,
    val textSecondaryColor: Color,
    val textTertiaryColor: Color,
    val textDisabledColor: Color,
    val fillSubtleTransparentColor: Color,
    val fillSubtleSecondaryColor: Color,
    val fillSubtleTertiaryColor: Color,
    val fillSubtleDisabledColor: Color,
    val shellCloseColor: Color = Color(0xFFC42B1C),
)

private fun lightWindowsColorScheme() = WindowsColorScheme(
    textPrimaryColor = Color(0xE4000000),
    textSecondaryColor = Color(0x9B000000),
    textTertiaryColor = Color(0x72000000),
    textDisabledColor = Color(0x5C000000),
    fillSubtleTransparentColor = Color.Transparent,
    fillSubtleSecondaryColor = Color(0x09000000),
    fillSubtleTertiaryColor = Color(0x06000000),
    fillSubtleDisabledColor = Color.Transparent,
)

private fun darkWindowsColorScheme() = WindowsColorScheme(
    textPrimaryColor = Color(0xFFFFFFFF),
    textSecondaryColor = Color(0xC5FFFFFF),
    textTertiaryColor = Color(0x87FFFFFF),
    textDisabledColor = Color(0x5DFFFFFF),
    fillSubtleTransparentColor = Color.Transparent,
    fillSubtleSecondaryColor = Color(0x0FFFFFFF),
    fillSubtleTertiaryColor = Color(0x0AFFFFFF),
    fillSubtleDisabledColor = Color.Transparent,
)

@Immutable
private data class CaptionButtonColorScheme(
    val default: CaptionButtonStateColors,
    val hovered: CaptionButtonStateColors,
    val pressed: CaptionButtonStateColors,
    val disabled: CaptionButtonStateColors,
)

@Immutable
private data class CaptionButtonStateColors(
    val activeBackground: Color,
    val activeForeground: Color,
    val inactiveBackground: Color,
    val inactiveForeground: Color,
)

private fun WindowsColorScheme.toDefaultCaptionButtonColorScheme() = CaptionButtonColorScheme(
    default = CaptionButtonStateColors(
        activeBackground = fillSubtleTransparentColor,
        activeForeground = textPrimaryColor,
        inactiveBackground = fillSubtleTransparentColor,
        inactiveForeground = textDisabledColor,
    ),
    hovered = CaptionButtonStateColors(
        activeBackground = fillSubtleSecondaryColor,
        activeForeground = textPrimaryColor,
        inactiveBackground = fillSubtleSecondaryColor,
        inactiveForeground = textPrimaryColor,
    ),
    pressed = CaptionButtonStateColors(
        activeBackground = fillSubtleTertiaryColor,
        activeForeground = textSecondaryColor,
        inactiveBackground = fillSubtleTertiaryColor,
        inactiveForeground = textTertiaryColor,
    ),
    disabled = CaptionButtonStateColors(
        activeBackground = fillSubtleTransparentColor,
        activeForeground = textDisabledColor,
        inactiveBackground = fillSubtleTransparentColor,
        inactiveForeground = textDisabledColor,
    )
)

private fun WindowsColorScheme.toAccentCaptionButtonColorScheme(
    accentColor: Color = shellCloseColor
) = CaptionButtonColorScheme(
    default = CaptionButtonStateColors(
        activeBackground = fillSubtleTransparentColor,
        activeForeground = textPrimaryColor,
        inactiveBackground = fillSubtleTransparentColor,
        inactiveForeground = textDisabledColor,
    ),
    hovered = CaptionButtonStateColors(
        activeBackground = accentColor,
        activeForeground = Color.White,
        inactiveBackground = accentColor,
        inactiveForeground = Color.White,
    ),
    pressed = CaptionButtonStateColors(
        activeBackground = accentColor.copy(0.9f),
        activeForeground = Color.White.copy(0.7f),
        inactiveBackground = accentColor.copy(0.9f),
        inactiveForeground = Color.White.copy(0.7f),
    ),
    disabled = CaptionButtonStateColors(
        activeBackground = fillSubtleTransparentColor,
        activeForeground = textDisabledColor,
        inactiveBackground = fillSubtleTransparentColor,
        inactiveForeground = textDisabledColor,
    )
)

private enum class CaptionButtonIcon(
    val glyph: Char,
    val imageVector: DrawableResource,
) {
    Minimize(
        glyph = '\uE921',
        imageVector = Res.drawable.ic_fluent_subtract_48_filled
    ),
    Maximize(
        glyph = '\uE922',
        imageVector = Res.drawable.ic_fluent_square_48_regular
    ),
    Restore(
        glyph = '\uE923',
        imageVector = Res.drawable.ic_fluent_square_multiple_48_regular
    ),
    Close(
        glyph = '\uE8BB',
        imageVector = Res.drawable.ic_fluent_dismiss_48_regular
    )
}
