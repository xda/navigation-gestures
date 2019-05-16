package com.xda.nobar.util.helpers

import android.content.Context
import android.provider.Settings
import android.view.ViewTreeObserver
import com.xda.nobar.util.*
import com.xda.nobar.views.ImmersiveHelperViewHorizontal
import com.xda.nobar.views.ImmersiveHelperViewVertical

class ImmersiveHelperManager(private val context: Context) {
    val horizontal = ImmersiveHelperViewHorizontal(context, this)
    val vertical = ImmersiveHelperViewVertical(context, this)

    var isHorizontalImmersive = false
        set(value) {
            if (field != value) {
                field = value

                updateImmersiveListener()
            }
        }
    var isVerticalImmersive = false
        set(value) {
            if (field != value) {
                field = value

                updateImmersiveListener()
            }
        }

    var horizontalHelperAdded = false
        set(value) {
            field = value

            updateHelperState()
        }
    var verticalHelperAdded = false
        set(value) {
            field = value

            updateHelperState()
        }


    var immersiveListener: ((Boolean) -> Unit)? = null

    private var oldImm: String? = null
    private var hasRunForcedImm = false

    init {
        horizontal.immersiveListener = { imm ->
            isHorizontalImmersive = imm
        }
        vertical.immersiveListener = { imm ->
            isVerticalImmersive = imm
        }
    }

    private fun updateImmersiveListener() {
        if (context.isLandscape && !context.prefManager.useTabletMode) {
            immersiveListener?.invoke(isVerticalImmersive)
        } else {
            immersiveListener?.invoke(isHorizontalImmersive && isVerticalImmersive)
        }
    }

    private fun updateHelperState() {
        context.app.helperAdded = horizontalHelperAdded && verticalHelperAdded
    }

    fun add() {
        val wm = context.app.wm

        try {
            if (!horizontalHelperAdded) {
                wm.addView(horizontal, horizontal.params)
            } else {
                wm.updateViewLayout(horizontal, horizontal.params)
            }
        } catch (e: Exception) {}

        try {
            if (!verticalHelperAdded) {
                wm.addView(vertical, vertical.params)
            } else {
                wm.updateViewLayout(vertical, vertical.params)
            }
        } catch (e: Exception) {}
    }

    fun remove() {
        val wm = context.app.wm

        try {
            wm.removeView(horizontal)
        } catch (e: Exception) {}

        try {
            wm.removeView(vertical)
        } catch (e: Exception) {}
    }

    fun addOnGlobalLayoutListener(listener: ViewTreeObserver.OnGlobalLayoutListener) {
        horizontal.viewTreeObserver.addOnGlobalLayoutListener(listener)
        vertical.viewTreeObserver.addOnGlobalLayoutListener(listener)
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