package com.xda.nobar.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.v4.content.LocalBroadcastManager
import android.view.MotionEvent
import android.view.SoundEffectConstants
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import com.xda.nobar.App
import com.xda.nobar.services.Actions
import com.xda.nobar.views.BarView
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

class GestureManager(private val barView: BarView) {
    companion object {
        private const val FIRST_SECTION = 0
        private const val SECOND_SECTION = 1
        private const val THIRD_SECTION = 2
    }

    val actionMap = HashMap<String, Int>()
    private val pill = barView.pill
    private val context = barView.context
    private val params = barView.params
    private val app = context.applicationContext as App
    private val handler = barView.handler
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val pool = Executors.newScheduledThreadPool(1)
    
    private val tapLock = Any()

    private var isSwipeUp = false
    private var isSwipeLeft = false
    private var isSwipeRight = false
    private var isOverrideTap = false
    private var wasHidden = false

    var beingTouched = false
    var isActing = false

    private var upHoldHandle: ScheduledFuture<*>? = null
    private var leftHoldHandle: ScheduledFuture<*>? = null
    private var rightHoldHandle: ScheduledFuture<*>? = null

    private var isRunningLongUp = false
    private var isRunningLongLeft = false
    private var isRunningLongRight = false

    private var oldEvent: MotionEvent? = null
    private var oldY = 0F
    private var oldX = 0F

    fun handleTouchEvent(ev: MotionEvent?): Boolean {
        val animDurScale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
        val time = (getAnimationDurationMs() * animDurScale)
        var ultimateReturn = false

        when (ev?.action) {
            MotionEvent.ACTION_DOWN -> {
                wasHidden = barView.isHidden
                app.uiHandler.onGlobalLayout()
                oldY = ev.rawY
                oldX = ev.rawX
                beingTouched = true
                barView.isCarryingOutTouchAction = true
            }

            MotionEvent.ACTION_UP -> {
                beingTouched = false

                if (wasHidden) {
                    isSwipeUp = false
                }

                if (isSwipeUp || (isRunningLongUp &&  getSectionedUpHoldAction(oldX) == app.typeNoAction)) {
                    upHoldHandle?.cancel(true)
                    upHoldHandle = null
                    sendAction(app.actionUp, ev.rawX)
                }

                if (isSwipeLeft || (isRunningLongLeft && actionMap[app.actionLeftHold] == app.typeNoAction)) {
                    leftHoldHandle?.cancel(true)
                    leftHoldHandle = null
                    sendAction(app.actionLeft, ev.rawX)
                }

                if (isSwipeRight || (isRunningLongRight && actionMap[app.actionRightHold] == app.typeNoAction)) {
                    rightHoldHandle?.cancel(true)
                    rightHoldHandle = null
                    sendAction(app.actionRight, ev.rawX)
                }

                if (pill.translationX != 0f) {
                    pill.animate()
                            .translationX(0f)
                            .setDuration(getAnimationDurationMs())
                            .withEndAction {
                                if (params.x == Utils.getHomeX(context)) {
                                    isActing = false
                                    isSwipeLeft = false
                                    isSwipeRight = false
                                }
                            }
                            .start()
                }

                when {
                    params.y > barView.getAdjustedHomeY() -> {
                        val distance = (params.y - barView.getAdjustedHomeY()).absoluteValue
                        if (barView.yDownAnimator != null) {
                            barView.yDownAnimator?.cancel()
                            barView.yDownAnimator = null
                        }
                        barView.yDownAnimator = ValueAnimator.ofInt(params.y, barView.getAdjustedHomeY())
                        barView.yDownAnimator?.interpolator = DecelerateInterpolator()
                        barView.yDownAnimator?.addUpdateListener {
                            params.y = it.animatedValue.toString().toInt()
                            barView.updateLayout(params)
                        }
                        barView.yDownAnimator?.addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator?) {
//                                            if (isSwipeUp) jiggleDown()
//
                                isActing = false
                                isSwipeUp = false
                                barView.isCarryingOutTouchAction = false

                                barView.yDownAnimator = null
                            }

                            override fun onAnimationCancel(animation: Animator?) {
                                onAnimationEnd(animation)
                            }
                        })
                        barView.yDownAnimator?.duration = (time * distance / 100f).toLong()
                        barView.yDownAnimator?.start()
                    }
                    params.x < Utils.getHomeX(context) || params.x > Utils.getHomeX(context) -> {
                        val distance = (params.x - Utils.getHomeX(context)).absoluteValue
                        val animator = ValueAnimator.ofInt(params.x, Utils.getHomeX(context))
                        animator.interpolator = DecelerateInterpolator()
                        animator.addUpdateListener {
                            params.x = it.animatedValue.toString().toInt()
                            barView.updateLayout(params)
                        }
                        animator.addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator?) {
//                                            if (isSwipeLeft && actionMap[app.actionLeft] != app.typeNoAction) jiggleRight()
//                                            if (isSwipeRight && actionMap[app.actionRight] != app.typeNoAction) jiggleLeft()

                                isActing = false
                                isSwipeLeft = false
                                isSwipeRight = false
                                barView.isCarryingOutTouchAction = false
                            }
                        })
                        animator.duration = (time * distance / 100f).toLong()
                        animator.start()
                    }
                    else -> {
//                                    if (isSwipeLeft && actionMap[app.actionLeft] != app.typeNoAction) jiggleRight()
//                                    if (isSwipeRight && actionMap[app.actionRight] != app.typeNoAction) jiggleLeft()
//                                    if (isSwipeUp) jiggleDown()

                        isActing = false
                        isSwipeUp = false
                        isSwipeLeft = false
                        isSwipeRight = false
                        barView.isCarryingOutTouchAction = false
                    }
                }

                isRunningLongRight = false
                isRunningLongLeft = false
                isRunningLongUp = false

                wasHidden = barView.isHidden
            }
            MotionEvent.ACTION_MOVE -> {
                ultimateReturn = ultimateReturn || handlePotentialSwipe(ev)

                if (isSwipeUp && !isSwipeLeft && !isSwipeRight) {
                    if (!isActing) isActing = true

                    val velocity = (oldY - ev.rawY)
                    oldY = ev.rawY

                    if (params.y < Utils.getRealScreenSize(context).y / 6 + barView.getAdjustedHomeY() && getAnimationDurationMs() > 0) {
                        params.y = params.y + (velocity / 2).toInt()
                        barView.updateLayout(params)
                    }

                    if (upHoldHandle == null) {
                        upHoldHandle = pool.schedule({
                            handler?.post {
                                isRunningLongUp = true
                                sendAction(app.actionUpHold, ev.rawX)
                                isSwipeUp = false
                                upHoldHandle = null
                            }
                        }, getHoldTime().toLong(), TimeUnit.MILLISECONDS)
                    }
                }

                if ((isSwipeLeft || isSwipeRight) && !isSwipeUp) {
                    if (!isActing) isActing = true

                    val velocity = ev.rawX - oldX
                    oldX = ev.rawX

                    val half = (Utils.getRealScreenSize(context).x.toFloat() / 2f - Utils.getCustomWidth(context).toFloat() / 2f).toInt()

                    if (getAnimationDurationMs() > 0) {
                        when {
                            params.x <= -half && !isSwipeRight -> pill.translationX -= velocity
                            params.x >= half && !isSwipeLeft -> pill.translationX += velocity
                            else -> {
                                params.x = params.x + (velocity / 2).toInt()
                                barView.updateLayout(params)
                            }
                        }
                    }

                    if (isSwipeLeft) {
                        if (leftHoldHandle == null) {
                            leftHoldHandle = pool.schedule({
                                handler?.post {
                                    isRunningLongLeft = true
                                    sendAction(app.actionLeftHold, ev.rawX)
                                    isSwipeLeft = false
                                    leftHoldHandle = null
                                }
                            }, getHoldTime().toLong(), TimeUnit.MILLISECONDS)
                        }
                    }

                    if (isSwipeRight) {
                        if (rightHoldHandle == null) {
                            rightHoldHandle = pool.schedule({
                                handler?.post {
                                    isRunningLongRight = true
                                    sendAction(app.actionRightHold, ev.rawX)
                                    isSwipeRight = false
                                    rightHoldHandle = null
                                }
                            }, getHoldTime().toLong(), TimeUnit.MILLISECONDS)
                        }
                    }
                }
            }
        }

        oldEvent = MotionEvent.obtain(ev)

        return ultimateReturn
    }

    fun onSingleTapUp(ev: MotionEvent): Boolean {
        return if (actionMap[app.actionDouble] == app.typeNoAction && !isActing && !wasHidden) {
            synchronized(tapLock) {
                isOverrideTap = true
                sendAction(app.actionTap, ev.rawX)
            }
            isActing = false
            true
        } else false
    }
    
    fun onLongPress(ev: MotionEvent) {
        if (!barView.isHidden && !isActing) {
            isActing = true
            sendAction(app.actionHold, ev.rawX)
        }
    }
    
    fun onDoubleTap(ev: MotionEvent): Boolean {
        return if (!barView.isHidden &&!isActing) {
            isActing = true
            sendAction(app.actionDouble, ev.rawX)
            true
        } else false
    }
    
    fun onSingleTapConfirmed(ev: MotionEvent): Boolean {
        synchronized(tapLock) {
            return if (!isOverrideTap && !barView.isHidden) {
                isActing = false

                sendAction(app.actionTap, ev.rawX)
                true
            } else if (barView.isHidden) {
                isOverrideTap = false
                vibrate(getVibrationDuration().toLong())
                barView.showPill(false)
                true
            } else {
                isOverrideTap = false
                false
            }
        }
    }

    private fun handlePotentialSwipe(motionEvent: MotionEvent?): Boolean {
        if (oldEvent == null || motionEvent == null) return false

        val oldEvent = MotionEvent.obtain(this.oldEvent)
        val distanceX = motionEvent.rawX - oldEvent.rawX
        val distanceY = motionEvent.rawY - oldEvent.rawY
        val xThresh = Utils.dpAsPx(context, 4)
        val yThresh = Utils.dpAsPx(context, 2)

        val ret = if (!barView.isHidden && !isActing) {
            when {
                distanceX < -xThresh && distanceY.absoluteValue <= distanceX.absoluteValue -> { //left swipe
                    isSwipeLeft = true
                    true
                }
                distanceX > xThresh && distanceY.absoluteValue <= distanceX.absoluteValue -> { //right swipe
                    isSwipeRight = true
                    true
                }
                distanceY > yThresh && distanceY.absoluteValue > distanceX.absoluteValue -> { //down swipe
                    isActing = true
                    sendAction(app.actionDown, oldEvent.rawX)
                    true
                }
                distanceY < -yThresh && distanceY.absoluteValue > distanceX.absoluteValue -> { //up swipe and up hold-swipe
                    isSwipeUp = true
                    true
                }
                else -> false
            }
        } else if (barView.isHidden
                && !isActing
                && distanceY < -yThresh
                && distanceY.absoluteValue > distanceX.absoluteValue) { //up swipe
            barView.showPill(false)
            true
        } else false

        oldEvent.recycle()

        return ret
    }

    private fun getSectionedUpHoldAction(x: Float): Int? {
        return if (!Utils.sectionedPill(context)) actionMap[app.actionUpHold]
        else when (getSection(x)) {
            FIRST_SECTION -> actionMap[app.actionUpHoldLeft]
            SECOND_SECTION -> actionMap[app.actionUpHoldCenter]
            else -> actionMap[app.actionUpHoldRight]
        }
    }

    private fun String.isEligible() = arrayListOf(
            app.actionUp,
            app.actionUpHold
    ).contains(this) && Utils.sectionedPill(context)

    private fun getSection(x: Float): Int {
        val third = params.width / 3f

        return when {
            x < third -> FIRST_SECTION
            x <= (2f * third) -> SECOND_SECTION
            else -> SECOND_SECTION
        }
    }

    private fun sendAction(action: String, lastX: Float) {
        if (action.isEligible()) {
            when(getSection(lastX)) {
                FIRST_SECTION -> sendAction("${action}_left")
                SECOND_SECTION -> sendAction("${action}_center")
                THIRD_SECTION -> sendAction("${action}_right")
            }
        } else {
            sendAction(action)
        }
    }

    /**
     * Parse the action index and broadcast to {@link com.xda.nobar.services.Actions}
     * @param key one of app.action*
     */
    private fun sendAction(key: String) {
        val which = actionMap[key] ?: return

        if (which == app.typeNoAction) return

        vibrate(getVibrationDuration().toLong())

        if (key == app.actionDouble) handler?.postDelayed({ vibrate(getVibrationDuration().toLong()) }, getVibrationDuration().toLong())

        if (which == app.typeHide) {
            if (key == app.actionUp || key == app.actionUpHold) {
                barView.yDownAnimator?.cancel()
                barView.yDownAnimator = null
            }
            barView.hidePill(false)
            return
        }

        when (key) {
            app.actionDouble -> jiggleDoubleTap()
            app.actionHold -> jiggleHold()
//            app.actionDown -> jiggleDown()
            app.actionTap -> jiggleTap()
            app.actionUpHold -> jiggleHoldUp()
            app.actionLeftHold -> jiggleLeftHold()
            app.actionRightHold -> jiggleRightHold()
        }

        if (key == app.actionUp || key == app.actionLeft || key == app.actionRight) {
            barView.animate(null, BarView.ALPHA_ACTIVE)
        }

        val intent = Intent(Actions.ACTION)
        intent.putExtra(Actions.EXTRA_ACTION, which)
        intent.putExtra(Actions.EXTRA_GESTURE, key)

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)

        if (Utils.shouldUseRootCommands(context)) {
            app.rootBinder?.handle(which)
        }
    }

    /**
     * Get the user-defined or default time the user must hold a swipe to perform the swipe and hold action
     * @return the time, in ms
     */
    private fun getHoldTime(): Int {
        return prefs.getInt("hold_time", 1000)
    }

    /**
     * Get the user-defined or default duration of the feedback vibration
     * @return the duration, in ms
     */
    private fun getVibrationDuration(): Int {
        return prefs.getInt("vibration_duration", BarView.VIB_SHORT.toInt())
    }

    /**
     * Get the user-defined or default duration of the pill animations
     * @return the duration, in ms
     */
    private fun getAnimationDurationMs(): Long {
        return prefs.getInt("anim_duration", BarView.DEFAULT_ANIM_DURATION.toInt()).toLong()
    }

    /**
     * The animation for a single tap on the pill
     */
    private fun jiggleTap() {
        barView.animate()
                .scaleX(BarView.SCALE_MID)
                .setInterpolator(BarView.ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    barView.animate()
                            .scaleX(BarView.SCALE_NORMAL)
                            .setInterpolator(BarView.EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .start()
                    animateActiveLayer(BarView.BRIGHTEN_INACTIVE)
                }
                .start()
        animateActiveLayer(BarView.BRIGHTEN_ACTIVE)
    }

    /**
     * The animation for a left-swipe on the pill
     */
    private fun jiggleLeft() {
        barView.animate()
                .scaleX(BarView.SCALE_MID)
//                .alpha(ALPHA_ACTIVE)
                .x(-barView.width * (1 - BarView.SCALE_MID) / 2)
                .setInterpolator(BarView.ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    barView.animate()
                            .scaleX(BarView.SCALE_NORMAL)
                            .x(0f)
//                            .alpha(ALPHA_INACTIVE)
                            .setInterpolator(BarView.EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .start()
                    animateActiveLayer(BarView.BRIGHTEN_INACTIVE)
                }
                .start()
        animateActiveLayer(BarView.BRIGHTEN_ACTIVE)
    }

    /**
     * The animation for a swipe-left and hold on the pill
     */
    private fun jiggleLeftHold() {
        barView.animate()
                .scaleX(BarView.SCALE_SMALL)
//                .alpha(ALPHA_ACTIVE)
                .x(-barView.width * (1 - BarView.SCALE_SMALL) / 2)
                .setInterpolator(BarView.ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    barView.animate()
                            .scaleX(BarView.SCALE_NORMAL)
                            .x(0f)
//                            .alpha(ALPHA_INACTIVE)
                            .setInterpolator(BarView.EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .start()
                    animateActiveLayer(BarView.BRIGHTEN_INACTIVE)
                }
        animateActiveLayer(BarView.BRIGHTEN_ACTIVE)
    }

    /**
     * The animation for a right-swipe on the pill
     */
    private fun jiggleRight() {
        barView.animate()
                .scaleX(BarView.SCALE_MID)
//                .alpha(ALPHA_ACTIVE)
                .x(barView.width * (1 - BarView.SCALE_MID) / 2)
                .setInterpolator(BarView.ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    barView.animate()
                            .scaleX(BarView.SCALE_NORMAL)
                            .x(0f)
//                            .alpha(ALPHA_INACTIVE)
                            .setInterpolator(BarView.EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .start()
                    animateActiveLayer(BarView.BRIGHTEN_INACTIVE)
                }
                .start()
        animateActiveLayer(BarView.BRIGHTEN_ACTIVE)
    }

    /**
     * The animation for a swipe-right and hold on the pill
     */
    private fun jiggleRightHold() {
        barView.animate()
                .scaleX(BarView.SCALE_SMALL)
//                .alpha(ALPHA_ACTIVE)
                .x(barView.width * (1 - BarView.SCALE_SMALL) / 2)
                .setInterpolator(BarView.ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    barView.animate()
                            .scaleX(BarView.SCALE_NORMAL)
                            .x(0f)
//                            .alpha(ALPHA_INACTIVE)
                            .setInterpolator(BarView.EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .start()
                    animateActiveLayer(BarView.BRIGHTEN_INACTIVE)
                }
        animateActiveLayer(BarView.BRIGHTEN_ACTIVE)
    }

    /**
     * The animation for a long-press on the pill
     */
    private fun jiggleHold() {
        barView.animate()
                .scaleX(BarView.SCALE_SMALL)
//                .alpha(ALPHA_ACTIVE)
                .setInterpolator(BarView.ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    barView.animate()
                            .scaleX(BarView.SCALE_NORMAL)
//                            .alpha(ALPHA_INACTIVE)
                            .setInterpolator(BarView.EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .start()
                    animateActiveLayer(BarView.BRIGHTEN_INACTIVE)
                }
                .start()
        animateActiveLayer(BarView.BRIGHTEN_ACTIVE)
    }

    /**
     * The animation for an up-swipe on the pill
     */
    private fun jiggleUp() {
        barView.animate()
                .scaleY(BarView.SCALE_MID)
                .y(-barView.height * (1 - BarView.SCALE_MID) / 2)
//                .alpha(ALPHA_ACTIVE)
                .setInterpolator(BarView.ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    barView.animate()
                            .scaleY(BarView.SCALE_NORMAL)
                            .y(0f)
//                            .alpha(ALPHA_INACTIVE)
                            .setInterpolator(BarView.EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .start()
                    animateActiveLayer(BarView.BRIGHTEN_INACTIVE)
                }
                .start()
        animateActiveLayer(BarView.BRIGHTEN_ACTIVE)
    }

    /**
     * The animation for an up-swipe and hold on the pill
     */
    private fun jiggleHoldUp() {
        barView.animate()
                .scaleY(BarView.SCALE_SMALL)
                .y(-barView.height * (1 - BarView.SCALE_SMALL) / 2)
//                .alpha(ALPHA_ACTIVE)
                .setInterpolator(BarView.ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    barView.animate()
                            .scaleY(BarView.SCALE_NORMAL)
                            .y(0f)
//                            .alpha(ALPHA_INACTIVE)
                            .setInterpolator(BarView.EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .start()
                    animateActiveLayer(BarView.BRIGHTEN_INACTIVE)
                }
                .start()
        animateActiveLayer(BarView.BRIGHTEN_ACTIVE)
    }

    /**
     * The animation for a down-swipe on the pill
     */
    private fun jiggleDown() {
        barView.animate()
                .scaleY(BarView.SCALE_MID)
                .y(barView.height * (1 - BarView.SCALE_MID) / 2)
//                .alpha(ALPHA_ACTIVE)
                .setInterpolator(BarView.ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    barView.animate()
                            .scaleY(BarView.SCALE_NORMAL)
                            .y(0f)
//                            .alpha(ALPHA_INACTIVE)
                            .setInterpolator(BarView.EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .start()
                    animateActiveLayer(BarView.BRIGHTEN_INACTIVE)
                }
                .start()
        animateActiveLayer(BarView.BRIGHTEN_ACTIVE)
    }

    /**
     * The animation for a double-tap on the pill
     */
    private fun jiggleDoubleTap() {
        barView.animate()
                .scaleX(BarView.SCALE_MID)
//                .alpha(ALPHA_ACTIVE)
                .setInterpolator(AccelerateInterpolator())
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    barView.animate()
                            .scaleX(BarView.SCALE_SMALL)
                            .setInterpolator(BarView.ENTER_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .withEndAction {
                                barView.animate()
                                        .scaleX(BarView.SCALE_NORMAL)
//                                        .alpha(ALPHA_INACTIVE)
                                        .setInterpolator(BarView.EXIT_INTERPOLATOR)
                                        .setDuration(getAnimationDurationMs())
                                        .start()
                                animateActiveLayer(BarView.BRIGHTEN_INACTIVE)
                            }
                            .start()
                }
                .start()
        animateActiveLayer(BarView.BRIGHTEN_ACTIVE)
    }

    /**
     * Load the user's custom gesture/action pairings; default values if a pairing doesn't exist
     */
    fun loadActionMap() {
        Utils.getActionsList(context, actionMap)
    }

    /**
     * This is called twice to "flash" the pill when an action is performed
     */
    fun animateActiveLayer(alpha: Float) {
        handler?.post {
            barView.pillFlash.apply {
                val alphaRatio = Color.alpha(Utils.getPillBGColor(context)).toFloat() / 255f
                animate()
                        .setDuration(getAnimationDurationMs())
                        .alpha(alpha * alphaRatio)
                        .start()
            }
        }
    }

    /**
     * Vibrate for the specified duration
     * @param duration the desired duration
     */
    fun vibrate(duration: Long) {
        if (duration > 0) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(duration)
            }
        }

        if (barView.isSoundEffectsEnabled) {
            barView.playSoundEffect(SoundEffectConstants.CLICK)
        }
    }
}