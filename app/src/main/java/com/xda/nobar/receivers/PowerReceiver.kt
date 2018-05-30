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
        val app = Utils.getHandler(context)
        when (intent.action) {
            Intent.ACTION_REBOOT -> {
                app.showNav()
            }
            Intent.ACTION_SHUTDOWN -> {
                app.showNav()
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                if (km.isKeyguardLocked) app.showNav()
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                if (app.areGesturesActivated()) app.addBar()
            }
        }
    }
}
