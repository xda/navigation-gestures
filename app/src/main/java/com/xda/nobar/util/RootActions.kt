package com.xda.nobar.util

import android.content.Context
import android.view.KeyEvent
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

    fun sendKeycode(code: Int) {
        outputStream.writeBytes("$code\n")
        outputStream.flush()
    }

    fun sendRepeatKeycode(code: Int) {
        outputStream.writeBytes("$code --repeat\n")
        outputStream.flush()
    }

    fun sendLongKeycode(code: Int) {
        outputStream.writeBytes("$code --longpress\n")
        outputStream.flush()
    }

    fun onDestroy() {
        if (this::outputStream.isInitialized) {
            try {
                outputStream.writeBytes("exit\n")
                outputStream.flush()
                injectorProc.waitFor()
                outputStream.close()
            } catch (e: Exception) {}
        }
    }
}