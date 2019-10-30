package com.xda.nobar.util.helpers

import android.annotation.SuppressLint
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.xda.nobar.util.actionHolder
import com.xda.nobar.util.actionManager
import com.xda.nobar.util.app
import com.xda.nobar.util.prefManager
import com.xda.nobar.views.SideSwipeView
import kotlin.math.absoluteValue

class SideSwipeGestureManager(private val swipeView: SideSwipeView) : ContextWrapper(swipeView.context) {
    companion object {
        private const val MSG_LONG = 100
    }

    private val detector = GestureDetector(this, Listener())
    private val viewConfig = ViewConfiguration.get(this)!!
    private val bar = app.bar
    private val actionHandler = actionManager.actionHandler
    private val longHandler = LongHandler()

    private var downRawX = 0f
    private var downRawY = 0f
    private var prevRawX = 0f
    private var prevRawY = 0f

    private var slop = viewConfig.scaledTouchSlop

    private var lastSwipe: Swipe? = null
    private var performedLong = false

    fun onTouchEvent(e: MotionEvent?): Boolean {
        return handleTouchEvent(e) or detector.onTouchEvent(e)
    }

    private fun handleTouchEvent(e: MotionEvent?): Boolean {
        when (e?.action) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = e.rawX
                downRawY = e.rawY

                prevRawX = downRawX
                prevRawY = downRawY

                slop = viewConfig.scaledTouchSlop
            }

            MotionEvent.ACTION_MOVE -> {
                val newX = e.rawX
                val newY = e.rawY

                val xSlop = (downRawX - newX).absoluteValue > slop
                val ySlop = (downRawY - newY).absoluteValue > slop

                if (!xSlop && !ySlop) return false

                parseSwipe(newX, newY)

                prevRawX = newX
                prevRawY = newY
            }

            MotionEvent.ACTION_UP -> {
                longHandler.removeCallbacksAndMessages(null)

                if (!performedLong) {
                    when (lastSwipe) {
                        Swipe.UP -> {
                        }
                        Swipe.DOWN -> {
                        }
                        Swipe.LEFT -> {
                            if (swipeView.side == SideSwipeView.Side.RIGHT) {
                                send(actionHolder.sideRightIn)
                            }
                        }
                        Swipe.RIGHT -> {
                            if (swipeView.side == SideSwipeView.Side.LEFT) {
                                send(actionHolder.sideLeftIn)
                            }
                        }
                    }
                }

                performedLong = false
            }
        }

        return true
    }

    private fun send(gesture: String) {
        actionHandler.sendActionInternal(gesture, force = true, isBar = false)
    }

    private fun parseSwipe(newX: Float, newY: Float) {
        val fullDistanceX = prevRawX - downRawX
        val fullDistanceY = prevRawY - downRawY

        val distanceX = newX - prevRawX
        val distanceY = newY - prevRawY

        if (fullDistanceX < 0
            && distanceX.absoluteValue >= distanceY.absoluteValue) {

            lastSwipe = when {
                bar.is90Vertical -> {
                    Swipe.UP
                }
                bar.is270Vertical -> {
                    Swipe.DOWN
                }
                else -> {
                    Swipe.LEFT
                }
            }
            longHandler.scheduleLong()
        } else if (fullDistanceX > 0
            && distanceX.absoluteValue >= distanceY.absoluteValue) {
            lastSwipe = when {
                bar.is90Vertical -> {
                    Swipe.DOWN
                }
                bar.is270Vertical -> {
                    Swipe.UP
                }
                else -> {
                    Swipe.RIGHT
                }
            }
            longHandler.scheduleLong()
        } else if (fullDistanceY < 0) {
            lastSwipe = when {
                bar.is90Vertical -> {
                    Swipe.RIGHT
                }
                bar.is270Vertical -> {
                    Swipe.LEFT
                }
                else -> {
                    Swipe.UP
                }
            }
            longHandler.scheduleLong()
        } else if (fullDistanceY > 0) {
            lastSwipe = when {
                bar.is90Vertical -> {
                    Swipe.LEFT
                }
                bar.is270Vertical -> {
                    Swipe.RIGHT
                }
                else -> {
                    Swipe.DOWN
                }
            }
            longHandler.scheduleLong()
        }
    }

    inner class Listener : GestureDetector.SimpleOnGestureListener()

    @SuppressLint("HandlerLeak")
    private inner class LongHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message?) {
            when (msg?.what) {
                MSG_LONG -> {
                    when (lastSwipe) {
                        Swipe.UP -> {
                        }
                        Swipe.DOWN -> {
                        }
                        Swipe.LEFT -> {
                            if (swipeView.side == SideSwipeView.Side.RIGHT && actionHolder.hasAnyOfActions(actionHolder.sideRightInLong)) {
                                performedLong = true
                                send(actionHolder.sideRightInLong)
                            }
                        }
                        Swipe.RIGHT -> {
                            if (swipeView.side == SideSwipeView.Side.LEFT && actionHolder.hasAnyOfActions(actionHolder.sideLeftInLong)) {
                                performedLong = true
                                send(actionHolder.sideLeftInLong)
                            }
                        }
                    }

                    postRepeat()
                }
            }
        }

        fun postRepeat() {
            if (prefManager.allowRepeatLong) {
                scheduleLong()
            }
        }

        fun scheduleLong() {
            if (!hasMessages(MSG_LONG)) {
                sendEmptyMessageDelayed(MSG_LONG, prefManager.holdTime.toLong())
            }
        }
    }

    private enum class Swipe {
        UP,
        DOWN,
        LEFT,
        RIGHT
    }
}