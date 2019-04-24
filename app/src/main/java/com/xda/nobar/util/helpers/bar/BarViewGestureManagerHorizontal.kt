package com.xda.nobar.util.helpers.bar

import android.annotation.SuppressLint
import android.os.Looper
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
            MotionEvent.ACTION_MOVE -> {
                ultimateReturn = handlePotentialSwipe(ev)

                when {
                    isSwipeUp -> {
                        if (!isActing) isActing = true

                        val velocity = (oldY - ev.rawY)
                        oldY = ev.rawY

                        val screenHeight = context.realScreenSize.y

                        val threshold = screenHeight -
                                screenHeight / 6 -
                                context.app.prefManager.homeY -
                                if (context.prefManager.dontMoveForKeyboard) 0
                                else context.app.imm.inputMethodWindowVisibleHeight

                        if (bar.params.y > threshold
                                && bar.shouldAnimate) {
                            bar.params.y -= (velocity / 2).toInt()
                            bar.updateLayout()
                        }

                        gestureHandler.queueUpHold()
                    }

                    isSwipeDown -> {
                        if (!isActing) isActing = true

                        val velocity = (oldY - ev.rawY)
                        oldY = ev.rawY

                        if (bar.shouldAnimate) {
                            bar.params.y -= (velocity / 2).toInt()
                            bar.updateLayout()
                        }

                        gestureHandler.queueDownHold()
                    }

                    isSwipeLeft || isSwipeRight -> {
                        if (!isActing) isActing = true

                        val velocity = ev.rawX - oldX
                        oldX = ev.rawX

                        val halfScreen = context.realScreenSize.x / 2f
                        val leftParam = bar.params.x - context.app.prefManager.customWidth.toFloat() / 2f
                        val rightParam = bar.params.x + context.app.prefManager.customWidth.toFloat() / 2f

                        if (bar.shouldAnimate) {
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
        }

        return ultimateReturn
    }

    override fun handleActionUp(isForce: Boolean) {
        super.handleActionUp(isForce)

        if (wasHidden) {
            isSwipeUp = false
        }

        gestureHandler.clearLongQueues()

        if (!isForce) parseSwipe()

        bar.animatePillToHome(
                {
                    if (bar.params.x == bar.adjustedHomeX) {
                        isActing = false
                        isSwipeLeft = false
                        isSwipeRight = false
                    }
                },
                {
                    if (bar.params.y == bar.adjustedHomeY) {
                        isActing = false
                        bar.isCarryingOutTouchAction = false
                    }
                }
        )

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

    private fun handlePotentialSwipe(motionEvent: MotionEvent?): Boolean {
        if (motionEvent == null) return false

        val distanceX = motionEvent.rawX - origX
        val distanceY = motionEvent.rawY - origY
        val xThresh = context.prefManager.xThresholdPx
        val yThreshUp = context.prefManager.yThresholdUpPx
        val yThreshDown = context.prefManager.yThresholdDownPx

        val slop = bar.viewConfig.scaledTouchSlop

        if (distanceX.absoluteValue < slop && distanceY.absoluteValue < slop) return false

        return if (!bar.isHidden && !isActing) {
            when {
                context.actionHolder.run { hasAnyOfActions(actionLeft, actionLeftHold) }
                        && distanceX < 0
//                        && distanceX < -xThresh
                        && distanceY.absoluteValue <= distanceX.absoluteValue -> { //left swipe
                    isSwipeLeft = true
                    true
                }
                context.actionHolder.run { hasAnyOfActions(actionRight, actionRightHold) }
                        && distanceX > 0
//                        && distanceX > xThresh
                        && distanceY.absoluteValue <= distanceX.absoluteValue -> { //right swipe
                    isSwipeRight = true
                    true
                }
                context.actionHolder.run { hasAnyOfActions(actionDown, actionDownHold) }
                        && distanceY > 0
//                        && distanceY > yThreshDown
                        && distanceY.absoluteValue > distanceX.absoluteValue -> { //down swipe and down hold-swipe
                    isSwipeDown = true
                    true
                }
                context.actionHolder.hasSomeUpAction()
                        && distanceY < 0
//                        && distanceY < -yThreshUp
                        && distanceY.absoluteValue > distanceX.absoluteValue -> { //up swipe and up hold-swipe
                    isSwipeUp = true
                    true
                }
                else -> false
            }
        } else if (bar.isHidden
                && !isActing
                && distanceY < 0
//                && distanceY < -yThreshUp
                && distanceY.absoluteValue > distanceX.absoluteValue) { //up swipe
            if (!bar.isPillHidingOrShowing && !bar.beingTouched) {
                bar.vibrate(context.prefManager.vibrationDuration.toLong())
                bar.showPill(HiddenPillReasonManager.MANUAL, true)
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