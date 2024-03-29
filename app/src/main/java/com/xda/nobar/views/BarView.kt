package com.xda.nobar.views

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.*
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import com.xda.nobar.R
import com.xda.nobar.util.*
import com.xda.nobar.util.IWindowManager
import com.xda.nobar.util.helpers.HiddenPillReasonManagerNew
import com.xda.nobar.util.helpers.bar.NewBarViewGestureManager
import kotlinx.android.synthetic.main.pill.view.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
        private const val MSG_FADE = 2
        private const val MSG_UNFADE = 3
    }

    internal val actionHolder = context.actionHolder

    var shouldReAddOnDetach = false

    val params = WindowManager.LayoutParams().apply {
        x = adjustedHomeX
        y = adjustedHomeY

        width = adjustedWidth
        height = adjustedHeight
        gravity = Gravity.CENTER or Gravity.TOP
        type = run {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP)
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            else WindowManager.LayoutParams.TYPE_PRIORITY_PHONE
        }
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        format = PixelFormat.TRANSLUCENT
        softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED

        if (context.prefManager.overlayNav) {
            flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        }
    }
    private val hiddenPillReasons = HiddenPillReasonManagerNew()
    private val fadedPillReasons = HiddenPillReasonManagerNew()

    /**
     * Get the user-defined or default duration of the pill animations
     * @return the duration, in ms
     */
    val animationDurationMs: Long
        get() = context.prefManager.animationDurationMs.toLong()

    val shouldAnimate: Boolean
        get() = animationDurationMs > 0

    val adjustedHomeY: Int
        get() = if (isVertical) (if (is270Vertical) 1 else -1) * anchoredHomeX else actualHomeY

    private val actualHomeY: Int
        get() = context.realScreenSize.y - context.prefManager.homeY - context.prefManager.customHeight

    val zeroY: Int
        get() = if (isVertical) 0 else context.realScreenSize.y - context.prefManager.customHeight

    val zeroX: Int
        get() = if (isVertical && isLandscape) {
            context.unadjustedRealScreenSize.x - context.prefManager.customHeight
        } else 0

    val adjustedHomeX: Int
        get() = if (isVertical) anchoredHomeY else actualHomeX

    val viewConfig = ViewConfiguration.get(context)!!

    private val actualHomeX: Int
        get() {
            return context.prefManager.homeX - if (immersiveNav && !context.prefManager.useTabletMode) {
                (when (cachedRotation) {
                    Surface.ROTATION_90 -> -IWindowManager.bottomOverscan
                    Surface.ROTATION_270 -> if (context.prefManager.useRot270Fix) -IWindowManager.topOverscan else IWindowManager.bottomOverscan
                    else -> 0
                } / 2f).toInt()
            } else 0
        }

    private val anchoredHomeX: Int
        get() {
            return context.prefManager.homeX - if (immersiveNav && context.prefManager.useTabletMode) {
                (when (cachedRotation) {
                    Surface.ROTATION_90,
                    Surface.ROTATION_270 -> -IWindowManager.bottomOverscan
                    else -> 0
                } / 2f).toInt()
            } else 0
        }

    private val anchoredHomeY: Int
        get() {
            return context.unadjustedRealScreenSize.x - context.prefManager.homeY - context.prefManager.customHeight
        }

    val adjustedWidth: Int
        get() = context.prefManager.run { if (isVertical) customHeight else customWidth }

    val adjustedHeight: Int
        get() = context.prefManager.run { if (isVertical) customWidth else customHeight }

    val isVertical: Boolean
        get() = context.prefManager.anchorPill
                && isLandscape

    val is90Vertical: Boolean
        get() = isVertical
                && cachedRotation == Surface.ROTATION_90

    val is270Vertical: Boolean
        get() = isVertical
                && cachedRotation == Surface.ROTATION_270

    private val currentGestureDetector: NewBarViewGestureManager = NewBarViewGestureManager(this)

    private val wm: WindowManager = context.app.wm
    val animator = BarAnimator(this)

    private val hideHandler = HideHandler(Looper.getMainLooper())

    var isHidden = false
    var beingTouched = false
    var isCarryingOutTouchAction = false
    var isPillHidingOrShowing = false
    private val isImmersive: Boolean
        get() = context.app.immersiveHelperManager.isFullImmersive()
    private val immersiveNav: Boolean
        get() = context.app.immersiveHelperManager.isNavImmersive()

    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet)
    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr)
    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attributeSet, defStyleAttr, defStyleRes)

    init {
        alpha = ALPHA_GONE
        View.inflate(context, R.layout.pill, this)

        isSoundEffectsEnabled = context.prefManager.feedbackSound
    }

    private var colorAnimation: ValueAnimator? = null
    private var previousColor = ContextCompat.getColor(context, R.color.pill_color)

    private val hiddenX: Float
        get() = (if (is270Vertical) -1 else 1) * pill.width.toFloat() / 2f
    private val hiddenY: Float
        get() = pill.height.toFloat() / 2f

    private fun updatePillColorsAndRadii() {
        val fgColor = context.prefManager.pillFGColor

        val dp = context.resources.getDimensionPixelSize(R.dimen.pill_border_stroke_width)
        val radius = context.prefManager.pillCornerRadiusPx.toFloat()

        try {
            (pill.background as GradientDrawable).apply {
                val auto = context.prefManager.autoPillBGColor
                val new = if (auto != 0) auto else context.prefManager.pillBGColor

                colorAnimation?.cancel()

                colorAnimation = ValueAnimator.ofArgb(previousColor, new)
                colorAnimation?.duration = animationDurationMs
                colorAnimation?.addUpdateListener {
                    val color = it.animatedValue.toString().toInt()

                    setColor(color)
                    previousColor = color
                }
                colorAnimation?.start()

                setStroke(dp, fgColor)
                cornerRadius = radius
            }
        } catch (e: Exception) {
            Log.e("NoBar", "error", e)
        }

//        (pill_tap_flash.background as GradientDrawable).apply {
//            cornerRadius = radius
//        }
//
//        (section_1_flash.background as GradientDrawable).apply {
//            cornerRadii = floatArrayOf(radius, radius, 0f, 0f, 0f, 0f, radius, radius)
//        }
//
//        (section_3_flash.background as GradientDrawable).apply {
//            cornerRadii = floatArrayOf(0f, 0f, radius, radius, radius, radius, 0f, 0f)
//        }
    }

    private fun updateDividers() = mainScope.launch {
        val dividerColor = context.prefManager.pillDividerColor

        val dp = context.resources.getDimensionPixelSize(R.dimen.pill_border_stroke_width)
        val pillWidth = context.prefManager.customWidth
        val splitPill = context.prefManager.sectionedPill

        val color = if (splitPill) dividerColor else Color.TRANSPARENT

//        pill_tap_flash.visibility = if (splitPill) View.GONE else View.VISIBLE

        divider_1.setBackgroundColor(color)
        divider_2.setBackgroundColor(color)

        val pos1Left = pillWidth / 3f - dp / 2f
        val pos2Left = pillWidth * 2f/3f - dp /2f

        val params = FrameLayout.LayoutParams(
                if (isVertical) FrameLayout.LayoutParams.MATCH_PARENT else dp,
                if (isVertical) dp else FrameLayout.LayoutParams.MATCH_PARENT
        )

        val flashWidth = (pillWidth / 3f - dp / 2f).toInt()

        val flashParams = FrameLayout.LayoutParams(
                if (isVertical) FrameLayout.LayoutParams.MATCH_PARENT else flashWidth,
                if (isVertical) flashWidth else FrameLayout.LayoutParams.MATCH_PARENT
        )

        divider_1.apply {
            layoutParams = params

            if (isVertical) {
                y = pos1Left
                x = dp.toFloat()
            } else {
                x = pos1Left
                y = dp.toFloat()
            }
        }
        divider_2.apply {
            layoutParams = params

            if (isVertical) {
                y = pos2Left
                x = dp.toFloat()
            } else {
                x = pos2Left
                y = dp.toFloat()
            }
        }

//        section_1_flash.apply {
//            layoutParams = flashParams
//            visibility = if (splitPill) View.VISIBLE else View.GONE
//
//            if (isVertical) {
//                y = 0f
//                x = dp.toFloat()
//            } else {
//                x = 0f
//                y = dp.toFloat()
//            }
//        }
//
//        section_2_flash.apply {
//            layoutParams = flashParams
//            visibility = if (splitPill) View.VISIBLE else View.GONE
//
//            if (isVertical) {
//                y = pos1Left + dp
//                x = dp.toFloat()
//            } else {
//                x = pos1Left + dp
//                y = dp.toFloat()
//            }
//        }
//
//        section_3_flash.apply {
//            layoutParams = flashParams
//            visibility = if (splitPill) View.VISIBLE else View.GONE
//
//            if (isVertical) {
//                y = pos2Left + dp
//                x = dp.toFloat()
//            } else {
//                x = pos2Left + dp
//                y = dp.toFloat()
//            }
//        }
    }

    private fun updateFlashColor() {
        val auto = context.prefManager.autoPillBGColor
        val bgColor = if (auto != 0) auto else context.prefManager.pillBGColor
        val hsl = FloatArray(3)

        ColorUtils.colorToHSL(bgColor, hsl)

        val newColor = ColorStateList.valueOf(resources.getColor(if (hsl[2] < 0.5) android.R.color.white else android.R.color.black))

        (pill_tap_flash.background as RippleDrawable).setColor(newColor)
        (section_1_flash.background as RippleDrawable).setColor(newColor)
        (section_2_flash.background as RippleDrawable).setColor(newColor)
        (section_3_flash.background as RippleDrawable).setColor(newColor)
    }

    fun onCreate() {
        //This is just to allow BarView to be lazy-init on the main thread from App
    }

    /**
     * Perform setup
     */
    override fun onAttachedToWindow() {
        context.app.pillShown = true

        if (context.prefManager.overlayNav && !context.prefManager.useTabletMode) context.prefManager.anchorPill = true

        updatePillColorsAndRadii()
        updateDividers()

        super.onAttachedToWindow()

        show(null)

        if (context.prefManager.autoHide) {
            addHideReason(HiddenPillReasonManagerNew.AUTO)
        }

        if (context.prefManager.autoFade) {
            addFadeReason(HiddenPillReasonManagerNew.AUTO)
        }

        handleRotationOrAnchorUpdate()
        updateFlashColor()

        updateHideStatus()
        updateFadeStatus()

        context.app.registerOnSharedPreferenceChangeListener(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return currentGestureDetector.onTouchEvent(event)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return true
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
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
                updateDividers()
            }

            PrefManager.ANCHOR_PILL -> {
                context.refreshScreenSize()
                handleRotationOrAnchorUpdate()
                updateDividers()
            }

            PrefManager.PILL_BG,
            PrefManager.PILL_FG,
            PrefManager.PILL_DIVIDER_COLOR,
            PrefManager.PILL_CORNER_RADIUS,
            PrefManager.AUTO_PILL_BG,
            PrefManager.SECTIONED_PILL -> {
                updatePillColorsAndRadii()
                updateDividers()
                updateFlashColor()
            }

            PrefManager.SHOW_SHADOW -> {
                adjustPillShadowAndHitbox()
            }

            PrefManager.AUDIO_FEEDBACK -> {
                isSoundEffectsEnabled = context.prefManager.feedbackSound

            }

            PrefManager.LARGER_HITBOX -> {
                adjustPillShadowAndHitbox()
            }

            PrefManager.AUTO_HIDE_PILL -> {
                if (context.prefManager.autoHide) {
                    addHideReason(HiddenPillReasonManagerNew.AUTO)
                } else {
                    removeHideReason(HiddenPillReasonManagerNew.AUTO)
                }
            }

            PrefManager.HIDE_IN_FULLSCREEN -> {
                if (context.prefManager.hideInFullscreen && isImmersive) {
                    addHideReason(HiddenPillReasonManagerNew.FULLSCREEN)
                } else {
                    removeHideReason(HiddenPillReasonManagerNew.FULLSCREEN)
                }
            }

            PrefManager.FADE_AFTER_SPECIFIED_DELAY -> {
                if (context.prefManager.autoFade) {
                    addFadeReason(HiddenPillReasonManagerNew.AUTO)
                } else {
                    removeFadeReason(HiddenPillReasonManagerNew.AUTO)
                }
            }

            PrefManager.OVERLAY_NAV,
            PrefManager.HIDE_NAV -> {
                val overlayNav = context.prefManager.overlayNav
                setOverlayNav(overlayNav)
                if (overlayNav && !context.prefManager.useTabletMode) context.prefManager.anchorPill = true
            }
            PrefManager.OVERLAY_NAV_BLACKOUT -> {
                context.app.postAction {
                    it.remBar()

                    mainHandler.postDelayed({ it.addBarAndBlackout() }, 100)
                }
            }
        }
    }

//    override fun onConfigurationChanged(newConfig: Configuration?) {
//        super.onConfigurationChanged(newConfig)
//
//        forceActionUp()
//    }

    /**
     * Perform cleanup
     */
    override fun onDetachedFromWindow() {
        context.app.pillShown = false

        super.onDetachedFromWindow()

        if (shouldReAddOnDetach) {
            context.app.addBarInternal(false)
            shouldReAddOnDetach = false
        }

        hideHandler.isFaded = false

        forceActionUp()
    }

    fun forceActionUp() {
        mainScope.launch {
            currentGestureDetector.handleActionUp(true)
        }
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
        animate().alpha(alpha).setDuration(animationDurationMs)
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

    fun addHideReason(reason: String) {
        hiddenPillReasons.addReason(reason)
    }

    fun removeHideReason(reason: String, forceShow: Boolean = false) {
        hiddenPillReasons.removeReason(reason)
        if (forceShow) hideHandler.updateHideStatus(forceShow)
    }

    fun addFadeReason(reason: String) {
        fadedPillReasons.addReason(reason)
    }

    fun removeFadeReason(reason: String, forceUnfade: Boolean = false) {
        fadedPillReasons.removeReason(reason)
        if (forceUnfade) hideHandler.updateFadeStatus(forceUnfade)
    }

    private fun parseHideTime(reason: String) =
            when (reason) {
                HiddenPillReasonManagerNew.AUTO -> context.prefManager.autoHideTime.toLong()
                HiddenPillReasonManagerNew.FULLSCREEN -> context.prefManager.hideInFullscreenTime.toLong()
                HiddenPillReasonManagerNew.KEYBOARD -> context.prefManager.hideOnKeyboardTime.toLong()
                HiddenPillReasonManagerNew.MANUAL -> 0
                else -> throw IllegalArgumentException("$reason is not a valid hide reason")
            }

    private fun parseFadeTime(reason: String) =
            when (reason) {
                HiddenPillReasonManagerNew.AUTO -> context.prefManager.autoFadeTime
                HiddenPillReasonManagerNew.FULLSCREEN -> context.prefManager.fullscreenFadeTime
                HiddenPillReasonManagerNew.MANUAL -> 0
                else -> throw java.lang.IllegalArgumentException("$reason is not a valid fade reason")
            }

    fun animatePillToHome(xCompletionListener: () -> Unit, yCompletionListener: () -> Unit) {
        val xAnim = SpringAnimation(pill, SpringAnimation.TRANSLATION_X, 0f)
        xAnim.addEndListener { _, _, _, _ ->
            xCompletionListener.invoke()
        }

        val yAnim = SpringAnimation(pill, SpringAnimation.TRANSLATION_Y, 0f)
        yAnim.addEndListener { _, _, _, _ ->
            yCompletionListener.invoke()
        }

        xAnim.start()
        yAnim.start()
    }

    fun changePillMargins(margins: Rect) {
        mainScope.launch {
            (pill.layoutParams as LayoutParams).apply {
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

        (pill.layoutParams as LayoutParams).apply {
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

    fun updateLayout(params: WindowManager.LayoutParams = this.params) = mainScope.launch {
        try {
            wm.updateViewLayout(this@BarView, params)
        } catch (e: Exception) {}
    }

//    /**
//     * This is called twice to "flash" the pill when an action is performed
//     */
//    private fun animateActiveLayer(alpha: Float) {
//        pill_tap_flash.apply {
//            val alphaRatio = Color.alpha(context.prefManager.pillBGColor).toFloat() / 255f
//            animate()
//                    .setDuration(getAnimationDurationMs())
//                    .alpha(alpha * alphaRatio)
//                    .start()
//        }
//    }

    private fun adjustPillShadowAndHitbox() = logicScope.launch {
        val largerHitbox = context.prefManager.largerHitbox
        val shadow = context.prefManager.shouldShowShadow

        val margins = getPillMargins()

        val hitboxM = resources.getDimensionPixelSize((if (largerHitbox) R.dimen.pill_margin_top_large_hitbox else R.dimen.pill_margin_top_normal))
        val r = if (shadow) context.dpAsPx(DEF_MARGIN_RIGHT_DP) else 0
        val l = if (shadow) context.dpAsPx(DEF_MARGIN_LEFT_DP) else 0
        val b = if (shadow) context.dpAsPx(DEF_MARGIN_BOTTOM_DP) else 0

        var hitboxChanged = false

        if (isVertical) {
            if (is270Vertical) {
                if (margins.right != hitboxM
                        || margins.left != b) {
                    margins.right = hitboxM
                    margins.left = b

                    hitboxChanged = true
                }
            } else {
                if (margins.left != hitboxM
                        || margins.right != b) {
                    margins.left = hitboxM
                    margins.right = b

                    hitboxChanged = true
                }
            }

            if (margins.top != r
                    || margins.bottom != l) {
                margins.top = r
                margins.bottom = l

                hitboxChanged = true
            }
        } else {
            if (margins.top != hitboxM
                    || margins.left != l
                    || margins.bottom != b
                    || margins.right != r) {
                margins.left = l
                margins.bottom = b
                margins.top = hitboxM
                margins.right = r

                hitboxChanged = true
            }
        }

        if (hitboxChanged) {
            mainScope.launch {
                pill.elevation = context.dpAsPx(if (context.prefManager.shouldShowShadow) 2 else 0).toFloat()
                changePillMargins(margins)
            }
        }

        updatePositionAndDimens().join()
    }

    private fun setOverlayNav(overlay: Boolean) {
        val old = params.flags and WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS != 0

        if (overlay != old) {
            mainScope.launch {
                if (overlay) {
                    params.flags = params.flags or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    context.app.postAction {
                        it.remBar()
                        mainHandler.postDelayed({
                            it.addBarAndBlackout()
                        }, 100)
                    }
                } else {
                    params.flags = params.flags and
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS.inv()
                    context.app.postAction { it.remBlackout() }
                    updateLayout()
                }
            }
        }
    }

    /**
     * Vibrate for the specified duration
     * @param duration the desired duration
     */
    fun vibrate(duration: Long) {
        mainScope.launch {
            if (isSoundEffectsEnabled) {
                try {
                    playSoundEffect(SoundEffectConstants.CLICK)
                } catch (e: Exception) {}
            }
        }

        if (duration > 0) {
            val vibrator = context.vibrator

            try {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
                    vibrator.vibrate(VibrationEffect.createOneShot(duration, context.prefManager.vibrationStrength))
                } else {
                    vibrator.vibrate(duration)
                }
            } catch (e: NullPointerException) {}
        }
    }

    fun handleRotationOrAnchorUpdate() = logicScope.launch {
        verticalMode(isVertical).join()
        adjustPillShadowAndHitbox().join()
        updateDividers().join()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)

        if (isHidden && !isPillHidingOrShowing) {
            if (isVertical) pill.translationX = hiddenX
            else pill.translationY = hiddenY
        }
    }

    private fun verticalMode(enabled: Boolean) = logicScope.launch {
        var changed = false
        if (enabled) {
            val is270 = is270Vertical

            val newGrav = Gravity.CENTER or
                    if (!is270) Gravity.LEFT else Gravity.RIGHT

            if (params.gravity != newGrav) {
                params.gravity = newGrav

                changed = true
            }
        } else {
            val newGrav = Gravity.TOP or Gravity.CENTER

            if (params.gravity != newGrav) {
                params.gravity = newGrav

                changed = true
            }
        }

        if (changed) {
            updateLayout().join()
        }
    }

    private val positionLock = Mutex()

    fun updatePositionAndDimens() = logicScope.launch {
        positionLock.withLock {
            val newX = if (isVertical && isHidden) zeroX else adjustedHomeX
            val newY = if (!isVertical && isHidden) zeroY else adjustedHomeY
            val newW = adjustedWidth
            val newH = adjustedHeight

            if ((newX != params.x
                            || newY != params.y
                            || newW != params.width
                            || newH != params.height)) {
                params.x = newX
                params.y = newY
                params.width = newW
                params.height = newH
                updateLayout().join()
            }
        }
    }

    fun updateHideStatus() {
        hideHandler.updateHideStatus()
    }

    fun updateFadeStatus() {
        hideHandler.updateFadeStatus()
    }

    inner class HideHandler(looper: Looper) : Handler(looper) {
        var isFaded = false

        override fun handleMessage(msg: Message?) {
            when (msg?.what) {
                MSG_HIDE -> {
                    if (((!beingTouched && !isCarryingOutTouchAction) || msg.arg1 == 1) && !isPillHidingOrShowing) {
                        if (context.app.isPillShown()) {
                            isPillHidingOrShowing = true

                            animator.hide(DynamicAnimation.OnAnimationEndListener { _, _, _, _ ->
                                pill.animate()
                                        .alpha(ALPHA_HIDDEN)
                                        .setInterpolator(ENTER_INTERPOLATOR)
                                        .setDuration(animationDurationMs)
                                        .withEndAction {
                                            isHidden = true

                                            isPillHidingOrShowing = false
                                        }
                                        .apply {
                                            if (isVertical) translationX(hiddenX)
                                            else translationY(hiddenY)
                                        }
                                        .start()
                            })

                            showHiddenToast()
                        }
                    }
                }
                MSG_SHOW -> {
                    if (!isPillHidingOrShowing) {
                        if (context.app.isPillShown()) {
                            isPillHidingOrShowing = true
                            val reallyForceNotAuto = hiddenPillReasons.isEmpty()

                            if (reallyForceNotAuto) {
                                hideHandler.removeMessages(MSG_HIDE)
                            }

                            if (reallyForceNotAuto || msg.arg1 == 1) {
                                synchronized(this@BarView) {
                                    pill.animate()
                                            .alpha(ALPHA_ACTIVE)
                                            .setInterpolator(EXIT_INTERPOLATOR)
                                            .setDuration(animationDurationMs)
                                            .withEndAction {
                                                animator.home(DynamicAnimation.OnAnimationEndListener { _, _, _, _ ->
                                                    mainHandler.postDelayed(Runnable {
                                                        isHidden = false
                                                        isPillHidingOrShowing = false
                                                        hideHandler.updateHideStatus()
                                                    }, (if (animationDurationMs < 12) 12 else 0))
                                                })
                                            }
                                            .apply {
                                                translationX(0f)
                                                translationY(0f)
                                            }
                                            .start()
                                }
                            } else isPillHidingOrShowing = false
                        }
                    }
                }

                MSG_FADE -> {
                    if (!isFaded && !beingTouched) {
                        animate()
                                .alpha(context.prefManager.fadeOpacity / 100f)
                                .setDuration(context.prefManager.fadeDuration)
                                .withEndAction {
                                    isFaded = true
                                }
                                .start()
                    }
                }

                MSG_UNFADE -> {
                    if (isFaded) {
                        animate()
                                .alpha(ALPHA_ACTIVE)
                                .setDuration(context.prefManager.fadeDuration)
                                .withEndAction {
                                    isFaded = false
                                    updateFadeStatus()
                                }
                                .start()
                    }
                }
            }
        }

        fun updateHideStatus(forceShow: Boolean = false) {
            synchronized(hiddenPillReasons) {
                if (hiddenPillReasons.isEmpty() || forceShow) {
                    show(true)

                    if (hiddenPillReasons.isEmpty()) removeMessages(MSG_HIDE)
                } else {
                    val reason = hiddenPillReasons.getMostRecentReason()!!

                    hide(parseHideTime(reason), reason == HiddenPillReasonManagerNew.MANUAL)
                }
            }
        }

        fun updateFadeStatus(forceUnfade: Boolean = false) {
            synchronized(fadedPillReasons) {
                if (fadedPillReasons.isEmpty() || forceUnfade) {
                    unfade()

                    if (fadedPillReasons.isEmpty()) removeMessages(MSG_FADE)
                } else {
                    val reason = fadedPillReasons.getMostRecentReason()!!

                    fade(parseFadeTime(reason))
                }
            }
        }

        private fun hide(time: Long, overrideBeingTouched: Boolean) {
            val msg = Message.obtain(this)
            msg.what = MSG_HIDE
            msg.arg1 = if (overrideBeingTouched) 1 else 0

            if (!isHidden) sendMessageAtTime(msg, SystemClock.uptimeMillis() + time)
        }

        private fun show(forceShow: Boolean = false) {
            val msg = Message.obtain(this)
            msg.what = MSG_SHOW
            msg.arg1 = if (forceShow) 1 else 0

            if (isHidden) sendMessage(msg)
        }

        private fun fade(time: Long) {
            val msg = Message.obtain(this)
            msg.what = MSG_FADE

            removeMessages(MSG_UNFADE)

            if (!isFaded && !hasMessages(MSG_FADE)) sendMessageAtTime(msg, SystemClock.uptimeMillis() + time)
        }

        private fun unfade(forceUnfade: Boolean = false) {
            val msg = Message.obtain(this)
            msg.what = MSG_UNFADE
            msg.arg1 = if (forceUnfade) 1 else 0

            removeMessages(MSG_FADE)

            if (isFaded && !hasMessages(MSG_UNFADE)) sendMessage(msg)
        }
    }
}