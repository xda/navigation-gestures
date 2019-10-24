package com.xda.nobar.util.helpers

import android.content.ContextWrapper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.xda.nobar.util.actionHolder
import com.xda.nobar.util.actionManager
import com.xda.nobar.util.app
import com.xda.nobar.views.SideSwipeView
import kotlin.math.absoluteValue

class SideSwipeGestureManager(private val swipeView: SideSwipeView) : ContextWrapper(swipeView.context) {
    private val detector = GestureDetector(this, Listener())
    private val viewConfig = ViewConfiguration.get(this)!!
    private val bar = app.bar
    private val actionHandler = actionManager.actionHandler

    private var downRawX = 0f
    private var downRawY = 0f
    private var prevRawX = 0f
    private var prevRawY = 0f

    private var slop = viewConfig.scaledTouchSlop

    private var lastSwipe: Swipe? = null

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

                parseSwipe(newX, newY)

                prevRawX = newX
                prevRawY = newY

                val xSlop = (downRawX - newX).absoluteValue > slop
                val ySlop = (downRawY - newY).absoluteValue > slop

                if (!xSlop && !ySlop) return false
            }

            MotionEvent.ACTION_UP -> {
                when (lastSwipe) {
                    Swipe.UP -> {
                    }
                    Swipe.DOWN -> {
                    }
                    Swipe.LEFT -> {
                        if (swipeView.side == SideSwipeView.Side.RIGHT) {
                            actionHandler.sendActionInternal(actionHolder.sideRightIn, true)
                        }
                    }
                    Swipe.RIGHT -> {
                        if (swipeView.side == SideSwipeView.Side.LEFT) {
                            actionHandler.sendActionInternal(actionHolder.sideLeftIn, true)
                        }
                    }
                }
            }
        }

        return true
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
        }
    }

    inner class Listener : GestureDetector.SimpleOnGestureListener()

    private enum class Swipe {
        UP,
        DOWN,
        LEFT,
        RIGHT
    }
}