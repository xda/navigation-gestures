package com.xda.nobar.services

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.app.SearchManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.support.v4.content.LocalBroadcastManager
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.activities.DialogActivity
import com.xda.nobar.activities.IntroActivity
import com.xda.nobar.util.Utils

/**
 * Where most of the magic happens
 */
class Actions : AccessibilityService() {
    companion object {
        const val BASE = "com.xda.nobar.action"
        const val ACTION = "$BASE.ACTION"

        const val EXTRA_ACTION = "action"
    }

    private lateinit var receiver: ActionHandler
    private lateinit var wm: WindowManager
    private lateinit var am: ActivityManager
    private lateinit var audio: AudioManager
    private lateinit var app: App

    private val handler = Handler(Looper.getMainLooper())

    private var isBarHiddenForLauncher = false

    override fun onCreate() {
        receiver = ActionHandler()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        app = application as App
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
//        Log.e("NoBar", event.toString())

//        Log.e("NoBar", "${event.packageName} ${Utils.getLauncherPackage(this)}")

        if (!IntroActivity.needsToRun(this)) {
            if (Utils.getLauncherPackage(this).contains(event.packageName)) {
                if (Utils.hideOnLauncher(this)) {
                    isBarHiddenForLauncher = true
                    app.removeBar()
                }
            } else {
                if (isBarHiddenForLauncher) {
                    isBarHiddenForLauncher = false
                    app.addBar()
                }
            }
            app.uiHandler.onGlobalLayout()
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        receiver.destroy()
    }

    /**
     * Special BroadcastReceiver to handle actions sent to this service by {@link com.xda.nobar.views.BarView}
     */
    inner class ActionHandler : BroadcastReceiver() {
        init {
            val filter = IntentFilter()
            filter.addAction(ACTION)

            LocalBroadcastManager.getInstance(this@Actions).registerReceiver(this, filter)
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION) {
                when (intent.getIntExtra(EXTRA_ACTION, app.typeNoAction)) {
                    app.typeHome -> performGlobalAction(GLOBAL_ACTION_HOME)
                    app.typeRecents -> performGlobalAction(GLOBAL_ACTION_RECENTS)
                    app.typeBack -> performGlobalAction(GLOBAL_ACTION_BACK)
                    app.typeSwitch -> runNougatAction {
                        performGlobalAction(GLOBAL_ACTION_RECENTS)
                        handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_RECENTS) }, 100)
                    }
                    app.typeSplit -> runNougatAction { performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN) }
                    app.premTypeNotif -> runPremiumAction { performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) }
                    app.premTypeQs -> runPremiumAction { performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS) }
                    app.premTypePower -> runPremiumAction { performGlobalAction(GLOBAL_ACTION_POWER_DIALOG) }
                    app.typeAssist -> {
                        val assist = Intent(RecognizerIntent.ACTION_WEB_SEARCH)
                        assist.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                        try {
                            startActivity(assist)
                        } catch (e: Exception) {
                            assist.action = RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE

                            try {
                                startActivity(assist)
                            } catch (e: Exception) {
                                assist.action = Intent.ACTION_VOICE_ASSIST

                                try {
                                    startActivity(assist)
                                } catch (e: Exception) {
                                    assist.action = Intent.ACTION_VOICE_COMMAND

                                    try {
                                        startActivity(assist)
                                    } catch (e: Exception) {
                                        assist.action = Intent.ACTION_ASSIST

                                        try {
                                            startActivity(assist)
                                        } catch (e: Exception) {
                                            val searchMan = getSystemService(Context.SEARCH_SERVICE) as SearchManager
                                            searchMan.launchAssist(null)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    app.typeOhm -> {
                        val ohm = Intent("com.xda.onehandedmode.intent.action.TOGGLE_OHM")
                        ohm.setClassName("com.xda.onehandedmode", "com.xda.onehandedmode.receivers.OHMReceiver")
                        sendBroadcast(ohm)
                    }
                    app.premTypePlayPause -> runPremiumAction {
                        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                    }
                    app.premTypePrev -> runPremiumAction {
                        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                    }
                    app.premTypeNext -> runPremiumAction {
                        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT))
                        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT))
                    }
                    app.premTypeVibe -> {
                        //TODO: Implement
                    }
                    app.premTypeSilent -> {
                        //TODO: Implement
                    }
                    app.premTypeMute -> {
                        //TODO: Implement
                    }
                }
            }
        }

        fun destroy() {
            try {
                unregisterReceiver(this)
            } catch (e: Exception) {}
        }

        /**
         * Check for valid premium and run the action if possible
         * Otherwise show a warning dialog
         */
        private fun runPremiumAction(action: () -> Unit) {
            if (app.isValidPremium) action.invoke()
            else {
                DialogActivity.Builder(this@Actions).apply {
                    title = R.string.premium_required
                    message = R.string.premium_required_desc
                    showYes = true
                    showNo = true
                    yesUrl = "https://play.google.com/store/apps/details?id=com.xda.nobar.premium"
                    start()
                }
            }
        }

        /**
         * Run action if device is on Nougat or later
         * Otherwise show a warning dialog
         */
        private fun runNougatAction(action: () -> Unit) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                action.invoke()
            } else {
                DialogActivity.Builder(this@Actions).apply {
                    title = R.string.nougat_required
                    message = R.string.nougat_required_desc
                    showYes = true
                    yesRes = android.R.string.ok
                    start()
                }
            }
        }
    }
}