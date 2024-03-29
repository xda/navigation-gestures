package com.xda.nobar.util.helpers

import android.content.Context
import android.graphics.Rect
import android.provider.Settings
import android.view.Surface
import android.view.WindowManager
import com.xda.nobar.util.*
import com.xda.nobar.views.ImmersiveHelperView

class ImmersiveHelperManager(private val context: Context,
                             private val immersiveListener: (Boolean) -> Unit)
    : IImersiveHelperManager() {
    private val view = ImmersiveHelperView(context) { left, top, right, bottom ->
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
            isRemovingOrAdding = false
        }
    var isRemovingOrAdding = false

    private var oldImm: String? = null
    private var hasRunForcedImm = false

    private fun updateImmersiveListener() {
        immersiveListener.invoke(isFullImmersive())
    }

    private fun updateHelperState() {
        context.app.helperAdded = helperAdded
    }

    fun add(wm: WindowManager) {
        try {
            if (!helperAdded) {
                isRemovingOrAdding = true
                wm.addView(view, view.params)
            } else {
                wm.updateViewLayout(view, view.params)
            }
        } catch (e: Exception) {}
    }

    fun remove(wm: WindowManager) {
        try {
            if (helperAdded) {
                isRemovingOrAdding = true
                wm.removeView(view)
            }
        } catch (e: Exception) {}
    }

    override fun enterNavImmersive() {
        view.enterNavImmersive()
    }

    override fun exitNavImmersive() {
        view.exitNavImmersive()
    }

    override fun isStatusImmersive() = run {
        val top = layout.top
        top <= 0 || isFullPolicyControl() || isStatusPolicyControl()
    }

    override fun isNavImmersive(): Boolean {
        val screenSize = context.unadjustedRealScreenSize
        val overscan = Rect(IWindowManager.leftOverscan, IWindowManager.topOverscan, IWindowManager.rightOverscan, IWindowManager.bottomOverscan)

        return if (isLandscape && !context.prefManager.useTabletMode) {
            when {
                cachedRotation == Surface.ROTATION_90 -> layout.right >= screenSize.x - overscan.bottom
                context.prefManager.useRot270Fix -> layout.right >= screenSize.x - overscan.top
                else -> layout.left <= overscan.bottom
            }
        } else {
            layout.bottom >= screenSize.y - overscan.bottom
        } || isNavPolicyControl() || isFullPolicyControl()
    }

    override fun isFullImmersive() = isNavImmersive() && isStatusImmersive()

    override fun isFullPolicyControl() = Settings.Global.getString(context.contentResolver, Settings.Global.POLICY_CONTROL)?.contains("immersive.full") == true
    override fun isNavPolicyControl() = Settings.Global.getString(context.contentResolver, Settings.Global.POLICY_CONTROL)?.contains("immersive.nav") == true
    override fun isStatusPolicyControl() = Settings.Global.getString(context.contentResolver, Settings.Global.POLICY_CONTROL)?.contains("immersive.status") == true

    override fun tempForcePolicyControlForRecents() {
        oldImm = Settings.Global.getString(context.contentResolver, POLICY_CONTROL)
        if (context.hasWss) Settings.Global.putString(context.contentResolver, POLICY_CONTROL, "immersive.navigation=*")
        hasRunForcedImm = true
    }

    override fun putBackOldImmersive() {
        if (hasRunForcedImm) {
            if (context.hasWss) Settings.Global.putString(context.contentResolver, POLICY_CONTROL, oldImm)
            hasRunForcedImm = false
        }
    }
}