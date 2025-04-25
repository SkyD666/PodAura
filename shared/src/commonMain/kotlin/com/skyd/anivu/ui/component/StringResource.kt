package com.skyd.anivu.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

@Composable
fun suspendString(block: suspend () -> String): String {
    return suspendString(block) { block() }
}

@Composable
fun <T : Any> suspendString(value: T, block: suspend (T) -> String): String {
    var str by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(value, block) {
        str = block(value)
    }
    return str
}

fun blockString(resource: StringResource): String = runBlocking {
    getString(resource)
}

fun blockString(resource: StringResource, vararg formatArgs: Any): String = runBlocking {
    getString(resource, formatArgs)
}

fun blockPluralString(resource: PluralStringResource, quantity: Int): String = runBlocking {
    getPluralString(resource, quantity)
}

fun blockPluralString(
    resource: PluralStringResource,
    quantity: Int,
    vararg formatArgs: Any
): String = runBlocking {
    getPluralString(resource, quantity, formatArgs)
}