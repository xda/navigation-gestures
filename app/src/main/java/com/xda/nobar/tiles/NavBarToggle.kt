package com.xda.nobar.tiles

import android.annotation.TargetApi
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.interfaces.OnNavBarHideStateChangeListener
import com.xda.nobar.util.app

/**
 * QS Tile to toggle navbar
 */
@TargetApi(24)
class NavBarToggle : TileService(), OnNavBarHideStateChangeListener {
    override fun onCreate() {
        app.addNavBarHideListener(this)
    }

    override fun onStartListening() {
        updateState()
    }

    override fun onDestroy() {
        super.onDestroy()
        app.removeNavbarHideListener(this)
    }

    override fun onClick() {
        app.toggleNavState()
        updateState()
    }

    override fun onNavStateChange(hidden: Boolean) {
        updateState()
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun updateState() {
        val active = app.isNavBarHidden()

        qsTile?.apply {
            state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            icon = Icon.createWithResource(packageName, (if (active) R.drawable.ic_border_clear_black_24dp else R.drawable.ic_border_bottom_black_24dp))
            label = resources.getText(if (active) R.string.nav_hidden else R.string.nav_shown)
            updateTile()
        }
    }
}