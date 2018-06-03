package com.xda.nobar.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
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
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.services.Actions
import com.xda.nobar.util.Utils
import com.xda.nobar.util.Utils.getCustomHeight
import com.xda.nobar.util.Utils.getCustomWidth
import com.xda.nobar.util.Utils.getHomeX
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
        const val ALPHA_HIDDEN = 0.2f
        const val ALPHA_ACTIVE = 1.0f
        const val ALPHA_GONE = 0.0f

        const val BRIGHTEN_INACTIVE = 0.0f
        const val BRIGHTEN_ACTIVE = 0.5f

        const val DEFAULT_ANIM_DURATION = 500L

        const val SCALE_NORMAL = 1.0f
        const val SCALE_MID = 0.7f
        const val SCALE_SMALL = 0.3f

        const val VIB_SHORT = 50L

        const val DEF_MARGIN_LEFT_DP = 2
        const val DEF_MARGIN_RIGHT_DP = 2
        const val DEF_MARGIN_BOTTOM_DP = 2

        val ENTER_INTERPOLATOR = DecelerateInterpolator()
        val EXIT_INTERPOLATOR = AccelerateInterpolator()
    }

    val params: WindowManager.LayoutParams
        get() = layoutParams as WindowManager.LayoutParams

    private val app = context.applicationContext as App
    private val gestureDetector = GestureManager()
    private val wm: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val actionMap = HashMap<String, Int>()
    private val pool = Executors.newScheduledThreadPool(1)

    var isHidden = false
    var beingTouched = false
    var isTransparent = false
    var isAutoHidden = false

    private var view: View
    private var pill: LinearLayout
    private var pillFlash: LinearLayout
    private var yDownAnimator: ValueAnimator? = null

    private val hideLock = Any()
    private val tapLock = Any()

    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet)
    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr)
    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attributeSet, defStyleAttr, defStyleRes)

    init {
        alpha = ALPHA_GONE
        view = View.inflate(context, R.layout.pill, this)
        pill = view.findViewById(R.id.pill)
        pillFlash = pill.findViewById(R.id.pill_tap_flash)

        loadActionMap()
    }

    /**
     * Perform setup
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        prefs.registerOnSharedPreferenceChangeListener(this)

        val layers = pill.background as LayerDrawable
        (layers.findDrawableByLayerId(R.id.background) as GradientDrawable).apply {
            setColor(Utils.getPillBGColor(context))
            cornerRadius = Utils.getPillCornerRadiusInPx(context).toFloat()
        }
        (layers.findDrawableByLayerId(R.id.foreground) as GradientDrawable).apply {
            setStroke(Utils.dpAsPx(context, 1), Utils.getPillFGColor(context))
            cornerRadius = Utils.getPillCornerRadiusInPx(context).toFloat()
        }

        (pillFlash.background as GradientDrawable).apply {
            cornerRadius = Utils.getPillCornerRadiusInPx(context).toFloat()
        }

        pill.elevation = Utils.dpAsPx(context, if (Utils.shouldShowShadow(context)) 2 else 0).toFloat()
        (pill.layoutParams as FrameLayout.LayoutParams).apply {
            val shadow = Utils.shouldShowShadow(context)
            marginEnd = if (shadow) Utils.dpAsPx(context, DEF_MARGIN_RIGHT_DP) else 0
            marginStart = if (shadow) Utils.dpAsPx(context, DEF_MARGIN_LEFT_DP) else 0
            bottomMargin = if (shadow) Utils.dpAsPx(context, DEF_MARGIN_BOTTOM_DP) else 0

            pill.layoutParams = this
        }

        layoutParams.width = getCustomWidth(context)
        layoutParams.height = getCustomHeight(context)
        layoutParams = layoutParams

        isSoundEffectsEnabled = Utils.feedbackSound(context)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return gestureDetector.gestureDetector.onTouchEvent(event)
    }

    /**
     * Listen for relevant changes in the SharedPreferences
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (actionMap.keys.contains(key)) {
            loadActionMap()
        }
        if (key == "custom_width") {
            layoutParams.width = getCustomWidth(context)
            layoutParams = layoutParams
            updateLayout(params)
        }
        if (key == "custom_height") {
            layoutParams.height = getCustomHeight(context)
            layoutParams = layoutParams
            updateLayout(params)
        }
        if (key == "custom_y") {
            params.y = getHomeY(context)
            updateLayout(params)
        }
        if (key == "custom_x") {
            params.x = getHomeX(context)
            updateLayout(params)
        }
        if (key == "pill_bg" || key == "pill_fg") {
            val layers = pill.background as LayerDrawable
            (layers.findDrawableByLayerId(R.id.background) as GradientDrawable).apply {
                setColor(Utils.getPillBGColor(context))
            }
            (layers.findDrawableByLayerId(R.id.foreground) as GradientDrawable).apply {
                setStroke(Utils.dpAsPx(context, 1), Utils.getPillFGColor(context))
            }
        }
        if (key == "show_shadow") {
            val shadow = Utils.shouldShowShadow(context)
            pill.elevation = Utils.dpAsPx(context, if (shadow) 2 else 0).toFloat()

            (pill.layoutParams as FrameLayout.LayoutParams).apply {
                marginEnd = if (shadow) Utils.dpAsPx(context, DEF_MARGIN_RIGHT_DP) else 0
                marginStart = if (shadow) Utils.dpAsPx(context, DEF_MARGIN_LEFT_DP) else 0
                bottomMargin = if (shadow) Utils.dpAsPx(context, DEF_MARGIN_BOTTOM_DP) else 0

                pill.layoutParams = this
            }
        }
        if (key == "static_pill") {
            if (Utils.dontMoveForKeyboard(context)) {
                params.flags = params.flags or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN and
                        WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM.inv()
                params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            } else {
                params.flags = params.flags or
                        WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM and
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN.inv()
                params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            }

            updateLayout(params)
        }
        if (key == "audio_feedback") {
            isSoundEffectsEnabled = Utils.feedbackSound(context)
        }
        if (key == "pill_corner_radius") {
            val layers = pill.background as LayerDrawable
            (layers.findDrawableByLayerId(R.id.background) as GradientDrawable).apply {
                cornerRadius = Utils.dpAsPx(context, Utils.getPillCornerRadiusInDp(context)).toFloat()
            }
            (layers.findDrawableByLayerId(R.id.foreground) as GradientDrawable).apply {
                cornerRadius = Utils.dpAsPx(context, Utils.getPillCornerRadiusInDp(context)).toFloat()
            }
            (pillFlash.background as GradientDrawable).apply {
                cornerRadius = Utils.getPillCornerRadiusInPx(context).toFloat()
            }
        }
        if (key == "larger_hitbox") {
            val enabled = Utils.largerHitbox(context)
            val margins = getPillMargins()
            params.height = Utils.getCustomHeight(context)
            margins.top = resources.getDimensionPixelSize((if (enabled) R.dimen.pill_margin_top_large_hitbox else R.dimen.pill_margin_top_normal))
            changePillMargins(margins)
            updateLayout(params)
        }
    }

    /**
     * Perform cleanup
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        prefs.unregisterOnSharedPreferenceChangeListener(this)
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
        animate(listener, ALPHA_ACTIVE)
    }

    /**
     * Animate to a chosen alpha
     * @param listener optional animation listener
     * @param alpha desired alpha level (0-1)
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
     * This is called twice to "flash" the pill when an action is performed
     */
    fun animateActiveLayer(alpha: Float) {
        handler?.post {
            pillFlash.apply {
                val alphaRatio = Color.alpha(Utils.getPillBGColor(context)).toFloat() / 255f
                animate()
                        .setDuration(getAnimationDurationMs())
                        .alpha(alpha * alphaRatio)
                        .start()
            }
        }
    }

    /**
     * "Hide" the pill by moving it partially offscreen
     */
    fun hidePill(auto: Boolean) {
        try {
            if (!isHidden && app.isPillShown()) {
                isAutoHidden = auto

                val animDurScale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
                val time = (getAnimationDurationMs() * animDurScale)
                val distance = params.y

                val animator = ValueAnimator.ofInt(params.y, 0)
                animator.interpolator = DecelerateInterpolator()
                animator.addUpdateListener {
                    params.y = it.animatedValue.toString().toInt()
                    updateLayout(params)
                }
                animator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        animateHide()
                    }
                })
                animator.duration = (time * distance / 100f).toLong()
                animator.start()

                showHiddenToast()
            }
        } catch (e: IllegalArgumentException) {}
    }

    private fun animateHide() {
        pill.animate()
                .translationY(pill.height.toFloat() / 2f)
                .alpha(ALPHA_HIDDEN)
                .setInterpolator(ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    pill.translationY = pill.height.toFloat() / 2f

                    handler?.post {
                        jiggleDown()
                    }

                    updateLayout(params)

                    isHidden = true
                }
                .start()
    }

    private var hideHandle: ScheduledFuture<*>? = null

    /**
     * "Show" the pill by moving it back to its normal position
     */
    fun showPill(forceNotAuto: Boolean) {
        try {
            if (isHidden && app.isPillShown()) {
                synchronized(hideLock) {
                    if (hideHandle != null) {
                        hideHandle?.cancel(true)
                        hideHandle = null
                    }

                    if (isAutoHidden && !forceNotAuto) {
                        hideHandle = pool.schedule({
                            if (isAutoHidden && !forceNotAuto) hidePill(true)
                        }, 1500, TimeUnit.MILLISECONDS)
                    }

                    pill.animate()
                            .translationY(0f)
                            .alpha(ALPHA_ACTIVE)
                            .setInterpolator(EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .withEndAction {
                                pill.translationY = 0f

                                animateShow()
                            }
                            .start()
                }
            }
        } catch (e: IllegalArgumentException) {}
    }

    private fun animateShow() {
        val animDurScale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
        val time = (getAnimationDurationMs() * animDurScale)
        val distance = Utils.getHomeY(context)
        val animator = ValueAnimator.ofInt(params.y, getHomeY(context))

        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener {
            params.y = it.animatedValue.toString().toInt()
            updateLayout(params)
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                jiggleUp()

                handler?.postDelayed(Runnable { isHidden = false }, (if (getAnimationDurationMs() < 12) 12 else 0))
            }
        })
        animator.duration = (time * distance / 100f).toLong()
        animator.start()
    }

    fun changePillMargins(margins: Rect) {
        (pill.layoutParams as FrameLayout.LayoutParams).apply {
            bottomMargin = margins.bottom
            topMargin = margins.top
            marginStart = margins.left
            marginEnd = margins.right

            pill.layoutParams = pill.layoutParams
        }
    }

    fun getPillMargins(): Rect {
        val rect = Rect()

        (pill.layoutParams as FrameLayout.LayoutParams).apply {
            rect.bottom = bottomMargin
            rect.top = topMargin
            rect.left = marginStart
            rect.right = marginEnd
        }

        return rect
    }

    /**
     * Make the pill transparent.
     * In this mode, the pill will be invisible until touched, when it will become visible, allowing the user to perform an action.
     */
    fun enterTransparencyMode() {
        if (!isTransparent) {
            isTransparent = true

            animate()
                    .alpha(ALPHA_GONE)
                    .setDuration(getAnimationDurationMs())
                    .start()
        }
    }

    /**
     * Make the pill opaque again
     */
    fun exitTransparencyMode() {
        if (isTransparent) {
            isTransparent = false

            animate()
                    .alpha(ALPHA_ACTIVE)
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
        val which = actionMap[key] ?: return

        if (which == app.typeNoAction) return

        vibrate(getVibrationDuration().toLong())

        if (key == app.actionDouble) handler?.postDelayed({ vibrate(getVibrationDuration().toLong()) }, getVibrationDuration().toLong())

        if (which == app.typeHide) {
            if (key == app.actionUp || key == app.actionUpHold) {
                yDownAnimator?.cancel()
                yDownAnimator = null
            }
            hidePill(false)
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

        if (Utils.shouldUseRootCommands(context)) {
            app.rootBinder?.handle(which)
        }
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
//                .alpha(ALPHA_ACTIVE)
                .setInterpolator(ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    animate()
                        .scaleX(SCALE_NORMAL)
//                        .alpha(ALPHA_INACTIVE)
                        .setInterpolator(EXIT_INTERPOLATOR)
                        .setDuration(getAnimationDurationMs())
                        .start()
                    animateActiveLayer(BRIGHTEN_INACTIVE)
                }
                .start()
        animateActiveLayer(BRIGHTEN_ACTIVE)
    }

    /**
     * The animation for a left-swipe on the pill
     */
    private fun jiggleLeft() {
        animate()
                .scaleX(SCALE_MID)
//                .alpha(ALPHA_ACTIVE)
                .x(-width * (1 - SCALE_MID) / 2)
                .setInterpolator(ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    animate()
                            .scaleX(SCALE_NORMAL)
                            .x(0f)
//                            .alpha(ALPHA_INACTIVE)
                            .setInterpolator(EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .start()
                    animateActiveLayer(BRIGHTEN_INACTIVE)
                }
                .start()
        animateActiveLayer(BRIGHTEN_ACTIVE)
    }

    /**
     * The animation for a swipe-left and hold on the pill
     */
    private fun jiggleLeftHold() {
        animate()
                .scaleX(SCALE_SMALL)
//                .alpha(ALPHA_ACTIVE)
                .x(-width * (1 - SCALE_SMALL) / 2)
                .setInterpolator(ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    animate()
                            .scaleX(SCALE_NORMAL)
                            .x(0f)
//                            .alpha(ALPHA_INACTIVE)
                            .setInterpolator(EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .start()
                    animateActiveLayer(BRIGHTEN_INACTIVE)
                }
        animateActiveLayer(BRIGHTEN_ACTIVE)
    }

    /**
     * The animation for a right-swipe on the pill
     */
    private fun jiggleRight() {
        animate()
                .scaleX(SCALE_MID)
//                .alpha(ALPHA_ACTIVE)
                .x(width * (1 - SCALE_MID) / 2)
                .setInterpolator(ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    animate()
                            .scaleX(SCALE_NORMAL)
                            .x(0f)
//                            .alpha(ALPHA_INACTIVE)
                            .setInterpolator(EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .start()
                    animateActiveLayer(BRIGHTEN_INACTIVE)
                }
                .start()
        animateActiveLayer(BRIGHTEN_ACTIVE)
    }

    /**
     * The animation for a swipe-right and hold on the pill
     */
    private fun jiggleRightHold() {
        animate()
                .scaleX(SCALE_SMALL)
//                .alpha(ALPHA_ACTIVE)
                .x(width * (1 - SCALE_SMALL) / 2)
                .setInterpolator(ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    animate()
                            .scaleX(SCALE_NORMAL)
                            .x(0f)
//                            .alpha(ALPHA_INACTIVE)
                            .setInterpolator(EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .start()
                    animateActiveLayer(BRIGHTEN_INACTIVE)
                }
        animateActiveLayer(BRIGHTEN_ACTIVE)
    }

    /**
     * The animation for a long-press on the pill
     */
    private fun jiggleHold() {
        animate()
                .scaleX(SCALE_SMALL)
//                .alpha(ALPHA_ACTIVE)
                .setInterpolator(ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    animate()
                            .scaleX(SCALE_NORMAL)
//                            .alpha(ALPHA_INACTIVE)
                            .setInterpolator(EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .start()
                    animateActiveLayer(BRIGHTEN_INACTIVE)
                }
                .start()
        animateActiveLayer(BRIGHTEN_ACTIVE)
    }

    /**
     * The animation for an up-swipe on the pill
     */
    private fun jiggleUp() {
        animate()
                .scaleY(SCALE_MID)
                .y(-height * (1 - SCALE_MID) / 2)
//                .alpha(ALPHA_ACTIVE)
                .setInterpolator(ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    animate()
                            .scaleY(SCALE_NORMAL)
                            .y(0f)
//                            .alpha(ALPHA_INACTIVE)
                            .setInterpolator(EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .start()
                    animateActiveLayer(BRIGHTEN_INACTIVE)
                }
                .start()
        animateActiveLayer(BRIGHTEN_ACTIVE)
    }

    /**
     * The animation for an up-swipe and hold on the pill
     */
    private fun jiggleHoldUp() {
        animate()
                .scaleY(SCALE_SMALL)
                .y(-height * (1 - SCALE_SMALL) / 2)
//                .alpha(ALPHA_ACTIVE)
                .setInterpolator(ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    animate()
                            .scaleY(SCALE_NORMAL)
                            .y(0f)
//                            .alpha(ALPHA_INACTIVE)
                            .setInterpolator(EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .start()
                    animateActiveLayer(BRIGHTEN_INACTIVE)
                }
                .start()
        animateActiveLayer(BRIGHTEN_ACTIVE)
    }

    /**
     * The animation for a down-swipe on the pill
     */
    private fun jiggleDown() {
        animate()
                .scaleY(SCALE_MID)
                .y(height * (1 - SCALE_MID) / 2)
//                .alpha(ALPHA_ACTIVE)
                .setInterpolator(ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    animate()
                            .scaleY(SCALE_NORMAL)
                            .y(0f)
//                            .alpha(ALPHA_INACTIVE)
                            .setInterpolator(EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .start()
                    animateActiveLayer(BRIGHTEN_INACTIVE)
                }
                .start()
        animateActiveLayer(BRIGHTEN_ACTIVE)
    }

    /**
     * The animation for a double-tap on the pill
     */
    private fun jiggleDoubleTap() {
        animate()
                .scaleX(SCALE_MID)
//                .alpha(ALPHA_ACTIVE)
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
//                                        .alpha(ALPHA_INACTIVE)
                                        .setInterpolator(EXIT_INTERPOLATOR)
                                        .setDuration(getAnimationDurationMs())
                                        .start()
                                animateActiveLayer(BRIGHTEN_INACTIVE)
                            }
                            .start()
                }
                .start()
        animateActiveLayer(BRIGHTEN_ACTIVE)
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
        return prefs.getInt("vibration_duration", VIB_SHORT.toInt())
    }

    /**
     * Get the user-defined or default duration of the pill animations
     * @return the duration, in ms
     */
    private fun getAnimationDurationMs(): Long {
        return prefs.getInt("anim_duration", DEFAULT_ANIM_DURATION.toInt()).toLong()
    }

    /**
     * Show a toast when the pill is hidden. Only shows once.
     */
    private fun showHiddenToast() {
        if (prefs.getBoolean("show_hidden_toast", true)) {
            Toast.makeText(context, resources.getString(R.string.pill_hidden), Toast.LENGTH_LONG).show()
            prefs.edit().putBoolean("show_hidden_toast", false).apply()
        }
    }
    
    private fun updateLayout(params: WindowManager.LayoutParams) {
        handler?.post {
            try {
                wm.updateViewLayout(this, params)
            } catch (e: Exception) {}
        }
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
        private var wasHidden = false

        private var upHoldHandle: ScheduledFuture<*>? = null
        private var leftHoldHandle: ScheduledFuture<*>? = null
        private var rightHoldHandle: ScheduledFuture<*>? = null

        private var oldY = 0F
        private var oldX = 0F

        /**
         * The main gesture detection
         */
        inner class GD : GestureDetector(context, GestureListener()) {
            override fun onTouchEvent(ev: MotionEvent?): Boolean {
                val animDurScale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
                val time = (getAnimationDurationMs() * animDurScale)

                synchronized(hideLock) {
                    when (ev?.action) {
                        MotionEvent.ACTION_DOWN -> {
                            wasHidden = isHidden
                            app.immersiveListener.onGlobalLayout()
                            oldY = ev.rawY
                            oldX = ev.rawX
                            beingTouched = true
                        }

                        MotionEvent.ACTION_UP -> {
                            beingTouched = false

                            if (wasHidden) {
                                isSwipeUp = false
                            }

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

                            if (pill.translationX != 0f) {
                                val currTrans = pill.translationX
                                pill.animate()
                                        .translationX(0f)
                                        .setDuration(getAnimationDurationMs())
                                        .withEndAction {
                                            handler?.post {
                                                if (params.x == getHomeX(context)) {
                                                    if (currTrans < 0 && actionMap[app.actionLeft] != app.typeNoAction) jiggleRight()
                                                    if (currTrans > 0 && actionMap[app.actionRight] != app.typeNoAction) jiggleLeft()
                                                }
                                            }

                                            isActing = false
                                            isSwipeLeft = false
                                            isSwipeRight = false
                                        }
                                        .start()
                            }

                            when {
                                params.y > getHomeY(context) -> {
                                    val distance = (params.y - getHomeY(context)).absoluteValue
                                    if (yDownAnimator != null) {
                                        yDownAnimator?.cancel()
                                        yDownAnimator = null
                                    }
                                    yDownAnimator = ValueAnimator.ofInt(params.y, getHomeY(context))
                                    yDownAnimator?.interpolator = DecelerateInterpolator()
                                    yDownAnimator?.addUpdateListener {
                                        params.y = it.animatedValue.toString().toInt()
                                        updateLayout(params)
                                    }
                                    yDownAnimator?.addListener(object : AnimatorListenerAdapter() {
                                        override fun onAnimationEnd(animation: Animator?) {
                                            if (isSwipeUp) jiggleDown()
//
                                            isActing = false
                                            isSwipeUp = false

                                            yDownAnimator = null
                                        }

                                        override fun onAnimationCancel(animation: Animator?) {
                                            onAnimationEnd(animation)
                                        }
                                    })
                                    yDownAnimator?.duration = (time * distance / 100f).toLong()
                                    yDownAnimator?.start()
                                }
                                params.x < getHomeX(context) || params.x > getHomeX(context) -> {
                                    val distance = (params.x - getHomeX(context)).absoluteValue
                                    val animator = ValueAnimator.ofInt(params.x, getHomeX(context))
                                    animator.interpolator = DecelerateInterpolator()
                                    animator.addUpdateListener {
                                        params.x = it.animatedValue.toString().toInt()
                                        updateLayout(params)
                                    }
                                    animator.addListener(object : AnimatorListenerAdapter() {
                                        override fun onAnimationEnd(animation: Animator?) {
                                            if (isSwipeLeft && actionMap[app.actionLeft] != app.typeNoAction) jiggleRight()
                                            if (isSwipeRight && actionMap[app.actionRight] != app.typeNoAction) jiggleLeft()

                                            isActing = false
                                            isSwipeLeft = false
                                            isSwipeRight = false
                                        }
                                    })
                                    animator.duration = (time * distance / 100f).toLong()
                                    animator.start()
                                }
                                else -> {
                                    if (actionMap[app.actionDouble] == app.typeNoAction && !isActing && !wasHidden) {
                                        synchronized(tapLock) {
                                            isOverrideTap = true
                                            sendAction(app.actionTap)
                                        }
                                    }

                                    if (isSwipeLeft && actionMap[app.actionLeft] != app.typeNoAction) jiggleRight()
                                    if (isSwipeRight && actionMap[app.actionRight] != app.typeNoAction) jiggleLeft()
                                    if (isSwipeUp) jiggleDown()

                                    isActing = false
                                    isSwipeUp = false
                                    isSwipeLeft = false
                                    isSwipeRight = false
                                }
                            }

                            wasHidden = isHidden
                        }
                        MotionEvent.ACTION_MOVE -> {
                            if (isSwipeUp && !isSwipeLeft && !isSwipeRight) {
                                if (!isActing) isActing = true

                                val velocity = (oldY - ev.rawY)
                                oldY = ev.rawY

                                if (params.y < Utils.getRealScreenSize(context).y / 6 + getHomeY(context) && getAnimationDurationMs() > 0) {
                                    params.y = params.y + (velocity / 2).toInt()
                                    updateLayout(params)
                                }

                                if (upHoldHandle == null) {
                                    upHoldHandle = pool.schedule({
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

                                val half = (Utils.getRealScreenSize(context).x.toFloat() / 2f - Utils.getCustomWidth(context).toFloat() / 2f).toInt()

                                if (getAnimationDurationMs() > 0) {
                                    when {
                                        params.x <= -half && !isSwipeRight -> pill.translationX -= velocity
                                        params.x >= half && !isSwipeLeft -> pill.translationX += velocity
                                        else -> {
                                            params.x = params.x + (velocity / 2).toInt()
                                            updateLayout(params)
                                        }
                                    }
                                }

                                if (isSwipeLeft) {
                                    if (leftHoldHandle == null) {
                                        leftHoldHandle = pool.schedule({
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
                                        rightHoldHandle = pool.schedule({
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

                    return super.onTouchEvent(ev)
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
                            e2.y - e1.y > 30 && distanceY.absoluteValue > distanceX.absoluteValue -> { //down swipe
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
                        showPill(false)
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
                synchronized(tapLock) {
                    synchronized(hideLock) {
                        return if (!isOverrideTap && !isHidden) {
                            isActing = false

                            if (!isHidden) {
                                sendAction(app.actionTap)
                            }
                            true
                        } else if (isHidden) {
                            isOverrideTap = false
                            vibrate(getVibrationDuration().toLong())
                            showPill(false)
                            true
                        } else {
                            isOverrideTap = false
                            false
                        }
                    }
                }
            }
        }
    }
}