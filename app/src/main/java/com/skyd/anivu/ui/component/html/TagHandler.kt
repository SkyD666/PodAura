package com.skyd.anivu.ui.component.html

import android.text.Editable
import android.text.Html.TagHandler
import org.xml.sax.XMLReader

internal class TagHandler(private val handlers: List<TagHandler>) : TagHandler {
    override fun handleTag(opening: Boolean, tag: String, output: Editable, xmlReader: XMLReader) {
        handlers.forEach { it.handleTag(opening, tag, output, xmlReader) }
    }
}