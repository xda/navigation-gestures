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
    fun lock() = sendKeycode(KeyEvent.KEYCODE_POWER)

    fun sendKeycode(code: Int) {
        try {
            outputStream.writeBytes("$code\n")
            outputStream.flush()
        } catch (e: Exception) {}
    }

    fun sendRepeatKeycode(code: Int) {
        try {
            outputStream.writeBytes("$code --repeat\n")
            outputStream.flush()
        } catch (e: Exception) {}
    }

    fun sendLongKeycode(code: Int) {
        try {
            outputStream.writeBytes("$code --longpress\n")
            outputStream.flush()
        } catch (e: Exception) {}
    }

    fun sendCommand(command: String) {
        try {
            outputStream.writeBytes("$command\n")
            outputStream.flush()
        } catch (e: Exception) {}
    }

    fun onDestroy() {
        Runtime.getRuntime().exec("killall -9 NoBarKey")
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