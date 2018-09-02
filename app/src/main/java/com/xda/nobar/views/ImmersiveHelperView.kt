package com.xda.nobar.views

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.Surface
import android.view.View
import android.view.WindowManager
import com.xda.nobar.util.Utils
import com.xda.nobar.util.Utils.checkTouchWiz
import kotlin.math.absoluteValue

class ImmersiveHelperView(context: Context) : View(context) {
    val params = WindowManager.LayoutParams().apply {
        type = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_PHONE else WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        width = 1
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        format = PixelFormat.TRANSPARENT
        x = 0
        y = 0
        gravity = Gravity.LEFT or Gravity.BOTTOM
    }
    val app = Utils.getHandler(context)

    var shouldReAddOnDetach = false
    var immersiveListener: ((isImmersive: Boolean) -> Unit)? = null

    init {
        alpha = 0f
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        immersiveListener?.invoke(bottom.absoluteValue >= getProperScreenHeightForRotation().absoluteValue)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        app.helperAdded = true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        if (shouldReAddOnDetach) {
            app.addImmersiveHelper(false)
            shouldReAddOnDetach = false
        } else app.helperAdded = false
    }

    fun enterNavImmersive() {
        if (!isFlagNavImmersive()) {
            app.handler.post {
                systemUiVisibility = systemUiVisibility or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

                if (checkTouchWiz(context)) Utils.forceTouchWizNavNotEnabled(context)
            }
        }
    }

    fun exitNavImmersive() {
        if (isFlagNavImmersive()) {
            app.handler.post {
                systemUiVisibility = systemUiVisibility and
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION.inv() and
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY.inv()

                if (checkTouchWiz(context)) Utils.forceTouchWizNavEnabled(context)
            }
        }
    }

    private var oldImm: String? = null
    private var hasRunForcedImm = false

    fun tempForcePolicyControlForRecents() {
        oldImm = Settings.Global.getString(context.contentResolver, Settings.Global.POLICY_CONTROL)
        Settings.Global.putString(context.contentResolver, Settings.Global.POLICY_CONTROL, "immersive.navigation=*")
        hasRunForcedImm = true
    }

    fun putBackOldImmersive() {
        if (hasRunForcedImm) {
            Settings.Global.putString(context.contentResolver, Settings.Global.POLICY_CONTROL, oldImm)
            hasRunForcedImm = false
        }
    }

    fun isFlagNavImmersive() = systemUiVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION != 0

    fun isNavImmersive(): Boolean {
        val imm = Settings.Global.getString(context.contentResolver, Settings.Global.POLICY_CONTROL)
        return imm?.contains("navigation") == true
                || imm?.contains("immersive.full") == true
                || systemUiVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION != 0
    }

    fun isFullImmersive(): Boolean {
        val imm = Settings.Global.getString(context.contentResolver, Settings.Global.POLICY_CONTROL)
        return imm?.contains("immersive.full") == true
                || systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN != 0
    }

    fun toggleScreenOn(): Boolean {
        val hasScreenOn = params.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON == WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

        if (hasScreenOn) params.flags = params.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.inv()
        else params.flags = params.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

        handler?.post {
            app.wm.updateViewLayout(this, params)
        }

        return !hasScreenOn
    }

    private fun getProperScreenHeightForRotation(): Int {
        val rotation = app.wm.defaultDisplay.rotation
        val screenHeight = Utils.getRealScreenSize(context).y
        val navHeight = Utils.getNavBarHeight(context)

        return when (rotation) {
            Surface.ROTATION_90,
            Surface.ROTATION_270 -> {
                screenHeight + if (Utils.useTabletMode(context)) if (app.navHidden) navHeight else 0 else 0
            }
            else -> {
                screenHeight + if (app.navHidden) Utils.getNavBarHeight(context) else 0
            }
        }
    }
}