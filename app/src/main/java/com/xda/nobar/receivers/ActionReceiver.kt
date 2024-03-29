package com.xda.nobar.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.xda.nobar.util.app
import com.xda.nobar.util.mainScope
import kotlinx.coroutines.launch

class ActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_TOGGLE_SCREEN_ON = "toggle_screen_on"
        const val ACTION_SCREEN_OFF = "screen_off"
        const val ACTION_TOGGLE_NAV = "toggle_nav"

        fun toggleScreenOn(context: Context) {
            sendIntent(context, ACTION_TOGGLE_SCREEN_ON)
        }

        fun turnScreenOff(context: Context) {
            sendIntent(context, ACTION_SCREEN_OFF)
        }

        fun toggleNav(context: Context) {
            sendIntent(context, ACTION_TOGGLE_NAV)
        }

        private fun sendIntent(context: Context, action: String, extras: Bundle? = null) {
            mainScope.launch {
                val intent = Intent(context, ActionReceiver::class.java)
                intent.action = action
                if (extras != null) intent.putExtras(extras)

                try {
                    context.sendBroadcast(intent)
                } catch (e: Exception) {}
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE_SCREEN_ON -> context.app.toggleScreenOn()
            ACTION_SCREEN_OFF -> context.app.screenOffHelper.create()
            ACTION_TOGGLE_NAV -> context.app.toggleNavState()
        }
    }
}