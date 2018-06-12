package com.xda.nobar.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity

class LockScreenActivity : AppCompatActivity() {
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                performDestroy()
            }
        }
    }
    private val previousSettings = LockSettings()

    private var destroying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setup()
    }

    override fun onPause() {
        super.onPause()
        performDestroy()
    }

    private fun setup() {
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

        val currentBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, -1)
        val currentBrightnessMode = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, -1)
        val currentTimeout = Settings.System.getInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, -1)
        val keepScreenOn = Settings.Global.getInt(contentResolver, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, -1)

        previousSettings.brightness = currentBrightness
        previousSettings.brightnessMode = currentBrightnessMode
        previousSettings.timeout = currentTimeout
        previousSettings.keepScreenOn = keepScreenOn

        saveSettings(LockSettings(0, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL, 1000, 0))
    }

    private fun performDestroy() {
        if (!destroying) {
            destroying = true

            unregisterReceiver(screenOffReceiver)
            saveSettings(previousSettings)
            finish()
        }
    }

    private fun saveSettings(settings: LockSettings) {
        try {
            if (settings.brightness != -1) Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, settings.brightness)
            else Settings.System.putString(contentResolver, Settings.System.SCREEN_BRIGHTNESS, null)
            if (settings.brightness != -1) Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, settings.brightnessMode)
            else Settings.System.putString(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, null)
            if (settings.brightness != -1) Settings.System.putInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, settings.timeout)
            else Settings.System.putString(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, null)
        } catch (e: SecurityException) {
            performDestroy()
        }

        try {
            if (settings.keepScreenOn != -1) Settings.Global.putInt(contentResolver, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, settings.keepScreenOn)
            else Settings.Global.putString(contentResolver, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, null)
        } catch (e: SecurityException) {

        }
    }

    private class LockSettings(var brightness: Int = -1, var brightnessMode: Int = -1, var timeout: Int = -1, var keepScreenOn: Int = -1)
}
