package com.xda.nobar.views

import android.annotation.SuppressLint
import android.content.Context
import android.view.WindowManager
import com.xda.nobar.util.helpers.ImmersiveHelperManager

@SuppressLint("ViewConstructor")
@Suppress("DEPRECATION")
class ImmersiveHelperViewVertical(context: Context, manager: ImmersiveHelperManager,
                                  immersiveListener: (left: Int, top: Int, right: Int, bottom: Int) -> Unit) : BaseImmersiveHelperView(context, manager, immersiveListener) {
    init {
        alpha = 0f
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

//        manager.verticalHelperAdded = true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

//        manager.verticalHelperAdded = false
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