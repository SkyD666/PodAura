package com.skyd.anivu.model.preference.behavior

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.anivu.R
import com.skyd.anivu.appContext
import com.skyd.anivu.base.BasePreference
import com.skyd.ksp.preference.Preference

@Preference
object PickImageMethodPreference : BasePreference<String> {
    val methodList = arrayOf(
        "PickVisualMedia",
        "PickFromGallery",
        "OpenDocument",
        "GetContent",
    )

    private const val PICK_IMAGE_METHOD = "pickImageMethod"

    override val default = methodList[0]
    override val key = stringPreferencesKey(PICK_IMAGE_METHOD)

    fun toDisplayName(method: String) = appContext.getString(
        when (method) {
            "PickVisualMedia" -> R.string.pick_image_method_pick_visual_media
            "PickFromGallery" -> R.string.pick_image_method_pick_from_gallery
            "OpenDocument" -> R.string.pick_image_method_open_document
            "GetContent" -> R.string.pick_image_method_get_content
            else -> R.string.pick_image_method_pick_visual_media
        }, method
    )
}