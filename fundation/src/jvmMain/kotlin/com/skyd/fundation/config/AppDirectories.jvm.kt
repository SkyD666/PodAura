package com.skyd.fundation.config

import com.skyd.fundation.BuildKonfig
import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform
import java.io.File

interface AppDirectories {
    val homeDir: String get() = System.getProperty("user.home")
    val dataDir: String
    val cacheDir: String
}

val appDirectories: AppDirectories
    get() = when (platform) {
        Platform.Android,
        Platform.IOS -> error("Not supported platform")

        Platform.Linux -> LinuxAppDirectories
        Platform.MacOS -> MacAppDirectories
        Platform.Windows -> WindowsAppDirectories
    }

private object MacAppDirectories : AppDirectories {
    private val libraryDir = homeDir + File.separator + "Library"
    override val dataDir: String
        get() = libraryDir + File.separator + "Application Support" + File.separator + BuildKonfig.packageName
    override val cacheDir: String
        get() = libraryDir + File.separator + "Caches" + File.separator + BuildKonfig.packageName
}

private object WindowsAppDirectories : AppDirectories {
    private val appDataLocal = System.getenv("LOCALAPPDATA")
    override val dataDir: String
        get() = if (appDataLocal != null) {
            appDataLocal + File.separator + BuildKonfig.packageName + File.separator + "Data"
        } else {
            homeDir + File.separator + "AppData" + File.separator + "Local" +
                    File.separator + BuildKonfig.packageName + File.separator + "Data"
        }
    override val cacheDir: String
        get() = if (appDataLocal != null) {
            appDataLocal + File.separator + BuildKonfig.packageName + File.separator + "Cache"
        } else {
            homeDir + File.separator + "AppData" + File.separator + "Local" +
                    File.separator + BuildKonfig.packageName + File.separator + "Cache"
        }
}

private object LinuxAppDirectories : AppDirectories {
    override val dataDir: String
        get() = TODO("Not yet implemented")
    override val cacheDir: String
        get() = TODO("Not yet implemented")
}
