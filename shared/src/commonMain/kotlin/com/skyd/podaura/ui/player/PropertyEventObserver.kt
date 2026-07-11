package com.skyd.podaura.ui.player

import com.skyd.podaura.ui.player.mpv.EventListener

abstract class DefaultEventObserver : EventListener {
    override fun onPropertyChange(name: String) = Unit
    override fun onPropertyChange(name: String, value: Long) = Unit
    override fun onPropertyChange(name: String, value: Boolean) = Unit
    override fun onPropertyChange(name: String, value: String) = Unit
    override fun onPropertyChange(name: String, value: Double) = Unit
    override fun onEvent(event: Int) = Unit
}

open class PropertyEventObserver(
    private val onPropertyEvent: PropertyEventObserver.(property: String) -> Unit = {},
    private val onLongPropertyEvent: PropertyEventObserver.(property: String, value: Long) -> Unit = { _, _ -> },
    private val onBooleanPropertyEvent: PropertyEventObserver.(property: String, value: Boolean) -> Unit = { _, _ -> },
    private val onStringPropertyEvent: PropertyEventObserver.(property: String, value: String) -> Unit = { _, _ -> },
    private val onDoublePropertyEvent: PropertyEventObserver.(property: String, value: Double) -> Unit = { _, _ -> },
    private val onEvent: PropertyEventObserver.(event: Int) -> Unit = { _ -> },
) : DefaultEventObserver() {
    override fun onPropertyChange(name: String) =
        onPropertyEvent(name)

    override fun onPropertyChange(name: String, value: Long) =
        onLongPropertyEvent(name, value)

    override fun onPropertyChange(name: String, value: Boolean) =
        onBooleanPropertyEvent(name, value)

    override fun onPropertyChange(name: String, value: String) =
        onStringPropertyEvent(name, value)

    override fun onPropertyChange(name: String, value: Double) =
        onDoublePropertyEvent(name, value)

    override fun onEvent(event: Int) = onEvent.invoke(this, event)
}