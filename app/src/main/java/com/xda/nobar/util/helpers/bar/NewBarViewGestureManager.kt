package com.xda.nobar.util.helpers.bar

import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.GestureDetector
import android.view.IRotationWatcher
import android.view.MotionEvent
import android.view.Surface
import androidx.dynamicanimation.animation.DynamicAnimation
import com.xda.nobar.util.*
import com.xda.nobar.util.helpers.DisabledReasonManager
import com.xda.nobar.util.helpers.HiddenPillReasonManagerNew
import com.xda.nobar.views.BarView
import kotlinx.android.synthetic.main.pill.view.*
import kotlin.math.absoluteValue

class NewBarViewGestureManager(private val bar: BarView) : ContextWrapper(bar.context.applicationContext) {
    companion object {
        internal const val FIRST_SECTION = 0
        internal const val SECOND_SECTION = 1
        internal const val THIRD_SECTION = 2

        internal const val MSG_UP_HOLD = 0
        internal const val MSG_LEFT_HOLD = 1
        internal const val MSG_RIGHT_HOLD = 2
        internal const val MSG_DOWN_HOLD = 3

        internal const val MSG_UP = 4
        internal const val MSG_LEFT = 5
        internal const val MSG_RIGHT = 6
        internal const val MSG_DOWN = 7

        internal const val MSG_TAP = 8
        internal const val MSG_DOUBLE_TAP = 9
        internal const val MSG_HOLD = 10
        
        private const val MSG_LONG = 104

        private val patternLeftUp = arrayOf(Swipe.LEFT, Swipe.UP)
        private val patternUpLeft = arrayOf(Swipe.UP, Swipe.LEFT)
        private val patternRightUp = arrayOf(Swipe.RIGHT, Swipe.UP)
        private val patternUpRight = arrayOf(Swipe.UP, Swipe.RIGHT)
        private val patternLeftDown = arrayOf(Swipe.LEFT, Swipe.DOWN)
        private val patternDownLeft = arrayOf(Swipe.DOWN, Swipe.LEFT)
        private val patternRightDown = arrayOf(Swipe.RIGHT, Swipe.DOWN)
        private val patternDownRight = arrayOf(Swipe.DOWN, Swipe.RIGHT)
    }
    
    private enum class Mode {
        PORTRAIT,
        LANDSCAPE_90,
        LANDSCAPE_270,
    }

    private enum class Swipe {
        UP,
        DOWN,
        LEFT,
        RIGHT
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

    private var xThresh = prefManager.xThresholdPx
    private var yThreshUp = prefManager.yThresholdUpPx
    private var yThreshDown = prefManager.yThresholdDownPx

    private var slop = bar.viewConfig.scaledTouchSlop

    @Volatile
    private var forcedUp = false

    private val detector = GestureDetector(this, Listener())
    private val longHandler = LongHandler()
    private val actionHandler = actionManager.actionHandler

    private val swipes = ArrayList<Swipe>()
    private var lastSwipe: Swipe? = null

    private val adjCoord: Float
        get() = when {
            bar.is270Vertical || bar.is90Vertical -> {
                downAdjY
            }

            else -> {
                downAdjX
            }
        }

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
                forcedUp = false
                wasHidden = bar.isHidden

                xThresh = prefManager.xThresholdPx
                yThreshUp = prefManager.yThresholdUpPx
                yThreshDown = prefManager.yThresholdDownPx
                slop = bar.viewConfig.scaledTouchSlop

                downX = e.rawX
                downY = e.rawY
                prevX = downX
                prevY = downY
                downAdjX = e.x
                downAdjY = e.y

                bar.beingTouched = true
                bar.isCarryingOutTouchAction = true

                bar.removeFadeReason(HiddenPillReasonManagerNew.MANUAL, true)
            }

            MotionEvent.ACTION_MOVE -> {
                if (!forcedUp) {
                    val newX = e.rawX
                    val newY = e.rawY

                    val origVX = newX - prevX
                    var velocityX = origVX * if (bar.is270Vertical) -1 else 1
                    var velocityY = prevY - newY

                    val slop = bar.viewConfig.scaledTouchSlop

                    parseSwipe(newX, newY)

                    val xSlop = (downX - newX).absoluteValue > slop
                    val ySlop = (downY - newY).absoluteValue > slop

                    if (!xSlop && !ySlop) return false

                    if (!xSlop) velocityX = 0f
                    if (!ySlop) velocityY = 0f

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
                                        bar.params.y -= (velocityY / 2f).toInt()
                                    }
                                }

                                bar.params.x += (velocityX / 2f).toInt()
                            } else {
                                when {
                                    leftParam <= -halfScreen && velocityX < 0 -> {
                                        bar.pill.translationX += velocityX
                                    }
                                    rightParam >= halfScreen && velocityX > 0 -> {
                                        bar.pill.translationX += velocityX
                                    }
                                    else -> {
                                        bar.params.x += (velocityX / 2f).toInt()
                                    }
                                }

                                bar.params.y -= (velocityY / 2f).toInt()
                            }

                            bar.updateLayout()
                        }
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                handleActionUp()
                bar.updateHideStatus()
                bar.updateFadeStatus()

                wasHidden = bar.isHidden
            }
        }

        return true
    }

    fun handleActionUp(isForce: Boolean = false) {
        if (isForce) forcedUp = true

        bar.beingTouched = false
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
        } else {
            isXDone = true
            isYDone = true
            if (isParamDone) bar.isCarryingOutTouchAction = false
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
            if (bar.params.x != bar.adjustedHomeX || bar.params.y != bar.adjustedHomeY) {
                if (bar.isVertical && (bar.isHidden || bar.isPillHidingOrShowing)) {
                    bar.animator.horizontalHomeY(DynamicAnimation.OnAnimationEndListener { _, _, _, _ ->
                        isParamDone = true
                        if (isXDone && isYDone && isParamDone) bar.isCarryingOutTouchAction = false
                    })
                } else if (!bar.isVertical && (bar.isHidden || bar.isPillHidingOrShowing)) {
                    bar.animator.horizontalHomeX(DynamicAnimation.OnAnimationEndListener { _, _, _, _ ->
                        isParamDone = true
                        if (isXDone && isYDone && isParamDone) bar.isCarryingOutTouchAction = false
                    })
                } else {
                    bar.animator.home(DynamicAnimation.OnAnimationEndListener { _, _, _, _ ->
                        isParamDone = true
                        if (isXDone && isYDone && isParamDone) bar.isCarryingOutTouchAction = false
                    })
                }
            } else {
                isParamDone = true
                if (isXDone && isYDone && isParamDone) bar.isCarryingOutTouchAction = false
            }

            handleSwipe()
        } else {
            longHandler.removeCallbacksAndMessages(null)
        }

        swipes.clear()
        lastSwipe = null

        sentLongUp = false
        sentLongDown = false
        sentLongLeft = false
        sentLongRight = false
        sentTap = false
        sentLongComplexLeftUp = false
        sentLongComplexRightUp = false
        sentLongComplexLeftDown = false
        sentLongComplexRightDown = false

        downX = 0f
        downY = 0f
        prevX = 0f
        prevY = 0f
        downAdjX = 0f
        downAdjY = 0f
    }

    private val swipeThresh = dpAsPx(32)

    private fun addSwipe(swipe: Swipe, distance: Float) {
        if ((swipes.isEmpty() || swipes.last() != swipe)
                && distance.absoluteValue > swipeThresh) {
            if (swipes.size >= 2) swipes.removeAt(swipes.lastIndex)
            swipes.add(swipe)
        }
    }

    private fun parseSwipe(newX: Float, newY: Float) {
        val fullDistanceX = prevX - downX
        val fullDistanceY = prevY - downY

        val distanceX = newX - prevX
        val distanceY = newY - prevY

//        Log.e("NoBar", "$distanceX, $distanceY")

        if (fullDistanceX < 0
                && fullDistanceX < -xThresh
                && distanceX.absoluteValue >= distanceY.absoluteValue) {
            //(long) left swipe
//            Log.e("NoBar", "left")

            when {
                bar.is90Vertical -> {
                    lastSwipe = Swipe.UP
                    addSwipe(Swipe.UP, fullDistanceX)
                }
                bar.is270Vertical -> {
                    lastSwipe = Swipe.DOWN
                    addSwipe(Swipe.DOWN, fullDistanceX)
                }
                else -> {
                    lastSwipe = Swipe.LEFT
                    addSwipe(Swipe.LEFT, fullDistanceX)
                }
            }

            longHandler.postLong()
        } else if (fullDistanceX > 0
                && fullDistanceX > xThresh
                && distanceX.absoluteValue >= distanceY.absoluteValue) {
            //(long) right swipe
//            Log.e("NoBar", "right")

            when {
                bar.is90Vertical -> {
                    lastSwipe = Swipe.DOWN
                    addSwipe(Swipe.DOWN, fullDistanceX)
                }
                bar.is270Vertical -> {
                    lastSwipe = Swipe.UP
                    addSwipe(Swipe.UP, fullDistanceX)
                }
                else -> {
                    lastSwipe = Swipe.RIGHT
                    addSwipe(Swipe.RIGHT, fullDistanceX)
                }
            }

            longHandler.postLong()
        } else if (fullDistanceY < 0
                && fullDistanceY < -yThreshUp) {
            //(long) up swipes
//            Log.e("NoBar", "up")

            when {
                bar.is90Vertical -> {
                    lastSwipe = Swipe.RIGHT
                    addSwipe(Swipe.RIGHT, fullDistanceY)
                }
                bar.is270Vertical -> {
                    lastSwipe = Swipe.LEFT
                    addSwipe(Swipe.LEFT, fullDistanceY)
                }
                else -> {
                    lastSwipe = Swipe.UP
                    addSwipe(Swipe.UP, fullDistanceY)
                }
            }

            longHandler.postLong()
        } else if (fullDistanceY > 0
                && fullDistanceY > yThreshDown) {
            //(long) down swipe
//            Log.e("NoBar", "down")

            when {
                bar.is90Vertical -> {
                    lastSwipe = Swipe.LEFT
                    addSwipe(Swipe.LEFT, fullDistanceY)
                }
                bar.is270Vertical -> {
                    lastSwipe = Swipe.RIGHT
                    addSwipe(Swipe.RIGHT, fullDistanceY)
                }
                else -> {
                    lastSwipe = Swipe.DOWN
                    addSwipe(Swipe.DOWN, fullDistanceY)
                }
            }

            longHandler.postLong()
        }

//        Log.e("NoBar", "distanceX: $distanceX, \ndistanceY: $distanceY, \nxThresh: $xThresh, \nyThreshUp: $yThreshUp, \nyThreshDown: $yThreshDown")
    }

    private fun handleSwipe() {
        longHandler.removeCallbacksAndMessages(null)

        when {
            actionHolder.run { hasAnyOfActions(complexActionLeftUp) }
                    && (patternMatches(patternLeftUp)
                    || patternMatches(patternUpLeft)) -> {
                if (!sentLongLeft && !sentLongUp && !sentLongComplexLeftUp) {
                    sendAction(actionHolder.complexActionLeftUp)
                }
            }
            actionHolder.run { hasAnyOfActions(complexActionRightUp) }
                    && (patternMatches(patternRightUp)
                    || patternMatches(patternUpRight)) -> {
                if (!sentLongRight && !sentLongUp && !sentLongComplexRightUp) {
                    sendAction(actionHolder.complexActionRightUp)
                }
            }
            actionHolder.run { hasAnyOfActions(complexActionLeftDown) }
                    && (patternMatches(patternLeftDown)
                    || patternMatches(patternDownLeft)) -> {
                if (!sentLongLeft && !sentLongDown && !sentLongComplexLeftDown) {
                    sendAction(actionHolder.complexActionLeftDown)
                }
            }
            actionHolder.run { hasAnyOfActions(complexActionRightDown) }
                    && (patternMatches(patternRightDown)
                    || patternMatches(patternDownRight)) -> {
                if (!sentLongRight && !sentLongDown && !sentLongComplexRightDown) {
                    sendAction(actionHolder.complexActionRightDown)
                }
            }
            else -> when (swipes.lastOrNull() ?: lastSwipe) {
                Swipe.UP -> {
                    if (!sentLongUp) {
                        sendUp()
                        if (bar.isHidden) showPill()
                    }
                }
                Swipe.DOWN -> {
                    if (!sentLongDown) {
                        sendDown()
                    }
                }
                Swipe.LEFT -> {
                    if (!sentLongLeft) {
                        sendLeft()
                    }
                }
                Swipe.RIGHT -> {
                    if (!sentLongRight) {
                        sendRight()
                    }
                }
            }
        }
    }

    private fun patternMatches(pattern: Array<Swipe>): Boolean {
        return swipes.size > 1 && swipes.toTypedArray().contentEquals(pattern)
    }

    private fun setMode(rotation: Int) {
        mode = when (rotation) {
            Surface.ROTATION_0,
            Surface.ROTATION_180 -> Mode.PORTRAIT
            Surface.ROTATION_90 -> Mode.LANDSCAPE_90
            else -> Mode.LANDSCAPE_270
        }
    }

    private fun sendAction(action: String) {
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

    private fun getSection(coord: Float): Int {
        return when {
            bar.is90Vertical -> {
                val third = bar.adjustedHeight / 3f
                when {
                    coord < third -> THIRD_SECTION
                    coord <= (2f * third) -> SECOND_SECTION
                    else -> FIRST_SECTION
                }
            }

            bar.is270Vertical -> {
                val third = bar.adjustedHeight / 3f
                when {
                    coord < third -> FIRST_SECTION
                    coord <= (2f * third) -> SECOND_SECTION
                    else -> THIRD_SECTION
                }
            }

            else -> {
                val third = prefManager.customWidth / 3f
                when {
                    coord < third -> FIRST_SECTION
                    coord <= (2f * third) -> SECOND_SECTION
                    else -> THIRD_SECTION
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

    private var sentLongComplexLeftUp = false
    private var sentLongComplexRightUp = false
    private var sentLongComplexLeftDown = false
    private var sentLongComplexRightDown = false

    private fun sendUp() {
        if (!sentLongUp
                && !sentLongComplexLeftUp
                && !sentLongComplexRightUp
                && !bar.isHidden) {
//            Log.e("NoBar", "up")

            sendAction(actionHolder.actionUp)
        }
    }

    private fun sendDown() {
        if (!sentLongDown
                && !sentLongComplexLeftDown
                && !sentLongComplexRightDown
                && !bar.isHidden) {
//            Log.e("NoBar", "down")

            sendAction(actionHolder.actionDown)
        }
    }

    private fun sendLeft() {
        if (!sentLongLeft
                && !sentLongComplexLeftUp
                && !sentLongComplexLeftDown
                && !bar.isHidden) {
//            Log.e("NoBar", "left")

            sendAction(actionHolder.actionLeft)
        }
    }

    private fun sendRight() {
        if (!sentLongRight
                && !sentLongComplexRightUp
                && !sentLongComplexRightDown
                && !bar.isHidden) {
//            Log.e("NoBar", "right")

            sendAction(actionHolder.actionRight)
        }
    }

    private fun sendLongUp() {
        val upHold = actionHolder.actionUpHold

        if (!bar.isHidden
                && (!sentLongUp || prefManager.allowRepeatLong)
                && !sentLongComplexRightUp
                && !sentLongComplexLeftUp
                && actionHolder.hasSomeUpHoldAction()) {
            sentLongUp = true
//            Log.e("NoBar", "longUp")

            sendAction(upHold)
            longHandler.postRepeatLong()
        }
    }

    private fun sendLongDown() {
        val downHold = actionHolder.actionDownHold

        if (!bar.isHidden
                && (!sentLongDown || prefManager.allowRepeatLong)
                && !sentLongComplexRightDown
                && !sentLongComplexLeftDown
                && actionHolder.hasAnyOfActions(downHold)) {
            sentLongDown = true
//            Log.e("NoBar", "longDown")

            sendAction(downHold)
            longHandler.postRepeatLong()
        }
    }

    private fun sendLongLeft() {
        val leftHold = actionHolder.actionLeftHold

        if (!bar.isHidden
                && (!sentLongLeft || prefManager.allowRepeatLong)
                && !sentLongComplexLeftDown
                && !sentLongComplexLeftUp
                && actionHolder.hasAnyOfActions(leftHold)) {
            sentLongLeft = true
//            Log.e("NoBar", "longLeft")

            sendAction(leftHold)
            longHandler.postRepeatLong()
        }
    }

    private fun sendLongRight() {
        val rightHold = actionHolder.actionRightHold

        if (!bar.isHidden
                && (!sentLongRight || prefManager.allowRepeatLong)
                && !sentLongComplexRightDown
                && !sentLongComplexRightUp
                && actionHolder.hasAnyOfActions(rightHold)) {
            sentLongRight = true
//            Log.e("NoBar", "longRight")

            sendAction(rightHold)
            longHandler.postRepeatLong()
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

    private fun sendComplexLongLeftUp() {
        if ((!sentLongComplexLeftUp || prefManager.allowRepeatLong)
                && !sentLongLeft
                && !sentLongUp) {
            sentLongComplexLeftUp = true

            sendAction(actionHolder.complexActionLongLeftUp)
            longHandler.postRepeatLong()
        }
    }

    private fun sendComplexLongRightUp() {
        if ((!sentLongComplexRightUp || prefManager.allowRepeatLong)
                && !sentLongRight
                && !sentLongUp) {
            sentLongComplexRightUp = true

            sendAction(actionHolder.complexActionLongRightUp)
            longHandler.postRepeatLong()
        }
    }

    private fun sendComplexLongLeftDown() {
        if ((!sentLongComplexLeftDown || prefManager.allowRepeatLong)
                && !sentLongLeft
                && !sentLongDown) {
            sentLongComplexLeftDown = true

            sendAction(actionHolder.complexActionLongLeftDown)
            longHandler.postRepeatLong()
        }
    }

    private fun sendComplexLongRightDown() {
        if ((!sentLongComplexRightDown || prefManager.allowRepeatLong)
                && !sentLongRight
                && !sentLongDown) {
            sentLongComplexRightDown = true

            sendAction(actionHolder.complexActionLongRightDown)
            longHandler.postRepeatLong()
        }
    }

    private fun showPill() {
        bar.vibrate(prefManager.vibrationDuration.toLong())
        bar.removeHideReason(HiddenPillReasonManagerNew.MANUAL, true)
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
            if (!bar.isHidden) {
                if (isPinned) {
                    app.disabledNavReasonManager.add(DisabledReasonManager.NavBarReasons.APP_PINNED)
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
                MSG_LONG -> {
                    when {
                        actionHolder.run { hasAnyOfActions(complexActionLongLeftUp) }
                                && (patternMatches(patternLeftUp)
                                || patternMatches(patternUpLeft)) -> {
                            sendComplexLongLeftUp()
                        }
                        actionHolder.run { hasAnyOfActions(complexActionLongRightUp) }
                                && (patternMatches(patternRightUp)
                                || patternMatches(patternUpRight)) -> {
                            sendComplexLongRightUp()
                        }
                        actionHolder.run { hasAnyOfActions(complexActionLongLeftDown) }
                                && (patternMatches(patternLeftDown)
                                || patternMatches(patternDownLeft)) -> {
                            sendComplexLongLeftDown()
                        }
                        actionHolder.run { hasAnyOfActions(complexActionLongRightDown) }
                                && (patternMatches(patternRightDown)
                                || patternMatches(patternDownRight)) -> {
                            sendComplexLongRightDown()
                        }
                        else -> when (swipes.lastOrNull()) {
                            Swipe.UP -> sendLongUp()
                            Swipe.DOWN -> sendLongDown()
                            Swipe.LEFT -> sendLongLeft()
                            Swipe.RIGHT -> sendLongRight()
                        }
                    }
                }
            }
        }

        fun postRepeatLong() {
            if (prefManager.allowRepeatLong) {
                postLong()
            }
        }

        fun postLong() {
            if (!hasMessages(MSG_LONG))
                sendEmptyMessageDelayed(MSG_LONG, prefManager.holdTime.toLong())
        }
    }
}