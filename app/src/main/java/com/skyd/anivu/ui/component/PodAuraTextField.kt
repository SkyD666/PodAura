package com.skyd.anivu.ui.component

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.skyd.anivu.R
import com.skyd.anivu.ext.thenIf
import com.skyd.generated.preference.LocalTextFieldStyle


// https://issuetracker.google.com/issues/204502668#comment15
suspend fun FocusRequester.safeRequestFocus(windowInfo: WindowInfo) {
    snapshotFlow { windowInfo.isWindowFocused }.collect { isWindowFocused ->
        if (isWindowFocused) {
            requestFocus()
        }
    }
}

enum class PodAuraTextFieldStyle(val value: String) {
    Normal("Normal"),
    Outlined("Outlined");

    companion object {
        fun toEnum(value: String): PodAuraTextFieldStyle {
            return if (value == Normal.value) Normal else Outlined
        }
    }
}

val DefaultTrailingIcon: @Composable () -> Unit = {}

@Composable
fun PodAuraTextField(
    modifier: Modifier = Modifier,
    value: String,
    label: String = "",
    enabled: Boolean = true,
    readOnly: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    singleLine: Boolean = maxLines == 1,
    style: PodAuraTextFieldStyle = PodAuraTextFieldStyle.toEnum(LocalTextFieldStyle.current),
    autoRequestFocus: Boolean = true,
    onValueChange: (String) -> Unit,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isPassword: Boolean = false,
    placeholder: String = "",
    trailingIcon: @Composable (() -> Unit)? = DefaultTrailingIcon,
    errorMessage: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    colors: TextFieldColors =
        if (style == PodAuraTextFieldStyle.Normal) TextFieldDefaults.colors()
        else OutlinedTextFieldDefaults.colors(),
) {
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val focusRequester = remember { FocusRequester() }

    val windowInfo = LocalWindowInfo.current
    LaunchedEffect(windowInfo, autoRequestFocus) {
        if (autoRequestFocus) {
            focusRequester.safeRequestFocus(windowInfo)
        }
    }

    val newModifier = modifier.thenIf(autoRequestFocus) { focusRequester(focusRequester) }
    val newLabel: @Composable (() -> Unit)? =
        if (label.isBlank()) null else {
            { Text(label) }
        }
    val newOnValueChange: (String) -> Unit = { if (!readOnly) onValueChange(it) }
    val newVisualTransformation =
        if (isPassword && !showPassword) PasswordVisualTransformation() else visualTransformation
    val newPlaceholder: @Composable () -> Unit = {
        Text(
            text = placeholder,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium
        )
    }
    val newTrailingIcon: (@Composable () -> Unit)? = if (trailingIcon == DefaultTrailingIcon) {
        {
            if (value.isNotEmpty()) {
                PodAuraIconButton(
                    imageVector = if (isPassword) {
                        if (showPassword) Icons.Rounded.Visibility
                        else Icons.Rounded.VisibilityOff
                    } else Icons.Rounded.Close,
                    contentDescription = if (isPassword) {
                        if (showPassword) stringResource(R.string.password_visibility_off)
                        else stringResource(R.string.password_visibility_on)
                    } else stringResource(R.string.clear_input_text),
                    onClick = {
                        if (isPassword) {
                            showPassword = !showPassword
                        } else if (!readOnly) {
                            onValueChange("")
                        }
                    }
                )
            } else {
                PodAuraIconButton(
                    imageVector = Icons.Rounded.ContentPaste,
                    contentDescription = stringResource(R.string.paste),
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = { onValueChange(clipboardManager.getText()?.text.orEmpty()) }
                )
            }
        }
    } else {
        trailingIcon
    }

    when (style) {
        PodAuraTextFieldStyle.Normal -> TextField(
            modifier = newModifier,
            maxLines = maxLines,
            readOnly = readOnly,
            enabled = enabled,
            value = value,
            label = newLabel,
            onValueChange = newOnValueChange,
            visualTransformation = newVisualTransformation,
            placeholder = newPlaceholder,
            isError = errorMessage.isNotEmpty(),
            singleLine = singleLine,
            trailingIcon = newTrailingIcon,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            colors = colors,
        )

        PodAuraTextFieldStyle.Outlined -> OutlinedTextField(
            modifier = newModifier,
            maxLines = maxLines,
            readOnly = readOnly,
            enabled = enabled,
            value = value,
            label = newLabel,
            onValueChange = newOnValueChange,
            visualTransformation = newVisualTransformation,
            placeholder = newPlaceholder,
            isError = errorMessage.isNotEmpty(),
            singleLine = singleLine,
            trailingIcon = newTrailingIcon,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            colors = colors,
        )
    }
}
