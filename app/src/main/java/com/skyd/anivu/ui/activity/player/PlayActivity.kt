package com.skyd.anivu.ui.activity.player

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.lifecycleScope
import com.skyd.anivu.ext.getOrDefault
import com.skyd.anivu.ext.getString
import com.skyd.anivu.ext.safeLaunch
import com.skyd.anivu.ext.savePictureToMediaStore
import com.skyd.anivu.model.preference.dataStore
import com.skyd.anivu.model.preference.player.BackgroundPlayPreference
import com.skyd.anivu.model.repository.player.PlayDataMode
import com.skyd.anivu.ui.activity.BaseComposeActivity
import com.skyd.anivu.ui.component.showToast
import com.skyd.anivu.ui.mpv.PlayerCommand
import com.skyd.anivu.ui.mpv.PlayerViewRoute
import com.skyd.anivu.ui.mpv.service.PlayerService
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.player_no_permission_cannot_save_screenshot
import java.io.File


class PlayActivity : BaseComposeActivity() {
    companion object {
        const val PLAY_DATA_MODE_KEY = "playDataMode"

        fun playArticleList(
            activity: Activity,
            articleId: String,
            url: String,
        ) {
            activity.startActivity(
                Intent(activity, PlayActivity::class.java).apply {
                    putExtra(PLAY_DATA_MODE_KEY, PlayDataMode.ArticleList(articleId, url))
                }
            )
        }

        fun playMediaList(
            activity: Activity,
            startMediaPath: String,
            mediaList: List<PlayDataMode.MediaLibraryList.PlayMediaListItem>,
        ) {
            activity.startActivity(
                Intent(activity, PlayActivity::class.java).apply {
                    putExtra(
                        PLAY_DATA_MODE_KEY, PlayDataMode.MediaLibraryList(
                            startMediaPath, ArrayList(mediaList)
                        )
                    )
                }
            )
        }

        fun playPlaylist(
            activity: Activity,
            playlistId: String,
            mediaUrl: String?,
        ) {
            activity.startActivity(
                Intent(activity, PlayActivity::class.java).apply {
                    putExtra(PLAY_DATA_MODE_KEY, PlayDataMode.Playlist(playlistId, mediaUrl))
                }
            )
        }
    }

    private val viewModel: PlayerViewModel by viewModel()
    private lateinit var picture: File
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            picture.savePictureToMediaStore(this)
        } else {
            getString(Res.string.player_no_permission_cannot_save_screenshot).showToast()
        }
    }

    private lateinit var service: PlayerService
    private var serviceBound by mutableStateOf(false)
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as PlayerService.PlayerServiceBinder
            if (this@PlayActivity.isDestroyed) {
                unbindService(this)
                if (!dataStore.getOrDefault(BackgroundPlayPreference)) {
                    binder.getService().stopSelf()
                    return
                }
            }
            this@PlayActivity.service = binder.getService()
            lifecycleScope.launch {
                viewModel.mediaInfos.filter { it.first != null }.collect { (path, playlist) ->
                    this@PlayActivity.service.onCommand(
                        PlayerCommand.LoadList(
                            playlist = playlist,
                            startPath = path,
                        )
                    )
                }
            }

            serviceBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            serviceBound = false
            finish()
        }
    }

    private val serviceStopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context == null || intent == null) return
            if (intent.action == PlayerService.FINISH_PLAY_ACTIVITY_ACTION) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        viewModel.handleIntent(intent)

        ContextCompat.registerReceiver(
            this,
            serviceStopReceiver,
            IntentFilter(PlayerService.FINISH_PLAY_ACTIVITY_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        val serviceIntent = Intent(this, PlayerService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        bindService(serviceIntent, connection, BIND_AUTO_CREATE)

        setContentBase {
            DisposableEffect(Unit) {
                val listener = Consumer<Intent> { newIntent -> viewModel.handleIntent(newIntent) }
                addOnNewIntentListener(listener)
                onDispose { removeOnNewIntentListener(listener) }
            }
            PlayerViewRoute(
                service = if (serviceBound) service else null,
                onBack = { finish() },
                onSaveScreenshot = {
                    picture = it
                    saveScreenshot()
                },
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(serviceStopReceiver)
        unbindService(connection)
        serviceBound = false
        if (!dataStore.getOrDefault(BackgroundPlayPreference)) {
            stopService(Intent(this, PlayerService::class.java))
        }
    }

    private fun saveScreenshot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            picture.savePictureToMediaStore(this)
        } else {
            requestPermissionLauncher.safeLaunch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (serviceBound && service.player.onKey(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}