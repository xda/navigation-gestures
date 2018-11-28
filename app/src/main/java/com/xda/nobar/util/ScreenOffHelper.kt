package com.xda.nobar.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import android.widget.LinearLayout
import com.xda.nobar.App

/**
 * Use this class to lock the screen
 * Before Android P, Android only has 3 ways to lock the screen by a third party:
 *     - Root (input keyevent POWER)
 *     - Device Admin — blocks fingerprint unlock
 *     - Screen timeout
 * NoBar uses the third option.
 * When calling create(), this class shows a black screen.
 * It also does the following:
 *     - Screen timeout is set to 1 second (the Android framework may force this higher, up to 10 seconds total)
 *     - Auto brightness is deactivated
 *     - Screen brightness is set to 0 (the Android framework may force this higher)
 *     - If WRITE_SECURE_SETTINGS is granted, the DevOp "Stay Awake" is deactivated
 * Once destroy() is called, either by screen off or the user forcing their way out of it,
 * each setting gets reverted to its original value.
 */

class ScreenOffHelper(private val app: App) {
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                destroy()
            }
        }
    }
    private val previousSettings = LockSettings()
    private val lockView = LockView(app)

    private var destroying = false

    fun create() {
        destroying = false
        lockView.add()
        app.registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

        previousSettings.brightness = Settings.System.getInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS, -1)
        previousSettings.brightnessMode = Settings.System.getInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, -1)
        previousSettings.timeout = Settings.System.getInt(app.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, -1)
        previousSettings.keepScreenOn = Settings.Global.getInt(app.contentResolver, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, -1)

        saveSettings(LockSettings(0, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL, 1000, 0))

        if (app.prefManager.isActive) app.removeBar(false)
    }

    private fun destroy() {
        if (!destroying) {
            destroying = true
            saveSettings(previousSettings)
            lockView.remove()

            if (app.prefManager.isActive && !app.pillShown) app.addBar(false)

            app.unregisterReceiver(screenOffReceiver)
        }
    }

    private fun saveSettings(settings: LockSettings) {
        try {
            if (settings.brightnessMode != -1) Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, settings.brightnessMode)
            else Settings.System.putString(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, null)
            if (settings.brightness != -1) Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS, settings.brightness)
            else Settings.System.putString(app.contentResolver, Settings.System.SCREEN_BRIGHTNESS, null)
            if (settings.timeout != -1) Settings.System.putInt(app.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, settings.timeout)
            else Settings.System.putString(app.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, null)
        } catch (e: SecurityException) {
            destroy()
        }

        try {
            if (settings.keepScreenOn != -1) Settings.Global.putInt(app.contentResolver, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, settings.keepScreenOn)
            else Settings.Global.putString(app.contentResolver, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, null)
        } catch (e: SecurityException) {}
    }

    /**
     * Helper class to consolidate the changed values, and make them easier to change/restore
     */
    private class LockSettings(var brightness: Int = -1, var brightnessMode: Int = -1, var timeout: Int = -1, var keepScreenOn: Int = -1)

    class LockView(context: Context) : LinearLayout(context) {
        private val params = WindowManager.LayoutParams().apply {
            val size = Utils.getRealScreenSize(context)
            width = size.x
            height = size.y
            flags = WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            type = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_PHONE else WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        init {
            setBackgroundColor(Color.BLACK)
        }

        fun add() {
            try {
                wm.removeView(this)
            } catch (e: Exception) {}

            try {
                wm.addView(this, params)
            } catch (e: Exception) {}
        }

        fun remove() {
            try {
                wm.removeView(this)
            } catch (e: Exception) {}
        }
    }
}