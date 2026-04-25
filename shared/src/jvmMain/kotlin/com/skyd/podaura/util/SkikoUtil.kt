package com.skyd.podaura.util

import androidx.compose.ui.awt.ComposeWindow
import org.jetbrains.skiko.SkiaLayer
import java.awt.Container
import javax.swing.JComponent

fun <T : JComponent> findComponent(
    container: Container,
    klass: Class<T>,
): T? {
    val componentSequence = container.components.asSequence()
    return componentSequence.filter { klass.isInstance(it) }.ifEmpty {
        componentSequence.filterIsInstance<Container>()
            .mapNotNull { findComponent(it, klass) }
    }.map { klass.cast(it) }.firstOrNull()
}

inline fun <reified T : JComponent> Container.findComponent() =
    findComponent(this, T::class.java)

fun ComposeWindow.findSkiaLayer(): SkiaLayer? = findComponent<SkiaLayer>()
