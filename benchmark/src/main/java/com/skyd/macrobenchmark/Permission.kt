package com.skyd.macrobenchmark

import android.Manifest
import android.os.Build
import androidx.benchmark.macro.MacrobenchmarkScope
import org.junit.Assert

fun MacrobenchmarkScope.allowManageFiles() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // todo
    } else {
        val commandWrite = "pm grant $packageName ${Manifest.permission.WRITE_EXTERNAL_STORAGE}"
        Assert.assertEquals("", device.executeShellCommand(commandWrite))
        val commandRead = "pm grant $packageName ${Manifest.permission.READ_EXTERNAL_STORAGE}"
        Assert.assertEquals("", device.executeShellCommand(commandRead))
    }
}