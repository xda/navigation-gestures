package com.xda.nobar.util.helpers.bar

import android.annotation.SuppressLint
import android.os.Looper
import android.view.MotionEvent
import androidx.dynamicanimation.animation.DynamicAnimation
import com.xda.nobar.util.actionHolder
import com.xda.nobar.util.app
import com.xda.nobar.util.helpers.HiddenPillReasonManager
import com.xda.nobar.util.prefManager
import com.xda.nobar.util.realScreenSize
import com.xda.nobar.views.BarView
import kotlinx.android.synthetic.main.pill.view.*
import kotlin.math.absoluteValue

/**
 * Manage all the gestures on the pill
 */
class BarViewGestureManagerVertical(bar: BarView) : BaseBarViewGestureManager(bar) {
    override val adjCoord: Float
        get() = origAdjY
    override val gestureHandler by lazy { GestureHandler(gestureThread.looper) }

    override fun handleTouchEvent(ev: MotionEvent?): Boolean {
        super.handleTouchEvent(ev)

        var ultimateReturn = false

        when (ev?.action) {
            MotionEvent.ACTION_UP -> {
                if (wasHidden) {
                    isSwipeLeft = false
                }

                gestureHandler.clearLongQueues()

                parseSwipe()

                bar.animatePillToHome(
                        {
                            if (bar.params.y == bar.adjustedHomeY) {
                                isActing = false
                                isSwipeUp = false
                                isSwipeDown = false
                            }
                        },
                        {
                            if (bar.params.x == bar.adjustedHomeX) {
                                isActing = false
                                bar.isCarryingOutTouchAction = false
                            }
                        }
                )

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

                finishUp()
            }
            MotionEvent.ACTION_MOVE -> {
                ultimateReturn = handlePotentialSwipe(ev)

                when {
                    isSwipeLeft -> {
                        if (!isActing) isActing = true

                        val velocity = (oldX - ev.rawX)
                        oldX = ev.rawX

                        val screenWidth = context.realScreenSize.y

                        if (bar.params.x < screenWidth / 6 + bar.adjustedHomeX
                                && bar.getAnimationDurationMs() > 0) {
                            bar.params.x += (velocity / 2).toInt()
                            bar.updateLayout()
                        }

                        gestureHandler.queueLeftHold()
                    }

                    isSwipeRight -> {
                        if (!isActing) isActing = true

                        val velocity = (oldX - ev.rawX)
                        oldX = ev.rawX

                        if (bar.getAnimationDurationMs() > 0) {
                            bar.params.x += (velocity / 2).toInt()
                            bar.updateLayout()
                        }

                        gestureHandler.queueRightHold()
                    }

                    isSwipeUp || isSwipeDown -> {
                        if (!isActing) isActing = true

                        val velocity = ev.rawY - oldY
                        oldY = ev.rawY

                        val halfScreen = context.realScreenSize.x / 2f
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

                        if (isSwipeDown) {
                            gestureHandler.queueDownHold()
                        }

                        if (isSwipeUp) {
                            gestureHandler.queueUpHold()
                        }
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
        val yThreshDown = context.prefManager.yThresholdDownPx

        return if (!bar.isHidden && !isActing) {
            when {
                context.actionHolder.run { hasAnyOfActions(actionLeft, actionLeftHold) }
                        && distanceY > xThresh
                        && distanceY.absoluteValue > distanceX.absoluteValue -> { //down swipe
                    isSwipeDown = true
                    true
                }
                context.actionHolder.run { hasAnyOfActions(actionRight, actionRightHold) }
                        && distanceY < -xThresh
                        && distanceY.absoluteValue > distanceX.absoluteValue -> { //up swipe
                    isSwipeUp = true
                    true
                }
                context.actionHolder.run { hasAnyOfActions(actionDown, actionDownHold) }
                        && distanceX > yThreshDown
                        && distanceY.absoluteValue < distanceX.absoluteValue -> { //right swipe
                    isSwipeRight = true
                    true
                }
                context.actionHolder.hasSomeUpAction()
                        && distanceX < -yThreshUp
                        && distanceY.absoluteValue < distanceX.absoluteValue -> { //left swipe
                    isSwipeLeft = true
                    true
                }
                else -> false
            }
        } else if (bar.isHidden
                && !isActing
                && distanceX < -xThresh
                && distanceY.absoluteValue < distanceX.absoluteValue) { //left swipe
            if (bar.isHidden && !bar.isPillHidingOrShowing && !bar.beingTouched) {
                bar.vibrate(context.prefManager.vibrationDuration.toLong())
                bar.showPill(HiddenPillReasonManager.MANUAL, true)
            }
            true
        } else false
    }

    override fun getSection(coord: Float): Int {
        val third = bar.adjustedHeight / 3f

        return when {
            coord < third -> THIRD_SECTION
            coord <= (2f * third) -> SECOND_SECTION
            else -> FIRST_SECTION
        }
    }

    @SuppressLint("HandlerLeak")
    inner class GestureHandler(looper: Looper) : BaseGestureHandler(looper) {
        override fun handleLongUp() {
            if (actionMap[bar.actionHolder.actionRightHold] != bar.actionHolder.typeNoAction) {
                isRunningLongUp = true
                sendAction(bar.actionHolder.actionRightHold)
            }
        }

        override fun handleLongDown() {
            if (actionMap[bar.actionHolder.actionLeftHold] != bar.actionHolder.typeNoAction) {
                isRunningLongDown = true
                sendAction(bar.actionHolder.actionLeftHold)
            }
        }

        override fun handleLongLeft() {
            if (getSectionedUpHoldAction(adjCoord) != bar.actionHolder.typeNoAction) {
                isRunningLongLeft = true
                sendAction(bar.actionHolder.actionUpHold)
            }
        }

        override fun handleLongRight() {
            if (actionMap[bar.actionHolder.actionDownHold] != bar.actionHolder.typeNoAction) {
                isRunningLongRight = true
                sendAction(bar.actionHolder.actionDownHold)
            }
        }

        override fun handleUp() {
            sendAction(bar.actionHolder.actionRight)
        }

        override fun handleDown() {
            sendAction(bar.actionHolder.actionLeft)
        }

        override fun handleLeft() {
            sendAction(bar.actionHolder.actionUp)
        }

        override fun handleRight() {
            sendAction(bar.actionHolder.actionDown)
        }
    }
}