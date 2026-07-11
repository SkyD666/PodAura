package com.skyd.podaura.ui.player.mpv

interface EventListener {
    /**
     * Notify a property change with type MPV_FORMAT_NONE.
     */
    fun onPropertyChange(name: String)
    /**
     * Notify a property change with type MPV_FORMAT_FLAG.
     */
    fun onPropertyChange(name: String, value: Boolean)
    /**
     * Notify a property change with type MPV_FORMAT_INT64.
     */
    fun onPropertyChange(name: String, value: Long)
    /**
     * Notify a property change with type MPV_FORMAT_DOUBLE.
     */
    fun onPropertyChange(name: String, value: Double)
    /**
     * Notify a property change with type MPV_FORMAT_STRING.
     */
    fun onPropertyChange(name: String, value: String)
    fun onEvent(event: Int)
}