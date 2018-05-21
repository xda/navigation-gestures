package com.xda.nobar.tiles

import android.annotation.TargetApi
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.util.Utils

/**
 * QS Tile to toggle NoBar
 */
@TargetApi(24)
class Toggle : TileService(), App.ActivationListener {
    private lateinit var handler: App

    override fun onCreate() {
        handler = Utils.getHandler(this)
        handler.addActivationListener(this)
    }

    override fun onStartListening() {
        updateState()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeActivationListener(this)
    }

    override fun onClick() {
        handler.toggle()
        Utils.setOffForRebootOrScreenLock(this, false)
        updateState()
    }

    override fun onChange(activated: Boolean) {
        updateState()
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun updateState() {
        val active = handler.isActivated()

        qsTile?.apply {
            state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            icon = Icon.createWithResource(packageName, (if (active) R.drawable.ic_border_clear_black_24dp else R.drawable.ic_border_bottom_black_24dp))
            label = resources.getText(if (active) R.string.hidden else R.string.shown)
            updateTile()
        }
    }
}