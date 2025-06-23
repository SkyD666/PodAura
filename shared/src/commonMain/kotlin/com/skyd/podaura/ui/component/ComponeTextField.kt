package com.skyd.podaura.ui.component

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import com.skyd.compone.component.ComponeTextFieldStyle
import com.skyd.compone.component.DefaultTrailingIcon
import com.skyd.podaura.model.preference.appearance.TextFieldStylePreference


@Composable
fun ComponeTextField(
    modifier: Modifier = Modifier,
    value: String,
    label: String = "",
    enabled: Boolean = true,
    readOnly: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    singleLine: Boolean = maxLines == 1,
    style: ComponeTextFieldStyle = ComponeTextFieldStyle.toEnum(TextFieldStylePreference.current),
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
        if (style == ComponeTextFieldStyle.Normal) TextFieldDefaults.colors()
        else OutlinedTextFieldDefaults.colors(),
) = com.skyd.compone.component.ComponeTextField(
    modifier = modifier,
    value = value,
    label = label,
    enabled = enabled,
    readOnly = readOnly,
    maxLines = maxLines,
    singleLine = singleLine,
    style = style,
    autoRequestFocus = autoRequestFocus,
    onValueChange = onValueChange,
    visualTransformation = visualTransformation,
    isPassword = isPassword,
    placeholder = placeholder,
    trailingIcon = trailingIcon,
    errorMessage = errorMessage,
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    colors = colors,
)
