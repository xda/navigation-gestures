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
class ImmersiveHelperViewVertical(context: Context, manager: ImmersiveHelperManager) : BaseImmersiveHelperView(context, manager) {
    init {
        alpha = 0f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val height = getProperScreenHeightForRotation().absoluteValue

        immersiveListener?.invoke(
                h.absoluteValue >= height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val height = getProperScreenHeightForRotation().absoluteValue

        immersiveListener?.invoke(
                top.absoluteValue + bottom.absoluteValue
                        >= height)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        manager.verticalHelperAdded = true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        manager.verticalHelperAdded = false
    }

    override fun updateDimensions() {
        val width = 1
        val height = WindowManager.LayoutParams.MATCH_PARENT

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