package com.xda.nobar.views

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.Surface
import android.view.View
import android.view.WindowManager
import com.xda.nobar.util.*
import kotlin.math.absoluteValue

@Suppress("DEPRECATION")
class ImmersiveHelperView(context: Context) : View(context) {
    val params = WindowManager.LayoutParams().apply {
        type = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_PHONE
        else WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        width = 1
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        format = PixelFormat.TRANSPARENT
        x = 0
        y = 0
        gravity = Gravity.LEFT or Gravity.BOTTOM
    }

    var immersiveListener: ((isImmersive: Boolean) -> Unit)? = null

    init {
        alpha = 0f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        immersiveListener?.invoke(h >= getProperScreenHeightForRotation().absoluteValue)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        immersiveListener?.invoke(bottom.absoluteValue >= getProperScreenHeightForRotation().absoluteValue)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        context.app.helperAdded = true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        context.app.helperAdded = false
    }

    fun enterNavImmersive() {
        context.app.handler.post {
            systemUiVisibility = systemUiVisibility or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

            if (context.isTouchWiz) context.touchWizNavEnabled = false
        }
    }

    fun exitNavImmersive() {
        context.app.handler.post {
            systemUiVisibility = systemUiVisibility and
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION.inv() and
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY.inv()

            if (context.isTouchWiz) context.touchWizNavEnabled = true
        }
    }

    private var oldImm: String? = null
    private var hasRunForcedImm = false

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

    fun isFlagNavImmersive() = systemUiVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION != 0

    fun isNavImmersive(): Boolean {
        val imm = Settings.Global.getString(context.contentResolver, POLICY_CONTROL)
        return imm?.contains("navigation") == true
                || imm?.contains("immersive.full") == true
                || systemUiVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION != 0
    }

    fun isFullImmersive(): Boolean {
        val imm = Settings.Global.getString(context.contentResolver, POLICY_CONTROL)
        return imm?.contains("immersive.full") == true
                || systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN != 0
    }

    private fun getProperScreenHeightForRotation(): Int {
        val rotation = context.app.wm.defaultDisplay.rotation
        val screenHeight = context.realScreenSize.y
        val navHeight = context.navBarHeight

        return when (rotation) {
            Surface.ROTATION_90,
            Surface.ROTATION_270 -> {
                screenHeight + if (context.app.prefManager.useTabletMode) if (context.app.navHidden) navHeight else 0 else 0
            }
            else -> {
                screenHeight + if (context.app.navHidden) navHeight else 0
            }
        }
    }
}