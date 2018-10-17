package com.xda.nobar.services

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.view.accessibility.AccessibilityEvent
import com.xda.nobar.interfaces.ReceiverCallback
import com.xda.nobar.receivers.ActionReceiver
import com.xda.nobar.receivers.StartupReceiver
import com.xda.nobar.util.ActionHolder
import com.xda.nobar.util.RootActions
import com.xda.nobar.util.Utils


/**
 * Where most of the magic happens
 */
class Actions : AccessibilityService(), ReceiverCallback {
    companion object {
        const val BASE = "com.xda.nobar.action"
        const val ACTION = "$BASE.ACTION"
        const val PREMIUM_UPDATE = "$BASE.PREM_UPDATE"

        const val EXTRA_ACTION = "action"
        const val EXTRA_GESTURE = "gesture"
        const val EXTRA_PREM = "premium"
        const val EXTRA_ALT_HOME = "alt_home"
        const val EXTRA_PACKAGE = "package_name"
        const val EXTRA_ACTIVITY = "activity_name"
        const val EXTRA_INTENT_KEY = "intent_key"

        fun updatePremium(context: Context, premium: Boolean) {
            val options = Bundle()
            options.putBoolean(EXTRA_PREM, premium)
            sendAction(context, PREMIUM_UPDATE, options)
        }

        fun sendAction(context: Context, action: String, options: Bundle) {
            val intent = Intent(action)
            intent.putExtras(options)
            context.sendBroadcast(intent)
        }
    }

    private val receiver by lazy { ActionHandler() }
    private val rootActions by lazy { RootActions(this) }

    private val handler = Handler()

    private var validPremium = false

    override fun onCreate() {
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
                val actionHolder = ActionHolder(this)
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
            PREMIUM_UPDATE -> {
                validPremium = intent.getBooleanExtra(EXTRA_PREM, validPremium)
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        receiver.destroy(this)
        rootActions.onDestroy()
    }

    private fun runNougatAction(action: () -> Unit) = Utils.runNougatAction(this, action)
    private fun runPremiumAction(action: () -> Unit) = Utils.runPremiumAction(this, validPremium, action)

    /**
     * Special BroadcastReceiver to handle actions sent to this service by {@link com.xda.nobar.views.BarView}
     */
    class ActionHandler : BroadcastReceiver() {
        private var callback: ReceiverCallback? = null

        fun register(context: Context, callback: ReceiverCallback) {
            this.callback = callback

            val filter = IntentFilter()
            filter.addAction(ACTION)
            filter.addAction(PREMIUM_UPDATE)

            context.registerReceiver(this, filter, com.xda.nobar.Manifest.permission.SEND_BROADCAST, null)
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            callback?.onActionReceived(intent)
        }

        fun destroy(context: Context) {
            context.unregisterReceiver(this)
        }
    }
}