package com.xda.nobar.services

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.*
import android.view.KeyEvent
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.activities.DialogActivity
import com.xda.nobar.util.SuUtils
import java.io.DataOutputStream

/**
 * Supplemental actions for rooted devices
 */
class RootService : Service() {
    val handler = Handler(Looper.getMainLooper())
    var su: java.lang.Process? = null

    override fun onBind(intent: Intent): IBinder {
        return RootBinder(this)
    }

    override fun onCreate() {
        super.onCreate()
        Thread {
            su = SuUtils.getSudo()
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

        fun handle(which: Int) {
            when (which) {
                app.typeRootHoldBack -> goHoldBack()
                app.typeRootForward -> goForward()
                app.typeRootMenu -> goMenu()
                app.typeRootSleep -> goScreenOff()
                app.premTypeRootVolDown -> goPremiumVolDown()
                app.premTypeRootVolUp -> goPremiumVolUp()
            }
        }

        private fun goHoldBack() {
            holdKeyEvent(KeyEvent.KEYCODE_BACK)
        }

        private fun goForward() {
            keyEvent(KeyEvent.KEYCODE_FORWARD)
        }

        private fun goMenu() {
            keyEvent(KeyEvent.KEYCODE_MENU)
        }

        private fun goScreenOff() {
            keyEvent(KeyEvent.KEYCODE_POWER)
        }

        private fun goPremiumVolDown() {
            runPremiumAction {
                keyEvent(KeyEvent.KEYCODE_VOLUME_DOWN)
            }
        }

        private fun goPremiumVolUp() {
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
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://play.google.com/store/apps/details?id=com.xda.nobar.premium")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    service.startActivity(intent)
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
                    yesRes = android.R.string.ok
                    start()
                }
            }
        }

        private fun command(vararg commands: String) {
            Thread {
                val outputStream = DataOutputStream(service.su?.outputStream ?: return@Thread)

                for (s in commands) {
                    outputStream.writeBytes(s + "\n")
                    outputStream.flush()
                }
            }.start()
        }
    }
}
