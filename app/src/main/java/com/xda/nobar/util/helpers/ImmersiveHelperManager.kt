package com.xda.nobar.util.helpers

import android.content.Context
import android.graphics.Rect
import android.provider.Settings
import android.view.Surface
import android.view.ViewTreeObserver
import com.xda.nobar.util.*
import com.xda.nobar.views.BaseImmersiveHelperView

class ImmersiveHelperManager(private val context: Context, private val immersiveListener: (Boolean) -> Unit) {
    val base = BaseImmersiveHelperView(context, this) { left, top, right, bottom ->
        layout = Rect(left, top, right, bottom)
    }

    var layout = Rect()
        set(value) {
            field.set(value)

            updateImmersiveListener()
        }

    var helperAdded = false
        set(value) {
            field = value

            updateHelperState()
        }


    private var oldImm: String? = null
    private var hasRunForcedImm = false

    private fun updateImmersiveListener() {
        immersiveListener.invoke(isFullImmersiveSync())
    }

    private fun updateHelperState() {
        context.app.helperAdded = helperAdded
    }

    fun add() {
        val wm = context.app.wm

        try {
            if (!helperAdded) {
                wm.addView(base, base.params)
            } else {
                wm.updateViewLayout(base, base.params)
            }
        } catch (e: Exception) {}
    }

    fun remove() {
        val wm = context.app.wm

        try {
            wm.removeView(base)
        } catch (e: Exception) {}
    }

    fun addOnGlobalLayoutListener(listener: ViewTreeObserver.OnGlobalLayoutListener) {
        base.viewTreeObserver.addOnGlobalLayoutListener(listener)
    }

    fun enterNavImmersive() {
        base.enterNavImmersive()
    }

    fun exitNavImmersive() {
        base.exitNavImmersive()
    }

    fun isStatusImmersive() = run {
        val top = layout.top
        top <= 0 || isFullPolicyControl() || isStatusPolicyControl()
    }

    fun isNavImmersiveSync(): Boolean {
        val screenSize = context.unadjustedRealScreenSize
        val overscan = Rect(IWindowManager.leftOverscan, IWindowManager.topOverscan, IWindowManager.rightOverscan, IWindowManager.bottomOverscan)

        return if (isLandscape && !context.prefManager.useTabletMode) {
            when {
                cachedRotation == Surface.ROTATION_90 -> layout.right >= screenSize.x - overscan.bottom
                context.prefManager.useRot270Fix -> layout.right >= screenSize.x - overscan.top
                else -> layout.left <= 0
            }
        } else {
            layout.bottom >= screenSize.y - overscan.bottom
        } || isNavPolicyControl() || isFullPolicyControl()
    }

    fun isFullImmersiveSync() = isNavImmersiveSync() && isStatusImmersive()

    fun isFullPolicyControl() = Settings.Global.getString(context.contentResolver, Settings.Global.POLICY_CONTROL)?.contains("immersive.full") == true
    fun isNavPolicyControl() = Settings.Global.getString(context.contentResolver, Settings.Global.POLICY_CONTROL)?.contains("immersive.nav") == true
    fun isStatusPolicyControl() = Settings.Global.getString(context.contentResolver, Settings.Global.POLICY_CONTROL)?.contains("immersive.status") == true

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