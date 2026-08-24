package com.skyd.podaura.ui.screen.image

import co.touchlab.kermit.Logger
import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import javax.swing.JComponent

internal fun installJvmMagnificationListener(
    component: JComponent,
    onScaleFactor: (Float) -> Unit,
): AutoCloseable {
    if (platform != Platform.macOS_Jvm) return AutoCloseable { }

    return runCatching {
        MacMagnificationApi.install(component, onScaleFactor)
    }.getOrElse { error ->
        when (error) {
            is ReflectiveOperationException,
            is RuntimeException,
            is LinkageError -> unavailableMagnificationRegistration(error)

            else -> throw error
        }
    }
}

private fun unavailableMagnificationRegistration(error: Throwable): AutoCloseable {
    val cause = (error as? InvocationTargetException)?.targetException ?: error
    Logger.w(throwable = cause, tag = "JvmMagnificationEvents") {
        "macOS trackpad pinch zoom is unavailable because the gesture API cannot be accessed"
    }
    return AutoCloseable { }
}

private object MacMagnificationApi {
    private val magnificationListenerClass =
        Class.forName("com.apple.eawt.event.MagnificationListener")
    private val gesturePhaseListenerClass =
        Class.forName("com.apple.eawt.event.GesturePhaseListener")
    private val gestureListenerClass =
        Class.forName("com.apple.eawt.event.GestureListener")
    private val magnificationEventClass =
        Class.forName("com.apple.eawt.event.MagnificationEvent")
    private val gestureEventClass =
        Class.forName("com.apple.eawt.event.GestureEvent")
    private val gestureUtilitiesClass =
        Class.forName("com.apple.eawt.event.GestureUtilities")

    private val getMagnification = magnificationEventClass.getMethod("getMagnification")
    private val consumeGesture = gestureEventClass.getMethod("consume")
    private val addListener = gestureUtilitiesClass.getMethod(
        "addGestureListenerTo",
        JComponent::class.java,
        gestureListenerClass,
    )
    private val removeListener = gestureUtilitiesClass.getMethod(
        "removeGestureListenerFrom",
        JComponent::class.java,
        gestureListenerClass,
    )

    fun install(
        component: JComponent,
        onScaleFactor: (Float) -> Unit,
    ): AutoCloseable {
        // GestureAdapter officially combines phase and magnification listeners.
        val listener = Proxy.newProxyInstance(
            ClassLoader.getSystemClassLoader(),
            arrayOf(magnificationListenerClass, gesturePhaseListenerClass),
        ) { proxy, method, arguments ->
            when (method.name) {
                "magnify" -> {
                    val event = requireNotNull(arguments?.singleOrNull())
                    val magnification = getMagnification.invoke(event) as Number
                    onScaleFactor(
                        (1.0 + magnification.toDouble()).toFloat().coerceAtLeast(0.01f)
                    )
                    consumeGesture.invoke(event)
                    null
                }

                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === arguments?.singleOrNull()
                "toString" -> "PodAuraMagnificationListener"
                else -> null
            }
        }
        addListener.invoke(null, component, listener)

        return AutoCloseable {
            removeListener.invoke(null, component, listener)
        }
    }
}
