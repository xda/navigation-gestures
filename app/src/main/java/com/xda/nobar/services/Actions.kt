package com.xda.nobar.services

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.SearchManager
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.UserHandle
import android.provider.MediaStore
import android.provider.Settings
import android.speech.RecognizerIntent
import android.support.v4.content.ContextCompat
import android.view.KeyEvent
import android.view.OrientationEventListener
import android.view.Surface
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.joaomgcd.taskerpluginlibrary.extensions.requestQuery
import com.xda.nobar.R
import com.xda.nobar.activities.IntentSelectorActivity
import com.xda.nobar.activities.RequestPermissionsActivity
import com.xda.nobar.activities.ScreenshotActivity
import com.xda.nobar.interfaces.ReceiverCallback
import com.xda.nobar.receivers.ActionReceiver
import com.xda.nobar.receivers.StartupReceiver
import com.xda.nobar.tasker.activities.EventConfigureActivity
import com.xda.nobar.tasker.updates.EventUpdate
import com.xda.nobar.util.ActionHolder
import com.xda.nobar.util.FlashlighControllerLollipop
import com.xda.nobar.util.FlashlightControllerMarshmallow
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

    private val audio by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private val imm by lazy { getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }
    private val wifiManager by lazy { applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager }

    private val handler = Handler()

    private val flashlightController by lazy {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) FlashlightControllerMarshmallow(this)
        else FlashlighControllerLollipop(this)
    }

    private val orientationEventListener by lazy {
        object : OrientationEventListener(this) {
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
                val currentAcc = Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1)
                if (currentAcc == 0) {
                    val rotation = when (currentDegree) {
                        in 45..134 -> Surface.ROTATION_270
                        in 135..224 -> Surface.ROTATION_180
                        in 225..314 -> Surface.ROTATION_90
                        else -> Surface.ROTATION_0
                    }

                    Settings.System.putInt(contentResolver, Settings.System.USER_ROTATION, rotation)
                }
            }, 20)
        }

    private var validPremium = false

    override fun onCreate() {
        receiver.register(this, this)
        flashlightController.onCreate()

        val bc = Intent(this, StartupReceiver::class.java)
        bc.action = StartupReceiver.ACTION_ACTIVATE
        sendBroadcast(bc)
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
                val gesture = intent.getStringExtra(EXTRA_GESTURE)
                val actionHolder = ActionHolder(this)
                when (intent.getIntExtra(EXTRA_ACTION, actionHolder.typeNoAction)) {
                    actionHolder.typeHome -> {
                        if (intent.getBooleanExtra(EXTRA_ALT_HOME, false)) {
                            val homeIntent = Intent(Intent.ACTION_MAIN)
                            homeIntent.addCategory(Intent.CATEGORY_HOME)
                            homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(homeIntent)
                        } else {
                            performGlobalAction(GLOBAL_ACTION_HOME)
                        }
                    }
                    actionHolder.typeRecents -> performGlobalAction(GLOBAL_ACTION_RECENTS)
                    actionHolder.typeBack -> performGlobalAction(GLOBAL_ACTION_BACK)
                    actionHolder.typeSwitch -> runNougatAction {
                        performGlobalAction(GLOBAL_ACTION_RECENTS)
                        handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_RECENTS) }, 100)
                    }
                    actionHolder.typeSplit -> runNougatAction { performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN) }
                    actionHolder.premTypeNotif -> runPremiumAction { performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) }
                    actionHolder.premTypeQs -> runPremiumAction { performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS) }
                    actionHolder.premTypePower -> runPremiumAction { performGlobalAction(GLOBAL_ACTION_POWER_DIALOG) }
                    actionHolder.typeAssist -> {
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
                    actionHolder.typeOhm -> {
                        val ohm = Intent("com.xda.onehandedmode.intent.action.TOGGLE_OHM")
                        ohm.setClassName("com.xda.onehandedmode", "com.xda.onehandedmode.receivers.OHMReceiver")
                        sendBroadcast(ohm)
                    }
                    actionHolder.premTypePlayPause -> runPremiumAction {
                        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                    }
                    actionHolder.premTypePrev -> runPremiumAction {
                        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                    }
                    actionHolder.premTypeNext -> runPremiumAction {
                        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT))
                        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT))
                    }
                    actionHolder.premTypeSwitchIme -> runPremiumAction {
                        imm.showInputMethodPicker()
                    }
                    actionHolder.premTypeLaunchApp -> runPremiumAction {
                        val launchPackage = intent.getStringExtra(EXTRA_PACKAGE)

                        if (launchPackage != null) {
                            val launch = Intent(Intent.ACTION_MAIN)
                            launch.addCategory(Intent.CATEGORY_LAUNCHER)
                            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            launch.`package` = launchPackage.split("/")[0]
                            launch.component = ComponentName(launch.`package`, launchPackage.split("/")[1])

                            try {
                                startActivity(launch)
                            } catch (e: Exception) {}
                        }
                    }
                    actionHolder.premTypeLaunchActivity -> runPremiumAction {
                        val activity = intent.getStringExtra(EXTRA_ACTIVITY) ?: return@runPremiumAction

                        val p = activity.split("/")[0]
                        val c = activity.split("/")[1]

                        val launch = Intent()
                        launch.component = ComponentName(p, c)
                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                        try {
                            startActivity(launch)
                        } catch (e: Exception) {}
                    }
                    actionHolder.premTypeLockScreen -> runPremiumAction {
                        runSystemSettingsAction {
                            ActionReceiver.turnScreenOff(this)
                        }
                    }
                    actionHolder.premTypeScreenshot -> runPremiumAction {
                        val screenshot = Intent(this, ScreenshotActivity::class.java)
                        screenshot.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(screenshot)
                    }
                    actionHolder.premTypeRot -> runPremiumAction {
                        runSystemSettingsAction {
                            orientationEventListener.enable()
                        }
                    }
                    actionHolder.premTypeTaskerEvent -> runPremiumAction {
                        EventConfigureActivity::class.java.requestQuery(this, EventUpdate(gesture))
                    }
                    actionHolder.typeToggleNav -> {
                        ActionReceiver.toggleNav(this)
                    }
                    actionHolder.premTypeFlashlight -> runPremiumAction {
                        flashlightController.flashlightEnabled = !flashlightController.flashlightEnabled
                    }
                    actionHolder.premTypeVolumePanel -> runPremiumAction {
                        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
                    }
                    actionHolder.premTypeBluetooth -> runPremiumAction {
                        val adapter = BluetoothAdapter.getDefaultAdapter()
                        if (adapter.isEnabled) adapter.disable() else adapter.enable()
                    }
                    actionHolder.premTypeWiFi -> runPremiumAction {
                        wifiManager.isWifiEnabled = !wifiManager.isWifiEnabled
                    }
                    actionHolder.premTypeIntent -> runPremiumAction {
                        val key = intent.getIntExtra(EXTRA_INTENT_KEY, -1)
                        if (key == -1) return@runPremiumAction

                        val broadcast = IntentSelectorActivity.INTENTS[key]
                        val type = broadcast?.which

                        try {
                            when (type) {
                                IntentSelectorActivity.ACTIVITY -> {
                                    broadcast.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(broadcast)
                                }
                                IntentSelectorActivity.SERVICE -> ContextCompat.startForegroundService(this, broadcast)
                                IntentSelectorActivity.BROADCAST -> sendBroadcast(broadcast)
                            }
                        } catch (e: SecurityException) {
                            when (broadcast?.action) {
                                MediaStore.ACTION_VIDEO_CAPTURE,
                                MediaStore.ACTION_IMAGE_CAPTURE -> {
                                    RequestPermissionsActivity.createAndStart(this,
                                            arrayOf(Manifest.permission.CAMERA),
                                            intent.extras,
                                            null)
                                }
                            }
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(this, R.string.unable_to_launch, Toast.LENGTH_SHORT).show()
                        }
                    }
                    actionHolder.premTypeBatterySaver -> {
                        runPremiumAction {
                            val current = Settings.Global.getInt(contentResolver, Settings.Global.LOW_POWER_MODE, 0)
                            Settings.Global.putInt(contentResolver, Settings.Global.LOW_POWER_MODE, if (current == 0) 1 else 0)
                        }
                    }
                    actionHolder.premTypeScreenTimeout -> {
                        runPremiumAction { ActionReceiver.toggleScreenOn(this) }
                    }
                    actionHolder.premTypeVibe -> {
                        //TODO: Implement
                    }
                    actionHolder.premTypeSilent -> {
                        //TODO: Implement
                    }
                    actionHolder.premTypeMute -> {
                        //TODO: Implement
                    }
                }
            }
            RequestPermissionsActivity.ACTION_RESULT -> {
                if (intent.getIntArrayExtra(RequestPermissionsActivity.EXTRA_RESULT_CODE)[0] == PackageManager.PERMISSION_GRANTED) {
                    intent.action = ACTION
                    onActionReceived(intent)
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
        flashlightController.onDestroy()
    }

    private fun runNougatAction(action: () -> Unit) = Utils.runNougatAction(this, action)
    private fun runPremiumAction(action: () -> Unit) = Utils.runPremiumAction(this, validPremium, action)
    private fun runSystemSettingsAction(action: () -> Unit) = Utils.runSystemSettingsAction(this, action)

    /**
     * Special BroadcastReceiver to handle actions sent to this service by {@link com.xda.nobar.views.BarView}
     */
    class ActionHandler : BroadcastReceiver() {
        private var callback: ReceiverCallback? = null

        fun register(context: Context, callback: ReceiverCallback) {
            this.callback = callback

            val filter = IntentFilter()
            filter.addAction(ACTION)
            filter.addAction(RequestPermissionsActivity.ACTION_RESULT)
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