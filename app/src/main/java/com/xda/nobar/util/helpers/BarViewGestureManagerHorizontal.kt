package com.xda.nobar.util.helpers

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.os.*
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SoundEffectConstants
import androidx.dynamicanimation.animation.DynamicAnimation
import com.topjohnwu.superuser.Shell
import com.xda.nobar.util.*
import com.xda.nobar.views.BarView
import com.xda.nobar.views.BarView.Companion.ALPHA_ACTIVE
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
                    isSwipeUp = false
                }

                gestureHandler.removeMessages(MSG_UP_HOLD)
                gestureHandler.removeMessages(MSG_LEFT_HOLD)
                gestureHandler.removeMessages(MSG_RIGHT_HOLD)
                gestureHandler.removeMessages(MSG_DOWN_HOLD)

                if (isSwipeUp && !isRunningLongUp) {
                    sendAction(bar.actionHolder.actionUp)
                }

                if (isSwipeLeft && !isRunningLongLeft) {
                    sendAction(bar.actionHolder.actionLeft)
                }

                if (isSwipeRight && !isRunningLongRight) {
                    sendAction(bar.actionHolder.actionRight)
                }

                if (isSwipeDown && !isRunningLongDown) {
                    sendAction(bar.actionHolder.actionDown)
                }

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
                        bar.animator.homeY(DynamicAnimation.OnAnimationEndListener { _, _, _, _ ->
                            isActing = false
                            bar.isCarryingOutTouchAction = false
                        })
                    }
                    bar.params.x < bar.adjustedHomeX || bar.params.x > bar.adjustedHomeX -> {
                        bar.animator.homeX(DynamicAnimation.OnAnimationEndListener { _, _, _, _ ->
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

                    if (!sentLongUp) {
                        sentLongUp = true
                        gestureHandler.sendEmptyMessageAtTime(MSG_UP_HOLD,
                                SystemClock.uptimeMillis() + context.prefManager.holdTime.toLong())
                    }
                }

                if (isSwipeDown && !isSwipeLeft && !isSwipeRight && !isSwipeUp) {
                    if (!isActing) isActing = true

                    val velocity = (oldY - ev.rawY)
                    oldY = ev.rawY

                    if (bar.getAnimationDurationMs() > 0) {
                        bar.params.y -= (velocity / 2).toInt()
                        bar.updateLayout()
                    }

                    if (!sentLongDown) {
                        sentLongDown = true
                        gestureHandler.sendEmptyMessageAtTime(MSG_DOWN_HOLD,
                                SystemClock.uptimeMillis() + context.prefManager.holdTime.toLong())
                    }
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

                    if (isSwipeLeft && !sentLongLeft) {
                        sentLongLeft = true
                        gestureHandler.sendEmptyMessageAtTime(MSG_LEFT_HOLD,
                                SystemClock.uptimeMillis() + context.prefManager.holdTime.toLong())
                    }

                    if (isSwipeRight && !sentLongRight) {
                        sentLongRight = true
                        gestureHandler.sendEmptyMessageAtTime(MSG_RIGHT_HOLD,
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
    inner class GestureHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message?) {
            when (msg?.what) {
                MSG_UP_HOLD -> {
                    if (getSectionedUpHoldAction(adjCoord) != bar.actionHolder.typeNoAction) {
                        isRunningLongUp = true
                        sendAction(bar.actionHolder.actionUpHold)
                    }
                }

                MSG_LEFT_HOLD -> {
                    if (actionMap[bar.actionHolder.actionLeftHold] != bar.actionHolder.typeNoAction) {
                        isRunningLongLeft = true
                        sendAction(bar.actionHolder.actionLeftHold)
                    }
                }

                MSG_RIGHT_HOLD -> {
                    if (actionMap[bar.actionHolder.actionRightHold] != bar.actionHolder.typeNoAction) {
                        isRunningLongRight = true
                        sendAction(bar.actionHolder.actionRightHold)
                    }
                }

                MSG_DOWN_HOLD -> {
                    if (actionMap[bar.actionHolder.actionDownHold] != bar.actionHolder.typeNoAction) {
                        isRunningLongDown = true
                        sendAction(bar.actionHolder.actionDownHold)
                    }
                }
            }
        }
    }
}