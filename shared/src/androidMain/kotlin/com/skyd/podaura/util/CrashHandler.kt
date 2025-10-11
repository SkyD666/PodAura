package com.skyd.podaura.util

import android.annotation.SuppressLint
import android.content.Context
import co.touchlab.kermit.Logger
import com.skyd.podaura.ui.activity.CrashActivity
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

class CrashHandler private constructor(
    val context: Context
) : Thread.UncaughtExceptionHandler {
    private val log = Logger.Companion.withTag(TAG)

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
            CrashActivity.start(context, unCaughtException)
            exitProcess(0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mDefaultHandler?.uncaughtException(thread, ex)
    }

    companion object {
        const val TAG = "AndroidCrashHandler"

        @SuppressLint("StaticFieldLeak")
        private var instance: CrashHandler? = null

        fun init(context: Context): CrashHandler? {
            if (instance == null) {
                synchronized(CrashHandler::class.java) {
                    if (instance == null) {
                        instance = CrashHandler(context)
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