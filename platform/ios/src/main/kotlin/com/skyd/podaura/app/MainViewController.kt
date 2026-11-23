package com.skyd.podaura.app

import com.skyd.podaura.createIosViewController
import platform.Foundation.NSURL
import platform.UIKit.UIViewController
import com.skyd.podaura.initializeIosApp as initializeHost
import com.skyd.podaura.ui.component.navigation.openIosDocument as openDocument

fun initializeIosApp() = initializeHost { modules(nanoAppModule) }

@Suppress("FunctionName")
fun MainViewController(): UIViewController {
    initializeIosApp()
    return createIosViewController()
}

fun openIosDocument(url: NSURL) = openDocument(url)
