package com.skyd.anivu.model.preference.behavior

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.anivu.model.preference.BasePreference
import com.skyd.ksp.preference.Preference
import org.jetbrains.compose.resources.getString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.pick_image_method_get_content
import podaura.shared.generated.resources.pick_image_method_open_document
import podaura.shared.generated.resources.pick_image_method_pick_from_gallery
import podaura.shared.generated.resources.pick_image_method_pick_visual_media

@Preference
actual object PickImageMethodPreference : BasePreference<String>() {
    actual val methodList = arrayOf(
        "PickVisualMedia",
        "PickFromGallery",
        "OpenDocument",
        "GetContent",
    )

    private const val PICK_IMAGE_METHOD = "pickImageMethod"

    actual override val default = methodList[0]
    actual override val key = stringPreferencesKey(PICK_IMAGE_METHOD)

    actual suspend fun toDisplayName(method: String) = getString(
        when (method) {
            "PickVisualMedia" -> Res.string.pick_image_method_pick_visual_media
            "PickFromGallery" -> Res.string.pick_image_method_pick_from_gallery
            "OpenDocument" -> Res.string.pick_image_method_open_document
            "GetContent" -> Res.string.pick_image_method_get_content
            else -> Res.string.pick_image_method_pick_visual_media
        }, method
    )
}