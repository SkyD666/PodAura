package com.skyd.podaura.ui.player.jumper

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.preference.AcceptTermsPreference
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.preference.player.DirectBackgroundPlayPreference

const val PLAY_DATA_MODE_KEY = "playDataMode"

@Composable
actual fun rememberPlayerJumper(): PlayerJumper {
    val context = LocalContext.current

    return remember {
        object : PlayerJumper {
            override fun jump(mode: PlayDataMode) {
                if (dataStore.getOrDefault(AcceptTermsPreference) && DirectBackgroundPlayPreference.enabled()) {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(
                            context,
                            Class.forName("com.skyd.podaura.ui.player.service.PlayerService")
                        ).putExtra(PLAY_DATA_MODE_KEY, mode.encodeToString()),
                    )
                    return
                }
                context.startActivity(
                    Intent(
                        context,
                        Class.forName("com.skyd.podaura.ui.activity.player.PlayActivity")
                    ).apply {
                        putExtra(PLAY_DATA_MODE_KEY, mode.encodeToString())
                    }
                )
            }
        }
    }
}
