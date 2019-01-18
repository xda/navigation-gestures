package com.xda.nobar.util.helpers

import android.content.Context
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import com.xda.nobar.util.POLICY_CONTROL
import com.xda.nobar.util.app
import com.xda.nobar.util.getSystemServiceCast
import com.xda.nobar.util.hasWss
import com.xda.nobar.views.ImmersiveHelperViewHorizontal
import com.xda.nobar.views.ImmersiveHelperViewVertical

class ImmersiveHelperManager(private val context: Context) {
    val horizontal = ImmersiveHelperViewHorizontal(context, this)
    val vertical = ImmersiveHelperViewVertical(context, this)

    var isHorizontalImmersive = false
    var isVerticalImmersive = false

    var horizontalHelperAdded = false
        set(value) {
            field = value

            context.app.helperAdded = horizontalHelperAdded && verticalHelperAdded
        }
    var verticalHelperAdded = false
        set(value) {
            field = value

            context.app.helperAdded = horizontalHelperAdded && verticalHelperAdded
        }


    var immersiveListener: ((Boolean) -> Unit)? = null

    private var oldImm: String? = null
    private var hasRunForcedImm = false

    init {
        horizontal.immersiveListener = { imm ->
            isHorizontalImmersive = imm

            immersiveListener?.invoke(isHorizontalImmersive && isVerticalImmersive)
        }
        vertical.immersiveListener = { imm ->
            isVerticalImmersive = imm

            immersiveListener?.invoke(isHorizontalImmersive && isVerticalImmersive)
        }
    }

    fun add() {
        val wm = context.getSystemServiceCast<WindowManager>(Context.WINDOW_SERVICE)

        try {
            wm?.addView(horizontal, horizontal.params)
        } catch (e: Exception) {}

        try {
            wm?.addView(vertical, vertical.params)
        } catch (e: Exception) {}
    }

    fun remove() {
        val wm = context.getSystemServiceCast<WindowManager>(Context.WINDOW_SERVICE)

        try {
            wm?.removeView(horizontal)
        } catch (e: Exception) {}

        try {
            wm?.removeView(vertical)
        } catch (e: Exception) {}
    }

    fun update() {
        horizontal.updateDimensions()
        vertical.updateDimensions()
    }

    fun addOnGlobalLayoutListener(listener: ViewTreeObserver.OnGlobalLayoutListener) {
        horizontal.viewTreeObserver.addOnGlobalLayoutListener(listener)
        vertical.viewTreeObserver.addOnGlobalLayoutListener(listener)
    }

    fun setOnSystemUiVisibilityChangeListener(listener: View.OnSystemUiVisibilityChangeListener) {
        horizontal.setOnSystemUiVisibilityChangeListener(listener)
        vertical.setOnSystemUiVisibilityChangeListener(listener)
    }

    fun enterNavImmersive() {
        horizontal.enterNavImmersive()
        vertical.enterNavImmersive()
    }

    fun exitNavImmersive() {
        horizontal.exitNavImmersive()
        vertical.exitNavImmersive()
    }

    fun isNavImmersive() =
            horizontal.isNavImmersive() || vertical.isNavImmersive()

    fun isFullImmersive() =
            horizontal.isFullImmersive() || vertical.isFullImmersive()

    fun tempForcePolicyControlForRecents() {
        oldImm = Settings.Global.getString(context.contentResolver, POLICY_CONTROL)
        if (context.hasWss) Settings.Global.putString(context.contentResolver, POLICY_CONTROL, "immersive.navigation=*")
        hasRunForcedImm = true
    }

    fun putBackOldImmersive() {
        if (hasRunForcedImm) {
            if (context.hasWss) Settings.Global.putString(context.contentResolver, POLICY_CONTROL, oldImm)
            hasRunForcedImm = false
        }
    }
}