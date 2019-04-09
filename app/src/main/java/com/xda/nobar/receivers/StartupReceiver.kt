package com.xda.nobar.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.xda.nobar.BuildConfig
import com.xda.nobar.services.ForegroundService

class StartupReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_RELAUNCH = "${BuildConfig.APPLICATION_ID}.action.RELAUNCH"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            ACTION_RELAUNCH -> {
                val service = Intent(context, ForegroundService::class.java)
                ContextCompat.startForegroundService(context, service)
            }
        }
    }
}