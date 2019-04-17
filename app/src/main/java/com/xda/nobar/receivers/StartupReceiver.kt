package com.xda.nobar.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xda.nobar.BuildConfig
import com.xda.nobar.services.Actions

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
                try {
                    val service = Intent(context, Actions::class.java)
                    context.startService(service)
                } catch (e: Exception) {}
            }
        }
    }
}