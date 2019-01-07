package com.xda.nobar.services

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.xda.nobar.interfaces.ReceiverCallback
import com.xda.nobar.receivers.ActionReceiver
import com.xda.nobar.receivers.StartupReceiver
import com.xda.nobar.util.actionHolder
import com.xda.nobar.util.runNougatAction
import com.xda.nobar.util.runPremiumAction


class Actions : AccessibilityService(), ReceiverCallback {
    companion object {
        const val BASE = "com.xda.nobar.action"
        const val ACTION = "$BASE.ACTION"

        const val EXTRA_ACTION = "action"
        const val EXTRA_GESTURE = "gesture"

        fun sendAction(context: Context, action: String, options: Bundle) {
            val intent = Intent(action)
            intent.putExtras(options)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
    }

    private val receiver = ActionHandler()

    private val handler = Handler()

    override fun onServiceConnected() {
        receiver.register(this, this)

        val bc = Intent(this, StartupReceiver::class.java)
        bc.action = StartupReceiver.ACTION_ACTIVATE
        sendBroadcast(bc)

        val foreground = Intent(this, ForegroundService::class.java)
        ContextCompat.startForegroundService(this, foreground)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val newEvent = AccessibilityEvent.obtain(event)

        ActionReceiver.handleEvent(this, newEvent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        receiver.onReceive(this, intent)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onActionReceived(intent: Intent?) {
        when(intent?.action) {
            ACTION -> {
                when (intent.getIntExtra(EXTRA_ACTION, actionHolder.typeNoAction)) {
                    actionHolder.typeHome -> {
                        performGlobalAction(GLOBAL_ACTION_HOME)
                    }
                    actionHolder.typeRecents -> {
                        performGlobalAction(GLOBAL_ACTION_RECENTS)
                    }
                    actionHolder.typeBack -> performGlobalAction(GLOBAL_ACTION_BACK)
                    actionHolder.typeSwitch -> runNougatAction {
                        performGlobalAction(GLOBAL_ACTION_RECENTS)
                        handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_RECENTS) }, 100)
                    }
                    actionHolder.typeSplit -> runNougatAction { performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN) }
                    actionHolder.premTypePower -> runPremiumAction { performGlobalAction(GLOBAL_ACTION_POWER_DIALOG) }
                }
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        receiver.destroy(this)
    }

    /**
     * Special BroadcastReceiver to handle actions sent to this service by {@link com.xda.nobar.views.BarView}
     */
    class ActionHandler : BroadcastReceiver() {
        private var callback: ReceiverCallback? = null

        fun register(context: Context, callback: ReceiverCallback) {
            this.callback = callback

            val filter = IntentFilter()
            filter.addAction(ACTION)

            LocalBroadcastManager.getInstance(context).registerReceiver(this, filter)
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            callback?.onActionReceived(intent)
        }

        fun destroy(context: Context) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(this)
        }
    }
}