package com.skyd.podaura.ui.player.pip

import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/*internal*/ actual val supportPip: Boolean
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

@Composable
/*internal*/ actual fun rememberOnEnterPip(): OnEnterPip {
    val activity = LocalActivity.current

    return remember {
        object : OnEnterPip {
            override fun enter() {
                activity?.manualEnterPictureInPictureMode()
            }
        }
    }
}
