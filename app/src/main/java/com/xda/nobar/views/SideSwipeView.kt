package com.xda.nobar.views

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.xda.nobar.util.*
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class LeftSideSwipeView : SideSwipeView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override val side = Side.LEFT
    override val keysToListenFor = arrayOf(
        PrefManager.LEFT_SIDE_GESTURE_POSITION,
        PrefManager.LEFT_SIDE_GESTURE_HEIGHT
    )
}

class RightSideSwipeView : SideSwipeView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override val side = Side.RIGHT
    override val keysToListenFor = arrayOf(
        PrefManager.RIGHT_SIDE_GESTURE_POSITION,
        PrefManager.RIGHT_SIDE_GESTURE_HEIGHT
    )
}

abstract class SideSwipeView : View, SharedPreferences.OnSharedPreferenceChangeListener {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    internal abstract val side: Side
    internal abstract val keysToListenFor: Array<String>

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
        get() = context.dpAsPx(2)

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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        context.app.registerOnSharedPreferenceChangeListener(this)

        //debug
        setBackgroundColor(
            when (side) {
                Side.LEFT -> Color.RED
                Side.RIGHT -> Color.GREEN
            }
        )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        context.app.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (keysToListenFor.contains(key)) update(context.app.wm)
    }

    fun add(wm: WindowManager) {
        try {
            wm.addView(this, params)
        } catch (e: Exception) {
            e.logStack()
        }
    }

    fun remove(wm: WindowManager) {
        try {
            wm.removeView(this)
        } catch (e: Exception) {
            e.logStack()
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
            type = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) TYPE_ACCESSIBILITY_OVERLAY else TYPE_PRIORITY_PHONE
            flags = FLAG_NOT_FOCUSABLE or
                    FLAG_LAYOUT_IN_SCREEN
        }
    }

    internal enum class Side {
        LEFT,
        RIGHT
    }
}