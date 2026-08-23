package com.skyd.podaura.ui.activity

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.skyd.podaura.ui.activity.player.PlayActivity
import com.skyd.podaura.ui.player.LocalPlayerSession
import com.skyd.podaura.ui.player.PlayerSession
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.service.PlayerService
import com.skyd.podaura.ui.screen.AppEntrance


class MainActivity : BaseComposeActivity(), PlayerSession {
    override var coordinator by mutableStateOf<PlayerCoordinator?>(null)
        private set
    override val isFullPlayerVisible: Boolean = false

    private var bindingRequested = false
    private var coordinatorLifecycleObserver: DefaultLifecycleObserver? = null

    private val playerServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            if (!bindingRequested) return
            val playerService = (service as PlayerService.PlayerServiceBinder).getService()
            connectPlayerCoordinator(playerService.playerCoordinator)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            clearPlayerCoordinator()
        }

        override fun onBindingDied(name: ComponentName) {
            releasePlayerServiceBinding()
        }

        override fun onNullBinding(name: ComponentName) {
            releasePlayerServiceBinding()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentBase {
            CompositionLocalProvider(LocalPlayerSession provides this@MainActivity) {
                AppEntrance()
            }
        }
    }

    override fun openFullPlayer() {
        startActivity(Intent(this, PlayActivity::class.java))
    }

    override fun destroySession() {
        sendBroadcast(Intent(PlayerService.CLOSE_ACTION).apply {
            `package` = packageName
        })
    }

    override fun onStart() {
        super.onStart()
        bindPlayerServiceIfRunning()
    }

    override fun onResume() {
        super.onResume()
        // A quickly finished PlayActivity may only pause MainActivity, so onStart is not repeated.
        bindPlayerServiceIfRunning()
    }

    private fun bindPlayerServiceIfRunning() {
        if (!bindingRequested) {
            bindingRequested = bindService(
                Intent(this, PlayerService::class.java),
                playerServiceConnection,
                0,
            )
        }
    }

    override fun onStop() {
        releasePlayerServiceBinding()
        super.onStop()
    }

    private fun connectPlayerCoordinator(coordinator: PlayerCoordinator) {
        clearPlayerCoordinator()
        this.coordinator = coordinator
        val observer = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                runOnUiThread { releasePlayerServiceBinding() }
            }
        }
        coordinatorLifecycleObserver = observer
        coordinator.lifecycle.addObserver(observer)

        if (coordinator.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            releasePlayerServiceBinding()
        }
    }

    private fun clearPlayerCoordinator() {
        val currentCoordinator = coordinator
        val observer = coordinatorLifecycleObserver
        this.coordinator = null
        coordinatorLifecycleObserver = null
        if (currentCoordinator != null && observer != null) {
            currentCoordinator.lifecycle.removeObserver(observer)
        }
    }

    private fun releasePlayerServiceBinding() {
        clearPlayerCoordinator()
        if (bindingRequested) {
            bindingRequested = false
            unbindService(playerServiceConnection)
        }
    }
}
