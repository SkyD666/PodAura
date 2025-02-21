package com.skyd.anivu.ui.activity.player

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.IntentCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyd.anivu.appContext
import com.skyd.anivu.ext.getImage
import com.skyd.anivu.ext.imageLoaderBuilder
import com.skyd.anivu.ui.activity.player.PlayActivity.Companion.VIDEO_THUMBNAIL_KEY
import com.skyd.anivu.ui.activity.player.PlayActivity.Companion.VIDEO_TITLE_KEY
import com.skyd.anivu.ui.activity.player.PlayActivity.Companion.VIDEO_URI_KEY
import com.skyd.anivu.ui.mpv.resolveUri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor() : ViewModel() {
    val currentPath: MutableStateFlow<String?> = MutableStateFlow(null)
    val title: MutableStateFlow<String?> = MutableStateFlow(null)
    val thumbnail: MutableStateFlow<Bitmap?> = MutableStateFlow(null)

    fun handleIntent(intent: Intent?) {
        intent ?: return

        val uri = IntentCompat.getParcelableExtra(
            intent, VIDEO_URI_KEY, Uri::class.java
        ) ?: intent.data ?: return
        this.currentPath.tryEmit(uri.resolveUri(appContext))
        this.title.tryEmit(intent.getStringExtra(VIDEO_TITLE_KEY))
        viewModelScope.launch {
            val bitmapUrl = intent.getStringExtra(VIDEO_THUMBNAIL_KEY)
            if (bitmapUrl != null) {
                appContext.imageLoaderBuilder().build()
                    .getImage(appContext, bitmapUrl)
                    ?.inputStream()
                    ?.use { thumbnail.tryEmit(BitmapFactory.decodeStream(it)) }
            }
        }
    }
}