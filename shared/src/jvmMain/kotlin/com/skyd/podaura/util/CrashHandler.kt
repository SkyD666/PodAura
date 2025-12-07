package com.skyd.podaura.util

import co.touchlab.kermit.Logger
import java.io.PrintWriter
import java.io.StringWriter

class CrashHandler private constructor(
    private val onCrash: (String) -> Unit,
) : Thread.UncaughtExceptionHandler {
    private val log = Logger.withTag(TAG)

    private val mDefaultHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    /**
     * When UncaughtException occurs, this function will handle it.
     */
    override fun uncaughtException(thread: Thread, ex: Throwable) {
        try {
            val stringWriter = StringWriter()
            val printWriter = PrintWriter(stringWriter)
            ex.printStackTrace(printWriter)
            var cause = ex.cause
            while (cause != null) {
                cause.printStackTrace(printWriter)
                cause = cause.cause
            }
            printWriter.close()
            val unCaughtException = stringWriter.toString()
            log.e("Crash Info: $unCaughtException")
            onCrash(unCaughtException)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mDefaultHandler?.uncaughtException(thread, ex)
    }

    companion object {
        const val TAG = "DesktopCrashHandler"

        private var instance: CrashHandler? = null

        fun init(onCrash: (String) -> Unit): CrashHandler? {
            if (instance == null) {
                synchronized(CrashHandler::class.java) {
                    if (instance == null) {
                        instance = CrashHandler(onCrash)
                    }
                }
            }
            return instance
        }
    }

    /**
     * Only one CrashHandler can be initialized
     */
    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }
}