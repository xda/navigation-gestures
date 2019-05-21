package com.xda.nobar.util.helpers

import android.content.Context
import android.graphics.Rect
import android.provider.Settings
import android.view.Surface
import android.view.ViewTreeObserver
import com.xda.nobar.util.*
import com.xda.nobar.views.ImmersiveHelperViewHorizontal
import com.xda.nobar.views.ImmersiveHelperViewVertical
import kotlinx.coroutines.launch

class ImmersiveHelperManager(private val context: Context) {
    val horizontal = ImmersiveHelperViewHorizontal(context, this)
    val vertical = ImmersiveHelperViewVertical(context, this)

    var horizontalLayout = Rect()
        set(value) {
            if (field != value) {
                field.set(value)

                updateImmersiveListener()
            }
        }

    var verticalLayout = Rect()
        set(value) {
            if (field != value) {
                field.set(value)

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
        horizontal.immersiveListener = { left, top, right, bottom ->
            horizontalLayout = Rect(left, top, right, bottom)
        }
        vertical.immersiveListener = { left, top, right, bottom ->
            verticalLayout = Rect(left, top, right, bottom)
        }
    }

    private fun updateImmersiveListener() {
        isFullImmersive { immersiveListener?.invoke(it) }
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

    fun isStatusImmersive() = run {
        val top = verticalLayout.top
        top <= 0 || isFullPolicyControl() || isStatusPolicyControl()
    }

    fun isNavImmersive(callback: (Boolean) -> Unit) {
        logicScope.launch {
            val isNav = isNavImmersiveSync()

            mainScope.launch {
                callback.invoke(isNav || isFullPolicyControl() || isNavPolicyControl())
            }
        }
    }

    fun isNavImmersiveSync(): Boolean {
        val screenSize = context.unadjustedRealScreenSize
        val overscan = Rect().apply { context.app.wm.defaultDisplay.getOverscanInsets(this) }

        return if (isLandscape && !context.prefManager.useTabletMode) {
            when {
                cachedRotation == Surface.ROTATION_90 -> horizontalLayout.right > screenSize.x
                context.prefManager.useRot270Fix -> horizontalLayout.right > screenSize.x
                else -> horizontalLayout.left <= 0
            }
        } else {
            verticalLayout.bottom >= screenSize.y + if (overscan.bottom < 0) overscan.bottom else 0
        }
    }

    fun isFullImmersive(callback: (Boolean) -> Unit) {
        isNavImmersive {
            callback.invoke(it && isStatusImmersive())
        }
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