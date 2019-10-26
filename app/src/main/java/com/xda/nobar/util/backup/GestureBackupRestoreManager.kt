package com.xda.nobar.util.backup

import android.content.Context
import android.net.Uri
import com.xda.nobar.util.PrefManager
import com.xda.nobar.util.prefManager

class GestureBackupRestoreManager(context: Context) : BaseBackupRestoreManager(context) {
    override val type = "gesture"
    override val name = "Gesture"
    override val prefsRes = arrayOf<Int>()

    override fun saveBackup(dest: Uri) {
        val data = HashMap<String, Any?>()
        val currentActions = HashMap<String, Int>().apply { prefManager.getActionsList(this) }

        data.putAll(currentActions)
        data[PrefManager.SECTIONED_PILL] = prefManager.sectionedPill

        serialize(dest, data)
    }
}