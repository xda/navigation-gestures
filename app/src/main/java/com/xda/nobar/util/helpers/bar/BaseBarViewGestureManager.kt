package com.xda.nobar.util.helpers.bar

import android.app.ActivityManager
import android.content.Context
import android.os.*
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.annotation.CallSuper
import com.xda.nobar.util.actionHolder
import com.xda.nobar.util.actionManager
import com.xda.nobar.util.app
import com.xda.nobar.util.helpers.HiddenPillReasonManagerNew
import com.xda.nobar.util.prefManager
import com.xda.nobar.views.BarView

abstract class BaseBarViewGestureManager(internal val bar: BarView) {
    companion object {
        internal const val FIRST_SECTION = 0
        internal const val SECOND_SECTION = 1
        internal const val THIRD_SECTION = 2

        internal const val MSG_UP_HOLD = 0
        internal const val MSG_LEFT_HOLD = 1
        internal const val MSG_RIGHT_HOLD = 2
        internal const val MSG_DOWN_HOLD = 3

        internal const val MSG_LONG = 11

        internal const val MSG_UP = 4
        internal const val MSG_LEFT = 5
        internal const val MSG_RIGHT = 6
        internal const val MSG_DOWN = 7

        internal const val MSG_TAP = 8
        internal const val MSG_DOUBLE_TAP = 9
        internal const val MSG_HOLD = 10
    }

    internal abstract val adjCoord: Float
    internal abstract val gestureHandler: BaseGestureHandler

    internal val context = bar.context

    internal var isSwipeUp = false
    internal var isSwipeLeft = false
    internal var isSwipeRight = false
    internal var isSwipeDown = false
    internal var isOverrideTap = false
    internal var wasHidden = false

    internal var isActing = false

    @Volatile
    internal var isRunningLongUp = false
    @Volatile
    internal var isRunningLongLeft = false
    @Volatile
    internal var isRunningLongRight = false
    @Volatile
    internal var isRunningLongDown = false

    internal var sentLongUp = false
    internal var sentLongLeft = false
    internal var sentLongRight = false
    internal var sentLongDown = false

    internal var sentLong = false

    internal var oldY = 0F
    internal var oldX = 0F

    internal var origX = 0F
    internal var origY = 0F

    internal var origAdjX = 0F
    internal var origAdjY = 0F

    internal val detector = BaseDetector()
    internal val manager by lazy { GestureDetector(bar.context, detector) }
    internal val actionManager = context.actionManager
    internal val actionHandler = actionManager.actionHandler
    internal val gestureThread = actionManager.gestureThread

    fun onTouchEvent(ev: MotionEvent?): Boolean {
        return handleTouchEvent(ev) || manager.onTouchEvent(ev)
    }

    internal abstract fun getSection(coord: Float): Int

    @CallSuper
    @Synchronized
    internal open fun handleTouchEvent(ev: MotionEvent?): Boolean {
        if (bar.isPillHidingOrShowing) return false

        when (ev?.action) {
            MotionEvent.ACTION_DOWN -> {
                wasHidden = bar.isHidden
                oldY = ev.rawY
                oldX = ev.rawX
                origX = ev.rawX
                origY = ev.rawY
                origAdjX = ev.x
                origAdjY = ev.y
                bar.beingTouched = true
                bar.isCarryingOutTouchAction = true

                bar.scheduleUnfade()

                displayProperFlash(true)

                if (!sentLong) {
                    sentLong = true
                    gestureHandler.sendEmptyMessageAtTime(MSG_LONG,
                            SystemClock.uptimeMillis() + context.prefManager.holdTime.toLong())
                }
            }

            MotionEvent.ACTION_UP -> {
                gestureHandler.removeMessages(MSG_LONG)
                handleActionUp()
            }
        }

        return true
    }

    private fun displayProperFlash(pressed: Boolean) {
//        val is90Vertical = bar.isVertical && !bar.is270Vertical
//
//        if (pressed) {
//            if (context.prefManager.sectionedPill) {
//                when (getSection(adjCoord)) {
//                    FIRST_SECTION -> if (is90Vertical) bar.section_3_flash.isPressed = pressed else bar.section_1_flash.isPressed = pressed
//                    SECOND_SECTION -> bar.section_2_flash.isPressed = pressed
//                    THIRD_SECTION -> if (is90Vertical) bar.section_1_flash.isPressed = pressed else bar.section_3_flash.isPressed = pressed
//                }
//            } else {
//                bar.pill_tap_flash.isPressed = pressed
//            }
//        } else {
//            bar.section_1_flash.isPressed = pressed
//            bar.section_2_flash.isPressed = pressed
//            bar.section_3_flash.isPressed = pressed
//            bar.pill_tap_flash.isPressed = pressed
//        }
    }

    internal fun finishUp() {
        isRunningLongRight = false
        isRunningLongLeft = false
        isRunningLongUp = false
        isRunningLongDown = false

        sentLongRight = false
        sentLongLeft = false
        sentLongUp = false
        sentLongDown = false

        sentLong = false

        isSwipeUp = false
        isSwipeLeft = false
        isSwipeRight = false
        isSwipeDown = false

        wasHidden = bar.isHidden
    }

    internal fun parseSwipe() {
        if (isSwipeUp) {
            gestureHandler.sendUp()
        }

        if (isSwipeLeft) {
            gestureHandler.sendLeft()
        }

        if (isSwipeRight) {
            gestureHandler.sendRight()
        }

        if (isSwipeDown) {
            gestureHandler.sendDown()
        }
    }

    internal fun getSectionedUpHoldAction(coord: Float): Int? {
        return if (!context.app.prefManager.sectionedPill) actionManager.getAction(bar.actionHolder.actionUpHold)
        else when (getSection(coord)) {
            FIRST_SECTION -> actionManager.getAction(bar.actionHolder.actionUpHoldLeft)
            SECOND_SECTION -> actionManager.getAction(bar.actionHolder.actionUpHoldCenter)
            else -> actionManager.getAction(bar.actionHolder.actionUpHoldRight)
        }
    }

    internal fun sendAction(action: String) {
        if (action.isEligible()) {
            when (getSection(adjCoord)) {
                FIRST_SECTION -> actionHandler.sendActionInternal("${action}_left")
                SECOND_SECTION -> actionHandler.sendActionInternal("${action}_center")
                THIRD_SECTION -> actionHandler.sendActionInternal("${action}_right")
            }
        } else {
            actionHandler.sendActionInternal(action)
        }
    }

    internal fun String.isEligible() = arrayListOf(
            bar.actionHolder.actionUp,
            bar.actionHolder.actionUpHold
    ).contains(this) && context.app.prefManager.sectionedPill

    @CallSuper
    open fun handleActionUp(isForce: Boolean = false) {
//        gestureHandler.clearLongQueues()
        bar.beingTouched = false

        displayProperFlash(false)
    }

    open inner class BaseDetector : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(ev: MotionEvent): Boolean {
            return if (!context.actionHolder.hasAnyOfActions(bar.actionHolder.actionDouble)
                    && !isActing && !wasHidden) {
                isOverrideTap = true
                gestureHandler.sendTap()
                isActing = false
                true
            } else false
        }

        override fun onLongPress(ev: MotionEvent) {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val isPinned = Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1 && am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE

            if (!bar.isHidden && !isActing) {
                if (isPinned) {
                    if (context.app.prefManager.shouldUseOverscanMethod) context.app.showNav()
                } else {
                    isActing = true
                    gestureHandler.sendHold()
                }
            }
        }

        override fun onDoubleTap(ev: MotionEvent): Boolean {
            return if (!bar.isHidden && !isActing) {
                isActing = true
                gestureHandler.sendDoubleTap()
                true
            } else false
        }

        override fun onSingleTapConfirmed(ev: MotionEvent): Boolean {
            return if (!isOverrideTap && !bar.isHidden) {
                isActing = false

                gestureHandler.sendTap()
                true
            } else if (bar.isHidden && !bar.isPillHidingOrShowing) {
                isOverrideTap = false
                bar.vibrate(context.prefManager.vibrationDuration.toLong())
                bar.showPill(HiddenPillReasonManagerNew.MANUAL, true)
                true
            } else {
                isOverrideTap = false
                false
            }
        }
    }

    abstract inner class BaseGestureHandler(looper: Looper) : Handler(looper) {
        @Synchronized
        override fun handleMessage(msg: Message?) {
            when (msg?.what) {
                MSG_LONG -> parseLongSwipe()

                MSG_UP_HOLD -> if (!isRunningLongUp) handleLongUp()
                MSG_DOWN_HOLD -> if (!isRunningLongDown) handleLongDown()
                MSG_LEFT_HOLD -> if (!isRunningLongLeft) handleLongLeft()
                MSG_RIGHT_HOLD -> if (!isRunningLongRight) handleLongRight()

                MSG_UP -> if (!isRunningLongUp) handleUp()
                MSG_DOWN -> if (!isRunningLongDown) handleDown()
                MSG_LEFT -> if (!isRunningLongLeft) handleLeft()
                MSG_RIGHT -> if (!isRunningLongRight) handleRight()

                MSG_TAP -> handleTap()
                MSG_DOUBLE_TAP -> handleDoubleTap()
                MSG_HOLD -> handleHold()
            }
        }

        internal abstract fun handleLongUp()
        internal abstract fun handleLongDown()
        internal abstract fun handleLongLeft()
        internal abstract fun handleLongRight()

        internal abstract fun handleUp()
        internal abstract fun handleDown()
        internal abstract fun handleLeft()
        internal abstract fun handleRight()

        internal open fun handleTap() {
            sendAction(bar.actionHolder.actionTap)
        }

        internal open fun handleDoubleTap() {
            sendAction(bar.actionHolder.actionDouble)
        }

        internal open fun handleHold() {
            sendAction(bar.actionHolder.actionHold)
        }

        internal fun parseLongSwipe() {
            if (isSwipeUp) {
                gestureHandler.sendLongUp()
            }

            if (isSwipeLeft) {
                gestureHandler.sendLongLeft()
            }

            if (isSwipeRight) {
                gestureHandler.sendLongRight()
            }

            if (isSwipeDown) {
                gestureHandler.sendLongDown()
            }
        }

//        fun clearLongQueues() {
//            removeMessages(MSG_UP_HOLD)
//            removeMessages(MSG_LEFT_HOLD)
//            removeMessages(MSG_RIGHT_HOLD)
//            removeMessages(MSG_DOWN_HOLD)
//        }

        fun sendLongUp() {
            if (!sentLongUp) {
                sentLongUp = true
                sendEmptyMessage(MSG_UP_HOLD)
            }
        }

        fun sendLongDown() {
            if (!sentLongDown) {
                sentLongDown = true
                sendEmptyMessage(MSG_DOWN_HOLD)
            }
        }

        fun sendLongLeft() {
            if (!sentLongLeft) {
                sentLongLeft = true
                sendEmptyMessage(MSG_LEFT_HOLD)
            }
        }

        fun sendLongRight() {
            if (!sentLongRight) {
                sentLongRight = true
                sendEmptyMessage(MSG_RIGHT_HOLD)
            }
        }

//        fun queueUpHold() {
//            if (!sentLongUp) {
//                sentLongUp = true
//                sendEmptyMessageAtTime(MSG_UP_HOLD,
//                        SystemClock.uptimeMillis() + context.prefManager.holdTime.toLong())
//            }
//        }
//
//        fun queueDownHold() {
//            if (!sentLongDown) {
//                sentLongDown = true
//                sendEmptyMessageAtTime(MSG_DOWN_HOLD,
//                        SystemClock.uptimeMillis() + context.prefManager.holdTime.toLong())
//            }
//        }
//
//        fun queueLeftHold() {
//            if (!sentLongLeft) {
//                sentLongLeft = true
//                sendEmptyMessageAtTime(MSG_LEFT_HOLD,
//                        SystemClock.uptimeMillis() + context.prefManager.holdTime.toLong())
//            }
//        }
//
//        fun queueRightHold() {
//            if (!sentLongRight) {
//                sentLongRight = true
//                sendEmptyMessageAtTime(MSG_RIGHT_HOLD,
//                        SystemClock.uptimeMillis() + context.prefManager.holdTime.toLong())
//            }
//        }

        fun sendUp() {
            if (!isRunningLongUp) {
                removeMessages(MSG_UP_HOLD)
                sendEmptyMessage(MSG_UP)
            }
        }

        fun sendDown() {
            if (!isRunningLongDown) {
                removeMessages(MSG_DOWN_HOLD)
                sendEmptyMessage(MSG_DOWN)
            }
        }

        fun sendLeft() {
            if (!isRunningLongLeft) {
                removeMessages(MSG_LEFT_HOLD)
                sendEmptyMessage(MSG_LEFT)
            }
        }

        fun sendRight() {
            if (!isRunningLongRight) {
                removeMessages(MSG_RIGHT_HOLD)
                sendEmptyMessage(MSG_RIGHT)
            }
        }

        fun sendTap() {
            sendEmptyMessage(MSG_TAP)
        }

        fun sendDoubleTap() {
            sendEmptyMessage(MSG_DOUBLE_TAP)
        }

        fun sendHold() {
            sendEmptyMessage(MSG_HOLD)
        }
    }
}