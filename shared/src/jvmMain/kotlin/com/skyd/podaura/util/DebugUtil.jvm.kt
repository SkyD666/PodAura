package com.skyd.podaura.util

import java.lang.management.ManagementFactory

actual val isDebug: Boolean = ManagementFactory.getRuntimeMXBean()
    .inputArguments.toString().contains("jdwp")