package com.skyd.anivu.model.bean

import com.skyd.anivu.model.bean.article.EnclosureBean

data class LinkEnclosureBean(
    val link: String,
) : BaseBean {
    val isMedia: Boolean
        get() = EnclosureBean.videoExtensions.any { link.endsWith(it) } ||
                EnclosureBean.audioExtensions.any { link.endsWith(it) }
}
