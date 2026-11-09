package com.skyd.podaura.app

import com.skyd.podaura.createIosViewController
import com.skyd.podaura.initializeIosApp as initializeHost
import com.skyd.podaura.app.podnyaAppModule
import com.skyd.podaura.ui.component.navigation.openIosDocument as openDocument
import platform.Foundation.NSURL
import platform.UIKit.UIViewController

fun initializeIosApp() = initializeHost { modules(podnyaAppModule) }

@Suppress("FunctionName")
fun MainViewController(): UIViewController {
    initializeIosApp()
    return createIosViewController()
}

fun openIosDocument(url: NSURL) = openDocument(url)
