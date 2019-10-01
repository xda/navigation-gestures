package com.xda.nobar.util.helpers.bar

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.HandlerThread
import com.xda.nobar.util.PrefManager
import com.xda.nobar.util.actionHolder
import com.xda.nobar.util.app
import com.xda.nobar.util.prefManager
import java.util.*

class ActionManager(private val context: Context): SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: ActionManager? = null

        fun getInstance(context: Context): ActionManager {
            if (instance == null) instance = ActionManager(context.applicationContext)

            return instance!!
        }
    }

    val actionMap = HashMap<String, Int>()
    val actionHandler by lazy { BarViewActionHandler(context) }
    val gestureThread = HandlerThread("NoBar-Gesture").apply { start() }

    init {
        loadActionMap()
        context.app.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (hasKey(key)) {
            loadActionMap()
        }

        when (key) {
            PrefManager.IS_ACTIVE -> {
                refreshFlashlightState()
            }

            PrefManager.FLASHLIGHT_COMPAT -> {
                refreshFlashlightState()
            }
        }
    }

    /**
     * Load the user's custom gesture/action pairings; default values if a pairing doesn't exist
     */
    fun loadActionMap() {
        context.prefManager.getActionsList(actionMap)

        refreshFlashlightState()
    }

    fun refreshFlashlightState() {
        if (context.prefManager.isActive
                && actionMap.values.contains(context.actionHolder.premTypeFlashlight)) {
            val flashlightCompat = context.prefManager.flashlightCompat

            if (!actionHandler.flashlightController.isCreated
                    && !flashlightCompat)
                actionHandler.flashlightController.onCreate()
            else if (flashlightCompat)
                actionHandler.flashlightController.onDestroy()
        } else {
            actionHandler.flashlightController.onDestroy()
        }
    }

    fun hasAction(action: String) = getAction(action) != context.actionHolder.typeNoAction
    fun getAction(action: String) = actionMap[action]

    fun hasKey(key: String?) = actionMap.keys.contains(key)
}