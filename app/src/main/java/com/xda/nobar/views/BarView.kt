package com.xda.nobar.views

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.v4.content.LocalBroadcastManager
import android.util.AttributeSet
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.Toast
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.services.Actions
import com.xda.nobar.util.Utils
import com.xda.nobar.util.Utils.getCustomHeight
import com.xda.nobar.util.Utils.getCustomWidth
import com.xda.nobar.util.Utils.getHomeY
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

/**
 * The Pill™©® (not really copyrighted)
 */
class BarView : LinearLayout, SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        const val ALPHA_INACTIVE = 0.7f
        const val ALPHA_ACTIVE = 1.0f
        const val ALPHA_GONE = 0.0f

        const val ANIM_DURATION = 500L

        const val SCALE_NORMAL = 1.0f
        const val SCALE_MID = 0.7f
        const val SCALE_SMALL = 0.3f

        const val VIB_SHORT = 50L

        val ENTER_INTERPOLATOR = DecelerateInterpolator()
        val EXIT_INTERPOLATOR = AccelerateInterpolator()
    }

    private val gestureDetector = GestureManager()
    private val wm: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val actionMap = HashMap<String, Int>()

    var isHidden = false
    var beingTouched = false
    var isTransparent = false

    private lateinit var view: View
    private lateinit var pill: LinearLayout

    private val app = context.applicationContext as App
    private val screenRotationListener = ScreenRotationListener()

    private var reenterTransparencyHandle: ScheduledFuture<*>? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet)
    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr)
    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attributeSet, defStyleAttr, defStyleRes)

    init {
        alpha = ALPHA_GONE

        loadActionMap()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        prefs.registerOnSharedPreferenceChangeListener(this)
        screenRotationListener.enable()

        val rot = wm.defaultDisplay.rotation

        if (rot == Surface.ROTATION_90 || rot == Surface.ROTATION_270) enterTransparencyMode()

        view = View.inflate(context, R.layout.pill, this)
        pill = view.findViewById(R.id.pill)

        val layers = pill.background as LayerDrawable
        layers.findDrawableByLayerId(R.id.background).setTint(Utils.getPillBGColor(context))
        (layers.findDrawableByLayerId(R.id.foreground) as GradientDrawable).setStroke(Utils.dpAsPx(context, 1), Utils.getPillFGColor(context))

        pill.elevation = Utils.dpAsPx(context, if (Utils.shouldShowShadow(context)) 2 else 0).toFloat()

        layoutParams.width = getCustomWidth(context)
        layoutParams.height = getCustomHeight(context)
        layoutParams = layoutParams

        isSoundEffectsEnabled = Utils.feedbackSound(context)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return gestureDetector.gestureDetector.onTouchEvent(event)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (actionMap.keys.contains(key)) {
            loadActionMap()
        }
        if (key == "custom_width") {
            layoutParams.width = getCustomWidth(context)
            layoutParams = layoutParams
            wm.updateViewLayout(this, layoutParams as WindowManager.LayoutParams)
        }
        if (key == "custom_height") {
            layoutParams.height = getCustomHeight(context)
            layoutParams = layoutParams
            wm.updateViewLayout(this, layoutParams as WindowManager.LayoutParams)
        }
        if (key == "custom_y") {
            val params = layoutParams as WindowManager.LayoutParams
            params.y = getHomeY(context)
            wm.updateViewLayout(this, params)
        }
        if (key == "pill_bg") {
            val layers = pill.background as LayerDrawable

            layers.findDrawableByLayerId(R.id.background).setTint(Utils.getPillBGColor(context))
        }
        if (key == "pill_fg") {
            val layers = pill.background as LayerDrawable

            (layers.findDrawableByLayerId(R.id.foreground) as GradientDrawable).setStroke(Utils.dpAsPx(context, 1), Utils.getPillFGColor(context))
        }
        if (key == "show_shadow") {
            pill.elevation = Utils.dpAsPx(context, if (Utils.shouldShowShadow(context)) 2 else 0).toFloat()
        }
        if (key == "static_pill") {
            val params = layoutParams as WindowManager.LayoutParams

            if (Utils.dontMoveForKeyboard(context)) {
                params.flags = params.flags or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN and
                        WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM.inv()
            } else {
                params.flags = params.flags or
                        WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM and
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN.inv()
            }

            wm.updateViewLayout(this, params)
        }
        if (key == "audio_feedback") {
            isSoundEffectsEnabled = Utils.feedbackSound(context)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        screenRotationListener.disable()
    }

    /**
     * Animate the pill to invisibility
     * Used during deactivation
     * @param listener optional animation listener
     */
    fun hide(listener: Animator.AnimatorListener?) {
        animate(listener, ALPHA_GONE)
    }

    /**
     * Animate the pill to full visibility
     * Used during activation
     * @param listener optional animation listener
     */
    fun show(listener: Animator.AnimatorListener?) {
        animate(listener, ALPHA_INACTIVE)
    }

    /**
     * Animate to a chosen alpha
     * @param listener optional animation listener
     * @param alpha desired alpha level
     */
    fun animate(listener: Animator.AnimatorListener?, alpha: Float) {
        animate().alpha(alpha).setDuration(getAnimationDurationMs()).setListener(object : Animator.AnimatorListener {
            override fun onAnimationCancel(animation: Animator?) {
                listener?.onAnimationCancel(animation)
            }

            override fun onAnimationEnd(animation: Animator?) {
                listener?.onAnimationEnd(animation)
            }

            override fun onAnimationRepeat(animation: Animator?) {
                listener?.onAnimationRepeat(animation)
            }

            override fun onAnimationStart(animation: Animator?) {
                listener?.onAnimationStart(animation)
            }
        })
        .withEndAction {
            this@BarView.alpha = alpha
        }
        .start()
    }

    /**
     * "Hide" the pill by moving it partially offscreen
     */
    fun hidePill() {
        try {
            if (!isHidden) {
                jiggleDown()
                val params = layoutParams as WindowManager.LayoutParams
                params.flags = params.flags or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

                Thread({
                    for (i in params.y downTo -(Utils.getCustomHeight(context) / 2)) {
                        handler?.post {
                            params.y = i
                            wm.updateViewLayout(this, params)
                        }

                        Thread.sleep(3L)
                    }
                    isHidden = true
                }).start()

                showHiddenToast()
            }
        } catch (e: IllegalArgumentException) {}
    }

    /**
     * "Show" the pill by moving it back to its normal position
     */
    fun showPill() {
        try {
            if (isHidden) {
                jiggleUp()
                val params = layoutParams as WindowManager.LayoutParams

                Thread({
                    for (i in params.y..getHomeY(context)) {
                        handler?.post {
                            params.y = i
                            wm.updateViewLayout(this, params)
                        }

                        Thread.sleep(3L)
                    }
                    isHidden = false

                    params.flags = params.flags and
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS.inv()

                    handler?.post {
                        wm.updateViewLayout(this, params)
                    }
                }).start()
            }
        } catch (e: IllegalArgumentException) {}
    }

    fun enterTransparencyMode() {
        if (!isTransparent) {
            isTransparent = true

            animate()
                    .alpha(0f)
                    .setDuration(getAnimationDurationMs())
                    .start()
        }
    }

    fun exitTransparencyMode() {
        if (isTransparent) {
            isTransparent = false

            animate()
                    .alpha(1f)
                    .setDuration(getAnimationDurationMs())
                    .start()
        }
    }

    /**
     * Load the user's custom gesture/action pairings; default values if a pairing doesn't exist
     */
    private fun loadActionMap() {
        Utils.getActionList(context, actionMap)
    }

    /**
     * Parse the action index and broadcast to {@link com.xda.nobar.services.Actions}
     * @param key one of ACTION_*
     */
    private fun sendAction(key: String) {
        val which = actionMap[key]

        if (which == app.typeNoAction) return

        vibrate(getVibrationDuration().toLong())

        if (key == app.actionDouble) handler?.postDelayed({ vibrate(getVibrationDuration().toLong()) }, getVibrationDuration().toLong())

        if (which == app.typeHide) {
            hidePill()
            return
        }

        when (key) {
            app.actionDouble -> jiggleDoubleTap()
            app.actionHold -> jiggleHold()
            app.actionDown -> jiggleDown()
            app.actionTap -> jiggleTap()
            app.actionUpHold -> jiggleHoldUp()
            app.actionLeftHold -> jiggleLeftHold()
            app.actionRightHold -> jiggleRightHold()
        }

        if (key == app.actionUp || key == app.actionLeft || key == app.actionRight) {
            animate(null, ALPHA_ACTIVE)
        }

        val intent = Intent(Actions.ACTION)
        intent.putExtra(Actions.EXTRA_ACTION, which)

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    /**
     * Vibrate for the specified duration
     * @param duration the desired duration
     */
    private fun vibrate(duration: Long) {
        if (duration > 0) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(duration)
            }
        }

        if (isSoundEffectsEnabled) {
            playSoundEffect(SoundEffectConstants.CLICK)
        }
    }

    /**
     * The animation for a single tap on the pill
     */
    private fun jiggleTap() {
        animate()
                .scaleX(SCALE_MID)
                .alpha(ALPHA_ACTIVE)
                .setInterpolator(ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    animate()
                        .scaleX(SCALE_NORMAL)
                        .alpha(ALPHA_INACTIVE)
                        .setInterpolator(EXIT_INTERPOLATOR)
                        .setDuration(getAnimationDurationMs())
                        .start()
                }
                .start()
    }

    /**
     * The animation for a left-swipe on the pill
     */
    private fun jiggleLeft() {
        animate()
                .scaleX(SCALE_MID)
                .alpha(ALPHA_ACTIVE)
                .x(-width * (1 - SCALE_MID) / 2)
                .setInterpolator(ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    animate()
                            .scaleX(SCALE_NORMAL)
                            .x(0f)
                            .alpha(ALPHA_INACTIVE)
                            .setInterpolator(EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .start()
                }
                .start()
    }

    private fun jiggleLeftHold() {
        animate()
                .scaleX(SCALE_SMALL)
                .alpha(ALPHA_ACTIVE)
                .x(-width * (1 - SCALE_SMALL) / 2)
                .setInterpolator(ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    animate()
                            .scaleX(SCALE_NORMAL)
                            .x(0f)
                            .alpha(ALPHA_INACTIVE)
                            .setInterpolator(EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .start()
                }
    }

    /**
     * The animation for a right-swipe on the pill
     */
    private fun jiggleRight() {
        animate()
                .scaleX(SCALE_MID)
                .alpha(ALPHA_ACTIVE)
                .x(width * (1 - SCALE_MID) / 2)
                .setInterpolator(ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    animate()
                            .scaleX(SCALE_NORMAL)
                            .x(0f)
                            .alpha(ALPHA_INACTIVE)
                            .setInterpolator(EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .start()
                }
                .start()
    }

    private fun jiggleRightHold() {
        animate()
                .scaleX(SCALE_SMALL)
                .alpha(ALPHA_ACTIVE)
                .x(width * (1 - SCALE_SMALL) / 2)
                .setInterpolator(ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    animate()
                            .scaleX(SCALE_NORMAL)
                            .x(0f)
                            .alpha(ALPHA_INACTIVE)
                            .setInterpolator(EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .start()
                }
    }

    /**
     * The animation for a long-press on the pill
     */
    private fun jiggleHold() {
        animate()
                .scaleX(SCALE_SMALL)
                .alpha(ALPHA_ACTIVE)
                .setInterpolator(ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    animate()
                            .scaleX(SCALE_NORMAL)
                            .alpha(ALPHA_INACTIVE)
                            .setInterpolator(EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .start()
                }
                .start()
    }

    /**
     * The animation for an up-swipe on the pill
     */
    private fun jiggleUp() {
        animate()
                .scaleY(SCALE_MID)
                .y(-height * (1 - SCALE_MID) / 2)
                .alpha(ALPHA_ACTIVE)
                .setInterpolator(ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    animate()
                            .scaleY(SCALE_NORMAL)
                            .y(0f)
                            .alpha(ALPHA_INACTIVE)
                            .setInterpolator(EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .start()
                }
                .start()
    }

    /**
     * The animation for an up-swipe and hold on the pill
     */
    private fun jiggleHoldUp() {
        animate()
                .scaleY(SCALE_SMALL)
                .y(-height * (1 - SCALE_SMALL) / 2)
                .alpha(ALPHA_ACTIVE)
                .setInterpolator(ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    animate()
                            .scaleY(SCALE_NORMAL)
                            .y(0f)
                            .alpha(ALPHA_INACTIVE)
                            .setInterpolator(EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .start()
                }
                .start()
    }

    /**
     * The animation for a down-swipe on the pill
     */
    private fun jiggleDown() {
        animate()
                .scaleY(SCALE_MID)
                .y(height * (1 - SCALE_MID) / 2)
                .alpha(ALPHA_ACTIVE)
                .setInterpolator(ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    animate()
                            .scaleY(SCALE_NORMAL)
                            .y(0f)
                            .alpha(ALPHA_INACTIVE)
                            .setInterpolator(EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .start()
                }
                .start()
    }

    /**
     * The animation for a double-tap on the pill
     */
    private fun jiggleDoubleTap() {
        animate()
                .scaleX(SCALE_MID)
                .alpha(ALPHA_ACTIVE)
                .setInterpolator(AccelerateInterpolator())
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    animate()
                            .scaleX(SCALE_SMALL)
                            .setInterpolator(ENTER_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .withEndAction {
                                animate()
                                        .scaleX(SCALE_NORMAL)
                                        .alpha(ALPHA_INACTIVE)
                                        .setInterpolator(EXIT_INTERPOLATOR)
                                        .setDuration(getAnimationDurationMs())
                                        .start()
                            }
                            .start()
                }
                .start()
    }

    private fun getHoldTime(): Int {
        return prefs.getInt("hold_time", 1000)
    }

    private fun getVibrationDuration(): Int {
        return prefs.getInt("vibration_duration", VIB_SHORT.toInt())
    }

    private fun getAnimationDurationMs(): Long {
        return prefs.getInt("anim_duration", ANIM_DURATION.toInt()).toLong()
    }

    private fun showHiddenToast() {
        if (prefs.getBoolean("show_hidden_toast", true)) {
            Toast.makeText(context, resources.getString(R.string.pill_hidden), Toast.LENGTH_LONG).show()
            prefs.edit().putBoolean("show_hidden_toast", false).apply()
        }
    }

    private fun getHomeX(): Int {
        return 0
    }

    /**
     * Manage all the gestures on the pill
     */
    inner class GestureManager {
        val gestureDetector = GD()

        private var isActing = false

        private var isSwipeUp = false
        private var isSwipeLeft = false
        private var isSwipeRight = false
        private var isOverrideTap = false

        private var upHoldHandle: ScheduledFuture<*>? = null
        private var leftHoldHandle: ScheduledFuture<*>? = null
        private var rightHoldHandle: ScheduledFuture<*>? = null

        private var oldY = 0F
        private var oldX = 0F

        inner class GD : GestureDetector(context, GestureListener()) {
            private fun scrollDown() {
                val params = layoutParams as WindowManager.LayoutParams

                if (params.y > getHomeY(context)) {
                    params.y = params.y - 1
                    handler?.post {
                        wm.updateViewLayout(this@BarView, params)
                    }
                }
            }

            private fun scrollLeft() {
                val params = layoutParams as WindowManager.LayoutParams

                if (params.x > getHomeX()) {
                    params.x = params.x - 1
                    handler?.post {
                        wm.updateViewLayout(this@BarView, params)
                    }
                }
            }

            private fun scrollRight() {
                val params = layoutParams as WindowManager.LayoutParams

                if (params.x < getHomeX()) {
                    params.x = params.x + 1
                    handler?.post {
                        wm.updateViewLayout(this@BarView, params)
                    }
                }
            }

            override fun onTouchEvent(ev: MotionEvent?): Boolean {
                return if (!isTransparent) {
                    if (reenterTransparencyHandle != null) {
                        reenterTransparencyHandle?.cancel(true)
                        reenterTransparencyHandle = null
                    }

                    val params = layoutParams as WindowManager.LayoutParams
                    val animDurScale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
                    val time = (getAnimationDurationMs() * animDurScale)

                    when (ev?.action) {
                        MotionEvent.ACTION_DOWN -> {
                            oldY = ev.rawY
                            oldX = ev.rawX
                            beingTouched = true
                        }

                        MotionEvent.ACTION_UP -> {
                            beingTouched = false

                            if (isSwipeUp) {
                                upHoldHandle?.cancel(true)
                                upHoldHandle = null
                                sendAction(app.actionUp)
                            }

                            if (isSwipeLeft) {
                                leftHoldHandle?.cancel(true)
                                leftHoldHandle = null
                                sendAction(app.actionLeft)
                            }

                            if (isSwipeRight) {
                                rightHoldHandle?.cancel(true)
                                rightHoldHandle = null
                                sendAction(app.actionRight)
                            }

                            when {
                                params.y > getHomeY(context) -> {
                                    val distance = (params.y - getHomeY(context)).toFloat()
                                    val sleepTime = time / distance * 1000F

                                    //TODO: this needs a proper fix
                                    try {
                                        val scheduler = Executors.newScheduledThreadPool(1)
                                        val handle = scheduler.scheduleAtFixedRate({ scrollDown() },
                                                0, sleepTime.toLong(),
                                                TimeUnit.MICROSECONDS)
                                        scheduler.schedule({
                                            handle.cancel(false)
                                            handler?.post {
                                                if (isSwipeUp) jiggleDown()

                                                isActing = false
                                                isSwipeUp = false
                                            }
                                        }, time.toLong(), TimeUnit.MILLISECONDS)
                                    } catch (e: IllegalArgumentException) {
                                        params.y = getHomeY(context)
                                        wm.updateViewLayout(this@BarView, params)
                                        isActing = false
                                        isSwipeUp = false
                                    }
                                }
                                params.x < getHomeX() -> {
                                    val distance = (params.x - getHomeX()).absoluteValue
                                    val sleepTime = time / distance * 1000F

                                    try {
                                        val scheduler = Executors.newScheduledThreadPool(1)
                                        val handle = scheduler.scheduleAtFixedRate({ scrollRight() },
                                                0, sleepTime.toLong(),
                                                TimeUnit.MICROSECONDS)
                                        scheduler.schedule({
                                            handle.cancel(false)
                                            handler?.post {
                                                if (isSwipeLeft && actionMap[app.actionLeft] != app.typeNoAction) jiggleRight()

                                                isActing = false
                                                isSwipeLeft = false
                                            }
                                        }, time.toLong(), TimeUnit.MILLISECONDS)
                                    } catch (e: IllegalArgumentException) {
                                        params.x = Utils.getRealScreenSize(context).x / 2
                                        wm.updateViewLayout(this@BarView, params)
                                        isActing = false
                                        isSwipeLeft = false
                                    }
                                }
                                params.x > getHomeX() -> {
                                    val distance = (params.x - getHomeX()).absoluteValue
                                    val sleepTime = time / distance * 1000F

                                    try {
                                        val scheduler = Executors.newScheduledThreadPool(1)
                                        val handle = scheduler.scheduleAtFixedRate({ scrollLeft() },
                                                0, sleepTime.toLong(),
                                                TimeUnit.MICROSECONDS)
                                        scheduler.schedule({
                                            handle.cancel(false)
                                            handler?.post {
                                                if (isSwipeRight && actionMap[app.actionRight] != app.typeNoAction) jiggleLeft()

                                                isActing = false
                                                isSwipeRight = false
                                            }
                                        }, time.toLong(), TimeUnit.MILLISECONDS)
                                    } catch (e: IllegalArgumentException) {
                                        params.x = Utils.getRealScreenSize(context).x / 2
                                        wm.updateViewLayout(this@BarView, params)
                                        isActing = false
                                        isSwipeRight = false
                                    }
                                }
                                else -> {
                                    if (actionMap[app.actionDouble] == app.typeNoAction && !isActing) {
                                        isOverrideTap = true
                                        sendAction(app.actionTap)
                                    }

                                    isActing = false
                                    isSwipeUp = false
                                    isSwipeLeft = false
                                    isSwipeRight = false
                                }
                            }
                        }
                        MotionEvent.ACTION_MOVE -> {
                            if (isSwipeUp && !isSwipeLeft && !isSwipeRight) {
                                if (!isActing) isActing = true

                                val velocity = (oldY - ev.rawY)
                                oldY = ev.rawY

                                if (params.y < Utils.getRealScreenSize(context).y / 6 + getHomeY(context)) {
                                    params.y = params.y + (velocity / 2).toInt()
                                    wm.updateViewLayout(this@BarView, params)
                                }

                                if (upHoldHandle == null) {
                                    upHoldHandle = Executors.newScheduledThreadPool(1).schedule({
                                        handler?.post {
                                            sendAction(app.actionUpHold)
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

                                params.x = params.x + (velocity / 2).toInt()
                                wm.updateViewLayout(this@BarView, params)

                                if (isSwipeLeft) {
                                    if (leftHoldHandle == null) {
                                        leftHoldHandle = Executors.newScheduledThreadPool(1).schedule({
                                            handler?.post {
                                                sendAction(app.actionLeftHold)
                                                isSwipeLeft = false
                                                leftHoldHandle = null
                                            }
                                        }, getHoldTime().toLong(), TimeUnit.MILLISECONDS)
                                    }
                                }

                                if (isSwipeRight) {
                                    if (rightHoldHandle == null) {
                                        rightHoldHandle = Executors.newScheduledThreadPool(1).schedule({
                                            handler?.post {
                                                sendAction(app.actionRightHold)
                                                isSwipeRight = false
                                                rightHoldHandle = null
                                            }
                                        }, getHoldTime().toLong(), TimeUnit.MILLISECONDS)
                                    }
                                }
                            }
                        }
                    }

                    super.onTouchEvent(ev)
                } else {
                    if (ev?.action == MotionEvent.ACTION_UP) {
                        exitTransparencyMode()
                        if (reenterTransparencyHandle == null) {
                            reenterTransparencyHandle = Executors.newScheduledThreadPool(1).schedule({
                                handler?.post {
                                    reenterTransparencyHandle = null
                                    enterTransparencyMode()
                                }
                            }, 1500, TimeUnit.MILLISECONDS)
                        }
                    }
                    false
                }
            }
        }

        inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
            /**
             * This is where the swipes are managed
             */
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
                return try {
                    if (!isHidden && !isActing && e1 != null && e2 != null) {
                        when {
                            e2.x - e1.x < -50 && distanceY.absoluteValue < distanceX.absoluteValue -> { //left swipe
                                isSwipeLeft = true
                                true
                            }
                            e2.x - e1.x > 50 && distanceY.absoluteValue < distanceX.absoluteValue -> { //right swipe
                                isSwipeRight = true
                                true
                            }
                            e2.y - e1.y > 50 && distanceY.absoluteValue > distanceX.absoluteValue -> { //down swipe
                                isActing = true
                                sendAction(app.actionDown)
                                true
                            }
                            e2.y - e1.y < -50 && distanceY.absoluteValue > distanceX.absoluteValue -> { //up swipe and up hold-swipe
                                isSwipeUp = true
                                true
                            }
                            else -> false
                        }
                    } else if (isHidden
                            && !isActing
                            && e1 != null
                            && e2 != null
                            && e2.y - e1.y < -50
                            && distanceY.absoluteValue > distanceX.absoluteValue) { //up swipe
                        showPill()
                        true
                    } else false
                } catch (e: IllegalArgumentException) {
                    false
                }
            }

            /**
             * Handle the long-press
             */
            override fun onLongPress(e: MotionEvent?) {
                if (!isHidden && !isActing) {
                    isActing = true
                    sendAction(app.actionHold)
                }
            }

            /**
             * Handle the double-tap
             */
            override fun onDoubleTap(e: MotionEvent?): Boolean {
                return if (!isHidden &&!isActing) {
                    isActing = true
                    sendAction(app.actionDouble)
                    true
                } else false
            }

            /**
             * Handle the single tap
             */
            override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                return if (!isOverrideTap) {
                    isActing = false

                    if (!isHidden) {
                        sendAction(app.actionTap)
                    }
                    true
                } else if (isHidden) {
                    isOverrideTap = false
                    vibrate(getVibrationDuration().toLong())
                    showPill()
                    true
                } else {
                    isOverrideTap = false
                    false
                }
            }
        }
    }

    inner class ScreenRotationListener : OrientationEventListener(context) {
        private var oldRot = Surface.ROTATION_0

        override fun onOrientationChanged(orientation: Int) {
            handler?.postDelayed({
                val newRot = wm.defaultDisplay.rotation

                if (newRot != oldRot) {
                    oldRot = newRot

                    if (newRot == Surface.ROTATION_270 || newRot == Surface.ROTATION_90) {
                        if (prefs.getBoolean("hide_in_landscape", false)) enterTransparencyMode()
                    } else {
                        if (reenterTransparencyHandle != null) {
                            reenterTransparencyHandle?.cancel(true)
                            reenterTransparencyHandle = null
                        }
                        exitTransparencyMode()
                    }
                }
            }, 200)
        }
    }
}