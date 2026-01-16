package com.skyd.podaura.ui.player.pip

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.skyd.podaura.ext.activity

/*internal*/ actual val supportPip: Boolean
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

@Composable
/*internal*/ actual fun rememberOnEnterPip(): OnEnterPip {
    val context = LocalContext.current
    return remember {
        object : OnEnterPip {
            override fun enter() {
                context.activity.manualEnterPictureInPictureMode()
            }
        }
    }
}
