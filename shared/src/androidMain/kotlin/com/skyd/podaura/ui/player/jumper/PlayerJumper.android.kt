package com.skyd.podaura.ui.player.jumper

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.skyd.podaura.ext.activity


const val PLAY_DATA_MODE_KEY = "playDataMode"

@Composable
actual fun rememberPlayerJumper(): PlayerJumper {
    val context = LocalContext.current
    return remember {
        object : PlayerJumper {
            override fun jump(mode: PlayDataMode) {
                val activity = context.activity
                activity.startActivity(
                    Intent(
                        activity,
                        Class.forName("com.skyd.podaura.ui.activity.player.PlayActivity")
                    ).apply {
                        putExtra(PLAY_DATA_MODE_KEY, mode.encodeToString())
                    }
                )
            }
        }
    }
}