package com.xda.nobar.util.helpers.bar

import android.annotation.SuppressLint
import android.os.*
import android.util.Log
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
class BarViewGestureManagerVertical270(bar: BarView) : BaseBarViewGestureManager(bar) {
    override val adjCoord: Float
        get() = origAdjY
    override val gestureHandler by lazy { GestureHandler(gestureThread.looper) }

    override fun handleTouchEvent(ev: MotionEvent?): Boolean {
        var ultimateReturn = false

        when (ev?.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchTime = System.currentTimeMillis()
                wasHidden = bar.isHidden
                oldY = ev.rawY
                oldX = ev.rawX
                origX = ev.rawX
                origY = ev.rawY
                origAdjX = ev.x
                origAdjY = ev.y
                bar.beingTouched = true
                bar.isCarryingOutTouchAction = true
            }

            MotionEvent.ACTION_UP -> {
                bar.beingTouched = false
                lastTouchTime = -1L

                if (wasHidden) {
                    isSwipeRight = false
                }

                gestureHandler.removeMessages(MSG_UP_HOLD)
                gestureHandler.removeMessages(MSG_LEFT_HOLD)
                gestureHandler.removeMessages(MSG_RIGHT_HOLD)
                gestureHandler.removeMessages(MSG_DOWN_HOLD)

                if (isSwipeUp && !isRunningLongUp) {
                    sendAction(bar.actionHolder.actionLeft)
                }

                if (isSwipeLeft && !isRunningLongLeft) {
                    sendAction(bar.actionHolder.actionDown)
                }

                if (isSwipeRight && !isRunningLongRight) {
                    sendAction(bar.actionHolder.actionUp)
                }

                if (isSwipeDown && !isRunningLongDown) {
                    sendAction(bar.actionHolder.actionRight)
                }

                if (bar.pill.translationY != 0f) {
                    bar.pill.animate()
                            .translationY(0f)
                            .setDuration(bar.getAnimationDurationMs())
                            .withEndAction {
                                if (bar.params.y == bar.adjustedHomeY) {
                                    isActing = false
                                    isSwipeUp = false
                                    isSwipeDown = false
                                }
                            }
                            .start()
                }

                if (bar.pill.translationX != 0f && !bar.isHidden && !bar.isPillHidingOrShowing) {
                    bar.pill.animate()
                            .translationX(0f)
                            .setDuration(bar.getAnimationDurationMs())
                            .withEndAction {
                                if (bar.params.x == bar.adjustedHomeX) {
                                    isActing = false
                                    bar.isCarryingOutTouchAction = false
                                }
                            }
                            .start()
                }

                when {
                    bar.params.x != bar.adjustedHomeX && !bar.isHidden && !bar.isPillHidingOrShowing -> {
                        bar.animator.verticalHomeX(DynamicAnimation.OnAnimationEndListener { _, _, _, _ ->
                            isActing = false
                            bar.isCarryingOutTouchAction = false
                        })
                    }
                    bar.params.y != bar.adjustedHomeY -> {
                        bar.animator.verticalHomeY(DynamicAnimation.OnAnimationEndListener { _, _, _, _ ->
                            isActing = false
                            bar.isCarryingOutTouchAction = false
                        })
                    }
                    else -> {
                        isActing = false
                        bar.isCarryingOutTouchAction = false
                    }
                }

                isRunningLongRight = false
                isRunningLongLeft = false
                isRunningLongUp = false
                isRunningLongDown = false

                sentLongRight = false
                sentLongLeft = false
                sentLongUp = false
                sentLongDown = false

                isSwipeUp = false
                isSwipeLeft = false
                isSwipeRight = false
                isSwipeDown = false

                wasHidden = bar.isHidden
            }
            MotionEvent.ACTION_MOVE -> {
                ultimateReturn = handlePotentialSwipe(ev)

                if (isSwipeRight && !isSwipeUp && !isSwipeLeft && !isSwipeDown) {
                    if (!isActing) isActing = true

                    val velocity = (oldX - ev.rawX)
                    oldX = ev.rawX

                    val screenWidth = context.realScreenSize.x

                    if (bar.params.x < screenWidth / 6 + bar.adjustedHomeX
                            && bar.getAnimationDurationMs() > 0) {
                        bar.params.x -= (velocity / 2).toInt()
                        bar.updateLayout()
                    }

                    if (!sentLongRight) {
                        sentLongRight = true
                        gestureHandler.sendEmptyMessageAtTime(MSG_RIGHT_HOLD,
                                SystemClock.uptimeMillis() + context.prefManager.holdTime.toLong())
                    }
                }

                if (isSwipeLeft && !isSwipeRight && !isSwipeDown && !isSwipeUp) {
                    if (!isActing) isActing = true

                    val velocity = (oldX - ev.rawX)
                    oldX = ev.rawX

                    if (bar.getAnimationDurationMs() > 0) {
                        bar.params.x -= (velocity / 2).toInt()
                        bar.updateLayout()
                    }

                    if (!sentLongLeft) {
                        sentLongLeft = true
                        gestureHandler.sendEmptyMessageAtTime(MSG_LEFT_HOLD,
                                SystemClock.uptimeMillis() + context.prefManager.holdTime.toLong())
                    }
                }

                if ((isSwipeUp || isSwipeDown) && !isSwipeLeft && !isSwipeRight) {
                    if (!isActing) isActing = true

                    val velocity = ev.rawY - oldY
                    oldY = ev.rawY

                    val halfScreen = context.realScreenSize.y / 2f
                    val bottomParam = bar.params.y - context.app.prefManager.customWidth.toFloat() / 2f
                    val topParam = bar.params.y + context.app.prefManager.customWidth.toFloat() / 2f

                    if (bar.getAnimationDurationMs() > 0) {
                        when {
                            bottomParam <= -halfScreen && !isSwipeUp -> {
                                bar.pill.translationY += velocity
                            }
                            topParam >= halfScreen && !isSwipeDown -> {
                                bar.pill.translationY += velocity
                            }
                            else -> {
                                bar.params.y = bar.params.y + (velocity / 2).toInt()
                                bar.updateLayout()
                            }
                        }
                    }

                    if (isSwipeDown && !sentLongDown) {
                        sentLongDown = true
                        gestureHandler.sendEmptyMessageAtTime(MSG_DOWN_HOLD,
                                SystemClock.uptimeMillis() + context.prefManager.holdTime.toLong())
                    }

                    if (isSwipeUp && !sentLongUp) {
                        sentLongUp = true
                        gestureHandler.sendEmptyMessageAtTime(MSG_UP_HOLD,
                                SystemClock.uptimeMillis() + context.prefManager.holdTime.toLong())
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
                context.actionHolder.run { hasAnyOfActions(actionRight, actionRightHold) }
                        && distanceY > xThresh
                        && distanceY.absoluteValue > distanceX.absoluteValue -> { //down swipe
                    isSwipeDown = true
                    true
                }
                context.actionHolder.run { hasAnyOfActions(actionLeft, actionLeftHold) }
                        && distanceY < -xThresh
                        && distanceY.absoluteValue > distanceX.absoluteValue -> { //up swipe
                    isSwipeUp = true
                    true
                }
                context.actionHolder.hasSomeUpAction()
                        && distanceX > yThreshUp
                        && distanceY.absoluteValue < distanceX.absoluteValue -> { //right swipe
                    isSwipeRight = true
                    true
                }
                context.actionHolder.run { hasAnyOfActions(actionDown, actionDownHold) }
                        && distanceX < -yThreshDown
                        && distanceY.absoluteValue < distanceX.absoluteValue -> { //left swipe
                    isSwipeLeft = true
                    true
                }
                else -> false
            }
        } else if (bar.isHidden
                && !isActing
                && distanceX > yThreshUp
                && distanceY.absoluteValue < distanceX.absoluteValue) { //right swipe
            if (bar.isHidden && !bar.isPillHidingOrShowing && !bar.beingTouched) {
                bar.vibrate(context.prefManager.vibrationDuration.toLong())
                bar.hiddenPillReasons.remove(HiddenPillReasonManager.MANUAL)
                bar.showPill(null, true)
            }
            true
        } else false
    }

    override fun getSection(coord: Float): Int {
        val third = bar.adjustedHeight / 3f

        return when {
            coord < third -> FIRST_SECTION
            coord <= (2f * third) -> SECOND_SECTION
            else -> THIRD_SECTION
        }
    }

    @SuppressLint("HandlerLeak")
    inner class GestureHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message?) {
            when (msg?.what) {
                MSG_LEFT_HOLD -> {
                    if (actionMap[bar.actionHolder.actionDownHold] != bar.actionHolder.typeNoAction) {
                        isRunningLongLeft = true
                        sendAction(bar.actionHolder.actionDownHold)
                    }
                }

                MSG_DOWN_HOLD -> {
                    if (actionMap[bar.actionHolder.actionRightHold] != bar.actionHolder.typeNoAction) {
                        isRunningLongDown = true
                        sendAction(bar.actionHolder.actionRightHold)
                    }
                }

                MSG_UP_HOLD -> {
                    if (actionMap[bar.actionHolder.actionLeftHold] != bar.actionHolder.typeNoAction) {
                        isRunningLongUp = true
                        sendAction(bar.actionHolder.actionLeftHold)
                    }
                }

                MSG_RIGHT_HOLD -> {
                    if (getSectionedUpHoldAction(adjCoord) != bar.actionHolder.typeNoAction) {
                        isRunningLongRight = true
                        sendAction(bar.actionHolder.actionUpHold)
                    }
                }
            }
        }
    }
}