package com.xda.nobar.services

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.SearchManager
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.UserHandle
import android.provider.MediaStore
import android.provider.Settings
import android.speech.RecognizerIntent
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.view.KeyEvent
import android.view.OrientationEventListener
import android.view.Surface
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.joaomgcd.taskerpluginlibrary.extensions.requestQuery
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.activities.IntentSelectorActivity
import com.xda.nobar.activities.LockScreenActivity
import com.xda.nobar.activities.RequestPermissionsActivity
import com.xda.nobar.activities.ScreenshotActivity
import com.xda.nobar.tasker.activities.EventConfigureActivity
import com.xda.nobar.tasker.updates.EventUpdate
import com.xda.nobar.util.FlashlighControllerLollipop
import com.xda.nobar.util.FlashlightControllerMarshmallow
import com.xda.nobar.util.Utils
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

    private val receiver by lazy { ActionHandler(app, this) }
    private val app by lazy { applicationContext as App }

    override fun onCreate() {
        app.logicHandler.post { receiver.register() }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val newEvent = AccessibilityEvent.obtain(event)

        app.logicHandler.post {
            app.uiHandler.setNodeInfoAndUpdate(newEvent)
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        app.logicHandler.post { receiver.destroy() }
    }

    /**
     * Special BroadcastReceiver to handle actions sent to this service by {@link com.xda.nobar.views.BarView}
     */
    class ActionHandler(private val app: App, private val actions: Actions) : BroadcastReceiver(), Serializable {
        private val audio by lazy { app.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
        private val imm by lazy { app.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }
        private val wifiManager by lazy { app.getSystemService(Context.WIFI_SERVICE) as WifiManager }

        private val handler = app.logicHandler

        private val flashlightController by lazy {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) FlashlightControllerMarshmallow(app)
            else FlashlighControllerLollipop(app)
        }

        private val orientationEventListener by lazy {
            object : OrientationEventListener(app) {
                override fun onOrientationChanged(orientation: Int) {
                    currentDegree = orientation
                }
            }
        }

        private var currentDegree = 0
            set(value) {
                field = value
                orientationEventListener.disable()
                handler.postDelayed({
                    val currentAcc = Settings.System.getInt(app.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1)
                    if (currentAcc == 0) {
                        val rotation = when (currentDegree) {
                            in 45..134 -> Surface.ROTATION_270
                            in 135..224 -> Surface.ROTATION_180
                            in 225..314 -> Surface.ROTATION_90
                            else -> Surface.ROTATION_0
                        }

                        Settings.System.putInt(app.contentResolver, Settings.System.USER_ROTATION, rotation)
                    }
                }, 20)
            }

        fun register() {
            handler.post {
                val filter = IntentFilter()
                filter.addAction(ACTION)
                filter.addAction(RequestPermissionsActivity.ACTION_RESULT)

                LocalBroadcastManager.getInstance(app).registerReceiver(this, filter)

                flashlightController.onCreate()
            }
        }

        @SuppressLint("InlinedApi")
        override fun onReceive(context: Context?, intent: Intent?) {
            handler.post {
                when(intent?.action) {
                    ACTION -> {
                        val gesture = intent.getStringExtra(EXTRA_GESTURE)
                        when (intent.getIntExtra(EXTRA_ACTION, app.typeNoAction)) {
                            app.typeHome -> actions.performGlobalAction(GLOBAL_ACTION_HOME)
                            app.typeRecents -> actions.performGlobalAction(GLOBAL_ACTION_RECENTS)
                            app.typeBack -> actions.performGlobalAction(GLOBAL_ACTION_BACK)
                            app.typeSwitch -> runNougatAction {
                                actions.performGlobalAction(GLOBAL_ACTION_RECENTS)
                                handler.postDelayed({ actions.performGlobalAction(GLOBAL_ACTION_RECENTS) }, 100)
                            }
                            app.typeSplit -> runNougatAction { actions.performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN) }
                            app.premTypeNotif -> runPremiumAction { actions.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) }
                            app.premTypeQs -> runPremiumAction { actions.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS) }
                            app.premTypePower -> runPremiumAction { actions.performGlobalAction(GLOBAL_ACTION_POWER_DIALOG) }
                            app.typeAssist -> {
                                val assist = Intent(RecognizerIntent.ACTION_WEB_SEARCH)
                                assist.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                                try {
                                    app.startActivity(assist)
                                } catch (e: Exception) {
                                    assist.action = RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE

                                    try {
                                        app.startActivity(assist)
                                    } catch (e: Exception) {
                                        assist.action = Intent.ACTION_VOICE_ASSIST

                                        try {
                                            app.startActivity(assist)
                                        } catch (e: Exception) {
                                            assist.action = Intent.ACTION_VOICE_COMMAND

                                            try {
                                                app.startActivity(assist)
                                            } catch (e: Exception) {
                                                assist.action = Intent.ACTION_ASSIST

                                                try {
                                                    app.startActivity(assist)
                                                } catch (e: Exception) {
                                                    val searchMan = app.getSystemService(Context.SEARCH_SERVICE) as SearchManager

                                                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                                                        try {
                                                            searchMan.launchAssist(null)
                                                        } catch (e: Exception) {

                                                            searchMan.launchLegacyAssist(null, UserHandle.USER_CURRENT, null)
                                                        }
                                                    } else {
                                                        val launchAssistAction = searchMan::class.java
                                                                .getMethod("launchAssistAction", Int::class.java, String::class.java, Int::class.java)
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
                                app.sendBroadcast(ohm)
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
                                val key = "${gesture}_package"
                                val launchPackage = app.prefs.getString(key, null)

                                if (launchPackage != null) {
                                    val launch = Intent(Intent.ACTION_MAIN)
                                    launch.addCategory(Intent.CATEGORY_LAUNCHER)
                                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    launch.`package` = launchPackage.split("/")[0]
                                    launch.component = ComponentName(launch.`package`, launchPackage.split("/")[1])

                                    try {
                                        app.startActivity(launch)
                                    } catch (e: Exception) {
                                    }
                                }
                            }
                            app.premTypeLaunchActivity -> runPremiumAction {
                                val key = "${gesture}_activity"
                                val activity = app.prefs.getString(key, null) ?: return@runPremiumAction

                                val p = activity.split("/")[0]
                                val c = activity.split("/")[1]

                                val launch = Intent()
                                launch.component = ComponentName(p, c)

                                try {
                                    app.startActivity(launch)
                                } catch (e: Exception) {
                                }
                            }
                            app.premTypeLockScreen -> runPremiumAction {
                                runSystemSettingsAction {
                                    val lock = Intent(app, LockScreenActivity::class.java)
                                    lock.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    app.startActivity(lock)
                                }
                            }
                            app.premTypeScreenshot -> runPremiumAction {
                                val screenshot = Intent(app, ScreenshotActivity::class.java)
                                screenshot.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                app.startActivity(screenshot)
                            }
                            app.premTypeRot -> runPremiumAction {
                                runSystemSettingsAction {
                                    orientationEventListener.enable()
                                }
                            }
                            app.premTypeTaskerEvent -> runPremiumAction {
                                EventConfigureActivity::class.java.requestQuery(app, EventUpdate(gesture))
                            }
                            app.typeToggleNav -> {
                                app.toggleNavState()
                            }
                            app.premTypeFlashlight -> runPremiumAction {
                                flashlightController.flashlightEnabled = !flashlightController.flashlightEnabled
                            }
                            app.premTypeVolumePanel -> runPremiumAction {
                                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
                            }
                            app.premTypeBluetooth -> runPremiumAction {
                                val adapter = BluetoothAdapter.getDefaultAdapter()
                                if (adapter.isEnabled) adapter.disable() else adapter.enable()
                            }
                            app.premTypeWiFi -> runPremiumAction {
                                wifiManager.isWifiEnabled = !wifiManager.isWifiEnabled
                            }
                            app.premTypeIntent -> runPremiumAction {
                                val broadcast = IntentSelectorActivity.INTENTS[Utils.getIntentKey(app, gesture)]
                                val type = broadcast?.which

                                try {
                                    when (type) {
                                        IntentSelectorActivity.ACTIVITY -> app.startActivity(broadcast)
                                        IntentSelectorActivity.SERVICE -> ContextCompat.startForegroundService(app, broadcast)
                                        IntentSelectorActivity.BROADCAST -> app.sendBroadcast(broadcast)
                                    }
                                } catch (e: SecurityException) {
                                    when (broadcast?.action) {
                                        MediaStore.ACTION_VIDEO_CAPTURE,
                                        MediaStore.ACTION_IMAGE_CAPTURE -> {
                                            RequestPermissionsActivity.createAndStart(app,
                                                    arrayOf(Manifest.permission.CAMERA),
                                                    intent.extras,
                                                    ComponentName(app, ActionHandler::class.java))
                                        }
                                    }
                                } catch (e: ActivityNotFoundException) {
                                    Toast.makeText(context, R.string.unable_to_launch, Toast.LENGTH_SHORT).show()
                                }
                            }
                            app.premTypeBatterySaver -> {
                                runPremiumAction {
                                    val current = Settings.Global.getInt(app.contentResolver, Settings.Global.LOW_POWER_MODE, 0)
                                    Settings.Global.putInt(app.contentResolver, Settings.Global.LOW_POWER_MODE, if (current == 0) 1 else 0)
                                }
                            }
                            app.premTypeScreenTimeout -> {
                                runPremiumAction { app.toggleScreenOn() }
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
                    RequestPermissionsActivity.ACTION_RESULT -> {
                        if (intent.getIntArrayExtra(RequestPermissionsActivity.EXTRA_RESULT_CODE)[0] == PackageManager.PERMISSION_GRANTED) {
                            intent.action = ACTION
                            onReceive(context, intent)
                        }
                    }
                }
            }
        }

        fun destroy() {
            handler.post {
                try {
                    LocalBroadcastManager.getInstance(app).unregisterReceiver(this)
                } catch (e: Exception) {}

                flashlightController.onDestroy()
            }
        }

        private fun runNougatAction(action: () -> Unit) = Utils.runNougatAction(app, action)
        private fun runPremiumAction(action: () -> Unit) = Utils.runPremiumAction(app, action)
        private fun runSystemSettingsAction(action: () -> Unit) = Utils.runSystemSettingsAction(app, action)
    }
}