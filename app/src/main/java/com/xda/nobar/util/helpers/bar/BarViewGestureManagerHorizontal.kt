package com.xda.nobar.util.helpers.bar

import android.annotation.SuppressLint
import android.os.*
import android.view.MotionEvent
import androidx.dynamicanimation.animation.DynamicAnimation
import com.xda.nobar.util.*
import com.xda.nobar.util.helpers.HiddenPillReasonManager
import com.xda.nobar.views.BarView
import kotlinx.android.synthetic.main.pill.view.*
import kotlin.math.absoluteValue

/**
 * Manage all the gestures on the pill
 */
class BarViewGestureManagerHorizontal(bar: BarView) : BaseBarViewGestureManager(bar) {
    override val adjCoord: Float
        get() = origAdjX
    override val gestureHandler by lazy { GestureHandler(gestureThread.looper) }

    override fun handleTouchEvent(ev: MotionEvent?): Boolean {
        super.handleTouchEvent(ev)

        var ultimateReturn = false

        when (ev?.action) {
            MotionEvent.ACTION_UP -> {
                if (wasHidden) {
                    isSwipeUp = false
                }

                gestureHandler.clearLongQueues()

                parseSwipe()

                if (bar.pill.translationX != 0f) {
                    bar.pill.animate()
                            .translationX(0f)
                            .setDuration(bar.getAnimationDurationMs())
                            .withEndAction {
                                if (bar.params.x == bar.adjustedHomeX) {
                                    isActing = false
                                    isSwipeLeft = false
                                    isSwipeRight = false
                                }
                            }
                            .start()
                }

                if (bar.pill.translationY != 0f && !bar.isHidden && !bar.isPillHidingOrShowing) {
                    bar.pill.animate()
                            .translationY(0f)
                            .setDuration(bar.getAnimationDurationMs())
                            .withEndAction {
                                if (bar.params.y == bar.adjustedHomeY) {
                                    isActing = false
                                    bar.isCarryingOutTouchAction = false
                                }
                            }
                            .start()
                }

                when {
                    bar.params.y != bar.adjustedHomeY && !bar.isHidden && !bar.isPillHidingOrShowing -> {
                        bar.animator.horizontalHomeY(DynamicAnimation.OnAnimationEndListener { _, _, _, _ ->
                            isActing = false
                            bar.isCarryingOutTouchAction = false
                        })
                    }
                    bar.params.x < bar.adjustedHomeX || bar.params.x > bar.adjustedHomeX -> {
                        bar.animator.horizontalHomeX(DynamicAnimation.OnAnimationEndListener { _, _, _, _ ->
                            isActing = false
                            bar.isCarryingOutTouchAction = false
                        })
                    }
                    else -> {
                        isActing = false
                        bar.isCarryingOutTouchAction = false
                    }
                }

                finishUp()
            }
            MotionEvent.ACTION_MOVE -> {
                ultimateReturn = handlePotentialSwipe(ev)

                if (isSwipeUp && !isSwipeLeft && !isSwipeRight && !isSwipeDown) {
                    if (!isActing) isActing = true

                    val velocity = (oldY - ev.rawY)
                    oldY = ev.rawY

                    val screenHeight = context.realScreenSize.y

                    if (bar.params.y > screenHeight - screenHeight / 6 - context.app.prefManager.homeY
                            && bar.getAnimationDurationMs() > 0) {
                        bar.params.y -= (velocity / 2).toInt()
                        bar.updateLayout()
                    }

                    gestureHandler.queueUpHold()
                }

                if (isSwipeDown && !isSwipeLeft && !isSwipeRight && !isSwipeUp) {
                    if (!isActing) isActing = true

                    val velocity = (oldY - ev.rawY)
                    oldY = ev.rawY

                    if (bar.getAnimationDurationMs() > 0) {
                        bar.params.y -= (velocity / 2).toInt()
                        bar.updateLayout()
                    }

                    gestureHandler.queueDownHold()
                }

                if ((isSwipeLeft || isSwipeRight) && !isSwipeUp && !isSwipeDown) {
                    if (!isActing) isActing = true

                    val velocity = ev.rawX - oldX
                    oldX = ev.rawX

                    val halfScreen = context.realScreenSize.x / 2f
                    val leftParam = bar.params.x - context.app.prefManager.customWidth.toFloat() / 2f
                    val rightParam = bar.params.x + context.app.prefManager.customWidth.toFloat() / 2f

                    if (bar.getAnimationDurationMs() > 0) {
                        when {
                            leftParam <= -halfScreen && !isSwipeRight -> {
                                bar.pill.translationX += velocity
                            }
                            rightParam >= halfScreen && !isSwipeLeft -> bar.pill.translationX += velocity
                            else -> {
                                bar.params.x = bar.params.x + (velocity / 2).toInt()
                                bar.updateLayout()
                            }
                        }
                    }

                    if (isSwipeLeft) {
                        gestureHandler.queueLeftHold()
                    }

                    if (isSwipeRight) {
                        gestureHandler.queueRightHold()
                    }
                }
            }
        }

        return ultimateReturn
    }

    private fun handlePotentialSwipe(motionEvent: MotionEvent?): Boolean {
        if (motionEvent == null) return false

        val distanceX = motionEvent.rawX - origX
        val distanceY = motionEvent.rawY - origY
        val xThresh = context.prefManager.xThresholdPx
        val yThreshUp = context.prefManager.yThresholdUpPx
        val yThreshDown = context.prefManager.yThresholdDownDp

        return if (!bar.isHidden && !isActing) {
            when {
                context.actionHolder.run { hasAnyOfActions(actionLeft, actionLeftHold) }
                        && distanceX < -xThresh
                        && distanceY.absoluteValue <= distanceX.absoluteValue -> { //left swipe
                    isSwipeLeft = true
                    true
                }
                context.actionHolder.run { hasAnyOfActions(actionRight, actionRightHold) }
                        && distanceX > xThresh
                        && distanceY.absoluteValue <= distanceX.absoluteValue -> { //right swipe
                    isSwipeRight = true
                    true
                }
                context.actionHolder.run { hasAnyOfActions(actionDown, actionDownHold) }
                        && distanceY > yThreshDown
                        && distanceY.absoluteValue > distanceX.absoluteValue -> { //down swipe and down hold-swipe
                    isSwipeDown = true
                    true
                }
                context.actionHolder.hasSomeUpAction()
                        && distanceY < -yThreshUp
                        && distanceY.absoluteValue > distanceX.absoluteValue -> { //up swipe and up hold-swipe
                    isSwipeUp = true
                    true
                }
                else -> false
            }
        } else if (bar.isHidden
                && !isActing
                && distanceY < -yThreshUp
                && distanceY.absoluteValue > distanceX.absoluteValue) { //up swipe
            if (bar.isHidden && !bar.isPillHidingOrShowing && !bar.beingTouched) {
                bar.vibrate(context.prefManager.vibrationDuration.toLong())
                bar.hiddenPillReasons.remove(HiddenPillReasonManager.MANUAL)
                bar.showPill(null, true)
            }
            true
        } else false
    }

    override fun getSection(coord: Float): Int {
        val third = context.app.prefManager.customWidth / 3f

        return when {
            coord < third -> FIRST_SECTION
            coord <= (2f * third) -> SECOND_SECTION
            else -> THIRD_SECTION
        }
    }

    @SuppressLint("HandlerLeak")
    inner class GestureHandler(looper: Looper) : BaseGestureHandler(looper) {
        override fun handleLongUp() {
            if (getSectionedUpHoldAction(adjCoord) != bar.actionHolder.typeNoAction) {
                isRunningLongUp = true
                sendAction(bar.actionHolder.actionUpHold)
            }
        }

        override fun handleLongDown() {
            if (actionMap[bar.actionHolder.actionDownHold] != bar.actionHolder.typeNoAction) {
                isRunningLongDown = true
                sendAction(bar.actionHolder.actionDownHold)
            }
        }

        override fun handleLongLeft() {
            if (actionMap[bar.actionHolder.actionLeftHold] != bar.actionHolder.typeNoAction) {
                isRunningLongLeft = true
                sendAction(bar.actionHolder.actionLeftHold)
            }
        }

        override fun handleLongRight() {
            if (actionMap[bar.actionHolder.actionRightHold] != bar.actionHolder.typeNoAction) {
                isRunningLongRight = true
                sendAction(bar.actionHolder.actionRightHold)
            }
        }

        override fun handleUp() {
            sendAction(bar.actionHolder.actionUp)
        }

        override fun handleDown() {
            sendAction(bar.actionHolder.actionDown)
        }

        override fun handleLeft() {
            sendAction(bar.actionHolder.actionLeft)
        }

        override fun handleRight() {
            sendAction(bar.actionHolder.actionRight)
        }
    }
}