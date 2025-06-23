package com.skyd.podaura.ui.component.dialog

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.skyd.compone.component.ComponeTextFieldStyle
import com.skyd.compone.component.DefaultTrailingIcon
import com.skyd.podaura.model.preference.appearance.TextFieldStylePreference
import compone.shared.generated.resources.cancel
import compone.shared.generated.resources.ok
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
    style: ComponeTextFieldStyle = ComponeTextFieldStyle.toEnum(TextFieldStylePreference.current),
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
) = com.skyd.compone.component.dialog.TextFieldDialog(
    modifier = modifier,
    visible = visible,
    readOnly = readOnly,
    maxLines = maxLines,
    singleLine = singleLine,
    style = style,
    autoRequestFocus = autoRequestFocus,
    icon = icon,
    titleText = titleText,
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

@Composable
fun TextFieldDialog(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    readOnly: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    singleLine: Boolean = maxLines == 1,
    style: ComponeTextFieldStyle = ComponeTextFieldStyle.Outlined,
    autoRequestFocus: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    value: String = "",
    placeholder: String = "",
    trailingIcon: @Composable (() -> Unit)? = DefaultTrailingIcon,
    isPassword: Boolean = false,
    errorText: String = "",
    dismissText: String = stringResource(compone.shared.generated.resources.Res.string.cancel),
    confirmText: String = stringResource(compone.shared.generated.resources.Res.string.ok),
    enableConfirm: (String) -> Boolean = { it.isNotBlank() && errorText.isEmpty() },
    onValueChange: (String) -> Unit = {},
    onDismissRequest: () -> Unit = {},
    onConfirm: (String) -> Unit = {},
    imeAction: ImeAction = if (maxLines == 1) ImeAction.Done else ImeAction.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = imeAction),
) = com.skyd.compone.component.dialog.TextFieldDialog(
    modifier = modifier,
    visible = visible,
    readOnly = readOnly,
    maxLines = maxLines,
    singleLine = singleLine,
    style = style,
    autoRequestFocus = autoRequestFocus,
    icon = icon,
    title = title,
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