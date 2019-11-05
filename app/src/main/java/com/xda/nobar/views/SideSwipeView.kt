package com.xda.nobar.views

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.animation.addListener
import com.xda.nobar.util.*
import com.xda.nobar.util.helpers.SideSwipeGestureManager
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class LeftSideSwipeView : SideSwipeView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override val side = Side.LEFT
    override val keysToListenFor = arrayOf(
        PrefManager.LEFT_SIDE_GESTURE_POSITION,
        PrefManager.LEFT_SIDE_GESTURE_HEIGHT,
        PrefManager.LEFT_SIDE_GESTURE_WIDTH
    )
}

class RightSideSwipeView : SideSwipeView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override val side = Side.RIGHT
    override val keysToListenFor = arrayOf(
        PrefManager.RIGHT_SIDE_GESTURE_POSITION,
        PrefManager.RIGHT_SIDE_GESTURE_HEIGHT,
        PrefManager.RIGHT_SIDE_GESTURE_WIDTH
    )
}

abstract class SideSwipeView : View, SharedPreferences.OnSharedPreferenceChangeListener {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    internal abstract val side: Side
    internal abstract val keysToListenFor: Array<String>

    private val gestureManager by lazy { SideSwipeGestureManager(this) }

    var isAttached = false

    private val heightPercent: Int
        get() = context.prefManager.run {
            when (side) {
                Side.LEFT -> leftSideGestureHeight
                Side.RIGHT -> rightSideGestureHeight
            }
        }
    private val positionPercent: Int
        get() = context.prefManager.run {
            when (side) {
                Side.LEFT -> leftSideGesturePosition
                Side.RIGHT -> rightSideGesturePosition
            }
        }
    private val windowWidth: Int
        get() = context.prefManager.run {
            dpAsPx(
                when (side) {
                    Side.LEFT -> leftSideGestureWidth
                    Side.RIGHT -> rightSideGestureWidth
                } / 10f
            )
        }

    private val params: WindowManager.LayoutParams
        get() = BaseParams().apply {
            gravity = when (side) {
                Side.LEFT -> context.app.bar.run {
                    when {
                        is270Vertical -> Gravity.TOP
                        is90Vertical -> Gravity.BOTTOM
                        else -> Gravity.LEFT
                    }
                }
                Side.RIGHT -> context.app.bar.run {
                    when {
                        is270Vertical -> Gravity.BOTTOM
                        is90Vertical -> Gravity.TOP
                        else -> Gravity.RIGHT
                    }
                }
            } or Gravity.CENTER

            width = if (!context.app.bar.isVertical) windowWidth else parseHeight()
            height = if (context.app.bar.isVertical) windowWidth else parseHeight()
            x = if (!context.app.bar.isVertical) 0 else parseY()
            y = if (context.app.bar.isVertical) 0 else parseY()
        }

    init {
        color = Color.TRANSPARENT
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        isAttached = true
        updateBackgroundColor()

        context.app.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        isAttached = false

        context.app.unregisterOnSharedPreferenceChangeListener(this)
        updateBackgroundColor(Color.TRANSPARENT)
        forceActionUp()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (keysToListenFor.contains(key)) update(context.app.wm)
        when (key) {
            PrefManager.SIDE_GESTURE_USE_PILL_COLOR,
            PrefManager.SIDE_GESTURE_COLOR,
            PrefManager.AUTO_PILL_BG -> {
                updateBackgroundColor()
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return super.onTouchEvent(event) or gestureManager.onTouchEvent(event)
    }

    fun onCreate() {}

    fun forceActionUp() = mainScope.launch {
        if (isAttachedToWindow) {
            gestureManager.handleActionUp(true)
        }
    }

    fun add(wm: WindowManager) {
        if (!isAttached) {
            try {
                wm.addView(this, params)
            } catch (e: Exception) {}
        }
    }

    fun remove(wm: WindowManager) {
        if (isAttached) {
            updateBackgroundColor(Color.TRANSPARENT) {
                try {
                    wm.removeView(this)
                } catch (e: Exception) {}
            }
        }
    }

    fun update(wm: WindowManager) = mainScope.launch {
        val params = async { this@SideSwipeView.params }

        try {
            wm.updateViewLayout(this@SideSwipeView, params.await())
        } catch (e: Exception) {
            e.logStack()
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

    private fun updateBackgroundColor(
        color: Int = if (context.prefManager.sideGestureUsePillColor) {
            val auto = context.prefManager.autoPillBGColor
            if (auto != 0) auto else context.prefManager.pillBGColor
        } else context.prefManager.sideGestureColor,
        completionListener: ((Animator) -> Unit)? = null
    ) {
        if (background !is ColorDrawable) background = ColorDrawable(Color.TRANSPARENT)

        colorAnimation?.cancel()

        colorAnimation = ValueAnimator.ofArgb(this.color, color)
        colorAnimation?.duration = context.prefManager.animationDurationMs.toLong()
        colorAnimation?.addUpdateListener {
            val new = it.animatedValue.toString().toInt()

            this.color = new
        }
        completionListener?.apply {
            colorAnimation?.addListener(
                onEnd = this,
                onCancel = this
            )
        }
        colorAnimation?.start()
    }

    private fun parseHeight(): Int {
        val percent = heightPercent / 10f
        val screenHeight = context.realScreenSize.y

        return (screenHeight.toFloat() * (percent / 100f)).toInt()
    }

    private fun parseY(): Int {
        val percent = positionPercent / 10f
        val screenHeight = context.realScreenSize.y

        return (screenHeight.toFloat() * (percent / 100f)).toInt() * if (context.app.bar.is270Vertical) 1 else -1
    }

    internal class BaseParams : WindowManager.LayoutParams() {
        init {
            type =
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) TYPE_ACCESSIBILITY_OVERLAY else TYPE_PRIORITY_PHONE
            flags = FLAG_NOT_FOCUSABLE or
                    FLAG_LAYOUT_IN_SCREEN
            format = PixelFormat.TRANSLUCENT
        }
    }

    internal enum class Side {
        LEFT,
        RIGHT
    }
}