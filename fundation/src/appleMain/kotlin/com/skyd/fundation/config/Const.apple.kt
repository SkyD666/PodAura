package com.skyd.fundation.config

import com.skyd.fundation.BuildKonfig
import com.skyd.fundation.util.Directories
import com.skyd.fundation.util.joinPath

val Const.DB_DIR: String
    get() = joinPath(Directories.applicationSupport, BuildKonfig.packageName, "Database")
val Const.DATA_STORE_DIR: String
    get() = joinPath(Directories.applicationSupport, BuildKonfig.packageName, "DataStore")
