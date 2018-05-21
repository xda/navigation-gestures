package com.xda.nobar.receivers

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xda.nobar.util.Utils

/**
 * Receive power-related broadcasts and act appropriately
 */
class PowerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_REBOOT -> {
                Utils.getHandler(context).toggle(true)
                Utils.setOffForRebootOrScreenLock(context, true)
            }
            Intent.ACTION_SHUTDOWN -> {
                Utils.getHandler(context).toggle(true)
                Utils.setOffForRebootOrScreenLock(context, true)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                if (km.isKeyguardLocked) Utils.getHandler(context).toggle(true)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val handler = Utils.getHandler(context)
                if (handler.isActivated()) handler.addBar()
            }
        }
    }
}
