package com.xda.nobar.views

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
import java.lang.Exception
import kotlin.math.absoluteValue

@Suppress("DEPRECATION")
class ImmersiveHelperView(context: Context) : View(context) {
    val params = WindowManager.LayoutParams().apply {
        type = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_PHONE
        else WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
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
        val rot = context.rotation

        immersiveListener?.invoke(
                if (rot == Surface.ROTATION_270 || rot == Surface.ROTATION_90)
                    w.absoluteValue >= getProperScreenWidthForRotation().absoluteValue &&
                            h.absoluteValue >= getProperScreenHeightForRotation().absoluteValue
                else h.absoluteValue >= getProperScreenHeightForRotation().absoluteValue
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val rot = context.rotation

        immersiveListener?.invoke(
                if (rot == Surface.ROTATION_270 || rot == Surface.ROTATION_90)
                    left.absoluteValue + right.absoluteValue
                            >= getProperScreenWidthForRotation().absoluteValue &&
                            top.absoluteValue + bottom.absoluteValue
                            >= getProperScreenHeightForRotation().absoluteValue
                else bottom.absoluteValue + top.absoluteValue
                        >= getProperScreenHeightForRotation().absoluteValue
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        updateDimensions()

        context.app.helperAdded = true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        context.app.helperAdded = false
    }

    fun updateDimensions() {
        val width = 1
        val height = WindowManager.LayoutParams.MATCH_PARENT

        val landscape = context.isLandscape
        val tablet = context.prefManager.useTabletMode

        val newW = if (landscape && tablet) height else width
        val newH = if (landscape && tablet) width else height

        var changed = false

        if (params.width != newW) {
            params.width = newW

            changed = true
        }

        if (params.height != newH) {
            params.height = newH

            changed = true
        }

        if (changed) updateLayout()
    }

    fun updateLayout() {
        context.app.handler.post {
            try {
                context.getSystemServiceCast<WindowManager>(Context.WINDOW_SERVICE)
                        ?.updateViewLayout(this, params)
            } catch (e: Exception) {
            }
        }
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
        val rotation = context.rotation
        val screenHeight = context.realScreenSize.y
        val navHeight = context.navBarHeight

        return when (rotation) {
            Surface.ROTATION_90,
            Surface.ROTATION_270 -> {
                screenHeight + if (context.prefManager.useTabletMode
                        && context.app.navHidden) navHeight else 0
            }
            else -> {
                screenHeight + if (context.app.navHidden) navHeight else 0
            }
        }
    }

    private fun getProperScreenWidthForRotation(): Int {
        val rotation = context.rotation
        val screenWidth = context.realScreenSize.x
        val navHeight = context.navBarHeight

        return when (rotation) {
            Surface.ROTATION_90,
            Surface.ROTATION_270 -> {
                screenWidth + if (!context.prefManager.useTabletMode
                        && context.app.navHidden) navHeight else 0
            }

            else -> {
                screenWidth
            }
        }
    }
}