package com.xda.nobar.util

import android.annotation.SuppressLint
import android.app.StatusBarManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.support.annotation.RequiresApi
import android.view.KeyEvent
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.activities.DialogActivity

class RootActionHandler(private val app: App) {
    private val handler = Handler(Looper.getMainLooper())

    fun goBack() {
        keyEvent(KeyEvent.KEYCODE_BACK)
    }

    fun goHome() {
        keyEvent(KeyEvent.KEYCODE_HOME)
    }

    fun goForward() {
        keyEvent(KeyEvent.KEYCODE_FORWARD)
    }

    fun goRecents() {
        keyEvent(KeyEvent.KEYCODE_APP_SWITCH)
    }

    @RequiresApi(24)
    fun goSplitScreen() {
        holdKeyEvent(KeyEvent.KEYCODE_APP_SWITCH)
    }

    fun goMenu() {
        keyEvent(KeyEvent.KEYCODE_MENU)
    }

    @RequiresApi(24)
    fun goPreviousApp() {
        doubleKeyEvent(KeyEvent.KEYCODE_APP_SWITCH)
    }

    fun goAssistant() {
        holdKeyEvent(KeyEvent.KEYCODE_HOME)
    }

    fun goPremiumPlayPause() {
        runPremiumAction { keyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) }
    }

    fun goPremiumPrevious() {
        runPremiumAction { keyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS) }
    }

    fun goPremiumNext() {
        runPremiumAction { keyEvent(KeyEvent.KEYCODE_MEDIA_NEXT) }
    }

    fun goPremiumScreenshot() {
        runPremiumAction { keyEvent(KeyEvent.KEYCODE_SYSRQ) }
    }

    @SuppressLint("WrongConstant")
    fun goPremiumNotifications() {
        runPremiumAction {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
                SuUtils.sudo("cmd statusbar expand-notifications")
            } else {
                val sbm = app.getSystemService(Context.STATUS_BAR_SERVICE) as StatusBarManager
                sbm.expandNotificationsPanel()
            }
        }
    }

    @SuppressLint("WrongConstant")
    fun goPremiumQs() {
        runPremiumAction {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
                SuUtils.sudo("cmd statusbar expand-settings")
            } else {
                val sbm = app.getSystemService(Context.STATUS_BAR_SERVICE) as StatusBarManager
                sbm.expandSettingsPanel()
            }
        }
    }

    private fun keyEvent(event: Int) {
        SuUtils.sudo("input keyevent $event")
    }

    private fun holdKeyEvent(event: Int) {
        SuUtils.sudo("input keyevent --longpress $event")
    }

    private fun doubleKeyEvent(event: Int) {
        keyEvent(event)
        handler.postDelayed({ keyEvent(event) }, 100)
    }

    private fun runPremiumAction(action: () -> Unit) {
        if (app.isValidPremium) action.invoke()
        else {
            DialogActivity.Builder(app).apply {
                title = R.string.premium_required
                message = R.string.premium_required_desc
                showYes = true
                showNo = true
                yesUrl = "https://play.google.com/store/apps/details?id=com.xda.nobar.premium"
                start()
            }
        }
    }
}