package com.skyd.htmlrender.ui.styler

import com.skyd.htmlrender.core.styler.IListItemStyler


class UnorderedListStyler : IListItemStyler {
    override fun getTag(): String = TAG_NAME

    override fun getAnnotation(): String = "•"

    companion object {
        const val TAG_NAME = "li-ul"
    }
}

class OrderedListStyler(private val index: Int) : IListItemStyler {
    override fun getTag(): String = TAG_NAME

    override fun getAnnotation(): String = "$index."

    companion object {
        const val TAG_NAME = "li-ol"
    }
}
