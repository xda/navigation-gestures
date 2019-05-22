package com.xda.nobar.util.helpers.bar

import android.Manifest
import android.app.SearchManager
import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.speech.RecognizerIntent
import android.view.KeyEvent
import android.view.OrientationEventListener
import android.view.Surface
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.joaomgcd.taskerpluginlibrary.extensions.requestQuery
import com.xda.nobar.BuildConfig
import com.xda.nobar.R
import com.xda.nobar.activities.helpers.RequestPermissionsActivity
import com.xda.nobar.activities.helpers.ScreenshotActivity
import com.xda.nobar.activities.selectors.IntentSelectorActivity
import com.xda.nobar.receivers.ActionReceiver
import com.xda.nobar.services.Actions
import com.xda.nobar.tasker.activities.EventConfigureActivity
import com.xda.nobar.tasker.updates.EventUpdate
import com.xda.nobar.util.*
import com.xda.nobar.util.flashlight.FlashlightControllerLollipop
import com.xda.nobar.util.flashlight.FlashlightControllerMarshmallow
import com.xda.nobar.views.BarView
import kotlinx.coroutines.launch

class BarViewActionHandler(private val bar: BarView) {
    private val context = bar.context

    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    val flashlightController by lazy {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) FlashlightControllerMarshmallow(context)
        else FlashlightControllerLollipop(context)
    }

    private val rootWrapper by lazy { context.app.rootWrapper }

    private val orientationEventListener by lazy {
        object : OrientationEventListener(context) {
            private var enabled = false

            override fun onOrientationChanged(orientation: Int) {
                synchronized(this) {
                    if (enabled) {
                        disable()

                        logicHandler.postDelayed({
                            val currentAcc = Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1)
                            if (currentAcc == 0) {
                                val rotation = when (orientation) {
                                    in 45..134 -> Surface.ROTATION_270
                                    in 135..224 -> Surface.ROTATION_180
                                    in 225..314 -> Surface.ROTATION_90
                                    else -> Surface.ROTATION_0
                                }

                                Settings.System.putInt(context.contentResolver, Settings.System.USER_ROTATION, rotation)
                            }
                        }, 20)
                    }
                }
            }

            override fun enable() {
                enabled = true
                super.enable()
            }

            override fun disable() {
                enabled = false
                super.disable()
            }
        }
    }

    fun sendActionInternal(key: String, map: Map<String, Int>) {
        mainScope.launch {
            val which = map[key] ?: return@launch

            if (which == bar.actionHolder.typeNoAction) return@launch

            if (bar.isHidden || bar.isPillHidingOrShowing) return@launch

            bar.vibrate(context.prefManager.vibrationDuration.toLong())

            if (key == bar.actionHolder.actionDouble)
                mainHandler.postDelayed({ bar.vibrate(context.prefManager.vibrationDuration.toLong()) },
                        context.prefManager.vibrationDuration.toLong())

            if (which == bar.actionHolder.typeHide) {
                bar.hidePill(false, null, true)
                return@launch
            }

            when (key) {
                bar.actionHolder.actionDouble -> bar.animator.jiggleDoubleTap()
                bar.actionHolder.actionHold -> bar.animator.jiggleHold()
                bar.actionHolder.actionTap -> bar.animator.jiggleTap()
                bar.actionHolder.actionUpHold -> bar.animator.jiggleHoldUp()
                bar.actionHolder.actionLeftHold -> bar.animator.jiggleLeftHold()
                bar.actionHolder.actionRightHold -> bar.animator.jiggleRightHold()
                bar.actionHolder.actionDownHold -> bar.animator.jiggleDownHold()
            }

            if (key == bar.actionHolder.actionUp
                    || key == bar.actionHolder.actionLeft
                    || key == bar.actionHolder.actionRight) {
                bar.animate(null, BarView.ALPHA_ACTIVE)
            }

            if (bar.isAccessibilityAction(which)) {
                if (which == bar.actionHolder.typeHome
                        && context.prefManager.useAlternateHome) {
                    handleAction(which, key)
                } else {
                    sendAccessibilityAction(which)
                }
            } else if (rootWrapper.isConnected && bar.isRootAction(which)) {
                sendRootAction(which, key)
            } else {
                handleAction(which, key)
            }
        }
    }

    fun handleAction(which: Int, key: String) {
        logicScope.launch {
            try {
                when (which) {
                    bar.actionHolder.typeAssist -> {
                        val assist = Intent(RecognizerIntent.ACTION_WEB_SEARCH)
                        assist.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                        try {
                            context.startActivity(assist)
                        } catch (e: Exception) {
                            assist.action = RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE

                            try {
                                context.startActivity(assist)
                            } catch (e: Exception) {
                                assist.action = "android.intent.action.VOICE_ASSIST"

                                try {
                                    context.startActivity(assist)
                                } catch (e: Exception) {
                                    assist.action = Intent.ACTION_VOICE_COMMAND

                                    try {
                                        context.startActivity(assist)
                                    } catch (e: Exception) {
                                        assist.action = Intent.ACTION_ASSIST

                                        try {
                                            context.startActivity(assist)
                                        } catch (e: Exception) {
                                            val searchMan = context.getSystemService(Context.SEARCH_SERVICE) as SearchManager

                                            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                                                try {
                                                    searchMan.launchAssist()
                                                } catch (e: Exception) {

                                                    searchMan.launchLegacyAssist()
                                                }
                                            } else {
                                                val launchAssistAction = searchMan::class.java
                                                        .getMethod("launchAssistAction", Int::class.java, String::class.java, Int::class.java)
                                                launchAssistAction.invoke(searchMan, 1, null, -2)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    bar.actionHolder.typeOhm -> {
                        val ohm = Intent("com.xda.onehandedmode.intent.action.TOGGLE_OHM")
                        ohm.setClassName("com.xda.onehandedmode", "com.xda.onehandedmode.receivers.OHMReceiver")
                        context.sendBroadcast(ohm)
                    }
                    bar.actionHolder.typeHome -> {
                        val homeIntent = Intent(Intent.ACTION_MAIN)
                        homeIntent.addCategory(Intent.CATEGORY_HOME)
                        homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(homeIntent)
                    }
                    bar.actionHolder.premTypePlayPause -> context.runPremiumAction {
                        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                    }
                    bar.actionHolder.premTypePrev -> context.runPremiumAction {
                        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                    }
                    bar.actionHolder.premTypeNext -> context.runPremiumAction {
                        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT))
                        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT))
                    }
                    bar.actionHolder.premTypeSwitchIme -> context.runPremiumAction {
                        imm.showInputMethodPicker()
                    }
                    bar.actionHolder.premTypeLaunchApp -> context.runPremiumAction {
                        val launchPackage = context.app.prefManager.getPackage(key)

                        if (launchPackage != null) {
                            val launch = Intent(Intent.ACTION_MAIN)
                            launch.addCategory(Intent.CATEGORY_LAUNCHER)
                            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            launch.`package` = launchPackage.split("/")[0]
                            launch.component = ComponentName(launch.`package`!!, launchPackage.split("/")[1])

                            try {
                                context.startActivity(launch)
                            } catch (e: Exception) {
                            }
                        }
                    }
                    bar.actionHolder.premTypeLaunchActivity -> context.runPremiumAction {
                        val activity = context.prefManager.getActivity(key)
                                ?: return@runPremiumAction

                        val p = activity.split("/")[0]
                        val c = activity.split("/")[1]

                        val launch = Intent()
                        launch.component = ComponentName(p, c)
                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                        try {
                            context.startActivity(launch)
                        } catch (e: Exception) {
                        }
                    }
                    bar.actionHolder.premTypeLaunchShortcut -> context.runPremiumAction {
                        try {
                            val shortcut = context.prefManager.getShortcut(key)
                                    ?: return@runPremiumAction
                            val intent = (shortcut.intent
                                    ?: return@runPremiumAction).clone() as Intent

                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    bar.actionHolder.premTypeLockScreen -> context.runPremiumAction {
                        context.runSystemSettingsAction {
                            ActionReceiver.turnScreenOff(context)
                        }
                    }
                    bar.actionHolder.premTypeScreenshot -> context.runPremiumAction {
                        val screenshot = Intent(context, ScreenshotActivity::class.java)
                        screenshot.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(screenshot)
                    }
                    bar.actionHolder.premTypeRot -> context.runPremiumAction {
                        context.runSystemSettingsAction {
                            orientationEventListener.enable()
                        }
                    }
                    bar.actionHolder.premTypeTaskerEvent -> context.runPremiumAction {
                        EventConfigureActivity::class.java.requestQuery(context, EventUpdate(key))
                    }
                    bar.actionHolder.typeToggleNav -> {
                        ActionReceiver.toggleNav(context)
                    }
                    bar.actionHolder.premTypeFlashlight -> context.runPremiumAction {
                        if (!flashlightController.isCreated) {
                            flashlightController.onCreate {
                                flashlightController.toggle()
                            }
                        } else {
                            flashlightController.toggle()

                            if (context.prefManager.flashlightCompat
                                    && !flashlightController.flashlightEnabled) {
                                flashlightController.onDestroy()
                            }
                        }
                    }
                    bar.actionHolder.premTypeVolumePanel -> context.runPremiumAction {
                        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
                    }
                    bar.actionHolder.premTypeBluetooth -> context.runPremiumAction {
                        val adapter = BluetoothAdapter.getDefaultAdapter()
                        if (adapter.isEnabled) adapter.disable() else adapter.enable()
                    }
                    bar.actionHolder.premTypeWiFi -> context.runPremiumAction {
                        wifiManager.isWifiEnabled = !wifiManager.isWifiEnabled
                    }
                    bar.actionHolder.premTypeIntent -> context.runPremiumAction {
                        val broadcast = IntentSelectorActivity.INTENTS[context.app.prefManager.getIntentKey(key)]
                        val type = broadcast?.which

                        try {
                            when (type) {
                                IntentSelectorActivity.ACTIVITY -> {
                                    broadcast.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(broadcast)
                                }
                                IntentSelectorActivity.SERVICE -> ContextCompat.startForegroundService(context, broadcast)
                                IntentSelectorActivity.BROADCAST -> context.sendBroadcast(broadcast)
                            }
                        } catch (e: SecurityException) {
                            when (broadcast?.action) {
                                MediaStore.ACTION_VIDEO_CAPTURE,
                                MediaStore.ACTION_IMAGE_CAPTURE -> {
                                    RequestPermissionsActivity.createAndStart(context,
                                            arrayOf(Manifest.permission.CAMERA),
                                            ComponentName(context, BarView::class.java),
                                            Bundle().apply {
                                                putInt(Actions.EXTRA_ACTION, which)
                                                putString(Actions.EXTRA_GESTURE, key)
                                            }
                                    )
                                }
                            }
                        } catch (e: ActivityNotFoundException) {
                            mainScope.launch {
                                Toast.makeText(context, R.string.unable_to_launch, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    bar.actionHolder.premTypeBatterySaver -> {
                        context.runPremiumAction {
                            context.runSecureSettingsAction {
                                val current = Settings.Global.getInt(context.contentResolver, "low_power", 0)
                                Settings.Global.putInt(context.contentResolver, "low_power", if (current == 0) 1 else 0)
                            }
                        }
                    }
                    bar.actionHolder.premTypeScreenTimeout -> {
                        context.runPremiumAction { ActionReceiver.toggleScreenOn(context) }
                    }
                    bar.actionHolder.premTypeNotif -> context.runPremiumAction {
                        expandNotificationsPanel()
                    }
                    bar.actionHolder.premTypeQs -> context.runPremiumAction {
                        expandSettingsPanel()
                    }
                    bar.actionHolder.premTypeKillBackground -> context.runPremiumAction {
                        killAllBackgroundProcesses()
                    }
                    bar.actionHolder.premTypeVolumeDown -> context.runPremiumAction {
                        audio.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
                        audio.adjustVolume(
                                AudioManager.ADJUST_LOWER,
                                AudioManager.FLAG_SHOW_UI or
                                        AudioManager.FLAG_VIBRATE
                        )
                    }
                    bar.actionHolder.premTypeVolumeUp -> context.runPremiumAction {
                        audio.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
                        audio.adjustVolume(
                                AudioManager.ADJUST_RAISE,
                                AudioManager.FLAG_SHOW_UI or
                                        AudioManager.FLAG_VIBRATE
                        )
                    }
                    bar.actionHolder.premTypeCycleRinger -> context.runPremiumAction {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || context.app.nm.isNotificationPolicyAccessGranted) {
                            val newMode = when (audio.ringerMode) {
                                AudioManager.RINGER_MODE_SILENT -> AudioManager.RINGER_MODE_VIBRATE
                                AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_NORMAL
                                else -> AudioManager.RINGER_MODE_SILENT
                            }

                            audio.ringerMode = newMode

                            audio.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
                        } else {
                            mainScope.launch {
                                Toast.makeText(context, R.string.grant_notification_policy, Toast.LENGTH_SHORT).show()
                                context.startActivity(
                                        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                                .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                )
                            }
                        }
                    }
                    bar.actionHolder.premTypeMute -> context.runPremiumAction {
                        //isStreamMute exists as a hidden method in Lollipop
                        val isMuted = audio.isStreamMute(AudioManager.STREAM_MUSIC)
                        if (!isMuted) {
                            context.prefManager.savedMediaVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
                            } else {
                                audio.setStreamMute(AudioManager.STREAM_MUSIC, true)
                            }
                        } else {
                            audio.setStreamVolume(AudioManager.STREAM_MUSIC, context.prefManager.savedMediaVolume, AudioManager.FLAG_SHOW_UI)
                        }
                    }
                    bar.actionHolder.premTypeToggleAutoBrightness -> context.runPremiumAction { context.runSystemSettingsAction {
                        val isAuto = Settings.System.getInt(
                                context.contentResolver,
                                Settings.System.SCREEN_BRIGHTNESS_MODE,
                                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC

                        Settings.System.putInt(
                                context.contentResolver,
                                Settings.System.SCREEN_BRIGHTNESS_MODE,
                                if (isAuto) Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                                else Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                        )
                    } }
                    bar.actionHolder.premTypeBrightnessDown -> context.runPremiumAction { context.runSystemSettingsAction {
                        val currentBrightness = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 0)
                        var newBrightness = currentBrightness - context.prefManager.brightnessStepSize

                        if (newBrightness < 0) newBrightness = 0

                        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, newBrightness)
                    } }
                    bar.actionHolder.premTypeBrightnessUp -> context.runPremiumAction { context.runSystemSettingsAction {
                        val currentBrightness = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 0)
                        var newBrightness = currentBrightness + context.prefManager.brightnessStepSize

                        if (newBrightness > 255) newBrightness = 255

                        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, newBrightness)
                    } }
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    e.logStack()
                }
                mainScope.launch {
                    Toast.makeText(context, R.string.unable_to_execute_action, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendAccessibilityAction(which: Int) {
        context.app.postAction { it.sendAction(which) }
    }

    private fun sendRootAction(which: Int, key: String) {
        rootWrapper.postAction {
            when (which) {
                bar.actionHolder.typeRootForward -> it.sendKeyEvent(KeyEvent.KEYCODE_FORWARD)
                bar.actionHolder.typeRootHoldBack -> it.sendLongKeyEvent(KeyEvent.KEYCODE_BACK)
                bar.actionHolder.typeRootMenu -> it.sendKeyEvent(KeyEvent.KEYCODE_MENU)
                bar.actionHolder.premTypeLockScreen -> it.lockScreen()
                bar.actionHolder.premTypeScreenshot -> it.screenshot()
                bar.actionHolder.typeRootKeycode -> {
                    val code = context.prefManager.getKeycode(key)
                    if (code != -1) {
                        it.sendKeyEvent(code)
                    }
                }
                bar.actionHolder.typeRootDoubleKeycode -> {
                    val code = context.prefManager.getKeycode(key)
                    if (code != -1) {
                        it.sendDoubleKeyEvent(code)
                    }
                }
                bar.actionHolder.typeRootLongKeycode -> {
                    val code = context.prefManager.getKeycode(key)
                    if (code != -1) {
                        it.sendLongKeyEvent(code)
                    }
                }
            }
        }
    }
}