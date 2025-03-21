package com.skyd.anivu.ui.component.html

import android.text.Editable
import android.text.Html.TagHandler
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.ImageSpan
import android.view.View
import org.xml.sax.XMLReader

internal class ImgTagHandler(private val onClick: (String) -> Unit) : TagHandler {
    override fun handleTag(opening: Boolean, tag: String, output: Editable, xmlReader: XMLReader) {
        if (tag.equals("img", ignoreCase = true)) {
            val len = output.length
            val images = output.getSpans(len - 1, len, ImageSpan::class.java)
            val imgURL = images.firstOrNull()?.source ?: return
            output.setSpan(
                ClickableImage(imgURL, onClick),
                len - 1,
                len,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
            )
        }
    }
}

internal class ClickableImage(
    private val url: String,
    private val onClick: (String) -> Unit,
) : ClickableSpan() {
    override fun onClick(widget: View) = onClick(url)
}