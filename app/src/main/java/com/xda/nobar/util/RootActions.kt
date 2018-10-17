package com.xda.nobar.util

import android.content.Context
import android.os.Build
import android.view.KeyEvent
import com.topjohnwu.superuser.Shell
import com.xda.nobar.R
import java.io.DataOutputStream

class RootActions(private val context: Context) {
    lateinit var injectorProc: Process
    lateinit var outputStream: DataOutputStream

    fun onCreate() {
        onDestroy()
        injectorProc = makeProc()
        outputStream = DataOutputStream(injectorProc.outputStream)

        val stream = context.resources.openRawResource(R.raw.keyinjector).bufferedReader()
        stream.forEachLine {
            outputStream.writeBytes("$it\n")
            outputStream.flush()
        }
    }

    fun makeProc() = Runtime.getRuntime().exec("su -c '/system/bin/sh'")

    fun home() = sendKeycode(KeyEvent.KEYCODE_HOME)
    fun recents() = sendKeycode(KeyEvent.KEYCODE_APP_SWITCH)
    fun back() = sendKeycode(KeyEvent.KEYCODE_BACK)
    fun switch() = sendRepeatKeycode(KeyEvent.KEYCODE_APP_SWITCH)
    fun split() = sendLongKeycode(KeyEvent.KEYCODE_APP_SWITCH)
    fun power() = sendLongKeycode(KeyEvent.KEYCODE_POWER)
    fun lock() = sendKeycode(KeyEvent.KEYCODE_POWER)

    fun sendKeycode(code: Int) {
        sendCommand("$code")
    }

    fun sendRepeatKeycode(code: Int) {
        sendCommand("$code --repeat")
    }

    fun sendLongKeycode(code: Int) {
        sendCommand("$code --longpress")
    }

    fun sendCommand(command: String) {
        if (!hasProc()) onCreate()
        try {
            outputStream.writeBytes("$command\n")
            outputStream.flush()
        } catch (e: Exception) {}
    }

    fun onDestroy() {
        if (this::outputStream.isInitialized) {
            try {
                injectorProc.destroy()
                outputStream.close()
            } catch (e: Exception) {}
        }
        Shell.su("killall -9 NoBarKey").submit()
    }

    fun hasProc(): Boolean {
        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) injectorProc.isAlive
        else try {
            outputStream.writeBytes("\n")
            true
        } catch (e: Exception) {
            false
        }
    }
}