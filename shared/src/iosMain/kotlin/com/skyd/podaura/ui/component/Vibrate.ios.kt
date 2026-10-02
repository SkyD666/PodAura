package com.skyd.podaura.ui.component

actual fun tickVibrate() {
    platform.UIKit.UISelectionFeedbackGenerator().selectionChanged()
}
