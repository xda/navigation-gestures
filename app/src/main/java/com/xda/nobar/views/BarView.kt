package com.xda.nobar.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.AttributeSet
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.util.Utils
import com.xda.nobar.util.Utils.getCustomHeight
import com.xda.nobar.util.Utils.getCustomWidth
import com.xda.nobar.util.Utils.getHomeX
import com.xda.nobar.util.Utils.getHomeY
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

        const val DEFAULT_ANIM_DURATION = 150L

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

    val params: WindowManager.LayoutParams = WindowManager.LayoutParams()

    private val app = context.applicationContext as App
    private val gestureDetector = GestureManager()
    private val wm: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val pool = Executors.newScheduledThreadPool(1)

    val gestureManager = com.xda.nobar.util.GestureManager(this)

    var isHidden = false
    var isCarryingOutTouchAction = false
        set(value) {
            field = value
            if (!value) {
                queuedLayoutUpdate?.invoke()
                queuedLayoutUpdate = null
            }
        }
    var isAutoHidden = false
    var isImmersive = false
        set(value) {
            field = value
            if (Utils.shouldUseOverscanMethod(context)) {
                queuedLayoutUpdate = {
                    if (params.y != getAdjustedHomeY()) {
                        params.y = getAdjustedHomeY()
                        updateLayout(params)
                    }
                }

                if (!isCarryingOutTouchAction) {
                    queuedLayoutUpdate?.invoke()
                    queuedLayoutUpdate = null
                }
            }
        }
    var immersiveNav: Boolean
        get() {
            val imm = Settings.Global.getString(context.contentResolver, Settings.Global.POLICY_CONTROL) ?: return false
            return imm.contains("navigation")
        }
        set(value) {
            if (Utils.shouldUseOverscanMethod(context)) {
                queuedLayoutUpdate = {
                    if (params.y != getAdjustedHomeY()) {
                        params.y = getAdjustedHomeY()
                        updateLayout(params)
                    }
                }

                if (!isCarryingOutTouchAction) {
                    queuedLayoutUpdate?.invoke()
                    queuedLayoutUpdate = null
                }
            }
        }

    private var queuedLayoutUpdate: (() -> Unit)? = null

    var view: View
    var pill: LinearLayout
    var pillFlash: LinearLayout
    var yDownAnimator: ValueAnimator? = null

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

        gestureManager.loadActionMap()
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
        if (gestureManager.actionMap.keys.contains(key)) {
            gestureManager.loadActionMap()
        }
        if (key != null && key.contains("use_pixels")) {
            params.width = getCustomWidth(context)
            params.height = getCustomHeight(context)
            params.x = getHomeX(context)
            params.y = getAdjustedHomeY()
            updateLayout(params)
        }
        if (key == "custom_width_percent" || key == "custom_width") {
            params.width = getCustomWidth(context)
            params.x = getHomeX(context)
            updateLayout(params)
        }
        if (key == "custom_height_percent" || key == "custom_height") {
            params.height = getCustomHeight(context)
            updateLayout(params)
        }
        if (key == "custom_y_percent" || key == "custom_y") {
            params.y = getAdjustedHomeY()
            updateLayout(params)
        }
        if (key == "custom_x_percent" || key == "custom_x") {
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
     * "Hide" the pill by moving it partially offscreen
     */
    fun hidePill(auto: Boolean) {
        handler?.post {
            if (app.isPillShown()) {
                isCarryingOutTouchAction = true
                isAutoHidden = auto

                val navHeight = getZeroY()

                val animDurScale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
                val time = (getAnimationDurationMs() * animDurScale)
                val distance = (params.y - navHeight).absoluteValue

                if (distance == 0) {
                    animateHide()
                } else {
                    val animator = ValueAnimator.ofInt(params.y, navHeight)
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
                }

                showHiddenToast()
            }
        }
    }

    private fun animateHide() {
        handler?.post {
            pill.animate()
                    .translationY(pill.height.toFloat() / 2f)
                    .alpha(ALPHA_HIDDEN)
                    .setInterpolator(ENTER_INTERPOLATOR)
                    .setDuration(getAnimationDurationMs())
                    .withEndAction {
                        pill.translationY = pill.height.toFloat() / 2f

                        isCarryingOutTouchAction = false

                        isHidden = true
                    }
                    .start()
        }
    }

    private var hideHandle: ScheduledFuture<*>? = null

    /**
     * "Show" the pill by moving it back to its normal position
     */
    fun showPill(forceNotAuto: Boolean) {
        handler?.post {
            if (app.isPillShown()) {
                isCarryingOutTouchAction = true
                synchronized(hideLock) {
                    if ((forceNotAuto || !isAutoHidden) && hideHandle != null) {
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
        }
    }

    private fun animateShow() {
        handler?.post {
            val animDurScale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
            val time = (getAnimationDurationMs() * animDurScale)
            val navHeight = getAdjustedHomeY()
            val distance = (navHeight - params.y).absoluteValue
            val animator = ValueAnimator.ofInt(params.y, navHeight)

            if (distance == 0) {
                handler?.postDelayed(Runnable { isHidden = false }, (if (getAnimationDurationMs() < 12) 12 else 0))
                isCarryingOutTouchAction = false
            } else {
                animator.interpolator = DecelerateInterpolator()
                animator.addUpdateListener {
                    params.y = it.animatedValue.toString().toInt()
                    updateLayout(params)
                }
                animator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        handler?.postDelayed(Runnable { isHidden = false }, (if (getAnimationDurationMs() < 12) 12 else 0))
                        isCarryingOutTouchAction = false
                    }
                })
                animator.duration = (time * distance / 100f).toLong()
                animator.start()
            }
        }
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

    fun getAdjustedHomeY(): Int {
        return if ((isImmersive || immersiveNav) && Utils.shouldUseOverscanMethod(context)) {
            if ((wm.defaultDisplay.rotation == Surface.ROTATION_90
                            || wm.defaultDisplay.rotation == Surface.ROTATION_270)
                    && !Utils.useTabletMode(context)) if (Utils.hideInFullscreen(context)) 0 else getHomeY(context)
            else if (Utils.origBarInFullscreen(context)) 0 else Utils.getNavBarHeight(context) + if (Utils.hideInFullscreen(context)) 0 else Utils.getHomeY(context)
        } else getHomeY(context)
    }

    fun getZeroY(): Int {
        return if ((isImmersive || immersiveNav) && Utils.shouldUseOverscanMethod(context)) {
            if ((wm.defaultDisplay.rotation == Surface.ROTATION_270
                            || wm.defaultDisplay.rotation == Surface.ROTATION_90)
                    && !Utils.useTabletMode(context)) 0
            else if (Utils.origBarInFullscreen(context)) 0 else Utils.getNavBarHeight(context)
        } else 0
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

    /**
     * Get the user-defined or default duration of the pill animations
     * @return the duration, in ms
     */
    private fun getAnimationDurationMs(): Long {
        return prefs.getInt("anim_duration", BarView.DEFAULT_ANIM_DURATION.toInt()).toLong()
    }
    
    fun updateLayout(params: WindowManager.LayoutParams) {
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

        /**
         * The main gesture detection
         */
        inner class GD : GestureDetector(context, GestureListener()) {
            override fun onTouchEvent(ev: MotionEvent?): Boolean {
                return gestureManager.handleTouchEvent(ev) || super.onTouchEvent(ev)
            }
        }

        inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                return gestureManager.onSingleTapUp(e)
            }

            /**
             * Handle the long-press
             */
            override fun onLongPress(e: MotionEvent) {
                gestureManager.onLongPress(e)
            }

            /**
             * Handle the double-tap
             */
            override fun onDoubleTap(e: MotionEvent): Boolean {
                return gestureManager.onDoubleTap(e)
            }

            /**
             * Handle the single tap
             */
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                return gestureManager.onSingleTapConfirmed(e)
            }
        }
    }
}