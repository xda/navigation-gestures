package com.xda.nobar.util.backup

import android.content.Context
import android.net.Uri
import com.xda.nobar.util.PrefManager
import com.xda.nobar.util.prefManager

class BehaviorBackupRestoreManager(context: Context) : BaseBackupRestoreManager(context) {
    override val type = "behavior"
    override val name = "Behavior"

    override fun saveBackup(dest: Uri) {
        val data = HashMap<String, Any?>()

        data[PrefManager.SHOW_NAV_WITH_KEYBOARD] = prefManager.get(PrefManager.SHOW_NAV_WITH_KEYBOARD)
        data[PrefManager.AUDIO_FEEDBACK] = prefManager.get(PrefManager.AUDIO_FEEDBACK)
        data[PrefManager.STATIC_PILL] = prefManager.get(PrefManager.STATIC_PILL)
        data[PrefManager.AUTO_HIDE_PILL] = prefManager.get(PrefManager.AUTO_HIDE_PILL)
        data[PrefManager.AUTO_HIDE_PILL_PROGRESS] = prefManager.get(PrefManager.AUTO_HIDE_PILL_PROGRESS)
        data[PrefManager.HIDE_IN_FULLSCREEN] = prefManager.get(PrefManager.HIDE_IN_FULLSCREEN)
        data[PrefManager.HIDE_IN_FULLSCREEN_PROGRESS] = prefManager.get(PrefManager.HIDE_IN_FULLSCREEN_PROGRESS)
        data[PrefManager.HIDE_PILL_ON_KEYBOARD] = prefManager.get(PrefManager.HIDE_PILL_ON_KEYBOARD)
        data[PrefManager.HIDE_PILL_ON_KEYBOARD_PROGRESS] = prefManager.get(PrefManager.HIDE_PILL_ON_KEYBOARD_PROGRESS)
        data[PrefManager.ENABLE_IN_CAR_MODE] = prefManager.get(PrefManager.ENABLE_IN_CAR_MODE)
        data[PrefManager.LOCKSCREEN_OVERSCAN] = prefManager.get(PrefManager.LOCKSCREEN_OVERSCAN)
        data[PrefManager.BLACKLISTED_NAV_APPS] = prefManager.get(PrefManager.BLACKLISTED_NAV_APPS)
        data[PrefManager.BLACKLISTED_BAR_APPS] = prefManager.get(PrefManager.BLACKLISTED_BAR_APPS)
        data[PrefManager.VIBRATION_DURATION] = prefManager.get(PrefManager.VIBRATION_DURATION)
        data[PrefManager.ANIM_DURATION] = prefManager.get(PrefManager.ANIM_DURATION)
        data[PrefManager.HOLD_TIME] = prefManager.get(PrefManager.HOLD_TIME)
        data[PrefManager.LARGER_HITBOX] = prefManager.get(PrefManager.LARGER_HITBOX)
        data[PrefManager.X_THRESHOLD] = prefManager.get(PrefManager.X_THRESHOLD)
        data[PrefManager.Y_THRESHOLD] = prefManager.get(PrefManager.Y_THRESHOLD)
        data[PrefManager.Y_THRESHOLD_DOWN] = prefManager.get(PrefManager.Y_THRESHOLD_DOWN)
        data[PrefManager.CUSTOM_VIBRATION_STRENGTH] = prefManager.get(PrefManager.CUSTOM_VIBRATION_STRENGTH)
        data[PrefManager.VIBRATION_STRENGTH] = prefManager.get(PrefManager.VIBRATION_STRENGTH)

        serialize(dest, data)
    }
}