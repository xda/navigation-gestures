package com.xda.nobar.util

import android.util.Log
import com.xda.nobar.App
import java.util.regex.Pattern

class ImmersiveHelper(private val app: App) {
    companion object {
        const val COMMAND = "dumpsys activity service com.android.systemui/.SystemUIService"

        const val WINDOW_STATE_SHOWING = "WINDOW_STATE_SHOWING"
        const val WINDOW_STATE_HIDING = "WINDOW_STATE_HIDING"
        const val WINDOW_STATE_UNKNOWN = "WINDOW_STATE_UNKNOWN"
    }

    private var completeOutput: String? = null

    private var statusBarWindowState: String? = null
    private var navigationBarWindowState: String? = null

    fun run() {
        completeOutput = Utils.runCommand(COMMAND)

        parseStatusBarState()
        parseNavBarState()
    }

    private fun parseStatusBarState() {
        val pattern = Pattern.compile("(.*?)mStatusBarWindowState=(.*?)\b")
        val matcher = pattern.matcher(completeOutput)

        while (!matcher.hitEnd()) {
            if (matcher.find()) statusBarWindowState = matcher.group()
        }

        Log.e("NoBar", statusBarWindowState)
    }

    private fun parseNavBarState() {
        val pattern = Pattern.compile("(.*?)mNavigationBarWindowState=(.*?)\b")
        val matcher = pattern.matcher(completeOutput)

        while (!matcher.hitEnd()) {
            if (matcher.find()) navigationBarWindowState = matcher.group()
        }

        Log.e("NoBar", navigationBarWindowState)
    }
}