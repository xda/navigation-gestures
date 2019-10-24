package com.xda.nobar.services

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.xda.nobar.IActionsBinder
import com.xda.nobar.util.*


class Actions : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        const val BASE = "com.xda.nobar.action"

        const val ACTIONS_STARTED = "$BASE.STARTED"
        const val ACTIONS_STOPPED = "$BASE.STOPPED"

        const val EXTRA_ACTION = "action"
        const val EXTRA_GESTURE = "gesture"
        const val EXTRA_BINDER = "actions_binder"
        const val EXTRA_BUNDLE = "bundle"
    }

    private val accWm: WindowManager
        get() = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val binder by lazy { IActionsBinderImpl() }

    override fun onServiceConnected() {
        prefManager.registerOnSharedPreferenceChangeListener(this)
        sendBound()
        loadInfo()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val newEvent = AccessibilityEvent.obtain(event)

        app.uiHandler.setNodeInfoAndUpdate(newEvent, this)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        handleDestroy()
        prefManager.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        handleDestroy()

        return super.onUnbind(intent)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PrefManager.ACCESSIBILITY_DELAY -> {
                loadInfo()
            }
        }
    }

    private fun handleDestroy() {
        sendUnbound()

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
                mainHandler.postDelayed({ performGlobalAction(GLOBAL_ACTION_RECENTS) }, prefManager.switchAppDelay.toLong())
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
            app.leftSide.add(accWm)
            app.rightSide.add(accWm)
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
            app.leftSide.remove(accWm)
            app.rightSide.remove(accWm)
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

    private fun loadInfo() {
        val info = serviceInfo
        info.notificationTimeout = prefManager.accessibilityDelay.toLong()

        serviceInfo = info
    }

    inner class IActionsBinderImpl : IActionsBinder.Stub() {
        override fun addBar() {
            this@Actions.addBar()
        }

        override fun addBlackout() {
            val ovsc = (prefManager.shouldUseOverscanMethod && !prefManager.useFullOverscan)
            if ((prefManager.isActive && prefManager.overlayNav) || ovsc) {
                if (prefManager.overlayNavBlackout || ovsc) {
                    this@Actions.addBlackout()
                } else {
                    this@Actions.addBar()
                }
            }
        }

        override fun remBar() {
            this@Actions.removeBar()
        }

        override fun remBlackout() {
            this@Actions.remBlackout()
        }

        override fun addImmersiveHelper() {
            this@Actions.addImmersiveHelper()
        }

        override fun removeImmersiveHelper() {
            this@Actions.removeImmersiveHelper()
        }

        override fun sendAction(action: Int) {
            this@Actions.sendAction(action)
        }

        override fun addBarAndBlackout() {
            remBlackout()
            addBlackout()
            if (!prefManager.overlayNav) {
                addBar()
            }
        }

        override fun remBarAndBlackout() {
            remBlackout()
            remBar()
        }
    }
}