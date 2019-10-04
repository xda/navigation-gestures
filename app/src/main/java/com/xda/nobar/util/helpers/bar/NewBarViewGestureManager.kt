package com.xda.nobar.util.helpers.bar

import android.content.ContextWrapper
import android.view.GestureDetector
import android.view.IRotationWatcher
import android.view.MotionEvent
import android.view.Surface
import androidx.dynamicanimation.animation.DynamicAnimation
import com.xda.nobar.util.app
import com.xda.nobar.util.cachedRotation
import com.xda.nobar.util.prefManager
import com.xda.nobar.util.realScreenSize
import com.xda.nobar.views.BarView
import kotlinx.android.synthetic.main.pill.view.*
import kotlin.math.absoluteValue

class NewBarViewGestureManager(private val bar: BarView) : ContextWrapper(bar.context.applicationContext) {
    private enum class Mode {
        PORTRAIT,
        LANDSCAPE_90,
        LANDSCAPE_270,
    }

    private var downX: Float = 0f
    private var downY: Float = 0f
    private var downAdjX: Float = 0f
    private var downAdjY: Float = 0f

    private var prevX: Float = 0f
    private var prevY: Float = 0f

    private var mode: Mode = Mode.PORTRAIT

    private val detector = GestureDetector(this, Listener())

    private val rotationWatcher = object : IRotationWatcher.Stub() {
        override fun onRotationChanged(rotation: Int) {
            setMode(rotation)
        }
    }

    init {
        app.addRotationWatcher(rotationWatcher)
        setMode(cachedRotation)
    }

    fun handleTouchEvent(e: MotionEvent?): Boolean {
        if (bar.isPillHidingOrShowing) return false

        when (e?.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.rawX
                downY = e.rawY
                prevX = downX
                prevY = downY
                downAdjX = e.x
                downAdjY = e.y

                bar.beingTouched = true
                bar.isCarryingOutTouchAction = true

                bar.scheduleUnfade()
            }

            MotionEvent.ACTION_MOVE -> {
                val newX = e.rawX
                val newY = e.rawY

                val velocityX = newX - prevX
                val velocityY = prevY - newY

                val slop = bar.viewConfig.scaledTouchSlop
                if (velocityX.absoluteValue < slop && velocityX.absoluteValue < slop) return false

                prevX = newX
                prevY = newY

                if (bar.shouldAnimate) {
                    val halfScreen = realScreenSize.x / 2f
                    val leftParam = bar.params.x - prefManager.customWidth.toFloat() / 2f
                    val rightParam = bar.params.x + prefManager.customWidth.toFloat() / 2f

                    when {
                        leftParam <= -halfScreen && velocityX < 0 -> {
                            bar.pill.translationX += velocityX
                        }
                        rightParam >= halfScreen && velocityX > 0 -> {
                            bar.pill.translationX += velocityX
                        }
                        else -> {
                            bar.params.x = bar.params.x + (velocityX / 2).toInt()
                        }
                    }

                    bar.params.y -= (velocityY / 2).toInt()
                    bar.updateLayout()
                }
            }

            MotionEvent.ACTION_UP -> {
                handleActionUp()

                bar.beingTouched = false
                bar.isCarryingOutTouchAction = false
            }
        }

        return true
    }

    fun handleActionUp(isForce: Boolean = false) {
        var isXDone = false
        var isYDone = false

        var isParamXDone = false
        var isParamYDone = false

        bar.animatePillToHome(
                {
                    isXDone = true
                    if (isXDone && isYDone && isParamXDone && isParamYDone) bar.isCarryingOutTouchAction = false
                },
                {
                    isYDone = true
                    if (isXDone && isYDone && isParamXDone && isParamYDone) bar.isCarryingOutTouchAction = false
                }
        )

        when {
            bar.params.x != bar.adjustedHomeX && (!bar.isVertical || (!bar.isHidden && !bar.isPillHidingOrShowing)) -> {
                bar.animator.horizontalHomeX(DynamicAnimation.OnAnimationEndListener { _, _, _, _ ->
                    isParamXDone = true
                    if (isXDone && isYDone && isParamXDone && isParamYDone) bar.isCarryingOutTouchAction = false
                })
            }
            bar.params.y != bar.adjustedHomeY && (bar.isVertical || (!bar.isHidden && !bar.isPillHidingOrShowing)) -> {
                bar.animator.horizontalHomeY(DynamicAnimation.OnAnimationEndListener { _, _, _, _ ->
                    isParamYDone = true
                    if (isXDone && isYDone && isParamXDone && isParamYDone) bar.isCarryingOutTouchAction = false
                })
            }
            else -> {
                isParamXDone = true
                isParamYDone = true
                if (isXDone && isYDone && isParamXDone && isParamYDone) bar.isCarryingOutTouchAction = false
            }
        }

        parseSwipe()
    }

    private fun parseSwipe() {
        val distanceX = prevX - downX
        val distanceY = prevY - downY
        val xThresh = prefManager.xThresholdPx
        val yThreshUp = prefManager.yThresholdUpPx
        val yThreshDown = prefManager.yThresholdDownPx


    }

    private fun setMode(rotation: Int) {
        mode = when (rotation) {
            Surface.ROTATION_0,
            Surface.ROTATION_180 -> Mode.PORTRAIT
            Surface.ROTATION_90 -> Mode.LANDSCAPE_90
            else -> Mode.LANDSCAPE_270
        }
    }

    inner class Listener : GestureDetector.SimpleOnGestureListener() {

    }
}