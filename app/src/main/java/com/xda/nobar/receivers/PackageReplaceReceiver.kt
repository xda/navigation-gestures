package com.xda.nobar.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xda.nobar.activities.IntroActivity
import com.xda.nobar.util.Utils

/**
 * Receive power-related broadcasts and act appropriately
 */
class PackageReplaceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = Utils.getHandler(context)
        when (intent.action) {
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                if (app.areGesturesActivated() && !IntroActivity.needsToRun(context)) app.addBar()
            }
        }
    }
}
