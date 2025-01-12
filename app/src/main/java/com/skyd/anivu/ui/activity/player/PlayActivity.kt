package com.skyd.anivu.ui.activity.player

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyd.anivu.R
import com.skyd.anivu.base.BaseComposeActivity
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.getOrDefault
import com.skyd.anivu.ext.savePictureToMediaStore
import com.skyd.anivu.model.preference.player.BackgroundPlayPreference
import com.skyd.anivu.ui.component.showToast
import com.skyd.anivu.ui.mpv.PlayerViewRoute
import com.skyd.anivu.ui.mpv.copyAssetsForMpv
import com.skyd.anivu.ui.mpv.service.PlayerService
import java.io.File


class PlayActivity : BaseComposeActivity() {
    companion object {
        const val VIDEO_URI_KEY = "videoUri"
        const val VIDEO_TITLE_KEY = "videoTitle"
        const val VIDEO_THUMBNAIL_KEY = "videoThumbnail"

        fun play(
            activity: Activity,
            uri: Uri,
            title: String? = null,
            thumbnail: String? = null,
        ) {
            activity.startActivity(
                Intent(activity, PlayActivity::class.java).apply {
                    putExtra(VIDEO_URI_KEY, uri)
                    putExtra(VIDEO_TITLE_KEY, title)
                    putExtra(VIDEO_THUMBNAIL_KEY, thumbnail)
                }
            )
        }
    }

    private val viewModel: PlayerViewModel by viewModels()
    private lateinit var picture: File
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            picture.savePictureToMediaStore(this)
        } else {
            getString(R.string.player_no_permission_cannot_save_screenshot).showToast()
        }
    }

    private lateinit var service: PlayerService
    private var serviceBound by mutableStateOf(false)
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as PlayerService.PlayerServiceBinder
            this@PlayActivity.service = binder.getService().apply {
                if (uri != Uri.EMPTY && viewModel.uri.value == Uri.EMPTY) {
                    viewModel.uri.tryEmit(uri)
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
        copyAssetsForMpv(this)

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
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)

        setContentBase {
            DisposableEffect(Unit) {
                val listener = Consumer<Intent> { newIntent -> viewModel.handleIntent(newIntent) }
                addOnNewIntentListener(listener)
                onDispose { removeOnNewIntentListener(listener) }
            }
            val uri by viewModel.uri.collectAsStateWithLifecycle()
            val title by viewModel.title.collectAsStateWithLifecycle()
            val thumbnail by viewModel.thumbnail.collectAsStateWithLifecycle()
            if (uri != Uri.EMPTY) {
                PlayerViewRoute(
                    service = if (serviceBound) service else null,
                    uri = uri,
                    title = title,
                    thumbnail = thumbnail,
                    onBack = { finish() },
                    onSaveScreenshot = {
                        picture = it
                        saveScreenshot()
                    },
                )
            }
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
            requestPermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (serviceBound && service.player.onKey(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}