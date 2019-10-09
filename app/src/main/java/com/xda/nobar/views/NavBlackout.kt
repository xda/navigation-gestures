package com.xda.nobar.views

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.Surface
import android.view.WindowManager
import android.widget.LinearLayout
import com.xda.nobar.util.*
import kotlinx.coroutines.launch

class NavBlackout : LinearLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    init {
        setBackgroundColor(Color.BLACK)
    }

    var isAdded = false
        set(value) {
            field = value
            waitingToAdd = false
        }

    private var waitingToAdd = false

    private val bottomParams = BaseParams().apply {
        gravity = Gravity.BOTTOM
        height = context.navBarHeight
        width = WindowManager.LayoutParams.MATCH_PARENT
        y = -context.navBarHeight
    }

    private val leftParams = BaseParams().apply {
        gravity = Gravity.LEFT
        height = WindowManager.LayoutParams.MATCH_PARENT
        width = context.navBarHeight
        x = -context.navBarHeight
    }

    private val rightParams = BaseParams().apply {
        gravity = Gravity.RIGHT
        height = WindowManager.LayoutParams.MATCH_PARENT
        width = context.navBarHeight
        x = -context.navBarHeight
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isAdded = true

        if (context.prefManager.overlayNav)
            context.app.postAction { it.addBar() }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isAdded = false

        if (context.prefManager.overlayNav)
            context.app.postAction { it.remBar() }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
//        context.app.bar.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    private val addLock = Any()

    private var oldParams: WindowManager.LayoutParams? = null

    fun add(wm: WindowManager) {
        logicScope.launch {
            synchronized(addLock) {
                val params = if (context.prefManager.useTabletMode) bottomParams
                else when (cachedRotation) {
                    Surface.ROTATION_0 -> bottomParams
                    Surface.ROTATION_180 -> bottomParams
                    Surface.ROTATION_90 -> rightParams
                    Surface.ROTATION_270 -> if (context.prefManager.useRot270Fix) rightParams else leftParams
                    else -> return@launch
                }

                if (!isAdded || !params.same(oldParams)) {
                    oldParams = params

                    mainHandler.post {
                        try {
                            if (isAdded) wm.updateViewLayout(this@NavBlackout, params)
                            else if (!waitingToAdd) {
                                waitingToAdd = true
                                wm.addView(this@NavBlackout, params)
                            }
                        } catch (e: Exception) {
                            e.logStack()
                        }
                    }
                }
            }
        }
    }

    private var isTryingToRemove = false

    fun remove(wm: WindowManager) {
        synchronized(isTryingToRemove) {
            if (!isTryingToRemove && isAdded) {
                isTryingToRemove = true

                mainHandler.post {
                    try {
                        wm.removeView(this@NavBlackout)
                    } catch (e: Exception) {
                        isAdded = false
                    }

                    isTryingToRemove = false
                }
            }
        }
    }

    private fun WindowManager.LayoutParams.same(other: WindowManager.LayoutParams?) = run {
        other != null
                && x == other.x
                && y == other.y
                && width == other.width
                && height == other.height
                && flags == other.flags
                && type == other.type
                && gravity == other.gravity
    }

    private class BaseParams : WindowManager.LayoutParams() {
        init {
            type = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) TYPE_ACCESSIBILITY_OVERLAY else TYPE_PRIORITY_PHONE
            flags = FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                    FLAG_NOT_FOCUSABLE or
                    FLAG_LAYOUT_NO_LIMITS or
                    FLAG_TRANSLUCENT_NAVIGATION or
                    FLAG_SLIPPERY
        }
    }
}