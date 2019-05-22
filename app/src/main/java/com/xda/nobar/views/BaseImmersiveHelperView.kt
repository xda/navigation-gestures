package com.xda.nobar.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import com.xda.nobar.util.app
import com.xda.nobar.util.helpers.ImmersiveHelperManager
import com.xda.nobar.util.isTouchWiz
import com.xda.nobar.util.mainScope
import com.xda.nobar.util.touchWizNavEnabled
import kotlinx.coroutines.launch

@SuppressLint("ViewConstructor")
@Suppress("DEPRECATION")
open class BaseImmersiveHelperView(context: Context, val manager: ImmersiveHelperManager,
                                   private val immersiveListener: (left: Int, top: Int, right: Int, bottom: Int) -> Unit) : View(context), ViewTreeObserver.OnGlobalLayoutListener {
    val params = WindowManager.LayoutParams().apply {
        width = WindowManager.LayoutParams.MATCH_PARENT
        height = WindowManager.LayoutParams.MATCH_PARENT
        type = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        else WindowManager.LayoutParams.TYPE_PRIORITY_PHONE
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        format = PixelFormat.TRANSPARENT
        x = 0
        y = 0
        gravity = Gravity.LEFT
    }

    init {
        alpha = 0f
        fitsSystemWindows = false
    }

    private val rect = Rect()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        viewTreeObserver.removeOnGlobalLayoutListener(this)
    }

    override fun onGlobalLayout() {
        synchronized(rect) {
            rect.apply { getBoundsOnScreen(this) }

            immersiveListener.invoke(rect.left, rect.top, rect.right, rect.bottom)
        }

        context.app.uiHandler.onGlobalLayout()
    }

    fun enterNavImmersive() {
        mainScope.launch {
            systemUiVisibility = systemUiVisibility or
                    SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    SYSTEM_UI_FLAG_IMMERSIVE_STICKY

            if (context.isTouchWiz) context.touchWizNavEnabled = false
        }
    }

    fun exitNavImmersive() {
        mainScope.launch {
            systemUiVisibility = systemUiVisibility and
                    SYSTEM_UI_FLAG_HIDE_NAVIGATION.inv() and
                    SYSTEM_UI_FLAG_IMMERSIVE_STICKY.inv()

            if (context.isTouchWiz) context.touchWizNavEnabled = true
        }
    }
}