package com.xda.nobar.views

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import com.xda.nobar.util.Utils
import com.xda.nobar.util.Utils.checkTouchWiz

class ImmersiveHelperView(context: Context) : View(context) {
    val params = WindowManager.LayoutParams().apply {
        type = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_PHONE else WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        width = 1
        height = 1
        flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        format = PixelFormat.TRANSPARENT
        x = 0
        y = 0
    }
    val app = Utils.getHandler(context)

    init {
        alpha = 0f
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
}