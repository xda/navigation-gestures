package com.xda.nobar.root

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.WindowManager
import com.xda.nobar.BuildConfig
import com.xda.nobar.RootActions
import com.xda.nobar.util.inputManager
import eu.chainfire.librootjava.RootIPC
import eu.chainfire.librootjava.RootJava

object RootHandler {
    private val handler by lazy { Handler() }
    private val screenshotHelper by lazy { ScreenshotHelper(RootJava.getSystemContext()) }

    @JvmStatic
    fun main(args: Array<String>) {
        Looper.prepare()

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

            val event = createKeyEvent(now, KeyEvent.ACTION_DOWN, code, 0, KeyEvent.FLAG_LONG_PRESS)
            injectKeyEvent(event)

            injectKeyEvent(createKeyEvent(now, KeyEvent.ACTION_UP, code, 0, 0))
        }

        override fun lockScreen() {
            sendKeyEvent(KeyEvent.KEYCODE_POWER)
        }

        override fun screenshot() {
            screenshotHelper.takeScreenshot(WindowManager.TAKE_SCREENSHOT_FULLSCREEN, true, true, handler)
        }

        private fun injectKeyEvent(event: KeyEvent) {
            inputManager.injectInputEvent(
                    event,
                    0)
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
