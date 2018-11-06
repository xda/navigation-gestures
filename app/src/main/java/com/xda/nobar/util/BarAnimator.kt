package com.xda.nobar.util

import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import com.xda.nobar.views.BarView

class BarAnimator(private val bar: BarView) {
    private val verticalAnimator: ValueAnimator = ValueAnimator()
    private val horizontalAnimator: ValueAnimator = ValueAnimator()

    fun homeY(listener: AnimatorListenerAdapter) {
        animateVertically(listener, bar.params.y, bar.getAdjustedHomeY())
    }

    fun homeX(listener: AnimatorListenerAdapter) {
        animateHorizontally(listener, bar.params.x, bar.getAdjustedHomeX())
    }

    fun show(listener: AnimatorListenerAdapter) {
        homeY(listener)
    }

    fun hide(listener: AnimatorListenerAdapter) {
        animateVertically(listener, bar.params.y, bar.getZeroY())
    }

    fun animateVertically(listener: AnimatorListenerAdapter? = null,
                          from: Int,
                          to: Int) {
        cancelVertical()
        verticalAnimator.setIntValues(from, to)
        verticalAnimator.addListener(listener)
        verticalAnimator.addUpdateListener {
            bar.params.y = it.animatedValue.toString().toInt()
            bar.updateLayout()
        }
        verticalAnimator.start()
    }

    fun animateHorizontally(listener: AnimatorListenerAdapter? = null,
                            from: Int,
                            to: Int) {
        cancelHorizontal()
        horizontalAnimator.setIntValues(from, to)
        horizontalAnimator.addListener(listener)
        horizontalAnimator.addUpdateListener {
            bar.params.x = it.animatedValue.toString().toInt()
            bar.updateLayout()
        }
        horizontalAnimator.start()
    }

    private fun cancelAllAnimations() {
        cancelVertical()
        cancelHorizontal()
    }

    private fun cancelVertical() {
        verticalAnimator.cancel()
        verticalAnimator.removeAllUpdateListeners()
        verticalAnimator.removeAllListeners()
    }

    private fun cancelHorizontal() {
        horizontalAnimator.cancel()
        horizontalAnimator.removeAllUpdateListeners()
        verticalAnimator.removeAllListeners()
    }
}