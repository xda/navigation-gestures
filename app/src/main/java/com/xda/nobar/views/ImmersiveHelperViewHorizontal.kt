package com.xda.nobar.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.Surface
import android.view.View
import android.view.WindowManager
import com.xda.nobar.util.*
import com.xda.nobar.util.helpers.ImmersiveHelperManager
import java.lang.Exception
import kotlin.math.absoluteValue

@SuppressLint("ViewConstructor")
@Suppress("DEPRECATION")
class ImmersiveHelperViewHorizontal(context: Context, manager: ImmersiveHelperManager) : BaseImmersiveHelperView(context, manager) {
    init {
        alpha = 0f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        immersiveListener?.invoke(
                w.absoluteValue >= getProperScreenWidthForRotation().absoluteValue)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        immersiveListener?.invoke(
                left.absoluteValue + right.absoluteValue
                        >= getProperScreenWidthForRotation().absoluteValue)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        manager.horizontalHelperAdded = true

        WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        manager.horizontalHelperAdded = false
    }

    override fun updateDimensions() {
        val width = WindowManager.LayoutParams.MATCH_PARENT
        val height = 1

        var changed = false

        if (params.width != width) {
            params.width = width

            changed = true
        }

        if (params.height != height) {
            params.height = height

            changed = true
        }

        if (changed) updateLayout()
    }
}