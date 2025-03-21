package com.skyd.anivu.ui.component.html

import android.content.Context
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.DrawableWrapper
import android.os.Build
import android.text.Html
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import coil3.asDrawable
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.error
import coil3.request.placeholder
import coil3.size.ScaleDrawable
import com.skyd.anivu.R
import com.skyd.anivu.ext.imageLoaderBuilder

class ImageGetter(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val maxWidth: () -> Int,
    private val onSuccess: (ImageRequest, SuccessResult) -> Unit,
    private val onError: (ImageRequest, ErrorResult) -> Unit = { _, _ -> },
) : Html.ImageGetter {
    override fun getDrawable(source: String): Drawable {
        val drawable = ImageGetterDrawable(
            AppCompatResources.getDrawable(context, R.drawable.ic_hourglass_24)!!
        )
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)

        fun setAndResizeDrawable(newDrawable: Drawable) {
            val drawableWidth = newDrawable.intrinsicWidth
            val maxWidth = maxWidth()
            check(maxWidth > 0) { "maxWidth must be greater than 0" }
            drawable.drawable = newDrawable
            if (drawableWidth > maxWidth) {
                val scale = maxWidth.toFloat() / drawableWidth
                drawable.setBounds(
                    0,
                    0,
                    maxWidth,
                    (newDrawable.intrinsicHeight * scale).toInt()
                )
            } else {
                drawable.setBounds(
                    0,
                    0,
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight
                )
            }
        }

        // 使用 Coil 加载图片
        val request = ImageRequest.Builder(context)
            .data(source)
            .placeholder(R.drawable.ic_hourglass_24)
            .error(R.drawable.ic_error_24)
            .listener(
                onSuccess = { request, result ->
                    val resultDrawable = result.image.asDrawable(context.resources)
                    preProcessDrawable(resultDrawable)
                    setAndResizeDrawable(resultDrawable)
                    onSuccess(request, result)
                },
                onError = { request, result ->
                    val resultDrawable = result.image?.asDrawable(context.resources)
                    if (resultDrawable != null) {
                        setAndResizeDrawable(resultDrawable)
                    }
                    onError(request, result)
                },
            )
            .build()

        // 返回占位符，直到图片加载完成
        context.imageLoaderBuilder().build().enqueue(request)

        return drawable
    }

    private fun preProcessDrawable(drawable: Drawable) {
        if (drawable is ScaleDrawable) {
            val child = drawable.child
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && child is AnimatedImageDrawable) {
                lifecycleOwner.lifecycle.addObserver(
                    object : DefaultLifecycleObserver {
                        override fun onResume(owner: LifecycleOwner) = child.start()
                        override fun onPause(owner: LifecycleOwner) = child.stop()
                        override fun onDestroy(owner: LifecycleOwner) {
                            lifecycleOwner.lifecycle.removeObserver(this)
                        }
                    }
                )
            }
        }
    }

    inner class ImageGetterDrawable(
        drawable: Drawable,
    ) : DrawableWrapper(drawable)
}