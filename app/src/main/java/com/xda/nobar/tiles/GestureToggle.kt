package com.xda.nobar.tiles

import android.annotation.TargetApi
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.interfaces.OnGestureStateChangeListener
import com.xda.nobar.util.Utils
import com.xda.nobar.views.BarView

/**
 * QS Tile to toggle NoBar gestures
 */
@TargetApi(24)
class GestureToggle : TileService(), OnGestureStateChangeListener {
    private lateinit var handler: App

    override fun onCreate() {
        handler = Utils.getHandler(this)
        handler.addGestureActivationListener(this)
    }

    override fun onStartListening() {
        updateState()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeGestureActivationListener(this)
    }

    override fun onClick() {
        handler.toggleGestureBar()
        updateState()
    }

    override fun invoke(barView: BarView, activated: Boolean) {
        updateState()
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun updateState() {
        val active = handler.isPillShown()

        qsTile?.apply {
            state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = resources.getText(if (active) R.string.gestures_on else R.string.gestures_off)
            updateTile()
        }
    }
}