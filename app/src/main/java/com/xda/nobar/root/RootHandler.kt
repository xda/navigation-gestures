package com.xda.nobar.root

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.hardware.input.InputManager
import android.os.Looper
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.accessibility.AccessibilityManager
import com.xda.nobar.BuildConfig
import com.xda.nobar.RootActions
import com.xda.nobar.util.inputManager
import eu.chainfire.librootjava.RootIPC
import eu.chainfire.librootjava.RootJava
import eu.chainfire.libsuperuser.Shell

@SuppressLint("PrivateApi")
object RootHandler {
    private val context: Context
        get() = RootJava.getSystemContext()

    private val im by lazy { inputManager }
    private val am by lazy { context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager }
    private val accm by lazy { context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager }

    @JvmStatic
    fun main(args: Array<String>) {
        RootJava.restoreOriginalLdLibraryPath()

        if (Looper.myLooper() == null) Looper.prepare()

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
            sendKeyEvent(code)
        }

        override fun sendLongKeyEvent(code: Int) {
            val now = SystemClock.uptimeMillis()

            injectKeyEvent(createKeyEvent(now, KeyEvent.ACTION_DOWN, code, 0, 0))
            injectKeyEvent(createKeyEvent(now, KeyEvent.ACTION_DOWN, code, 1, 0))
//            injectKeyEvent(createKeyEvent(now, KeyEvent.ACTION_UP, code, 0, 0))
        }

        override fun lockScreen() {
            sendKeyEvent(KeyEvent.KEYCODE_POWER)
        }

        override fun screenshot() {
            sendKeyEvent(KeyEvent.KEYCODE_SYSRQ)
        }

        override fun killCurrentApp() {
            val info = am.getRunningTasks(1)[0]
            am.forceStopPackage(info.topActivity.packageName)

            //this can be used to launch split screen (wrap in try-catch)
//            am.setTaskWindowingModeSplitScreenPrimary(info.id, ActivityManager.SPLIT_SCREEN_CREATE_MODE_TOP_OR_LEFT, true,
//                    true, null, true)
        }

        override fun notifyAccessibilityButtonClicked() {
            accm.notifyAccessibilityButtonClicked()
        }

        override fun launchAccessibilityButtonChooser() {
            Shell.SU.run("am start-activity -a ${AccessibilityManager.ACTION_CHOOSE_ACCESSIBILITY_BUTTON} -f ${Intent.FLAG_ACTIVITY_NEW_TASK} -f ${Intent.FLAG_ACTIVITY_CLEAR_TASK}")
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
