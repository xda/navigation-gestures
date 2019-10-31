package com.xda.nobar.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.Surface
import android.view.WindowManager
import android.widget.LinearLayout
import com.xda.nobar.util.*
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class NavBlackout : LinearLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    init {
        updateColor(Color.TRANSPARENT)
    }

    var isColorGone = false

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

    private var oldParams: WindowManager.LayoutParams? = null

    fun setGone(wm: WindowManager, gone: Boolean, instant: Boolean = false) = mainScope.launch {
        var newFlags = BaseParams.baseFlags
        var newColor = Color.BLACK
        if (gone) {
            newFlags = newFlags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            newColor = Color.TRANSPARENT
        }

        updateColor(newColor, instant).join()
        add(wm, newFlags).join()

        isColorGone = gone
    }

    private var runningAdd = false

    fun add(wm: WindowManager, flags: Int = BaseParams.baseFlags) = mainScope.launch {
        if (!runningAdd) {
            runningAdd = true
            val result = async {
                val params = if (context.prefManager.useTabletMode) bottomParams
                else when (cachedRotation) {
                    Surface.ROTATION_0 -> bottomParams
                    Surface.ROTATION_180 -> bottomParams
                    Surface.ROTATION_90 -> rightParams
                    Surface.ROTATION_270 -> if (context.prefManager.useRot270Fix) rightParams else leftParams
                    else -> return@async null
                }

                params.flags = flags

                if (!isAdded || !params.same(oldParams)) {
                    oldParams = params

                    return@async params
                } else return@async null
            }

            val await = result.await()

            if (await != null) {
                try {
                    if (isAdded) wm.updateViewLayout(this@NavBlackout, await)
                    else if (context.prefManager.isActive && !waitingToAdd) {
                        waitingToAdd = true
                        wm.addView(this@NavBlackout, await)
                    }
                } catch (e: Exception) {
                    e.logStack()
                }
            }

            runningAdd = false
        }
    }

    private var isTryingToRemove = false

    fun remove(wm: WindowManager, forRefresh: Boolean = false) = mainScope.launch {
        if (!isTryingToRemove && isAdded) {
            isTryingToRemove = true

            try {
                wm.removeView(this@NavBlackout)
            } catch (e: Exception) {
                isAdded = false
            }

            isTryingToRemove = false

            if (forRefresh) add(wm)
        }
    }

    private var colorAnimation: ValueAnimator? = null

    private var color: Int
        get() = synchronized(background) {
            if (background is ColorDrawable)
                (background as ColorDrawable).color
            else Color.TRANSPARENT
        }
        set(value) {
            if (background is ColorDrawable) {
                (background as ColorDrawable).color = value
            } else {
                background = ColorDrawable(value)
            }
        }

    private fun updateColor(newColor: Int, instant: Boolean = false) = mainScope.launch {
        if (background !is ColorDrawable) background = ColorDrawable(Color.TRANSPARENT)

        colorAnimation?.cancel()

        if (!instant) {
            colorAnimation = ValueAnimator.ofArgb(this@NavBlackout.color, newColor)
            colorAnimation?.duration = context.prefManager.animationDurationMs.toLong()
            colorAnimation?.addUpdateListener {
                val new = it.animatedValue.toString().toInt()

                this@NavBlackout.color = new
            }
            colorAnimation?.start()
        } else {
            this@NavBlackout.color = newColor
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
        companion object {
            const val baseFlags = FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                    FLAG_NOT_FOCUSABLE or
                    FLAG_LAYOUT_NO_LIMITS or
                    FLAG_TRANSLUCENT_NAVIGATION or
                    FLAG_SLIPPERY
        }

        init {
            type = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) TYPE_ACCESSIBILITY_OVERLAY else TYPE_PRIORITY_PHONE
            flags = baseFlags
            format = PixelFormat.TRANSLUCENT
        }
    }
}