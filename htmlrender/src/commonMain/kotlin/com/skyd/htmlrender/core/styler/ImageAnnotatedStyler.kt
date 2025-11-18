package com.skyd.htmlrender.core.styler

import androidx.compose.ui.text.AnnotatedString
import com.skyd.htmlrender.base.handler.ImageStyler

class ImageAnnotatedStyler(imageUrl: String) : IBeforeChildrenAnnotatedStyler,
    ImageStyler(imageUrl) {

    override fun beforeChildren(builder: AnnotatedString.Builder) {
        with(builder) {
            pushStringAnnotation(TAG_NAME, imageUrl)
            append(PLACE_HOLDER)
            pop()
        }
    }

    companion object {
        const val TAG_NAME = "img"
    }
}