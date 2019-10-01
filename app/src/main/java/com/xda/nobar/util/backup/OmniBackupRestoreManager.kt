package com.xda.nobar.util.backup

import android.content.Context
import android.net.Uri
import com.xda.nobar.util.prefManager

class OmniBackupRestoreManager(context: Context) : BaseBackupRestoreManager(context) {
    override val type = "omni"
    override val name = "Omni"

    override fun saveBackup(dest: Uri) {
        val data = HashMap<String, Any?>()

        data.putAll(prefManager.all)

        serialize(dest, data)
    }

    override fun onBeforeApply() {
        prefManager.clear()
    }
}