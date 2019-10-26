package com.xda.nobar.util.backup

import android.content.Context
import com.xda.nobar.R

class AppearanceBackupRestoreManager(context: Context) : BaseBackupRestoreManager(context) {
    override val type = "appearance"
    override val name = "Appearance"
    override val prefsRes = arrayOf(R.xml.prefs_appearance, R.xml.prefs_pill_appearance, R.xml.prefs_side_appearance)
}