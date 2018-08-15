package com.xda.nobar.views

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
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

    init {
        alpha = 0f
    }

    fun enterNavImmersive() {
        handler?.post {
            systemUiVisibility = systemUiVisibility or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

            if (checkTouchWiz(context)) Utils.forceTouchWizNavNotEnabled(context)
        }
    }

    fun exitNavImmersive() {
        handler?.post {
            systemUiVisibility = systemUiVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION.inv()

            if (checkTouchWiz(context)) Utils.forceTouchWizNavEnabled(context)
        }
    }
}