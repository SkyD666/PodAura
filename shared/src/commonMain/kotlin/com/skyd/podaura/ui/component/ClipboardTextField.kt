package com.skyd.podaura.ui.component

import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.text.input.ImeAction
import com.skyd.compone.component.ComponeTextFieldStyle
import com.skyd.compone.component.DefaultTrailingIcon
import com.skyd.podaura.model.preference.appearance.TextFieldStylePreference


@Composable
fun ClipboardTextField(
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    value: String = "",
    label: String = "",
    maxLines: Int = Int.MAX_VALUE,
    singleLine: Boolean = maxLines == 1,
    style: ComponeTextFieldStyle = ComponeTextFieldStyle.toEnum(TextFieldStylePreference.current),
    autoRequestFocus: Boolean = true,
    onValueChange: (String) -> Unit = {},
    placeholder: String = "",
    trailingIcon: @Composable (() -> Unit)? = DefaultTrailingIcon,
    isPassword: Boolean = false,
    errorText: String = "",
    imeAction: ImeAction = ImeAction.Done,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = imeAction),
    keyboardAction: (KeyboardActionScope.(focusManager: FocusManager?, value: String) -> Unit)? = null,
    focusManager: FocusManager? = null,
    onConfirm: (String) -> Unit = {},
) = com.skyd.compone.component.ClipboardTextField(
    modifier = modifier,
    readOnly = readOnly,
    value = value,
    label = label,
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
    keyboardAction = keyboardAction,
    focusManager = focusManager,
    onConfirm = onConfirm,
)