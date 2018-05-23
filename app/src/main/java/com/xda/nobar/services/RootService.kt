package com.xda.nobar.services

import android.annotation.SuppressLint
import android.app.Service
import android.app.StatusBarManager
import android.content.Context
import android.content.Intent
import android.os.*
import android.view.KeyEvent
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.activities.DialogActivity
import java.io.DataOutputStream

class RootService : Service() {
    val handler = Handler(Looper.getMainLooper())
    var su: java.lang.Process? = null

    override fun onBind(intent: Intent): IBinder {
        return RootBinder(this)
    }

    override fun onCreate() {
        super.onCreate()
        Thread {
            su = Runtime.getRuntime().exec("su")
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        su?.outputStream?.close()
        su?.destroy()
        su = null
    }

    class RootBinder(val service: RootService) : Binder() {
        private val app = service.application as App

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

        fun goSplitScreen() {
            runNougatAction { holdKeyEvent(KeyEvent.KEYCODE_APP_SWITCH) }
        }

        fun goMenu() {
            keyEvent(KeyEvent.KEYCODE_MENU)
        }

        fun goPreviousApp() {
            runNougatAction { doubleKeyEvent(KeyEvent.KEYCODE_APP_SWITCH) }
        }

        fun goAssistant() {
            holdKeyEvent(KeyEvent.KEYCODE_HOME)
        }

        fun goOhm() {
            val ohm = Intent("com.xda.onehandedmode.intent.action.TOGGLE_OHM")
            ohm.setClassName("com.xda.onehandedmode", "com.xda.onehandedmode.receivers.OHMReceiver")
            app.sendBroadcast(ohm)
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
                    command("cmd statusbar expand-notifications")
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
                    command("cmd statusbar expand-settings")
                } else {
                    val sbm = app.getSystemService(Context.STATUS_BAR_SERVICE) as StatusBarManager
                    sbm.expandSettingsPanel()
                }
            }
        }

        fun goPremiumPower() {
            runPremiumAction {
                holdKeyEvent(KeyEvent.KEYCODE_POWER)
            }
        }

        fun goPremiumVolDown() {
            runPremiumAction {
                keyEvent(KeyEvent.KEYCODE_VOLUME_DOWN)
            }
        }

        fun goPremiumVolUp() {
            runPremiumAction {
                keyEvent(KeyEvent.KEYCODE_VOLUME_UP)
            }
        }

        private fun keyEvent(event: Int) {
            command("input keyevent $event touchnavigation")
        }

        private fun holdKeyEvent(event: Int) {
            command("input keyevent --longpress $event touchnavigation")
        }

        private fun doubleKeyEvent(event: Int) {
            keyEvent(event)
            service.handler.postDelayed({ keyEvent(event) }, 100)
        }

        private fun runPremiumAction(action: () -> Unit) {
            if (app.isValidPremium) action.invoke()
            else {
                DialogActivity.Builder(service).apply {
                    title = R.string.premium_required
                    message = R.string.premium_required_desc
                    showYes = true
                    showNo = true
                    yesUrl = "https://play.google.com/store/apps/details?id=com.xda.nobar.premium"
                    start()
                }
            }
        }

        private fun runNougatAction(action: () -> Unit) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                action.invoke()
            } else {
                DialogActivity.Builder(service).apply {
                    title = R.string.nougat_required
                    message = R.string.nougat_required_desc
                    showYes = true
                    yesRes = android.R.string.ok
                    start()
                }
            }
        }

        private fun command(vararg commands: String) {
            Thread {
                val outputStream = DataOutputStream(service.su?.outputStream)

                for (s in commands) {
                    outputStream.writeBytes(s + "\n")
                    outputStream.flush()
                }
            }.start()
        }
    }
}
