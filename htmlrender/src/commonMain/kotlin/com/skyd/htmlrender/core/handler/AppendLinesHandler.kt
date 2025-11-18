package com.skyd.htmlrender.core.handler

import com.skyd.htmlrender.base.handler.AbsAppendLinesHandler
import com.skyd.htmlrender.base.model.TextStyler
import com.skyd.htmlrender.core.styler.NewLineStyler

class AppendLinesHandler(amount: Int) : AbsAppendLinesHandler(amount) {
    override fun getNewLineStyler(): TextStyler = NewLineStyler
}