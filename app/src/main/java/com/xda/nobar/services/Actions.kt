package com.xda.nobar.services

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.app.SearchManager
import android.content.*
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.support.v4.content.LocalBroadcastManager
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.activities.DialogActivity
import com.xda.nobar.activities.IntroActivity
import com.xda.nobar.activities.LockScreenActivity
import com.xda.nobar.activities.ScreenshotActivity
import java.io.Serializable


/**
 * Where most of the magic happens
 */
class Actions : AccessibilityService(), Serializable {
    companion object {
        const val BASE = "com.xda.nobar.action"
        const val ACTION = "$BASE.ACTION"

        const val EXTRA_ACTION = "action"
        const val EXTRA_GESTURE = "gesture"
    }

    private lateinit var receiver: ActionHandler
    private lateinit var wm: WindowManager
    private lateinit var am: ActivityManager
    private lateinit var audio: AudioManager
    private lateinit var imm: InputMethodManager
    private lateinit var app: App

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        receiver = ActionHandler()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        app = application as App
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        app.runAsync {
            if (!IntroActivity.needsToRun(this)) {
                try {
                    app.uiHandler.setNodeInfoAndUpdate(event.source) //We're listening for any changes to the window state, so we send those updates onto the UIHandler
                } catch (e: NullPointerException) {}
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        receiver.destroy()
    }

    /**
     * Special BroadcastReceiver to handle actions sent to this service by {@link com.xda.nobar.views.BarView}
     */
    inner class ActionHandler : BroadcastReceiver(), Serializable {
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

                                            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                                                try {
                                                    searchMan.launchAssist(null)
                                                } catch (e: Exception) {

                                                    searchMan.launchLegacyAssist(null, UserHandle.USER_CURRENT, null)
                                                }
                                            } else {
                                                val launchAssistAction = searchMan::class.java.getMethod("launchAssistAction", Int::class.java, String::class.java, Int::class.java)
                                                launchAssistAction.invoke(searchMan, 1, null, UserHandle.USER_CURRENT)
                                            }
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
                    app.premTypeSwitchIme -> runPremiumAction {
                        imm.showInputMethodPicker()
                    }
                    app.premTypeLaunchApp -> runPremiumAction {
                        val key = "${intent.getStringExtra(EXTRA_GESTURE)}_package"
                        val launchPackage = app.prefs.getString(key, null)

                        if (launchPackage != null) {
                            val launch = Intent(Intent.ACTION_MAIN)
                            launch.addCategory(Intent.CATEGORY_LAUNCHER)
                            launch.`package` = launchPackage.split("/")[0]
                            launch.component = ComponentName(launch.`package`, launchPackage.split("/")[1])

                            try {
                                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(launch)
                            } catch (e: Exception) {}
                        }
                    }
                    app.premTypeLockScreen -> runPremiumAction { runSystemSettingsAction {
                        val lock = Intent(this@Actions, LockScreenActivity::class.java)
                        lock.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(lock)
                    } }
                    app.premTypeScreenshot -> runPremiumAction {
                        val screenshot = Intent(this@Actions, ScreenshotActivity::class.java)
                        screenshot.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(screenshot)
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
                    yesAction = {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse("https://play.google.com/store/apps/details?id=com.xda.nobar.premium")
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
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
                    yesRes = android.R.string.ok
                    start()
                }
            }
        }

        /**
         * Run an action that requires WRITE_SETTINGS
         * Otherwise show a dialog prompting for permission
         */
        private fun runSystemSettingsAction(action: () -> Unit) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(this@Actions)) {
                action.invoke()
            } else {
                DialogActivity.Builder(this@Actions).apply {
                    title = R.string.grant_write_settings
                    message = R.string.grant_write_settings_desc
                    yesRes = android.R.string.ok
                    noRes = android.R.string.cancel

                    yesAction = {
                        val intent = Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS)
                        intent.data = Uri.parse("package:$packageName")
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }

                    start()
                }
            }
        }
    }
}