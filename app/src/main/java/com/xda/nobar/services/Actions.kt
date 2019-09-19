package com.xda.nobar.services

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Bundle
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.xda.nobar.interfaces.ReceiverCallback
import com.xda.nobar.util.*


class Actions : AccessibilityService(), ReceiverCallback {
    companion object {
        const val BASE = "com.xda.nobar.action"
        const val ACTION = "$BASE.ACTION"

        const val ADD_BAR = "$BASE.ADD_BAR"
        const val REM_BAR = "$BASE.REM_BAR"

        const val ACTIONS_STARTED = "$BASE.STARTED"
        const val ACTIONS_STOPPED = "$BASE.STOPPED"

        const val EXTRA_ACTION = "action"
        const val EXTRA_GESTURE = "gesture"
        const val EXTRA_BINDER = "actions_binder"
        const val EXTRA_BUNDLE = "bundle"

        fun sendAction(context: Context, action: String, options: Bundle) {
            val intent = Intent(action)
            intent.putExtras(options)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }

        fun addBar(context: Context) {
            val intent = Intent(context, Actions::class.java)
            intent.action = ADD_BAR

            context.startService(intent)
        }

        fun remBar(context: Context) {
            val intent = Intent(context, Actions::class.java)
            intent.action = REM_BAR

            context.startService(intent)
        }
    }

    private val receiver = ActionHandler(this)
    private val accWm: WindowManager
        get() = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val binder by lazy { IActionsBinderImpl() }

    override fun onServiceConnected() {
        receiver.register(this)
        sendBound()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val newEvent = AccessibilityEvent.obtain(event)

        app.uiHandler.setNodeInfoAndUpdate(newEvent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        receiver.onReceive(this, intent)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onActionReceived(intent: Intent?) {
        when(intent?.action) {
            ACTION -> {
                sendAction(intent.getIntExtra(EXTRA_ACTION, actionHolder.typeNoAction))
            }
            ADD_BAR -> {
                addBar()
            }
            REM_BAR -> {
                removeBar()
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        handleDestroy()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        handleDestroy()

        return super.onUnbind(intent)
    }

    private fun handleDestroy() {
        sendUnbound()
        receiver.destroy(this)

        removeBar()

        if (!prefManager.isActive) {
            IWindowManager.setOverscan(0, 0, 0, 0)
        }
        relaunch()
    }

    private fun sendBound() {
        val boundIntent = Intent(ACTIONS_STARTED)
        val bundle = Bundle()

        bundle.putBinder(EXTRA_BINDER, binder)
        boundIntent.putExtra(EXTRA_BUNDLE, bundle)

        LocalBroadcastManager.getInstance(this).sendBroadcast(boundIntent)
    }

    private fun sendUnbound() {
        val unboundIntent = Intent(ACTIONS_STOPPED)
        val bundle = Bundle()

        bundle.putBinder(EXTRA_BINDER, binder)
        unboundIntent.putExtra(EXTRA_BUNDLE, bundle)

        LocalBroadcastManager.getInstance(this).sendBroadcast(unboundIntent)
    }

    private fun sendAction(action: Int) {
        when (action) {
            actionHolder.typeHome -> {
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
            actionHolder.typeRecents -> {
                performGlobalAction(GLOBAL_ACTION_RECENTS)
            }
            actionHolder.typeBack -> performGlobalAction(GLOBAL_ACTION_BACK)
            actionHolder.typeSwitch -> runNougatAction {
                performGlobalAction(GLOBAL_ACTION_RECENTS)
                mainHandler.postDelayed({ performGlobalAction(GLOBAL_ACTION_RECENTS) }, 100)
            }
            actionHolder.typeSplit -> runNougatAction { performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN) }
            actionHolder.premTypePower -> runPremiumAction { performGlobalAction(GLOBAL_ACTION_POWER_DIALOG) }
            actionHolder.premTypeScreenshot -> runPremiumAction { runPieAction { performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT) } }
            actionHolder.premTypeLockScreen -> runPremiumAction { runPieAction { performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) } }
        }
    }

    private var waitingToAdd = false

    private fun addBar() {
        if (!waitingToAdd) {
            waitingToAdd = true
            try {
                accWm.addView(app.bar, app.bar.params)
            } catch (e: Exception) {}
            waitingToAdd = false
        }
    }

    private fun addBlackout() {
        app.blackout.add(accWm)
    }

    private var waitingToRemove = false

    private fun removeBar() {
        if (app.pillShown && !waitingToRemove) {
            waitingToRemove = true
            try {
                accWm.removeView(app.bar)
            } catch (e: Exception) {}
            waitingToRemove = false
        }
    }

    private fun remBlackout() {
        app.blackout.remove(accWm)
    }

    private fun addImmersiveHelper() {
        app.immersiveHelperManager.add(accWm)
    }

    private fun removeImmersiveHelper() {
        app.immersiveHelperManager.remove(accWm)
    }

    /**
     * Special BroadcastReceiver to handle actions sent to this service by {@link com.xda.nobar.views.BarView}
     */
    class ActionHandler(private val callback: ReceiverCallback) : BroadcastReceiver() {
        fun register(context: Context) {
            val filter = IntentFilter()
            filter.addAction(ACTION)
            filter.addAction(ADD_BAR)
            filter.addAction(REM_BAR)

            LocalBroadcastManager.getInstance(context.applicationContext).registerReceiver(this, filter)
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            callback.onActionReceived(intent)
        }

        fun destroy(context: Context) {
            LocalBroadcastManager.getInstance(context.applicationContext).unregisterReceiver(this)
        }
    }

    inner class IActionsBinderImpl : Binder() {
        fun addBar() {
            this@Actions.addBar()
        }

        fun addBlackout() {
            val ovsc = (prefManager.shouldUseOverscanMethod && !prefManager.useFullOverscan)
            if ((prefManager.isActive && prefManager.overlayNav) || ovsc) {
                if (prefManager.overlayNavBlackout || ovsc) {
                    this@Actions.addBlackout()
                } else {
                    this@Actions.addBar()
                }
            }
        }

        fun remBar() {
            this@Actions.removeBar()
        }

        fun remBlackout() {
            this@Actions.remBlackout()
        }

        fun addImmersiveHelper() {
            this@Actions.addImmersiveHelper()
        }

        fun removeImmersiveHelper() {
            this@Actions.removeImmersiveHelper()
        }

        fun sendAction(action: Int) {
            this@Actions.sendAction(action)
        }

        fun addBarAndBlackout() {
            remBlackout()
            addBlackout()
            if (!prefManager.overlayNav) {
                addBar()
            }
        }

        fun remBarAndBlackout() {
            remBlackout()
            remBar()
        }
    }
}