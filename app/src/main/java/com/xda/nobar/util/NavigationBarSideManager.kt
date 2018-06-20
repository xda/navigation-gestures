package com.xda.nobar.util

import android.content.Context
import android.graphics.Point
import android.util.DisplayMetrics
import android.view.Surface
import android.view.WindowManager

class NavigationBarSideManager(private val context: Context) {
    companion object {
        const val NAV_BAR_LEFT = 1 shl 0
        const val NAV_BAR_RIGHT = 1 shl 1
        const val NAV_BAR_BOTTOM = 1 shl 2
    }

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val display = wm.defaultDisplay

    private val size = Point().apply {
        display.getRealSize(this)
    }
    private val metrics = DisplayMetrics().apply {
        display.getRealMetrics(this)
    }

    private val navBarCanMove: Boolean

    init {
        val shortSize = if (size.x > size.y) size.y else size.x
        val shortSizeDp = shortSize * DisplayMetrics.DENSITY_DEFAULT / metrics.density

        navBarCanMove = size.x != size.y && shortSizeDp < 600
    }

    fun navBarPosition(): Int {
        val currentSize = Point().apply { display.getRealSize(this) }
        return if (navBarCanMove && currentSize.x > currentSize.y) {
            if (display.rotation == Surface.ROTATION_270 && !Utils.useRot270Fix(context))
                NAV_BAR_LEFT
            else
                NAV_BAR_RIGHT
        } else
            NAV_BAR_BOTTOM
    }
}