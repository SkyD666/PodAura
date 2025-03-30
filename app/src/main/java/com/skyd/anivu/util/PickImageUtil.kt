package com.skyd.anivu.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.skyd.anivu.R
import com.skyd.anivu.appContext
import com.skyd.anivu.ext.safeLaunch
import com.skyd.generated.preference.LocalPickImageMethod

@Composable
fun rememberImagePicker(
    method: String = LocalPickImageMethod.current,
    multiple: Boolean = false,
    onResult: (List<Uri?>) -> Unit
): ManagedActivityResultLauncher<*, *> {
    return when (method) {
        "PickVisualMedia" -> if (multiple) rememberLauncherForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(),
            onResult = onResult,
        ) else rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia(),
            onResult = { onResult(listOf(it)) },
        )

        "PickFromGallery" -> if (multiple) rememberLauncherForActivityResult(
            PickMultipleFromGallery(),
            onResult = onResult,
        ) else rememberLauncherForActivityResult(
            PickFromGallery(),
            onResult = { onResult(listOf(it)) },
        )

        "OpenDocument" -> if (multiple) rememberLauncherForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments(),
            onResult = onResult,
        ) else rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
            onResult = { onResult(listOf(it)) },
        )

        else -> if (multiple) rememberLauncherForActivityResult(
            ActivityResultContracts.GetMultipleContents(),
            onResult = onResult,
        ) else rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent(),
            onResult = { onResult(listOf(it)) },
        )
    }
}

@Suppress("UNCHECKED_CAST")
fun ManagedActivityResultLauncher<*, *>.launchImagePicker() {
    when (contract) {
        is ActivityResultContracts.PickMultipleVisualMedia -> {
            (this as ManagedActivityResultLauncher<PickVisualMediaRequest, List<@JvmSuppressWildcards Uri>>)
                .safeLaunch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        is ActivityResultContracts.PickVisualMedia -> {
            (this as ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>)
                .safeLaunch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        is PickMultipleFromGallery -> {
            (this as ManagedActivityResultLauncher<Unit?, List<Uri>>)
                .safeLaunch(null)
        }

        is PickFromGallery -> {
            (this as ManagedActivityResultLauncher<Unit?, Uri?>)
                .safeLaunch(null)
        }

        is ActivityResultContracts.OpenMultipleDocuments -> {
            (this as ManagedActivityResultLauncher<Array<String>, List<@JvmSuppressWildcards Uri>>)
                .safeLaunch(arrayOf("image/*"))
        }

        is ActivityResultContracts.OpenDocument -> {
            (this as ManagedActivityResultLauncher<Array<String>, Uri?>)
                .safeLaunch(arrayOf("image/*"))
        }

        is ActivityResultContracts.GetMultipleContents -> {
            (this as ManagedActivityResultLauncher<String, List<@JvmSuppressWildcards Uri>>)
                .safeLaunch("image/*")
        }

        is ActivityResultContracts.GetContent -> {
            (this as ManagedActivityResultLauncher<String, Uri?>)
                .safeLaunch("image/*")
        }

        else -> error("Unknown contract type!")
    }
}

class PickFromGallery : ActivityResultContract<Unit?, Uri?>() {
    override fun createIntent(context: Context, input: Unit?): Intent {
        return Intent.createChooser(
            Intent(Intent.ACTION_PICK)
                .setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*"),
            appContext.getString(R.string.use_the_following_to_open)
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return intent.takeIf { resultCode == Activity.RESULT_OK }?.data
    }
}

class PickMultipleFromGallery : ActivityResultContract<Unit?, List<Uri>>() {
    override fun createIntent(context: Context, input: Unit?): Intent {
        return Intent.createChooser(
            Intent(Intent.ACTION_PICK)
                .setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true),
            appContext.getString(R.string.use_the_following_to_open)
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        return intent.takeIf { resultCode == Activity.RESULT_OK }?.getClipDataUris().orEmpty()
    }
}

internal fun Intent.getClipDataUris(): List<Uri> {
    // Use a LinkedHashSet to maintain any ordering that may be
    // present in the ClipData
    val resultSet = LinkedHashSet<Uri>()
    data?.let { data ->
        resultSet.add(data)
    }
    val clipData = clipData
    if (clipData == null && resultSet.isEmpty()) {
        return emptyList()
    } else if (clipData != null) {
        for (i in 0 until clipData.itemCount) {
            val uri = clipData.getItemAt(i).uri
            if (uri != null) {
                resultSet.add(uri)
            }
        }
    }
    return ArrayList(resultSet)
}
