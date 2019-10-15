package com.xda.nobar.util

import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import com.xda.nobar.views.BarView

class BarAnimator(private val bar: BarView) {
    private var verticalAnimator: SpringAnimation? = null
    private var horizontalAnimator: SpringAnimation? = null

    fun horizontalHomeY(listener: DynamicAnimation.OnAnimationEndListener) {
        animateVertically(listener, bar.adjustedHomeY)
    }

    fun verticalHomeY(listener: DynamicAnimation.OnAnimationEndListener) {
        animateVertically(listener, bar.adjustedHomeY)
    }

    fun horizontalHomeX(listener: DynamicAnimation.OnAnimationEndListener) {
        animateHorizontally(listener, bar.adjustedHomeX)
    }

    fun verticalHomeX(listener: DynamicAnimation.OnAnimationEndListener) {
        animateHorizontally(listener, bar.adjustedHomeX)
    }

    fun home(listener: DynamicAnimation.OnAnimationEndListener) {
        animateBoth(listener, bar.adjustedHomeX, bar.adjustedHomeY)
    }

    fun show(listener: DynamicAnimation.OnAnimationEndListener) {
        if (bar.isVertical) verticalHomeY(listener)
        else horizontalHomeY(listener)
    }

    fun hide(listener: DynamicAnimation.OnAnimationEndListener) {
        if (bar.isVertical) animateHorizontally(listener, bar.zeroX)
        else animateVertically(listener, bar.zeroY)
    }

    fun animateVertically(listener: DynamicAnimation.OnAnimationEndListener? = null, to: Int) {
        cancelVertical()

        if (bar.shouldAnimate) {
            verticalAnimator = SpringAnimation(bar.params, object : FloatPropertyCompat<WindowManager.LayoutParams>("y") {
                override fun getValue(`object`: WindowManager.LayoutParams): Float {
                    return `object`.y.toFloat()
                }

                override fun setValue(`object`: WindowManager.LayoutParams, value: Float) {
                    `object`.y = value.toInt()
                    bar.updateLayout()
                }
            }, to.toFloat()).apply {
                addEndListener(listener)
            }

            verticalAnimator?.start()
        } else {
            listener?.onAnimationEnd(null, false, -1f, -1f)
        }
    }

    fun animateHorizontally(listener: DynamicAnimation.OnAnimationEndListener? = null, to: Int) {
        cancelHorizontal()

        if (bar.shouldAnimate) {
            horizontalAnimator = SpringAnimation(bar.params, object : FloatPropertyCompat<WindowManager.LayoutParams>("x") {
                override fun getValue(`object`: WindowManager.LayoutParams): Float {
                    return `object`.x.toFloat()
                }

                override fun setValue(`object`: WindowManager.LayoutParams, value: Float) {
                    `object`.x = value.toInt()
                    bar.updateLayout()
                }
            }, to.toFloat()).apply {
                addEndListener(listener)
            }

            horizontalAnimator?.start()
        } else {
            listener?.onAnimationEnd(null, false, -1f, -1f)
        }
    }

    fun animateBoth(listener: DynamicAnimation.OnAnimationEndListener?, toX: Int, toY: Int) {
        cancelHorizontal()
        cancelVertical()

        if (bar.shouldAnimate) {
            var endCount = 0
            val endListener = DynamicAnimation.OnAnimationEndListener { animation, canceled, value, velocity ->
                endCount++

                if (endCount > 1) {
                    listener?.onAnimationEnd(animation, canceled, value, velocity)
                }
            }

            horizontalAnimator = SpringAnimation(bar.params, object : FloatPropertyCompat<WindowManager.LayoutParams>("x") {
                override fun getValue(`object`: WindowManager.LayoutParams): Float {
                    return `object`.x.toFloat()
                }

                override fun setValue(`object`: WindowManager.LayoutParams, value: Float) {
                    `object`.x = value.toInt()
                    bar.updateLayout()
                }
            }, toX.toFloat()).apply {
                addEndListener(endListener)
            }

            horizontalAnimator?.start()

            verticalAnimator = SpringAnimation(bar.params, object : FloatPropertyCompat<WindowManager.LayoutParams>("y") {
                override fun getValue(`object`: WindowManager.LayoutParams): Float {
                    return `object`.y.toFloat()
                }

                override fun setValue(`object`: WindowManager.LayoutParams, value: Float) {
                    `object`.y = value.toInt()
                    bar.updateLayout()
                }
            }, toY.toFloat()).apply {
                addEndListener(endListener)
            }

            verticalAnimator?.start()
        } else {
            listener?.onAnimationEnd(null, false, -1f, -1f)
        }
    }

    fun jiggleTap() {
        if (bar.shouldAnimate) {
            bar.animate()
                    .setInterpolator(BarView.ENTER_INTERPOLATOR)
                    .setDuration(bar.animationDurationMs)
                    .withEndAction {
                        bar.animate()
                                .scaleX(BarView.SCALE_NORMAL)
                                .scaleY(BarView.SCALE_NORMAL)
                                .setInterpolator(BarView.EXIT_INTERPOLATOR)
                                .duration = bar.animationDurationMs
                    }
                    .apply {
                        if (bar.isVertical) scaleY(BarView.SCALE_MID)
                        else scaleX(BarView.SCALE_MID)
                    }
                    .start()
        }
    }

    fun jiggleLeftHold() {
        if (bar.shouldAnimate) {
            bar.animate()
                    .setInterpolator(BarView.ENTER_INTERPOLATOR)
                    .setDuration(bar.animationDurationMs)
                    .withEndAction {
                        bar.animate()
                                .scaleX(BarView.SCALE_NORMAL)
                                .scaleY(BarView.SCALE_NORMAL)
                                .x(0f)
                                .y(0f)
                                .setInterpolator(BarView.EXIT_INTERPOLATOR)
                                .duration = bar.animationDurationMs
                    }
                    .apply {
                        if (bar.isVertical) {
                            scaleY(BarView.SCALE_SMALL)
                            y((if (bar.is270Vertical) -1 else 1) * bar.height * (1 - BarView.SCALE_SMALL) / 2)
                        } else {
                            scaleX(BarView.SCALE_SMALL)
                            x(-bar.width * (1 - BarView.SCALE_SMALL) / 2)
                        }
                    }
                    .start()
        }
    }

    fun jiggleRightHold() {
        if (bar.shouldAnimate) {
            bar.animate()
                    .setInterpolator(BarView.ENTER_INTERPOLATOR)
                    .setDuration(bar.animationDurationMs)
                    .withEndAction {
                        bar.animate()
                                .scaleX(BarView.SCALE_NORMAL)
                                .scaleY(BarView.SCALE_NORMAL)
                                .x(0f)
                                .y(0f)
                                .setInterpolator(BarView.EXIT_INTERPOLATOR)
                                .duration = bar.animationDurationMs
                    }
                    .apply {
                        if (bar.isVertical) {
                            scaleY(BarView.SCALE_SMALL)
                            y((if (bar.is270Vertical) 1 else -1) * bar.height * (1 - BarView.SCALE_SMALL) / 2)
                        } else {
                            scaleX(BarView.SCALE_SMALL)
                            x(bar.width * (1 - BarView.SCALE_SMALL) / 2)
                        }
                    }
                    .start()
        }
    }

    fun jiggleDownHold() {
        if (bar.shouldAnimate) {
            bar.animate()
                    .setInterpolator(BarView.ENTER_INTERPOLATOR)
                    .setDuration(bar.animationDurationMs)
                    .withEndAction {
                        bar.animate()
                                .scaleX(BarView.SCALE_NORMAL)
                                .scaleY(BarView.SCALE_NORMAL)
                                .x(0f)
                                .y(0f)
                                .setInterpolator(BarView.EXIT_INTERPOLATOR)
                                .duration = bar.animationDurationMs
                    }
                    .apply {
                        if (bar.isVertical) {
                            scaleX(BarView.SCALE_SMALL)
                            x((if (bar.is270Vertical) 1 else -1) * bar.height * (1 - BarView.SCALE_SMALL) / 2)
                        } else {
                            scaleY(BarView.SCALE_SMALL)
                            y(bar.height * (1 - BarView.SCALE_SMALL) / 2)
                        }
                    }
                    .start()
        }
    }

    fun jiggleHold() {
        if (bar.shouldAnimate) {
            bar.animate()
                    .setInterpolator(BarView.ENTER_INTERPOLATOR)
                    .setDuration(bar.animationDurationMs)
                    .withEndAction {
                        bar.animate()
                                .scaleX(BarView.SCALE_NORMAL)
                                .scaleY(BarView.SCALE_NORMAL)
                                .setInterpolator(BarView.EXIT_INTERPOLATOR)
                                .duration = bar.animationDurationMs
                    }
                    .apply {
                        if (bar.isVertical) {
                            scaleY(BarView.SCALE_SMALL)
                        } else {
                            scaleX(BarView.SCALE_SMALL)
                        }
                    }
                    .start()
        }
    }

    fun jiggleHoldUp() {
        if (bar.shouldAnimate) {
            bar.animate()
                    .setInterpolator(BarView.ENTER_INTERPOLATOR)
                    .setDuration(bar.animationDurationMs)
                    .withEndAction {
                        bar.animate()
                                .scaleX(BarView.SCALE_NORMAL)
                                .scaleY(BarView.SCALE_NORMAL)
                                .x(0f)
                                .y(0f)
                                .setInterpolator(BarView.EXIT_INTERPOLATOR)
                                .duration = bar.animationDurationMs
                    }
                    .apply {
                        if (bar.isVertical) {
                            scaleX(BarView.SCALE_SMALL)
                            x((if (bar.is270Vertical) 1 else -1) * bar.width * (1 - BarView.SCALE_SMALL) / 2)
                        } else {
                            scaleY(BarView.SCALE_SMALL)
                            y(-bar.height * (1 - BarView.SCALE_SMALL) / 2)
                        }
                    }
                    .start()
        }
    }

    fun jiggleDoubleTap() {
        if (bar.shouldAnimate) {
            bar.animate()
                    .setInterpolator(AccelerateInterpolator())
                    .setDuration(bar.animationDurationMs)
                    .withEndAction {
                        bar.animate()
                                .setInterpolator(BarView.ENTER_INTERPOLATOR)
                                .setDuration(bar.animationDurationMs)
                                .withEndAction {
                                    bar.animate()
                                            .scaleX(BarView.SCALE_NORMAL)
                                            .scaleY(BarView.SCALE_NORMAL)
                                            .setInterpolator(BarView.EXIT_INTERPOLATOR)
                                            .duration = bar.animationDurationMs
                                }
                                .apply {
                                    if (bar.isVertical) {
                                        scaleY(BarView.SCALE_SMALL)
                                    } else {
                                        scaleX(BarView.SCALE_SMALL)
                                    }
                                }
                                .start()
                    }
                    .apply {
                        if (bar.isVertical) {
                            scaleY(BarView.SCALE_MID)
                        } else {
                            scaleX(BarView.SCALE_MID)
                        }
                    }
                    .start()
        }
    }

    fun jiggleComplexLongLeftUp() {
        if (bar.shouldAnimate) {
            bar.animate()
                    .setInterpolator(BarView.ENTER_INTERPOLATOR)
                    .setDuration(bar.animationDurationMs)
                    .withEndAction {
                        bar.animate()
                                .scaleX(BarView.SCALE_NORMAL)
                                .scaleY(BarView.SCALE_NORMAL)
                                .x(0f)
                                .y(0f)
                                .setInterpolator(BarView.EXIT_INTERPOLATOR)
                                .duration = bar.animationDurationMs
                    }
                    .apply {
                        when {
                            bar.is90Vertical -> {
                                x(-bar.width * (1 - BarView.SCALE_SMALL) / 2)
                                y(bar.height * (1 - BarView.SCALE_SMALL) / 2)
                            }
                            bar.is270Vertical -> {
                                x(bar.width * (1 - BarView.SCALE_SMALL) / 2)
                                y(-bar.height * (1 - BarView.SCALE_SMALL) / 2)
                            }
                            else -> {
                                x(-bar.width * (1 - BarView.SCALE_SMALL) / 2)
                                y(-bar.height * (1 - BarView.SCALE_SMALL) / 2)}
                        }

                        scaleX(BarView.SCALE_SMALL)
                        scaleY(BarView.SCALE_SMALL)
                    }
                    .start()
        }
    }

    fun jiggleComplexLongRightUp() {
        if (bar.shouldAnimate) {
            bar.animate()
                    .setInterpolator(BarView.ENTER_INTERPOLATOR)
                    .setDuration(bar.animationDurationMs)
                    .withEndAction {
                        bar.animate()
                                .scaleX(BarView.SCALE_NORMAL)
                                .scaleY(BarView.SCALE_NORMAL)
                                .x(0f)
                                .y(0f)
                                .setInterpolator(BarView.EXIT_INTERPOLATOR)
                                .duration = bar.animationDurationMs
                    }
                    .apply {
                        when {
                            bar.is90Vertical -> {
                                x(-bar.width * (1 - BarView.SCALE_SMALL) / 2)
                                y(-bar.height * (1 - BarView.SCALE_SMALL) / 2)
                            }
                            bar.is270Vertical -> {
                                x(bar.width * (1 - BarView.SCALE_SMALL) / 2)
                                y(bar.height * (1 - BarView.SCALE_SMALL) / 2)
                            }
                            else -> {
                                x(bar.width * (1 - BarView.SCALE_SMALL) / 2)
                                y(-bar.height * (1 - BarView.SCALE_SMALL) / 2)}
                        }

                        scaleX(BarView.SCALE_SMALL)
                        scaleY(BarView.SCALE_SMALL)
                    }
                    .start()
        }
    }

    fun jiggleComplexLongLeftDown() {
        if (bar.shouldAnimate) {
            bar.animate()
                    .setInterpolator(BarView.ENTER_INTERPOLATOR)
                    .setDuration(bar.animationDurationMs)
                    .withEndAction {
                        bar.animate()
                                .scaleX(BarView.SCALE_NORMAL)
                                .scaleY(BarView.SCALE_NORMAL)
                                .x(0f)
                                .y(0f)
                                .setInterpolator(BarView.EXIT_INTERPOLATOR)
                                .duration = bar.animationDurationMs
                    }
                    .apply {
                        when {
                            bar.is90Vertical -> {
                                x(bar.width * (1 - BarView.SCALE_SMALL) / 2)
                                y(bar.height * (1 - BarView.SCALE_SMALL) / 2)
                            }
                            bar.is270Vertical -> {
                                x(-bar.width * (1 - BarView.SCALE_SMALL) / 2)
                                y(-bar.height * (1 - BarView.SCALE_SMALL) / 2)
                            }
                            else -> {
                                x(-bar.width * (1 - BarView.SCALE_SMALL) / 2)
                                y(bar.height * (1 - BarView.SCALE_SMALL) / 2)}
                        }

                        scaleX(BarView.SCALE_SMALL)
                        scaleY(BarView.SCALE_SMALL)
                    }
                    .start()
        }
    }

    fun jiggledComplexLongRightDown() {
        if (bar.shouldAnimate) {
            bar.animate()
                    .setInterpolator(BarView.ENTER_INTERPOLATOR)
                    .setDuration(bar.animationDurationMs)
                    .withEndAction {
                        bar.animate()
                                .scaleX(BarView.SCALE_NORMAL)
                                .scaleY(BarView.SCALE_NORMAL)
                                .x(0f)
                                .y(0f)
                                .setInterpolator(BarView.EXIT_INTERPOLATOR)
                                .duration = bar.animationDurationMs
                    }
                    .apply {
                        when {
                            bar.is90Vertical -> {
                                x(bar.width * (1 - BarView.SCALE_SMALL) / 2)
                                y(-bar.height * (1 - BarView.SCALE_SMALL) / 2)
                            }
                            bar.is270Vertical -> {
                                x(-bar.width * (1 - BarView.SCALE_SMALL) / 2)
                                y(bar.height * (1 - BarView.SCALE_SMALL) / 2)
                            }
                            else -> {
                                x(bar.width * (1 - BarView.SCALE_SMALL) / 2)
                                y(bar.height * (1 - BarView.SCALE_SMALL) / 2)
                            }
                        }

                        scaleX(BarView.SCALE_SMALL)
                        scaleY(BarView.SCALE_SMALL)
                    }
                    .start()
        }
    }

    private fun cancelVertical() {
        verticalAnimator?.skipToEnd()
        verticalAnimator = null
    }

    private fun cancelHorizontal() {
        horizontalAnimator?.skipToEnd()
        horizontalAnimator = null
    }
}