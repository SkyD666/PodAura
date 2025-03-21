package com.skyd.anivu.model.bean

import androidx.annotation.Keep
import com.skyd.anivu.R
import com.skyd.anivu.appContext
import com.skyd.anivu.base.BaseBean

open class MediaGroupBean(
    open val name: String,
) : BaseBean {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MediaGroupBean) return false
        if (this === DefaultMediaGroup || other === DefaultMediaGroup) return false
        return name == other.name
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (this === DefaultMediaGroup).hashCode()
        return result
    }

    override fun toString(): String {
        return if (isDefaultGroup()) "default"
        else name
    }

    object DefaultMediaGroup : MediaGroupBean(appContext.getString(R.string.default_media_group)) {
        @Keep
        private fun readResolve(): Any = DefaultMediaGroup
        override val name: String
            get() = appContext.getString(R.string.default_media_group)
    }

    companion object {
        fun MediaGroupBean.isDefaultGroup(): Boolean = this === DefaultMediaGroup
    }
}