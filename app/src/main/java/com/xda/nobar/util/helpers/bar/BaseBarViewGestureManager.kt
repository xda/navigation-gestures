package com.xda.nobar.util.helpers.bar

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.GestureDetector
import android.view.MotionEvent
import com.xda.nobar.util.actionHolder
import com.xda.nobar.util.app
import com.xda.nobar.util.prefManager
import com.xda.nobar.views.BarView
import java.util.HashMap

abstract class BaseBarViewGestureManager(internal val bar: BarView) {
    companion object {
        internal const val FIRST_SECTION = 0
        internal const val SECOND_SECTION = 1
        internal const val THIRD_SECTION = 2

        internal const val MSG_UP_HOLD = 0
        internal const val MSG_LEFT_HOLD = 1
        internal const val MSG_RIGHT_HOLD = 2
        internal const val MSG_DOWN_HOLD = 3
    }

    class Singleton private constructor(private val bar: BarView) {
        companion object {
            @SuppressLint("StaticFieldLeak")
            private var instance: Singleton? = null

            fun getInstance(bar: BarView): Singleton {
                if (instance == null) instance = Singleton(bar)

                return instance!!
            }
        }

        val actionMap = HashMap<String, Int>()
        val context = bar.context
        val actionHandler = BarViewActionHandler(bar)
        val gestureThread = HandlerThread("NoBar-Gesture").apply { start() }

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
    }
    internal abstract val adjCoord: Float
    internal abstract val gestureHandler: Handler

    internal val context = bar.context

    internal var isSwipeUp = false
    internal var isSwipeLeft = false
    internal var isSwipeRight = false
    internal var isSwipeDown = false
    internal var isOverrideTap = false
    internal var wasHidden = false
    internal var lastTouchTime = -1L

    internal var isActing = false

    internal var isRunningLongUp = false
    internal var isRunningLongLeft = false
    internal var isRunningLongRight = false
    internal var isRunningLongDown = false

    internal var sentLongUp = false
    internal var sentLongLeft = false
    internal var sentLongRight = false
    internal var sentLongDown = false

    internal var oldY = 0F
    internal var oldX = 0F

    internal var origX = 0F
    internal var origY = 0F

    internal var origAdjX = 0F
    internal var origAdjY = 0F

    val singleton = Singleton.getInstance(bar)
    val actionMap = singleton.actionMap

    internal val detector = BaseDetector()
    internal val manager by lazy { GestureDetector(bar.context, detector) }
    internal val actionHandler = singleton.actionHandler
    internal val gestureThread = singleton.gestureThread

    fun onTouchEvent(ev: MotionEvent?): Boolean {
        return handleTouchEvent(ev) || manager.onTouchEvent(ev)
    }

    internal abstract fun getSection(coord: Float): Int
    internal abstract fun handleTouchEvent(ev: MotionEvent?): Boolean

    internal fun getSectionedUpHoldAction(coord: Float): Int? {
        return if (!context.app.prefManager.sectionedPill) actionMap[bar.actionHolder.actionUpHold]
        else when (getSection(coord)) {
            FIRST_SECTION -> actionMap[bar.actionHolder.actionUpHoldLeft]
            SECOND_SECTION -> actionMap[bar.actionHolder.actionUpHoldCenter]
            else -> actionMap[bar.actionHolder.actionUpHoldRight]
        }
    }

    internal fun sendAction(action: String) {
        if (action.isEligible()) {
            when (getSection(adjCoord)) {
                FIRST_SECTION -> actionHandler.sendActionInternal("${action}_left", actionMap)
                SECOND_SECTION -> actionHandler.sendActionInternal("${action}_center", actionMap)
                THIRD_SECTION -> actionHandler.sendActionInternal("${action}_right", actionMap)
            }
        } else {
            actionHandler.sendActionInternal(action, actionMap)
        }
    }

    internal fun String.isEligible() = arrayListOf(
            bar.actionHolder.actionUp,
            bar.actionHolder.actionUpHold
    ).contains(this) && context.app.prefManager.sectionedPill

    internal open inner class BaseDetector : GestureDetector.SimpleOnGestureListener() {
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
                bar.vibrate(context.prefManager.vibrationDuration.toLong())
                bar.showPill(null, true)
                true
            } else {
                isOverrideTap = false
                false
            }
        }
    }
}