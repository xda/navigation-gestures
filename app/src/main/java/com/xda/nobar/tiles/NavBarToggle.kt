package com.xda.nobar.tiles

import android.annotation.TargetApi
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.xda.nobar.R
import com.xda.nobar.interfaces.OnNavBarHideStateChangeListener
import com.xda.nobar.util.app
import com.xda.nobar.util.checkNavHiddenAsync

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
        checkNavHiddenAsync { active ->
            qsTile?.apply {
                state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                icon = Icon.createWithResource(packageName, (if (active) R.drawable.border_clear else R.drawable.border_bottom))
                label = resources.getText(if (active) R.string.nav_hidden else R.string.nav_shown)
                updateTile()
            }
        }
    }
}