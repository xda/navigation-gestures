package com.xda.nobar.util.helpers.bar

import android.app.ActivityManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.GestureDetector
import android.view.IRotationWatcher
import android.view.MotionEvent
import android.view.Surface
import androidx.dynamicanimation.animation.DynamicAnimation
import com.xda.nobar.util.*
import com.xda.nobar.util.helpers.HiddenPillReasonManagerNew
import com.xda.nobar.views.BarView
import kotlinx.android.synthetic.main.pill.view.*
import kotlin.math.absoluteValue

class NewBarViewGestureManager(private val bar: BarView) : ContextWrapper(bar.context.applicationContext) {
    companion object {
        const val MSG_LONG_UP = 100
        const val MSG_LONG_DOWN = 101
        const val MSG_LONG_LEFT = 102
        const val MSG_LONG_RIGHT = 103
    }

    private enum class Mode {
        PORTRAIT,
        LANDSCAPE_90,
        LANDSCAPE_270,
    }

    private var downX: Float = 0f
    private var downY: Float = 0f
    private var downAdjX: Float = 0f
    private var downAdjY: Float = 0f

    private var prevX: Float = 0f
    private var prevY: Float = 0f

    private var mode: Mode = Mode.PORTRAIT
    private var wasHidden = false
    private var isOverrideTap = false

    private val detector = GestureDetector(this, Listener())
    private val longHandler = LongHandler()
    private val actionHandler = actionManager.actionHandler

    private val rotationWatcher = object : IRotationWatcher.Stub() {
        override fun onRotationChanged(rotation: Int) {
            setMode(rotation)
        }
    }

    init {
        app.addRotationWatcher(rotationWatcher)
        setMode(cachedRotation)
    }

    fun onTouchEvent(ev: MotionEvent?): Boolean {
        return handleTouchEvent(ev) or detector.onTouchEvent(ev)
    }

    private fun handleTouchEvent(e: MotionEvent?): Boolean {
        if (bar.isPillHidingOrShowing) return false

        when (e?.action) {
            MotionEvent.ACTION_DOWN -> {
                wasHidden = bar.isHidden

                downX = e.rawX
                downY = e.rawY
                prevX = downX
                prevY = downY
                downAdjX = e.x
                downAdjY = e.y

                bar.beingTouched = true
                bar.isCarryingOutTouchAction = true

                bar.scheduleUnfade()
            }

            MotionEvent.ACTION_MOVE -> {
                val newX = e.rawX
                val newY = e.rawY

                val origVX = newX - prevX
                val velocityX = origVX * if (bar.is270Vertical) -1 else 1
                val velocityY = prevY - newY

                val slop = bar.viewConfig.scaledTouchSlop
                if ((newX - downX).absoluteValue < slop && (newY - downY).absoluteValue < slop) return false

                prevX = newX
                prevY = newY

                if (!bar.isHidden) {
                    if (bar.shouldAnimate) {
                        val halfScreen = realScreenSize.x / 2f
                        val leftParam = bar.params.x - prefManager.customWidth.toFloat() / 2f
                        val rightParam = bar.params.x + prefManager.customWidth.toFloat() / 2f
                        val topParam = bar.params.y - prefManager.customWidth.toFloat() / 2f
                        val bottomParam = bar.params.y + prefManager.customWidth.toFloat() / 2f

                        if (bar.isVertical) {
                            when {
                                topParam <= -halfScreen && velocityY > 0 -> {
                                    bar.pill.translationY -= velocityY
                                }
                                bottomParam >= halfScreen && velocityY < 0 -> {
                                    bar.pill.translationY -= velocityY
                                }
                                else -> {
                                    bar.params.y = bar.params.y - (velocityY / 2).toInt()
                                }
                            }

                            bar.params.x += (velocityX / 2).toInt()
                        } else {
                            when {
                                leftParam <= -halfScreen && velocityX < 0 -> {
                                    bar.pill.translationX += velocityX
                                }
                                rightParam >= halfScreen && velocityX > 0 -> {
                                    bar.pill.translationX += velocityX
                                }
                                else -> {
                                    bar.params.x = bar.params.x + (velocityX / 2).toInt()
                                }
                            }

                            bar.params.y -= (velocityY / 2).toInt()
                        }

                        bar.updateLayout()

                        parseSwipe(false)
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                handleActionUp()

                wasHidden = bar.isHidden
                bar.beingTouched = false
                bar.isCarryingOutTouchAction = false
            }
        }

        return true
    }

    fun handleActionUp(isForce: Boolean = false) {
        var isXDone = false
        var isYDone = false

        var isParamDone = false

        if (!bar.isHidden) {
            bar.animatePillToHome(
                    {
                        isXDone = true
                        if (isXDone && isYDone && isParamDone) bar.isCarryingOutTouchAction = false
                    },
                    {
                        isYDone = true
                        if (isXDone && isYDone && isParamDone) bar.isCarryingOutTouchAction = false
                    }
            )
        }

        if (bar.params.x != bar.adjustedHomeX || bar.params.y != bar.adjustedHomeY) {
            if (bar.isVertical && (bar.isHidden || bar.isPillHidingOrShowing)) {
                bar.animator.horizontalHomeY(DynamicAnimation.OnAnimationEndListener {_, _, _, _ ->
                    isParamDone = true
                    if (isXDone && isYDone && isParamDone) bar.isCarryingOutTouchAction = false
                })
            } else if (!bar.isVertical && (bar.isHidden || bar.isPillHidingOrShowing)) {
                bar.animator.horizontalHomeX(DynamicAnimation.OnAnimationEndListener {_, _, _, _ ->
                    isParamDone = true
                    if (isXDone && isYDone && isParamDone) bar.isCarryingOutTouchAction = false
                })
            } else {
                bar.animator.home(DynamicAnimation.OnAnimationEndListener {_, _, _, _ ->
                    isParamDone = true
                    if (isXDone && isYDone && isParamDone) bar.isCarryingOutTouchAction = false
                })
            }
        } else {
            isParamDone = true
            if (isXDone && isYDone && isParamDone) bar.isCarryingOutTouchAction = false
        }

//        when {
//            bar.params.x != bar.adjustedHomeX && (!bar.isVertical || (!bar.isHidden && !bar.isPillHidingOrShowing)) -> {
//                bar.animator.horizontalHomeX(DynamicAnimation.OnAnimationEndListener { _, _, _, _ ->
//                    isParamDone = true
//                    if (isXDone && isYDone && isParamDone) bar.isCarryingOutTouchAction = false
//                })
//            }
//            bar.params.y != bar.adjustedHomeY && (bar.isVertical || (!bar.isHidden && !bar.isPillHidingOrShowing)) -> {
//                bar.animator.horizontalHomeY(DynamicAnimation.OnAnimationEndListener { _, _, _, _ ->
//                    if (isXDone && isYDone && isParamDone && isParamYDone) bar.isCarryingOutTouchAction = false
//                })
//            }
//            else -> {
//                isParamDone = true
//                if (isXDone && isYDone && isParamDone && isParamYDone) bar.isCarryingOutTouchAction = false
//            }
//        }

        if (!isForce) {
            parseSwipe(true)
        }

        sentLongUp = false
        sentLongDown = false
        sentLongLeft = false
        sentLongRight = false
        sentTap = false
    }

    private fun parseSwipe(isActionUp: Boolean = false) {
        val distanceX = prevX - downX
        val distanceY = prevY - downY
        val xThresh = prefManager.xThresholdPx
        val yThreshUp = prefManager.yThresholdUpPx
        val yThreshDown = prefManager.yThresholdDownPx

        if (isActionUp) longHandler.removeCallbacksAndMessages(null)

        if ((actionHolder.hasAnyOfActions(actionHolder.actionLeft, actionHolder.actionLeftHold) || bar.isHidden)
                && distanceX < 0
                && distanceX < -xThresh
                && distanceX.absoluteValue >= distanceY.absoluteValue) {
            //(long) left swipe
//            Log.e("NoBar", "left")

            when {
                bar.is90Vertical -> if (isActionUp) {
                    sendUp()
                    if (bar.isHidden) showPill()
                } else longHandler.postLongUp()
                bar.is270Vertical -> if (isActionUp) sendDown() else longHandler.postLongDown()
                else -> if (isActionUp) sendLeft() else longHandler.postLongLeft()
            }
        } else if ((actionHolder.hasAnyOfActions(actionHolder.actionRight, actionHolder.actionRightHold) || bar.isHidden)
                && distanceX > 0
                && distanceX > xThresh
                && distanceX.absoluteValue >= distanceY.absoluteValue) {
            //(long) right swipe
//            Log.e("NoBar", "right")

            when {
                bar.is90Vertical -> if (isActionUp) sendDown() else longHandler.postLongDown()
                bar.is270Vertical -> if (isActionUp) {
                    sendUp()
                    if (bar.isHidden) showPill()
                } else longHandler.postLongUp()
                else -> if (isActionUp) sendRight() else longHandler.postLongRight()
            }
        } else if ((actionHolder.hasSomeUpAction() || bar.isHidden)
                && distanceY < 0
                && distanceY < -yThreshUp) {
            //(long) up swipes
//            Log.e("NoBar", "up")

            when {
                bar.is90Vertical -> if (isActionUp) sendRight() else longHandler.postLongRight()
                bar.is270Vertical -> if (isActionUp) sendLeft() else longHandler.postLongLeft()
                else -> if (isActionUp) {
                    sendUp()
                    if (bar.isHidden) showPill()
                } else longHandler.postLongUp()
            }
        } else if (actionHolder.hasAnyOfActions(actionHolder.actionDown, actionHolder.actionDownHold)
                && distanceY > 0
                && distanceY > yThreshDown) {
            //(long) down swipe
//            Log.e("NoBar", "down")

            when {
                bar.is90Vertical -> if (isActionUp) sendLeft() else longHandler.postLongLeft()
                bar.is270Vertical -> if (isActionUp) sendRight() else longHandler.postLongRight()
                else -> if (isActionUp) sendDown() else longHandler.postLongDown()
            }
        }

//        Log.e("NoBar", "distanceX: $distanceX, \ndistanceY: $distanceY, \nxThresh: $xThresh, \nyThreshUp: $yThreshUp, \nyThreshDown: $yThreshDown")
    }

    private fun setMode(rotation: Int) {
        mode = when (rotation) {
            Surface.ROTATION_0,
            Surface.ROTATION_180 -> Mode.PORTRAIT
            Surface.ROTATION_90 -> Mode.LANDSCAPE_90
            else -> Mode.LANDSCAPE_270
        }
    }

    private val adjCoord: Float
        get() = when {
            bar.is270Vertical || bar.is90Vertical -> {
                downAdjY
            }

            else -> {
                downAdjX
            }
        }

    private fun sendAction(action: String) {
        if (action.isEligible()) {
            when (getSection(adjCoord)) {
                BaseBarViewGestureManager.FIRST_SECTION -> actionHandler.sendActionInternal("${action}_left")
                BaseBarViewGestureManager.SECOND_SECTION -> actionHandler.sendActionInternal("${action}_center")
                BaseBarViewGestureManager.THIRD_SECTION -> actionHandler.sendActionInternal("${action}_right")
            }
        } else {
            actionHandler.sendActionInternal(action)
        }
    }

    private fun getSection(coord: Float): Int {
        return when {
            bar.is90Vertical -> {
                val third = bar.adjustedHeight / 3f
                when {
                    coord < third -> BaseBarViewGestureManager.THIRD_SECTION
                    coord <= (2f * third) -> BaseBarViewGestureManager.SECOND_SECTION
                    else -> BaseBarViewGestureManager.FIRST_SECTION
                }
            }

            bar.is270Vertical -> {
                val third = bar.adjustedHeight / 3f
                when {
                    coord < third -> BaseBarViewGestureManager.FIRST_SECTION
                    coord <= (2f * third) -> BaseBarViewGestureManager.SECOND_SECTION
                    else -> BaseBarViewGestureManager.THIRD_SECTION
                }
            }

            else -> {
                val third = prefManager.customWidth / 3f
                when {
                    coord < third -> BaseBarViewGestureManager.FIRST_SECTION
                    coord <= (2f * third) -> BaseBarViewGestureManager.SECOND_SECTION
                    else -> BaseBarViewGestureManager.THIRD_SECTION
                }
            }
        }
    }

    private fun String.isEligible() = arrayListOf(
            bar.actionHolder.actionUp,
            bar.actionHolder.actionUpHold
    ).contains(this) && prefManager.sectionedPill

    private var sentLongUp = false
    private var sentLongDown = false
    private var sentLongLeft = false
    private var sentLongRight = false
    private var sentTap = false

    private fun sendUp() {
        if (!sentLongUp && !bar.isHidden) {
//            Log.e("NoBar", "up")

            sendAction(actionHolder.actionUp)
        }
    }

    private fun sendDown() {
        if (!sentLongDown && !bar.isHidden) {
//            Log.e("NoBar", "down")

            sendAction(actionHolder.actionDown)
        }
    }

    private fun sendLeft() {
        if (!sentLongLeft && !bar.isHidden) {
//            Log.e("NoBar", "left")

            sendAction(actionHolder.actionLeft)
        }
    }

    private fun sendRight() {
        if (!sentLongRight && !bar.isHidden) {
//            Log.e("NoBar", "right")

            sendAction(actionHolder.actionRight)
        }
    }

    private fun sendLongUp() {
        if (!bar.isHidden) {
            sentLongUp = true
//            Log.e("NoBar", "longUp")

            sendAction(actionHolder.actionUpHold)
        }
    }

    private fun sendLongDown() {
        if (!bar.isHidden) {
            sentLongDown = true
//            Log.e("NoBar", "longDown")

            sendAction(actionHolder.actionDownHold)
        }
    }

    private fun sendLongLeft() {
        if (!bar.isHidden) {
            sentLongLeft = true
//            Log.e("NoBar", "longLeft")

            sendAction(actionHolder.actionLeftHold)
        }
    }

    private fun sendLongRight() {
        if (!bar.isHidden) {
            sentLongRight = false
//            Log.e("NoBar", "longRight")

            sendAction(actionHolder.actionRightHold)
        }
    }

    private fun sendTap() {
        if (!bar.isHidden) {
            sentTap = true

//            Log.e("NoBar", "tap")

            sendAction(actionHolder.actionTap)
        }
    }

    private fun sendHold() {
        if (!bar.isHidden) {
//            Log.e("NoBar", "hold")

            sendAction(actionHolder.actionHold)
        }
    }

    private fun sendDouble() {
        if (!bar.isHidden) {
            if (!sentTap) {
//                Log.e("NoBar", "double")

                sendAction(actionHolder.actionDouble)
            }
        }
    }

    private fun showPill() {
        bar.vibrate(prefManager.vibrationDuration.toLong())
        bar.showPill(HiddenPillReasonManagerNew.MANUAL, true)
    }

    inner class Listener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(ev: MotionEvent): Boolean {
            return if (!actionHolder.hasAnyOfActions(bar.actionHolder.actionDouble)
                    && !wasHidden) {
                isOverrideTap = true
                sendTap()
                true
            } else false
        }

        override fun onLongPress(ev: MotionEvent) {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val isPinned = Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1 && am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE

            if (!bar.isHidden) {
                if (isPinned) {
                    if (prefManager.shouldUseOverscanMethod) app.showNav()
                } else {
                    sendHold()
                }
            }
        }

        override fun onDoubleTap(ev: MotionEvent): Boolean {
            return if (!bar.isHidden) {
                sendDouble()
                true
            } else false
        }

        override fun onSingleTapConfirmed(ev: MotionEvent): Boolean {
            return if (!isOverrideTap && !bar.isHidden) {
                sendTap()
                true
            } else if (bar.isHidden && !bar.isPillHidingOrShowing) {
                isOverrideTap = false
                showPill()
                true
            } else {
                isOverrideTap = false
                false
            }
        }
    }

    inner class LongHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message?) {
            when (msg?.what) {
                MSG_LONG_UP -> {
                    sendLongUp()
                }

                MSG_LONG_DOWN -> {
                    sendLongDown()
                }

                MSG_LONG_LEFT -> {
                    sendLongLeft()
                }

                MSG_LONG_RIGHT -> {
                    sendLongRight()
                }
            }
        }

        fun postLongUp() {
            removeMessages(MSG_LONG_LEFT)
            removeMessages(MSG_LONG_RIGHT)
            removeMessages(MSG_LONG_DOWN)

            if (!hasMessages(MSG_LONG_UP)) {
                sendEmptyMessageDelayed(MSG_LONG_UP, prefManager.holdTime.toLong())
            }
        }

        fun postLongDown() {
            removeMessages(MSG_LONG_LEFT)
            removeMessages(MSG_LONG_RIGHT)
            removeMessages(MSG_LONG_UP)

            if (!hasMessages(MSG_LONG_DOWN)) {
                sendEmptyMessageDelayed(MSG_LONG_DOWN, prefManager.holdTime.toLong())
            }
        }

        fun postLongLeft() {
            removeMessages(MSG_LONG_UP)
            removeMessages(MSG_LONG_RIGHT)
            removeMessages(MSG_LONG_DOWN)

            if (!hasMessages(MSG_LONG_LEFT)) {
                sendEmptyMessageDelayed(MSG_LONG_LEFT, prefManager.holdTime.toLong())
            }
        }

        fun postLongRight() {
            removeMessages(MSG_LONG_UP)
            removeMessages(MSG_LONG_LEFT)
            removeMessages(MSG_LONG_DOWN)

            if (!hasMessages(MSG_LONG_RIGHT)) {
                sendEmptyMessageDelayed(MSG_LONG_RIGHT, prefManager.holdTime.toLong())
            }
        }
    }
}