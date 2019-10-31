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
import kotlinx.coroutines.launch


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

    private fun addBar() = mainScope.launch {
        if (!waitingToAdd && !app.bar.isAttachedToWindow) {
            waitingToAdd = true
            try {
                accWm.addView(app.bar, app.bar.params)
            } catch (e: Exception) {}
            waitingToAdd = false
        }
    }

    private fun addBlackout() = mainScope.launch {
        val ovsc = (prefManager.shouldUseOverscanMethod && !prefManager.useFullOverscan)
        if ((prefManager.isActive && prefManager.overlayNav) || ovsc) {
            if (prefManager.overlayNavBlackout || ovsc) {
                if (!app.blackout.isAttachedToWindow) {
                    app.blackout.add(accWm).join()
                    app.blackout.setGone(accWm, false).join()
                }
            } else {
                addBar().join()
            }
        }
    }

    private fun removeBar() = mainScope.launch {
        if (app.bar.isAttachedToWindow) {
            try {
                accWm.removeView(app.bar)
            } catch (e: Exception) {}
        }
    }

    private fun remBlackout(forRefresh: Boolean = false) = mainScope.launch {
        if (app.blackout.isAttachedToWindow) {
            app.blackout.setGone(accWm, gone = true, instant = forRefresh).join()
            app.blackout.remove(accWm, forRefresh).join()
        }
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
            this@Actions.addBlackout()
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

        private var alreadyAdding = false

        override fun addBarAndBlackout() {
            mainScope.launch {
                if (!alreadyAdding) {
                    alreadyAdding = true
                    if (app.blackout.isAdded) {
                        this@Actions.remBlackout(true).join()
                    } else {
                        this@Actions.addBlackout().join()
                    }
                    if (!prefManager.overlayNav) {
                        this@Actions.addBar().join()
                    }
                    alreadyAdding = false
                }
            }
        }

        override fun remBarAndBlackout() {
            mainScope.launch {
                if (app.bar.isAttachedToWindow || app.blackout.isAttachedToWindow) {
                    this@Actions.remBlackout().join()
                    this@Actions.removeBar().join()
                }
            }
        }

        override fun addLeftSide() {
            app.leftSide.add(accWm)
        }

        override fun addRightSide() {
            app.rightSide.add(accWm)
        }

        override fun remLeftSide() {
            app.leftSide.remove(accWm)
        }

        override fun remRightSide() {
            app.rightSide.remove(accWm)
        }

        override fun setBlackoutGone(gone: Boolean) {
            app.blackout.setGone(accWm, gone)
        }
    }
}