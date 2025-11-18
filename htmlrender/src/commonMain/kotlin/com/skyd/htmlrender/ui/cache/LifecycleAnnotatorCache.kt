package com.skyd.htmlrender.ui.cache

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.skyd.htmlrender.ui.RawHtmlData

class LifecycleAnnotatorCache<R>(lifecycle: Lifecycle) : HtmlAnnotatorCache<R> {
    private val cache = HashMap<RawHtmlData, R>()

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                cache.clear()
                owner.lifecycle.removeObserver(this)
            }
        })
    }

    override fun put(src: RawHtmlData, result: R) {
        cache[src] = result
    }

    override fun get(src: RawHtmlData) = cache[src]
}

@Composable
fun <R> rememberLifecycle(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
) = remember(lifecycleOwner) {
    LifecycleAnnotatorCache<R>(lifecycleOwner.lifecycle)
}