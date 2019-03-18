package com.xda.nobar.views

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.*
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.dynamicanimation.animation.DynamicAnimation
import com.xda.nobar.R
import com.xda.nobar.util.*
import com.xda.nobar.util.helpers.HiddenPillReasonManager
import com.xda.nobar.util.helpers.bar.BarViewGestureManagerHorizontal
import com.xda.nobar.util.helpers.bar.BarViewGestureManagerVertical
import com.xda.nobar.util.helpers.bar.BarViewGestureManagerVertical270
import com.xda.nobar.util.helpers.bar.BaseBarViewGestureManager
import kotlinx.android.synthetic.main.pill.view.*
import kotlin.math.absoluteValue

/**
 * The Pill™©® (not really copyrighted)
 */
@Suppress("DEPRECATION")
class BarView : LinearLayout, SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        const val ALPHA_HIDDEN = 0.2f
        const val ALPHA_ACTIVE = 1.0f
        const val ALPHA_GONE = 0.0f

        const val SCALE_NORMAL = 1.0f
        const val SCALE_MID = 0.7f
        const val SCALE_SMALL = 0.3f

        const val DEF_MARGIN_LEFT_DP = 4
        const val DEF_MARGIN_RIGHT_DP = 4
        const val DEF_MARGIN_BOTTOM_DP = 2

        val ENTER_INTERPOLATOR = DecelerateInterpolator()
        val EXIT_INTERPOLATOR = AccelerateInterpolator()

        private const val MSG_HIDE = 0
        private const val MSG_SHOW = 1
    }

    internal val actionHolder = context.actionHolder

    var view: View = View.inflate(context, R.layout.pill, this)
    var shouldReAddOnDetach = false

    val params = WindowManager.LayoutParams().apply {
        x = adjustedHomeX
        y = adjustedHomeY
        width = adjustedWidth
        height = adjustedHeight
        gravity = Gravity.CENTER or Gravity.TOP
        type =
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PRIORITY_PHONE
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        format = PixelFormat.TRANSLUCENT
        softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE

        if (context.prefManager.dontMoveForKeyboard) {
            flags = flags and
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM.inv()
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED
        }
    }
    val hiddenPillReasons = HiddenPillReasonManager()

    val adjustedHomeY: Int
        get() = if (isVertical) (if (is270Vertical) 1 else -1) * anchoredHomeX else actualHomeY

    private val actualHomeY: Int
        get() = context.realScreenSize.y - context.prefManager.homeY - context.prefManager.customHeight

    val zeroY: Int
        get() = if (isVertical) 0 else context.realScreenSize.y - context.prefManager.customHeight

    val adjustedHomeX: Int
        get() = if (isVertical) anchoredHomeY else actualHomeX

    private val actualHomeX: Int
        get() {
            val diff = try {
                val screenSize = context.realScreenSize
                val frame = Rect().apply { getWindowVisibleDisplayFrame(this) }
                (frame.left + frame.right) - screenSize.x
            } catch (e: Exception) {
                0
            }

            return context.prefManager.homeX - if (immersiveNav && !context.prefManager.useTabletMode) (diff / 2f).toInt() else 0
        }

    private val anchoredHomeX: Int
        get() {
            val diff = try {
                val screenSize = context.realScreenSize
                val frame = Rect().apply { getWindowVisibleDisplayFrame(this) }
                (frame.top + frame.bottom) - screenSize.y
            } catch (e: Exception) {
                0
            }

            return context.prefManager.homeX - if (immersiveNav && !context.prefManager.useTabletMode) (diff / 2f).toInt() else 0
        }

    private val anchoredHomeY: Int
        get() {
            val diff = try {
                if (context.prefManager.run { shouldUseOverscanMethod || useTabletMode }) {
                    val frame = Rect().apply { getWindowVisibleDisplayFrame(this) }

                    val rotation = context.rotation

                    if (rotation == Surface.ROTATION_270) {
                        if (!context.prefManager.useRot270Fix)
                            frame.left.absoluteValue
                        else
                            frame.right - context.realScreenSize.y
                    } else {
                        frame.right - context.realScreenSize.y
                    }
                } else 0
            } catch (e: Exception) {
                0
            }

            return (context.prefManager.homeY + diff.absoluteValue)
        }

    val adjustedWidth: Int
        get() = context.prefManager.run { if (isVertical) customHeight else customWidth }

    val adjustedHeight: Int
        get() = context.prefManager.run { if (isVertical) customWidth else customHeight }

    val isVertical: Boolean
        get() = context.prefManager.anchorPill
                && context.rotation.run { this == Surface.ROTATION_270 || this == Surface.ROTATION_90 }

    val is270Vertical: Boolean
        get() = isVertical
                && context.rotation == Surface.ROTATION_270

    private val horizontalGestureManager = BarViewGestureManagerHorizontal(this)
    private val verticalGestureManager = BarViewGestureManagerVertical(this)
    private val vertical270GestureMansger = BarViewGestureManagerVertical270(this)

    var currentGestureDetector: BaseBarViewGestureManager = horizontalGestureManager

    private val wm: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val animator = BarAnimator(this)

    private val hideThread = HandlerThread("NoBar-Hide").apply { start() }
    private val hideHandler = HideHandler(hideThread.looper)

    var isHidden = false
    var beingTouched = false
    var isCarryingOutTouchAction = false
    var isPillHidingOrShowing = false
    var isImmersive = false
    var immersiveNav = false

    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet)
    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr)
    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attributeSet, defStyleAttr, defStyleRes)

    init {
        alpha = ALPHA_GONE

        currentGestureDetector.singleton.loadActionMap()
        PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this)
        isSoundEffectsEnabled = context.prefManager.feedbackSound

        val layers = pill.background as LayerDrawable
        (layers.findDrawableByLayerId(R.id.background) as GradientDrawable).apply {
            setColor(context.prefManager.pillBGColor)
            cornerRadius = context.prefManager.pillCornerRadiusPx.toFloat()
        }
        (layers.findDrawableByLayerId(R.id.foreground) as GradientDrawable).apply {
            setStroke(context.dpAsPx(1), context.prefManager.pillFGColor)
            cornerRadius = context.prefManager.pillCornerRadiusPx.toFloat()
        }

        (pill_tap_flash.background as GradientDrawable).apply {
            cornerRadius = context.prefManager.pillCornerRadiusPx.toFloat()
        }

        adjustPillShadow()
    }

    /**
     * Perform setup
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (context.prefManager.largerHitbox) {
            updateLargerHitbox()
        }

        context.app.pillShown = true

        show(null)

        if (context.prefManager.autoHide) {
            val reason = HiddenPillReasonManager.AUTO
            scheduleHide(reason)
        }

        handleRotationOrAnchorUpdate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return currentGestureDetector.onTouchEvent(event)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (currentGestureDetector.actionMap.keys.contains(key)) {
            currentGestureDetector.singleton.loadActionMap()
        }

        when (key) {
            PrefManager.CUSTOM_WIDTH,
            PrefManager.CUSTOM_WIDTH_PERCENT,
            PrefManager.CUSTOM_HEIGHT,
            PrefManager.CUSTOM_HEIGHT_PERCENT,
            PrefManager.CUSTOM_X,
            PrefManager.CUSTOM_X_PERCENT,
            PrefManager.CUSTOM_Y,
            PrefManager.CUSTOM_Y_PERCENT,
            PrefManager.USE_PIXELS_WIDTH,
            PrefManager.USE_PIXELS_HEIGHT,
            PrefManager.USE_PIXELS_X,
            PrefManager.USE_PIXELS_Y -> {
                updatePositionAndDimens()
            }

            PrefManager.ANCHOR_PILL -> {
                handleRotationOrAnchorUpdate()
            }

            PrefManager.IS_ACTIVE -> {
                currentGestureDetector.singleton.refreshFlashlightState()
            }

            PrefManager.FLASHLIGHT_COMPAT -> {
                currentGestureDetector.singleton.refreshFlashlightState()
            }

            PrefManager.PILL_BG,
            PrefManager.PILL_FG -> {
                val layers = pill.background as LayerDrawable
                (layers.findDrawableByLayerId(R.id.background) as GradientDrawable).apply {
                    setColor(context.prefManager.pillBGColor)
                }
                (layers.findDrawableByLayerId(R.id.foreground) as GradientDrawable).apply {
                    setStroke(context.dpAsPx(1), context.prefManager.pillFGColor)
                }
            }

            PrefManager.SHOW_SHADOW -> {
                adjustPillShadow()
            }

            PrefManager.STATIC_PILL -> {
                setMoveForKeyboard(!context.prefManager.dontMoveForKeyboard)

            }

            PrefManager.AUDIO_FEEDBACK -> {
                isSoundEffectsEnabled = context.prefManager.feedbackSound

            }

            PrefManager.PILL_CORNER_RADIUS -> {
                val layers = pill.background as LayerDrawable
                (layers.findDrawableByLayerId(R.id.background) as GradientDrawable).apply {
                    cornerRadius = context.dpAsPx(context.prefManager.pillCornerRadiusDp).toFloat()
                }
                (layers.findDrawableByLayerId(R.id.foreground) as GradientDrawable).apply {
                    cornerRadius = context.dpAsPx(context.prefManager.pillCornerRadiusDp).toFloat()
                }
                (pill_tap_flash.background as GradientDrawable).apply {
                    cornerRadius = context.prefManager.pillCornerRadiusPx.toFloat()
                }
            }

            PrefManager.LARGER_HITBOX -> {
                updateLargerHitbox()
            }

            PrefManager.AUTO_HIDE_PILL -> {
                if (context.prefManager.autoHide) {
                    if (!isHidden) scheduleHide(HiddenPillReasonManager.AUTO)
                } else {
                    if (isHidden) hideHandler.show(HiddenPillReasonManager.AUTO)
                }
            }
        }
    }

    /**
     * Perform cleanup
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        if (shouldReAddOnDetach) {
            context.app.addBarInternal(false)
            shouldReAddOnDetach = false
        } else {
            PreferenceManager.getDefaultSharedPreferences(context).unregisterOnSharedPreferenceChangeListener(this)
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is BarView
    }

    override fun hashCode(): Int {
        return 1
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
        handler?.post {
            animate().alpha(alpha).setDuration(getAnimationDurationMs())
                    .setListener(object : Animator.AnimatorListener {
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
    }

    /**
     * "Hide" the pill by moving it partially offscreen
     */
    fun hidePill(auto: Boolean, autoReason: String?, overrideBeingTouched: Boolean = false) {
        context.app.handler.post {
            if (auto && autoReason == null) throw IllegalArgumentException("autoReason must not be null when auto is true")
            if (auto && autoReason != null) hiddenPillReasons.add(autoReason)

            if (!auto) hiddenPillReasons.add(HiddenPillReasonManager.MANUAL)

            if ((!beingTouched && !isCarryingOutTouchAction) || overrideBeingTouched) {
                if (context.app.isPillShown()) {
                    isPillHidingOrShowing = true

                    animator.hide(DynamicAnimation.OnAnimationEndListener { _, _, _, _ ->
                        animateHide()
                    })

                    showHiddenToast()
                }
            } else {
                scheduleHide(autoReason ?: return@post)
            }
        }
    }

    private fun animateHide() {
        pill.animate()
                .alpha(ALPHA_HIDDEN)
                .setInterpolator(ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    isHidden = true

                    isPillHidingOrShowing = false
                }
                .apply {
                    if (isVertical) translationX((if (is270Vertical) -1 else 1) * pill.width.toFloat() / 2f)
                    else translationY(pill.height.toFloat() / 2f)
                }
                .start()
    }

    fun scheduleHide(reason: String, time: Long = parseHideTime(reason)) {
        hideHandler.hide(time, reason)
    }

    private fun parseHideTime(reason: String) =
            when (reason) {
                HiddenPillReasonManager.AUTO -> context.prefManager.autoHideTime.toLong()
                HiddenPillReasonManager.FULLSCREEN -> context.prefManager.hideInFullscreenTime.toLong()
                HiddenPillReasonManager.KEYBOARD -> context.prefManager.hideOnKeyboardTime.toLong()
                else -> throw IllegalArgumentException("$reason is not a valid hide reason")
            }

    fun showPill(reason: String?, forceShow: Boolean = false) {
        hideHandler.show(reason, forceShow)
    }

    /**
     * "Show" the pill by moving it back to its normal position
     */
    private fun showPillInternal(autoReasonToRemove: String?, forceShow: Boolean = false) {
        context.app.handler.post {
            if (autoReasonToRemove != null) hiddenPillReasons.remove(autoReasonToRemove)

            if (context.app.isPillShown()) {
                isPillHidingOrShowing = true
                val reallyForceNotAuto = hiddenPillReasons.isEmpty()

                if (reallyForceNotAuto) {
                    hideHandler.removeMessages(MSG_HIDE)
                }

                if (reallyForceNotAuto || forceShow) {
                    pill.animate()
                            .alpha(ALPHA_ACTIVE)
                            .setInterpolator(EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .withEndAction {
                                animateShow(!reallyForceNotAuto, autoReasonToRemove)
                            }
                            .apply {
                                if (isVertical) translationX(0f)
                                else translationY(0f)
                            }
                            .start()
                } else isPillHidingOrShowing = false
            }
        }
    }

    private fun animateShow(rehide: Boolean, reason: String?) {
        animator.show(DynamicAnimation.OnAnimationEndListener { _, _, _, _ ->
            handler?.postDelayed(Runnable {
                isHidden = false
                isPillHidingOrShowing = false

                if (rehide && reason != null) {
                    scheduleHide(if (reason == HiddenPillReasonManager.MANUAL)
                        hiddenPillReasons.getMostRecentReason() ?: return@Runnable else reason)
                }
            }, (if (getAnimationDurationMs() < 12) 12 else 0))
        })
    }

    fun changePillMargins(margins: Rect) {
        handler?.post {
            (pill.layoutParams as FrameLayout.LayoutParams).apply {
                bottomMargin = margins.bottom
                topMargin = margins.top
                leftMargin = margins.left
                rightMargin = margins.right

                pill.layoutParams = pill.layoutParams
            }
        }
    }

    fun getPillMargins(): Rect {
        val rect = Rect()

        (pill.layoutParams as FrameLayout.LayoutParams).apply {
            rect.bottom = bottomMargin
            rect.top = topMargin
            rect.left = leftMargin
            rect.right = rightMargin
        }

        return rect
    }

    fun toggleScreenOn(): Boolean {
        val hasScreenOn = params.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON ==
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

        if (hasScreenOn) params.flags = params.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.inv()
        else params.flags = params.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

        return try {
            updateLayout()
            !hasScreenOn
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Show a toast when the pill is hidden. Only shows once.
     */
    private fun showHiddenToast() {
        if (context.prefManager.showHiddenToast) {
            Toast.makeText(context, resources.getString(R.string.pill_hidden), Toast.LENGTH_LONG).show()
            context.prefManager.showHiddenToast = false
        }
    }

    fun updateLayout(params: WindowManager.LayoutParams = this.params) {
        handler?.post {
            try {
                wm.updateViewLayout(this, params)
            } catch (e: Exception) {
            }
        }
    }

    /**
     * Get the user-defined or default duration of the pill animations
     * @return the duration, in ms
     */
    fun getAnimationDurationMs(): Long {
        return context.prefManager.animationDurationMs.toLong()
    }

    /**
     * This is called twice to "flash" the pill when an action is performed
     */
    private fun animateActiveLayer(alpha: Float) {
        pill_tap_flash.apply {
            val alphaRatio = Color.alpha(context.prefManager.pillBGColor).toFloat() / 255f
            animate()
                    .setDuration(getAnimationDurationMs())
                    .alpha(alpha * alphaRatio)
                    .start()
        }
    }

    private fun adjustPillShadow() {
        try {
            //apparently setElevation() doesn't exist on some devices?
            pill.elevation = context.dpAsPx(if (context.prefManager.shouldShowShadow) 2 else 0).toFloat()
            (pill.layoutParams as FrameLayout.LayoutParams).apply {
                val shadow = context.prefManager.shouldShowShadow

                val r = if (shadow) context.dpAsPx(DEF_MARGIN_RIGHT_DP) else 0
                val l = if (shadow) context.dpAsPx(DEF_MARGIN_LEFT_DP) else 0
                val b = if (shadow) context.dpAsPx(DEF_MARGIN_BOTTOM_DP) else 0

                if (isVertical) {
                    topMargin = r
                    bottomMargin = l
                    if (is270Vertical) {
                        leftMargin = b
                        rightMargin = 0
                    } else {
                        rightMargin = b
                        leftMargin = 0
                    }
                } else {
                    rightMargin = r
                    leftMargin = l
                    bottomMargin = b
                }

                pill.layoutParams = this
            }
        } catch (e: NoSuchMethodError) {}
    }

    fun setMoveForKeyboard(move: Boolean) {
        val wasMoving = params.flags and WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM != 0

        if (move != wasMoving) {
            context.app.handler.post {
                if (move) {
                    params.flags = params.flags or
                            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                    params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                } else {
                    params.flags = params.flags and
                            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM.inv()
                    params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED
                }

                updateLayout()
            }
        }
    }

    /**
     * Vibrate for the specified duration
     * @param duration the desired duration
     */
    fun vibrate(duration: Long) {
        handler?.post {
            if (isSoundEffectsEnabled) {
                try {
                    playSoundEffect(SoundEffectConstants.CLICK)
                } catch (e: Exception) {
                }
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

    fun handleRotationOrAnchorUpdate() {
        handler?.post {
            verticalMode(isVertical)

            updatePositionAndDimens()

            adjustPillShadow()
            updateLargerHitbox()
        }
    }

    private fun verticalMode(enabled: Boolean) {
        var changed = false

        if (enabled) {
            val is270 = is270Vertical

            currentGestureDetector =
                    if (is270) vertical270GestureMansger else verticalGestureManager

            val newGrav = Gravity.CENTER or
                    if (is270) Gravity.LEFT else Gravity.RIGHT

            if (params.gravity != newGrav) {
                params.gravity = newGrav

                changed = true
            }

            if (!context.prefManager.dontMoveForKeyboard) {
                if (params.flags and WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM != 0) {
                    params.flags = params.flags and
                            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM.inv()
                    params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED

                    changed = true
                }
            }
        } else {
            currentGestureDetector = horizontalGestureManager

            val newGrav = Gravity.TOP or Gravity.CENTER

            if (params.gravity != newGrav) {
                params.gravity = newGrav

                changed = true
            }

            if (!context.prefManager.dontMoveForKeyboard) {
                if (params.flags and WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM == 0) {
                    params.flags = params.flags or
                            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                    params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE

                    changed = true
                }
            }
        }

        if (changed) updateLayout()

        currentGestureDetector.singleton.loadActionMap()
    }

    fun updateLargerHitbox() {
        val enabled = context.prefManager.largerHitbox
        val margins = getPillMargins()
        val m = resources.getDimensionPixelSize((if (enabled) R.dimen.pill_margin_top_large_hitbox else R.dimen.pill_margin_top_normal))

        var changed = false

        updatePositionAndDimens()

        if (isVertical) {
            if (is270Vertical) {
                if (margins.right != m) {
                    margins.right = m
                    margins.left = 0

                    changed = true
                }
            } else {
                if (margins.left != m) {
                    margins.left = m
                    margins.right = 0

                    changed = true
                }
            }

            if (margins.top != 0) {
                margins.top = 0

                changed = true
            }
        } else {
            if (margins.top != m) {
                margins.left = 0
                margins.top = m

                changed = true
            }
        }

        if (changed) changePillMargins(margins)
    }

    fun updatePositionAndDimens() {
        val newX = adjustedHomeX
        val newY = adjustedHomeY
        val newW = adjustedWidth
        val newH = adjustedHeight

        if ((newX != params.x
                        || newY != params.y
                        || newW != params.width
                        || newH != params.height)
                && !beingTouched
                && !isCarryingOutTouchAction) {
            params.x = newX
            params.y = newY
            params.width = newW
            params.height = newH
            updateLayout()
        }
    }

    inner class HideHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message?) {
            when (msg?.what) {
                MSG_HIDE -> {
                    val reason = msg.obj?.toString()
                    hidePill(reason != null, reason)
                }
                MSG_SHOW -> {
                    val reason = msg.obj?.toString()
                    showPillInternal(reason, msg.arg1 == 1)
                }
            }
        }

        fun hide(time: Long, reason: String? = null) {
            val msg = Message.obtain(this)
            msg.what = MSG_HIDE
            msg.obj = reason

            removeMessages(MSG_SHOW)
            removeMessages(MSG_SHOW, reason)

            if (!isHidden) sendMessageAtTime(msg, SystemClock.uptimeMillis() + time)
        }

        fun show(reason: String?, forceShow: Boolean = false) {
            val msg = Message.obtain(this)
            msg.what = MSG_SHOW
            msg.arg1 = if (forceShow) 1 else 0
            msg.obj = reason

            removeMessages(MSG_HIDE)
            removeMessages(MSG_SHOW, reason)

            if (isHidden) sendMessage(msg)
        }
    }
}