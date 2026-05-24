/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:JvmName("ViewModelStoreNavEntryDecoratorKt")
@file:JvmMultifileClass

package com.skyd.podaura.ui.component.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.SaveableStateHolderNavEntryDecorator
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Returns a [ViewModelStoreNavEntryDecorator] that is remembered across recompositions.
 *
 * @param [viewModelStoreOwner] The [ViewModelStoreOwner] that provides the [ViewModelStore] to
 *   NavEntries. If this owner implements [HasDefaultViewModelProviderFactory],
 *   its default factory and creation extras will be propagated to the NavEntries.
 */
@Composable
fun <T : Any> rememberViewModelStoreNavEntryDecorator(
    viewModelStoreOwner: ViewModelStoreOwner =
        checkNotNull(LocalViewModelStoreOwner.current) {
            "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
        }
): ViewModelStoreNavEntryDecorator<T> {
    val viewModelStoreProvider = rememberViewModelStoreProvider(parent = viewModelStoreOwner)
    return remember(viewModelStoreOwner, viewModelStoreProvider) {
        ViewModelStoreNavEntryDecorator(viewModelStoreProvider)
    }
}

/**
 * Provides the content of a [NavEntry] with a [ViewModelStoreOwner] and provides that
 * [ViewModelStoreOwner] as a [LocalViewModelStoreOwner] so that it is available within the content.
 *
 * This requires the usage of [SaveableStateHolderNavEntryDecorator] to ensure that the [NavEntry]
 * scoped [ViewModel]s can properly provide access to [androidx.lifecycle.SavedStateHandle]s.
 *
 * @see onPop for more details on when this callback is invoked
 */
class ViewModelStoreNavEntryDecorator<T : Any>(viewModelStoreProvider: ViewModelStoreProvider) :
    NavEntryDecorator<T>(
        onPop = viewModelStoreProvider::clearKey,
        decorate = { entry ->
            val owner = rememberViewModelStoreOwner(
                entry.contentKey,
                viewModelStoreProvider,
                savedStateRegistryOwner = LocalSavedStateRegistryOwner.current,
            )
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) { entry.Content() }
        }
    )
