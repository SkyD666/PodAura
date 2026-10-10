package com.skyd.podaura.ui.component.navigation

import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventInput

/** Lets Compose handle selection, dialogs and inner routes before a native page is popped. */
internal class IosPageBackInput : NavigationEventInput() {
    private var attached = false
    private var hasHandlers = false
    val canPopPage: Boolean get() = attached && !hasHandlers

    override fun onAdded(dispatcher: NavigationEventDispatcher) {
        attached = true
    }

    override fun onRemoved() {
        attached = false
    }

    override fun onHasEnabledHandlersChanged(hasEnabledHandlers: Boolean) {
        hasHandlers = hasEnabledHandlers
    }

}
