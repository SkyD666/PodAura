package com.skyd.anivu.ui.component.dialog

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import com.skyd.anivu.model.preference.appearance.TextFieldStylePreference
import com.skyd.anivu.ui.component.ClipboardTextField
import com.skyd.anivu.ui.component.DefaultTrailingIcon
import com.skyd.anivu.ui.component.PodAuraTextFieldStyle
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.cancel
import podaura.shared.generated.resources.ok

@Composable
fun TextFieldDialog(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    readOnly: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    singleLine: Boolean = maxLines == 1,
    style: PodAuraTextFieldStyle = PodAuraTextFieldStyle.toEnum(TextFieldStylePreference.current),
    autoRequestFocus: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    titleText: String? = null,
    value: String = "",
    placeholder: String = "",
    trailingIcon: @Composable (() -> Unit)? = DefaultTrailingIcon,
    isPassword: Boolean = false,
    errorText: String = "",
    dismissText: String = stringResource(Res.string.cancel),
    confirmText: String = stringResource(Res.string.ok),
    enableConfirm: (String) -> Boolean = { it.isNotBlank() && errorText.isEmpty() },
    onValueChange: (String) -> Unit = {},
    onDismissRequest: () -> Unit = {},
    onConfirm: (String) -> Unit = {},
    imeAction: ImeAction = if (maxLines == 1) ImeAction.Done else ImeAction.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = imeAction),
) {
    TextFieldDialog(
        modifier = modifier,
        visible = visible,
        readOnly = readOnly,
        maxLines = maxLines,
        singleLine = singleLine,
        style = style,
        autoRequestFocus = autoRequestFocus,
        icon = icon,
        title = if (titleText == null) null else {
            { Text(text = titleText, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        },
        value = value,
        placeholder = placeholder,
        trailingIcon = trailingIcon,
        isPassword = isPassword,
        errorText = errorText,
        dismissText = dismissText,
        confirmText = confirmText,
        enableConfirm = enableConfirm,
        onValueChange = onValueChange,
        onDismissRequest = onDismissRequest,
        onConfirm = onConfirm,
        imeAction = imeAction,
        keyboardOptions = keyboardOptions,
    )
}

@Composable
fun TextFieldDialog(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    readOnly: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    singleLine: Boolean = maxLines == 1,
    style: PodAuraTextFieldStyle = PodAuraTextFieldStyle.toEnum(TextFieldStylePreference.current),
    autoRequestFocus: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    value: String = "",
    placeholder: String = "",
    trailingIcon: @Composable (() -> Unit)? = DefaultTrailingIcon,
    isPassword: Boolean = false,
    errorText: String = "",
    dismissText: String = stringResource(Res.string.cancel),
    confirmText: String = stringResource(Res.string.ok),
    enableConfirm: (String) -> Boolean = { it.isNotBlank() && errorText.isEmpty() },
    onValueChange: (String) -> Unit = {},
    onDismissRequest: () -> Unit = {},
    onConfirm: (String) -> Unit = {},
    imeAction: ImeAction = if (maxLines == 1) ImeAction.Done else ImeAction.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = imeAction),
) {
    val focusManager = LocalFocusManager.current

    PodAuraDialog(
        modifier = modifier,
        visible = visible,
        onDismissRequest = onDismissRequest,
        icon = icon,
        title = title,
        text = {
            ClipboardTextField(
                modifier = modifier.fillMaxWidth(),
                readOnly = readOnly,
                value = value,
                maxLines = maxLines,
                singleLine = singleLine,
                style = style,
                autoRequestFocus = autoRequestFocus,
                onValueChange = onValueChange,
                placeholder = placeholder,
                trailingIcon = trailingIcon,
                isPassword = isPassword,
                errorText = errorText,
                imeAction = imeAction,
                keyboardOptions = keyboardOptions,
                focusManager = focusManager,
                onConfirm = onConfirm,
            )
        },
        confirmButton = {
            TextButton(
                enabled = enableConfirm(value),
                onClick = {
                    focusManager.clearFocus()
                    onConfirm(value)
                }
            ) {
                Text(
                    text = confirmText,
                    color = if (enableConfirm(value)) {
                        Color.Unspecified
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = dismissText)
            }
        },
    )
}
