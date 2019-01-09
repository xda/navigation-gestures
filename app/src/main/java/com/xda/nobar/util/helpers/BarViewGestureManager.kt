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
import java.util.*
import kotlin.math.absoluteValue

/**
 * Manage all the gestures on the pill
 */
class BarViewGestureManager(private val bar: BarView) {
    companion object {
        private const val FIRST_SECTION = 0
        private const val SECOND_SECTION = 1
        private const val THIRD_SECTION = 2

        private const val MSG_UP_HOLD = 0
        private const val MSG_LEFT_HOLD = 1
        private const val MSG_RIGHT_HOLD = 2
        private const val MSG_DOWN_HOLD = 3
    }
    
    val actionMap = HashMap<String, Int>()

    private val context = bar.context

    private var isSwipeUp = false
    private var isSwipeLeft = false
    private var isSwipeRight = false
    private var isSwipeDown = false
    private var isOverrideTap = false
    private var wasHidden = false
    var lastTouchTime = -1L

    var isActing = false

    private var isRunningLongUp = false
    private var isRunningLongLeft = false
    private var isRunningLongRight = false
    private var isRunningLongDown = false

    private var sentLongUp = false
    private var sentLongLeft = false
    private var sentLongRight = false
    private var sentLongDown = false

    private var oldEvent: MotionEvent? = null
    private var oldY = 0F
    private var oldX = 0F

    private var origX = 0F
    private var origY = 0F

    private var origAdjX = 0F
    private var origAdjY = 0F

    private val manager = GestureDetector(bar.context, Detector())
    val actionHandler = BarViewActionHandler(bar)

    private val gestureThread = HandlerThread("NoBar-Gesture").apply { start() }
    private val gestureHandler = GestureHandler(gestureThread.looper)

    fun onTouchEvent(ev: MotionEvent?): Boolean {
        return handleTouchEvent(ev) || manager.onTouchEvent(ev)
    }

    private fun handleTouchEvent(ev: MotionEvent?): Boolean {
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
                                if (bar.params.x == bar.getAdjustedHomeX()) {
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
                                if (bar.params.y == bar.getAdjustedHomeY()) {
                                    isActing = false
                                    bar.isCarryingOutTouchAction = false
                                }
                            }
                            .start()
                }

                when {
                    bar.params.y != bar.getAdjustedHomeY() && !bar.isHidden && !bar.isPillHidingOrShowing -> {
                        bar.animator.homeY(DynamicAnimation.OnAnimationEndListener { _, _, _, _ ->
                            isActing = false
                            bar.isCarryingOutTouchAction = false
                        })
                    }
                    bar.params.x < bar.getAdjustedHomeX() || bar.params.x > bar.getAdjustedHomeX() -> {
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

        oldEvent = MotionEvent.obtain(ev)

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
                vibrate(context.prefManager.vibrationDuration.toLong())
                bar.hiddenPillReasons.remove(HiddenPillReasonManager.MANUAL)
                bar.showPill(null, true)
            }
            true
        } else false
    }

    private fun getSectionedUpHoldAction(x: Float): Int? {
        return if (!context.app.prefManager.sectionedPill) actionMap[bar.actionHolder.actionUpHold]
        else when (getSection(x)) {
            FIRST_SECTION -> actionMap[bar.actionHolder.actionUpHoldLeft]
            SECOND_SECTION -> actionMap[bar.actionHolder.actionUpHoldCenter]
            else -> actionMap[bar.actionHolder.actionUpHoldRight]
        }
    }

    private fun String.isEligible() = arrayListOf(
            bar.actionHolder.actionUp,
            bar.actionHolder.actionUpHold
    ).contains(this) && context.app.prefManager.sectionedPill

    private fun getSection(x: Float): Int {
        val third = context.app.prefManager.customWidth / 3f

        return when {
            x < third -> FIRST_SECTION
            x <= (2f * third) -> SECOND_SECTION
            else -> THIRD_SECTION
        }
    }

    private fun sendAction(action: String) {
        if (action.isEligible()) {
            when (getSection(origAdjX)) {
                FIRST_SECTION -> sendActionInternal("${action}_left")
                SECOND_SECTION -> sendActionInternal("${action}_center")
                THIRD_SECTION -> sendActionInternal("${action}_right")
            }
        } else {
            sendActionInternal(action)
        }
    }

    /**
     * Parse the action index and broadcast to {@link com.xda.nobar.services.Actions}
     * @param key one of ActionHolder's variables
     */
    private fun sendActionInternal(key: String) {
        bar.handler?.post {
            val which = actionMap[key] ?: return@post

            if (which == bar.actionHolder.typeNoAction) return@post

            if (bar.isHidden || bar.isPillHidingOrShowing) return@post

            vibrate(context.prefManager.vibrationDuration.toLong())

            if (key == bar.actionHolder.actionDouble)
                bar.handler?.postDelayed({ vibrate(context.prefManager.vibrationDuration.toLong()) },
                        context.prefManager.vibrationDuration.toLong())

            if (which == bar.actionHolder.typeHide) {
                bar.hidePill(false, null, true)
                return@post
            }

            when (key) {
                bar.actionHolder.actionDouble -> bar.animator.jiggleDoubleTap()
                bar.actionHolder.actionHold -> bar.animator.jiggleHold()
                bar.actionHolder.actionTap -> bar.animator.jiggleTap()
                bar.actionHolder.actionUpHold -> bar.animator.jiggleHoldUp()
                bar.actionHolder.actionLeftHold -> bar.animator.jiggleLeftHold()
                bar.actionHolder.actionRightHold -> bar.animator.jiggleRightHold()
                bar.actionHolder.actionDownHold -> bar.animator.jiggleDownHold()
            }

            if (key == bar.actionHolder.actionUp
                    || key == bar.actionHolder.actionLeft
                    || key == bar.actionHolder.actionRight) {
                bar.animate(null, ALPHA_ACTIVE)
            }

            if (bar.isAccessibilityAction(which)) {
                if (context.app.prefManager.useRoot && Shell.rootAccess()) {
                    actionHandler.sendRootAction(which, key)
                } else {
                    if (which == bar.actionHolder.typeHome
                            && context.prefManager.useAlternateHome) {
                        actionHandler.handleAction(which, key)
                    } else {
                        actionHandler.sendAccessibilityAction(which, key)
                    }
                }
            } else {
                actionHandler.handleAction(which, key)
            }
        }
    }

    /**
     * Load the user's custom gesture/action pairings; default values if a pairing doesn't exist
     */
    fun loadActionMap() {
        context.app.prefManager.getActionsList(actionMap)

        if (actionMap.values.contains(bar.actionHolder.premTypeFlashlight)) {
            if (!actionHandler.flashlightController.isCreated)
                actionHandler.flashlightController.onCreate()
        } else {
            actionHandler.flashlightController.onDestroy()
        }
    }

    /**
     * Vibrate for the specified duration
     * @param duration the desired duration
     */
    fun vibrate(duration: Long) {
        bar.handler?.post {
            if (bar.isSoundEffectsEnabled) {
                try {
                    bar.playSoundEffect(SoundEffectConstants.CLICK)
                } catch (e: Exception) {}
            }
        }

        if (duration > 0) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(duration)
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private inner class GestureHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message?) {
            when (msg?.what) {
                MSG_UP_HOLD -> {
                    if (getSectionedUpHoldAction(origAdjX) != bar.actionHolder.typeNoAction) {
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

    inner class Detector : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(ev: MotionEvent): Boolean {
            return if (!context.actionHolder.hasAnyOfActions(bar.actionHolder.actionDouble) && !isActing && !wasHidden) {
                isOverrideTap = true
                sendAction(bar.actionHolder.actionTap)
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
                    sendAction(bar.actionHolder.actionHold)
                }
            }
        }

        override fun onDoubleTap(ev: MotionEvent): Boolean {
            return if (!bar.isHidden && !isActing) {
                isActing = true
                sendAction(bar.actionHolder.actionDouble)
                true
            } else false
        }

        override fun onSingleTapConfirmed(ev: MotionEvent): Boolean {
            return if (!isOverrideTap && !bar.isHidden) {
                isActing = false

                sendAction(bar.actionHolder.actionTap)
                true
            } else if (bar.isHidden && !bar.isPillHidingOrShowing) {
                isOverrideTap = false
                vibrate(context.prefManager.vibrationDuration.toLong())
                bar.showPill(null, true)
                true
            } else {
                isOverrideTap = false
                false
            }
        }
    }
}