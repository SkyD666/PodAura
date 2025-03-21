package com.skyd.anivu.ui.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.skyd.anivu.ui.local.LocalTextFieldStyle


@Composable
fun ClipboardTextField(
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    value: String = "",
    label: String = "",
    maxLines: Int = Int.MAX_VALUE,
    singleLine: Boolean = maxLines == 1,
    style: PodAuraTextFieldStyle = PodAuraTextFieldStyle.toEnum(LocalTextFieldStyle.current),
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
) {
    Column {
        PodAuraTextField(
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
            errorMessage = errorText,
            keyboardActions = KeyboardActions(
                onDone = if (imeAction == ImeAction.Done) {
                    if (keyboardAction == null) action(focusManager, onConfirm, value)
                    else {
                        { keyboardAction(focusManager, value) }
                    }
                } else null,
                onGo = if (imeAction == ImeAction.Go) {
                    if (keyboardAction == null) action(focusManager, onConfirm, value)
                    else {
                        { keyboardAction(focusManager, value) }
                    }
                } else null,
                onNext = if (imeAction == ImeAction.Next) {
                    if (keyboardAction == null) action(focusManager, onConfirm, value)
                    else {
                        { keyboardAction(focusManager, value) }
                    }
                } else null,
                onPrevious = if (imeAction == ImeAction.Previous) {
                    if (keyboardAction == null) action(focusManager, onConfirm, value)
                    else {
                        { keyboardAction(focusManager, value) }
                    }
                } else null,
                onSearch = if (imeAction == ImeAction.Search) {
                    if (keyboardAction == null) action(focusManager, onConfirm, value)
                    else {
                        { keyboardAction(focusManager, value) }
                    }
                } else null,
                onSend = if (imeAction == ImeAction.Send) {
                    if (keyboardAction == null) action(focusManager, onConfirm, value)
                    else {
                        { keyboardAction(focusManager, value) }
                    }
                } else null,
            ),
            keyboardOptions = keyboardOptions,
        )
        if (errorText.isNotEmpty()) {
            SelectionContainer {
                Text(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .horizontalScroll(rememberScrollState()),
                    text = errorText,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

private fun action(
    focusManager: FocusManager?,
    onConfirm: (String) -> Unit,
    value: String,
): KeyboardActionScope.() -> Unit = {
    focusManager?.clearFocus()
    onConfirm(value)
}
