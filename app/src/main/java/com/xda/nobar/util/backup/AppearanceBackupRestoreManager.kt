package com.xda.nobar.util.backup

import android.content.Context
import android.net.Uri
import com.xda.nobar.util.PrefManager
import com.xda.nobar.util.prefManager

class AppearanceBackupRestoreManager(context: Context) : BaseBackupRestoreManager(context) {
    override val type = "appearance"
    override val name = "Appearance"

    override fun saveBackup(dest: Uri) {
        val data = HashMap<String, Any?>()

        data[PrefManager.SHOW_SHADOW] = prefManager.get(PrefManager.SHOW_SHADOW)
        data[PrefManager.PILL_BG] = prefManager.get(PrefManager.PILL_BG)
        data[PrefManager.PILL_FG] = prefManager.get(PrefManager.PILL_FG)
        data[PrefManager.PILL_CORNER_RADIUS] = prefManager.get(PrefManager.PILL_CORNER_RADIUS)
        data[PrefManager.USE_PIXELS_WIDTH] = prefManager.get(PrefManager.USE_PIXELS_WIDTH)
        data[PrefManager.CUSTOM_WIDTH_PERCENT] = prefManager.get(PrefManager.CUSTOM_WIDTH_PERCENT)
        data[PrefManager.CUSTOM_WIDTH] = prefManager.get(PrefManager.CUSTOM_WIDTH)
        data[PrefManager.USE_PIXELS_HEIGHT] = prefManager.get(PrefManager.USE_PIXELS_HEIGHT)
        data[PrefManager.CUSTOM_HEIGHT_PERCENT] = prefManager.get(PrefManager.CUSTOM_HEIGHT_PERCENT)
        data[PrefManager.CUSTOM_HEIGHT] = prefManager.get(PrefManager.CUSTOM_HEIGHT)
        data[PrefManager.USE_PIXELS_X] = prefManager.get(PrefManager.USE_PIXELS_X)
        data[PrefManager.CUSTOM_X_PERCENT] = prefManager.get(PrefManager.CUSTOM_X_PERCENT)
        data[PrefManager.CUSTOM_X] = prefManager.get(PrefManager.CUSTOM_X)
        data[PrefManager.USE_PIXELS_Y] = prefManager.get(PrefManager.USE_PIXELS_Y)
        data[PrefManager.CUSTOM_Y_PERCENT] = prefManager.get(PrefManager.CUSTOM_Y_PERCENT)
        data[PrefManager.CUSTOM_Y] = prefManager.get(PrefManager.CUSTOM_Y)
        data[PrefManager.PILL_DIVIDER_COLOR] = prefManager.get(PrefManager.PILL_DIVIDER_COLOR)

        serialize(dest, data)
    }
}