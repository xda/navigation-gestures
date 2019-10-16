package com.xda.nobar.util.backup

import android.content.Context
import android.net.Uri
import com.xda.nobar.R
import com.xda.nobar.util.PrefManager
import com.xda.nobar.util.prefManager

class BehaviorBackupRestoreManager(context: Context) : BaseBackupRestoreManager(context) {
    override val type = "behavior"
    override val name = "Behavior"
    override val prefsRes = R.xml.prefs_behavior

    override fun saveBackup(dest: Uri) {
        val data = buildData()

        data[PrefManager.AUTO_HIDE_PILL_PROGRESS] = prefManager.get(PrefManager.AUTO_HIDE_PILL_PROGRESS)
        data[PrefManager.HIDE_IN_FULLSCREEN_PROGRESS] = prefManager.get(PrefManager.HIDE_IN_FULLSCREEN_PROGRESS)
        data[PrefManager.HIDE_PILL_ON_KEYBOARD_PROGRESS] = prefManager.get(PrefManager.HIDE_PILL_ON_KEYBOARD_PROGRESS)
        data[PrefManager.FADE_AFTER_SPECIFIED_DELAY_PROGRESS] = prefManager.get(PrefManager.FADE_AFTER_SPECIFIED_DELAY_PROGRESS)
        data[PrefManager.FADE_IN_FULLSCREEN_APPS_PROGRESS] = prefManager.get(PrefManager.FADE_IN_FULLSCREEN_APPS_PROGRESS)

        serialize(dest, data)
    }
}