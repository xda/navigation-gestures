package com.xda.nobar.tiles

import android.annotation.TargetApi
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.interfaces.OnGestureStateChangeListener
import com.xda.nobar.views.BarView

/**
 * QS Tile to toggle NoBar gestures
 */
@TargetApi(24)
class GestureToggle : TileService(), OnGestureStateChangeListener {
    private val app by lazy { application as App }

    override fun onCreate() {
        app.addGestureActivationListener(this)
    }

    override fun onStartListening() {
        updateState()
    }

    override fun onDestroy() {
        super.onDestroy()
        app.removeGestureActivationListener(this)
    }

    override fun onClick() {
        app.toggleGestureBar()
        updateState()
    }

    override fun onGestureStateChange(barView: BarView?, activated: Boolean) {
        updateState()
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun updateState() {
        val active = app.isPillShown()

        qsTile?.apply {
            state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = resources.getText(if (active) R.string.gestures_on else R.string.gestures_off)
            updateTile()
        }
    }
}