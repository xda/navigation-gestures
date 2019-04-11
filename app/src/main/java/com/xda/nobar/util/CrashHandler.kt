package com.xda.nobar.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.DeadObjectException
import android.os.Process
import com.crashlytics.android.Crashlytics
import com.xda.nobar.BuildConfig
import com.xda.nobar.activities.ui.CrashActivity

class CrashHandler(private val prevHandler: Thread.UncaughtExceptionHandler, private val context: Context) : Thread.UncaughtExceptionHandler {
    private var isCrashing = false

    override fun uncaughtException(t: Thread?, e: Throwable) {
        if (!isCrashing) {
            isCrashing = true

            val needsToLog = needsLog(e)

            val crashIntent = PendingIntent.getActivity(
                    context,
                    100,
                    Intent(context, CrashActivity::class.java),
                    PendingIntent.FLAG_ONE_SHOT
            )

            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 100, crashIntent)

            if (needsToLog) {
                prevHandler.uncaughtException(t, e)
            } else {
                if (e !is DeadObjectException) {
                    Crashlytics.logException(e)
                }
            }

            Process.killProcess(Process.myPid())
        }
    }

    private fun needsLog(parent: Throwable): Boolean {
        var needsLog = false

        val cause = parent.cause

        if (cause != null) needsLog = needsLog(cause)
        else {
            val trace = parent.stackTrace

            trace.forEach {
                if (it.className.contains(BuildConfig.APPLICATION_ID)) {
                    needsLog = true
                    return@forEach
                }
            }
        }

        return needsLog
    }
}