package com.xda.nobar.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat
import com.xda.nobar.services.ForegroundService

class StartupReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_ACTIVATE = "com.xda.nobar.intent.action.ACTIVATE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_LOCKED_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                ACTION_ACTIVATE -> {
                val service = Intent(context, ForegroundService::class.java)
                ContextCompat.startForegroundService(context, service)
            }
        }
    }
}