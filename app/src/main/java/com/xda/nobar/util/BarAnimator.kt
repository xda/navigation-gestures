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

    fun homeY(listener: DynamicAnimation.OnAnimationEndListener) {
        animateVertically(listener, bar.adjustedHomeY)
    }

    fun homeX(listener: DynamicAnimation.OnAnimationEndListener) {
        animateHorizontally(listener, bar.adjustedHomeX)
    }

    fun show(listener: DynamicAnimation.OnAnimationEndListener) {
        homeY(listener)
    }

    fun hide(listener: DynamicAnimation.OnAnimationEndListener) {
        animateVertically(listener, bar.zeroY)
    }

    fun animateVertically(listener: DynamicAnimation.OnAnimationEndListener? = null,
                          to: Int) {
        cancelVertical()

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
    }

    fun animateHorizontally(listener: DynamicAnimation.OnAnimationEndListener? = null,
                            to: Int) {
        cancelHorizontal()

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
    }

    fun jiggleTap() {
        bar.animate()
                .scaleX(BarView.SCALE_MID)
                .setInterpolator(BarView.ENTER_INTERPOLATOR)
                .setDuration(bar.getAnimationDurationMs())
                .withEndAction {
                    bar.animate()
                            .scaleX(BarView.SCALE_NORMAL)
                            .setInterpolator(BarView.EXIT_INTERPOLATOR)
                            .duration = bar.getAnimationDurationMs()
                }
    }

    fun jiggleLeftHold() {
        bar.animate()
                .scaleX(BarView.SCALE_SMALL)
                .x(-bar.width * (1 - BarView.SCALE_SMALL) / 2)
                .setInterpolator(BarView.ENTER_INTERPOLATOR)
                .setDuration(bar.getAnimationDurationMs())
                .withEndAction {
                    bar.animate()
                            .scaleX(BarView.SCALE_NORMAL)
                            .x(0f)
                            .setInterpolator(BarView.EXIT_INTERPOLATOR)
                            .duration = bar.getAnimationDurationMs()
                }
    }

    fun jiggleRightHold() {
        bar.animate()
                .scaleX(BarView.SCALE_SMALL)
                .x(bar.width * (1 - BarView.SCALE_SMALL) / 2)
                .setInterpolator(BarView.ENTER_INTERPOLATOR)
                .setDuration(bar.getAnimationDurationMs())
                .withEndAction {
                    bar.animate()
                            .scaleX(BarView.SCALE_NORMAL)
                            .x(0f)
                            .setInterpolator(BarView.EXIT_INTERPOLATOR)
                            .duration = bar.getAnimationDurationMs()
                }
    }

    fun jiggleDownHold() {
        bar.animate()
                .scaleY(BarView.SCALE_SMALL)
                .y(bar.height * (1 - BarView.SCALE_SMALL) / 2)
                .setInterpolator(BarView.ENTER_INTERPOLATOR)
                .setDuration(bar.getAnimationDurationMs())
                .withEndAction {
                    bar.animate()
                            .scaleY(BarView.SCALE_NORMAL)
                            .y(0f)
                            .setInterpolator(BarView.EXIT_INTERPOLATOR)
                            .duration = bar.getAnimationDurationMs()
                }
    }

    fun jiggleHold() {
        bar.animate()
                .scaleX(BarView.SCALE_SMALL)
                .setInterpolator(BarView.ENTER_INTERPOLATOR)
                .setDuration(bar.getAnimationDurationMs())
                .withEndAction {
                    bar.animate()
                            .scaleX(BarView.SCALE_NORMAL)
                            .setInterpolator(BarView.EXIT_INTERPOLATOR)
                            .duration = bar.getAnimationDurationMs()
                }
    }

    fun jiggleHoldUp() {
        bar.animate()
                .scaleY(BarView.SCALE_SMALL)
                .y(-bar.height * (1 - BarView.SCALE_SMALL) / 2)
                .setInterpolator(BarView.ENTER_INTERPOLATOR)
                .setDuration(bar.getAnimationDurationMs())
                .withEndAction {
                    bar.animate()
                            .scaleY(BarView.SCALE_NORMAL)
                            .y(0f)
                            .setInterpolator(BarView.EXIT_INTERPOLATOR)
                            .duration = bar.getAnimationDurationMs()
                }
    }

    fun jiggleDoubleTap() {
        bar.animate()
                .scaleX(BarView.SCALE_MID)
                .setInterpolator(AccelerateInterpolator())
                .setDuration(bar.getAnimationDurationMs())
                .withEndAction {
                    bar.animate()
                            .scaleX(BarView.SCALE_SMALL)
                            .setInterpolator(BarView.ENTER_INTERPOLATOR)
                            .setDuration(bar.getAnimationDurationMs())
                            .withEndAction {
                                bar.animate()
                                        .scaleX(BarView.SCALE_NORMAL)
                                        .setInterpolator(BarView.EXIT_INTERPOLATOR)
                                        .duration = bar.getAnimationDurationMs()
                            }
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