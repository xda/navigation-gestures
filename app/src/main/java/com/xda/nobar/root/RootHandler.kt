package com.xda.nobar.root

import android.annotation.SuppressLint
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import com.xda.nobar.BuildConfig
import com.xda.nobar.RootActions
import com.xda.nobar.util.inputManager
import eu.chainfire.librootjava.RootIPC
import eu.chainfire.librootjava.RootJava

@SuppressLint("PrivateApi")
object RootHandler {
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private val im by lazy { inputManager }

    @JvmStatic
    fun main(args: Array<String>) {
        RootJava.restoreOriginalLdLibraryPath()

        val actions = RootActionsImpl()

        try {
            RootIPC(BuildConfig.APPLICATION_ID, actions, 200, 30 * 1000, true)
        } catch (e: RootIPC.TimeoutException) {
            e.printStackTrace()
        }

    }

    class RootActionsImpl : RootActions.Stub() {
        override fun sendKeyEvent(code: Int) {
            val now = SystemClock.uptimeMillis()

            injectKeyEvent(createKeyEvent(
                    now,
                    KeyEvent.ACTION_DOWN,
                    code,
                    0, 0
            ))
            injectKeyEvent(createKeyEvent(
                    now,
                    KeyEvent.ACTION_UP,
                    code,
                    0, 0
            ))
        }

        override fun sendDoubleKeyEvent(code: Int) {
            sendKeyEvent(code)

            handler.postDelayed({ sendKeyEvent(code) }, 10)
        }

        override fun sendLongKeyEvent(code: Int) {
            val now = SystemClock.uptimeMillis()

            injectKeyEvent(createKeyEvent(now, KeyEvent.ACTION_DOWN, code, 0, 0))
            injectKeyEvent(createKeyEvent(now, KeyEvent.ACTION_DOWN, code, 1, KeyEvent.FLAG_LONG_PRESS))
            injectKeyEvent(createKeyEvent(now, KeyEvent.ACTION_UP, code, 0, 0))
        }

        override fun lockScreen() {
            sendKeyEvent(KeyEvent.KEYCODE_POWER)
        }

        override fun screenshot() {
            sendKeyEvent(KeyEvent.KEYCODE_SYSRQ)
        }

        private fun injectKeyEvent(event: KeyEvent) {
            im.injectInputEvent(
                    event,
                    InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH)
        }

        private fun createKeyEvent(now: Long, action: Int, code: Int, repeat: Int, flags: Int): KeyEvent {
            return KeyEvent(
                    now, now,
                    action, code,
                    repeat, 0,
                    KeyCharacterMap.VIRTUAL_KEYBOARD,
                    0, flags,
                    InputDevice.SOURCE_KEYBOARD
            )
        }
    }
}
