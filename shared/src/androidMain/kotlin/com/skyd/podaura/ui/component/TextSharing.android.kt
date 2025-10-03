package com.skyd.podaura.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ShareCompat
import com.skyd.compone.component.blockString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.share

@Composable
actual fun rememberTextSharing(): TextSharing {
    val context = LocalContext.current
    return remember {
        object : TextSharing {
            override fun share(text: String) {
                val shareIntent = ShareCompat.IntentBuilder(context)
                    .setType("text/plain")
                    .setText(text)
                    .setChooserTitle(blockString(Res.string.share))
                    .createChooserIntent()
                context.startActivity(shareIntent)
            }
        }
    }
}