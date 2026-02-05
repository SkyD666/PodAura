package com.skyd.podaura.ui.player.mpv

import `is`.xyz.mpv.MPVLib

abstract class DefaultEventObserver : MPVLib.EventObserver {
    override fun eventProperty(property: String) = Unit
    override fun eventProperty(property: String, value: Long) = Unit
    override fun eventProperty(property: String, value: Boolean) = Unit
    override fun eventProperty(property: String, value: String) = Unit
    override fun eventProperty(property: String, value: Double) = Unit
    override fun event(eventId: Int) = Unit
    override fun efEvent(err: String?) = Unit
}

open class EventObserver(
    private val onPropertyEvent: EventObserver.(property: String) -> Unit = {},
    private val onLongPropertyEvent: EventObserver.(property: String, value: Long) -> Unit = { _, _ -> },
    private val onBooleanPropertyEvent: EventObserver.(property: String, value: Boolean) -> Unit = { _, _ -> },
    private val onStringPropertyEvent: EventObserver.(property: String, value: String) -> Unit = { _, _ -> },
    private val onDoublePropertyEvent: EventObserver.(property: String, value: Double) -> Unit = { _, _ -> },
    private val onEvent: EventObserver.(eventId: Int) -> Unit = {},
    private val onErrorEvent: EventObserver.(err: String?) -> Unit = {},
) : DefaultEventObserver() {
    override fun eventProperty(property: String) =
        onPropertyEvent(property)

    override fun eventProperty(property: String, value: Long) =
        onLongPropertyEvent(property, value)

    override fun eventProperty(property: String, value: Boolean) =
        onBooleanPropertyEvent(property, value)

    override fun eventProperty(property: String, value: String) =
        onStringPropertyEvent(property, value)

    override fun eventProperty(property: String, value: Double) =
        onDoublePropertyEvent(property, value)

    override fun event(eventId: Int) = onEvent(eventId)
    override fun efEvent(err: String?) = onErrorEvent(err)
}