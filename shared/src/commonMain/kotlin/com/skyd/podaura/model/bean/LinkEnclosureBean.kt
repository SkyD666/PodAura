package com.skyd.podaura.model.bean

import com.skyd.podaura.model.bean.article.EnclosureBean

data class LinkEnclosureBean(
    val link: String,
) : BaseBean {
    val isMedia: Boolean
        get() = EnclosureBean.videoExtensions.any { link.endsWith(it) } ||
                EnclosureBean.audioExtensions.any { link.endsWith(it) }
}
