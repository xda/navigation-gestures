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
open class BaseImmersiveHelperView(context: Context, val manager: ImmersiveHelperManager) : View(context) {
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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        updateDimensions()
    }

    open fun updateDimensions() {
        val width = 1
        val height = WindowManager.LayoutParams.MATCH_PARENT

        val landscape = context.isLandscape
        val tablet = context.prefManager.useTabletMode

        val newW = if (landscape && !tablet) height else width
        val newH = if (landscape && !tablet) width else height

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

    fun getProperScreenHeightForRotation(): Int {
        val screenHeight = context.realScreenSize.y
        val navHeight = context.navBarHeight

        return if (context.isLandscape) {
            screenHeight + (if (context.prefManager.useTabletMode
                    && context.app.navHidden && isNavImmersive()) navHeight else 0) - (if (context.prefManager.useFullOverscan) 0 else 1)
        } else {
            screenHeight + (if (context.app.navHidden && isNavImmersive()) navHeight else 0) - (if (context.prefManager.useFullOverscan) 0 else 1)
        }
    }

    fun getProperScreenWidthForRotation(): Int {
        val screenWidth = context.realScreenSize.x
        val navHeight = context.navBarHeight

        return if (context.isLandscape) {
            screenWidth + (if (!context.prefManager.useTabletMode
                    && context.app.navHidden && isNavImmersive()) navHeight else 0) - (if (context.prefManager.useFullOverscan) 0 else 1)
        } else screenWidth
    }
}