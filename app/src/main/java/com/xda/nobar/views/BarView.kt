package com.xda.nobar.views

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.SearchManager
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.*
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.provider.Settings
import android.speech.RecognizerIntent
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import com.android.internal.statusbar.IStatusBarService
import com.joaomgcd.taskerpluginlibrary.extensions.requestQuery
import com.topjohnwu.superuser.Shell
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.activities.IntentSelectorActivity
import com.xda.nobar.activities.RequestPermissionsActivity
import com.xda.nobar.activities.ScreenshotActivity
import com.xda.nobar.prefs.PrefManager
import com.xda.nobar.receivers.ActionReceiver
import com.xda.nobar.services.Actions
import com.xda.nobar.tasker.activities.EventConfigureActivity
import com.xda.nobar.tasker.updates.EventUpdate
import com.xda.nobar.util.*
import kotlinx.android.synthetic.main.pill.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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

        const val SCALE_NORMAL = 1.0f
        const val SCALE_MID = 0.7f
        const val SCALE_SMALL = 0.3f

        const val DEF_MARGIN_LEFT_DP = 2
        const val DEF_MARGIN_RIGHT_DP = 2
        const val DEF_MARGIN_BOTTOM_DP = 2

        val ENTER_INTERPOLATOR = DecelerateInterpolator()
        val EXIT_INTERPOLATOR = AccelerateInterpolator()

        private const val FIRST_SECTION = 0
        private const val SECOND_SECTION = 1
        private const val THIRD_SECTION = 2
    }
    private val app = context.applicationContext as App
    private val actionHolder = ActionHolder(context)
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val flashlightController =
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) FlashlightControllerMarshmallow(context)
        else FlashlightControllerLollipop(context)
    private val iStatusBarManager = IStatusBarService.Stub.asInterface(
            ServiceManager.checkService(Context.STATUS_BAR_SERVICE)
    )

    var view: View = View.inflate(context, R.layout.pill, this)
    var yHomeAnimator: ValueAnimator? = null
    var lastTouchTime = 0L
    var shouldReAddOnDetach = false

    val params = WindowManager.LayoutParams()
    val hiddenPillReasons = HiddenPillReasonManager()
    val gestureDetector = GestureManager()
    val rootActions = RootActions(context)

    private val wm: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val pool = Executors.newScheduledThreadPool(1)

    var isHidden = false
    var beingTouched = false
        set(value) {
            field = value
            if (!value) {
                if (needsScheduledHide) {
                    scheduleHide()
                    needsScheduledHide = false
                }
            }
        }
    var isCarryingOutTouchAction = false
    var isPillHidingOrShowing = false
    var isImmersive = false
    var immersiveNav: Boolean = false

    private var needsScheduledHide = false

    private val orientationEventListener by lazy {
        object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                currentDegree = orientation
            }
        }
    }

    private var currentDegree = 0
        set(value) {
            field = value
            orientationEventListener.disable()
            handler.postDelayed({
                val currentAcc = Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1)
                if (currentAcc == 0) {
                    val rotation = when (currentDegree) {
                        in 45..134 -> Surface.ROTATION_270
                        in 135..224 -> Surface.ROTATION_180
                        in 225..314 -> Surface.ROTATION_90
                        else -> Surface.ROTATION_0
                    }

                    Settings.System.putInt(context.contentResolver, Settings.System.USER_ROTATION, rotation)
                }
            }, 20)
        }

    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet)
    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr)
    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attributeSet, defStyleAttr, defStyleRes)

    init {
        alpha = ALPHA_GONE

        gestureDetector.loadActionMap()
        PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this)
        isSoundEffectsEnabled = app.prefManager.feedbackSound

        val layers = pill.background as LayerDrawable
        (layers.findDrawableByLayerId(R.id.background) as GradientDrawable).apply {
            setColor(app.prefManager.pillBGColor)
            cornerRadius = app.prefManager.pillCornerRadiusPx.toFloat()
        }
        (layers.findDrawableByLayerId(R.id.foreground) as GradientDrawable).apply {
            setStroke(Utils.dpAsPx(context, 1), app.prefManager.pillFGColor)
            cornerRadius = app.prefManager.pillCornerRadiusPx.toFloat()
        }

        (pill_tap_flash.background as GradientDrawable).apply {
            cornerRadius = app.prefManager.pillCornerRadiusPx.toFloat()
        }

        pill.elevation = Utils.dpAsPx(context, if (app.prefManager.shouldShowShadow) 2 else 0).toFloat()
        (pill.layoutParams as FrameLayout.LayoutParams).apply {
            val shadow = app.prefManager.shouldShowShadow
            marginEnd = if (shadow) Utils.dpAsPx(context, DEF_MARGIN_RIGHT_DP) else 0
            marginStart = if (shadow) Utils.dpAsPx(context, DEF_MARGIN_LEFT_DP) else 0
            bottomMargin = if (shadow) Utils.dpAsPx(context, DEF_MARGIN_BOTTOM_DP) else 0

            pill.layoutParams = this
        }
    }

    /**
     * Perform setup
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (app.prefManager.largerHitbox) {
            val margins = getPillMargins()
            margins.top = resources.getDimensionPixelSize(R.dimen.pill_margin_top_large_hitbox)
            changePillMargins(margins)
        }

        app.pillShown = true

        show(null)

        if (app.prefManager.autoHide) {
            hiddenPillReasons.add(HiddenPillReasonManager.AUTO)
            scheduleHide()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (gestureDetector.actionMap.keys.contains(key)) {
            gestureDetector.loadActionMap()
        }

        if (key == PrefManager.IS_ACTIVE) {
            if (!app.prefManager.isActive) {
                flashlightController.onDestroy()
            }
        }

        if (key != null && key.contains("use_pixels")) {
            params.width = app.prefManager.customWidth
            params.height = app.prefManager.customHeight
            params.x = getAdjustedHomeX()
            params.y = getAdjustedHomeY()
            updateLayout(params)
        }
        if (key == PrefManager.CUSTOM_WIDTH_PERCENT || key == PrefManager.CUSTOM_WIDTH) {
            params.width = app.prefManager.customWidth
            params.x = getAdjustedHomeX()
            updateLayout(params)
        }
        if (key == PrefManager.CUSTOM_HEIGHT_PERCENT || key == PrefManager.CUSTOM_HEIGHT) {
            params.height = app.prefManager.customHeight
            params.y = getAdjustedHomeY()
            updateLayout(params)
        }
        if (key == PrefManager.CUSTOM_Y_PERCENT || key == PrefManager.CUSTOM_Y) {
            params.y = getAdjustedHomeY()
            updateLayout(params)
        }
        if (key == PrefManager.CUSTOM_X_PERCENT || key == PrefManager.CUSTOM_X) {
            params.x = getAdjustedHomeX()
            updateLayout(params)
        }
        if (key == PrefManager.PILL_BG || key == PrefManager.PILL_FG) {
            val layers = pill.background as LayerDrawable
            (layers.findDrawableByLayerId(R.id.background) as GradientDrawable).apply {
                setColor(app.prefManager.pillBGColor)
            }
            (layers.findDrawableByLayerId(R.id.foreground) as GradientDrawable).apply {
                setStroke(Utils.dpAsPx(context, 1), app.prefManager.pillFGColor)
            }
        }
        if (key == PrefManager.SHOW_SHADOW) {
            val shadow = app.prefManager.shouldShowShadow
            pill.elevation = Utils.dpAsPx(context, if (shadow) 2 else 0).toFloat()

            (pill.layoutParams as FrameLayout.LayoutParams).apply {
                marginEnd = if (shadow) Utils.dpAsPx(context, DEF_MARGIN_RIGHT_DP) else 0
                marginStart = if (shadow) Utils.dpAsPx(context, DEF_MARGIN_LEFT_DP) else 0
                bottomMargin = if (shadow) Utils.dpAsPx(context, DEF_MARGIN_BOTTOM_DP) else 0

                pill.layoutParams = this
            }
        }
        if (key == PrefManager.STATIC_PILL) {
            if (app.prefManager.dontMoveForKeyboard) {
                params.flags = params.flags or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN and
                        WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM.inv()
                params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED
            } else {
                params.flags = params.flags or
                        WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM and
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN.inv()
                params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            }

            updateLayout(params)
        }
        if (key == PrefManager.AUDIO_FEEDBACK) {
            isSoundEffectsEnabled = app.prefManager.feedbackSound
        }
        if (key == PrefManager.PILL_CORNER_RADIUS) {
            val layers = pill.background as LayerDrawable
            (layers.findDrawableByLayerId(R.id.background) as GradientDrawable).apply {
                cornerRadius = Utils.dpAsPx(context, app.prefManager.pillCornerRadiusDp).toFloat()
            }
            (layers.findDrawableByLayerId(R.id.foreground) as GradientDrawable).apply {
                cornerRadius = Utils.dpAsPx(context, app.prefManager.pillCornerRadiusDp).toFloat()
            }
            (pill_tap_flash.background as GradientDrawable).apply {
                cornerRadius = app.prefManager.pillCornerRadiusPx.toFloat()
            }
        }
        if (key == PrefManager.LARGER_HITBOX) {
            val enabled = app.prefManager.largerHitbox
            val margins = getPillMargins()
            params.height = app.prefManager.customHeight
            params.y = getAdjustedHomeY()
            margins.top = resources.getDimensionPixelSize((if (enabled) R.dimen.pill_margin_top_large_hitbox else R.dimen.pill_margin_top_normal))
            updateLayout(params)
            changePillMargins(margins)
        }
        if (key == PrefManager.AUTO_HIDE_PILL) {
            if (app.prefManager.autoHide) {
                hiddenPillReasons.add(HiddenPillReasonManager.AUTO)
                if (!isHidden) scheduleHide()
            } else {
                if (isHidden) showPill(HiddenPillReasonManager.AUTO)
            }
        }
    }

    /**
     * Perform cleanup
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        if (shouldReAddOnDetach) {
            app.addBarInternal(false)
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

    private var hideHandle: ScheduledFuture<*>? = null

    /**
     * "Hide" the pill by moving it partially offscreen
     */
    fun hidePill(auto: Boolean, autoReason: String?, overrideBeingTouched: Boolean = false) {
        handler?.post {
            if (auto && autoReason == null) throw IllegalArgumentException("autoReason must not be null when auto is true")
            if (auto && autoReason != null) hiddenPillReasons.add(autoReason)

            if ((!beingTouched && !isCarryingOutTouchAction) || overrideBeingTouched) {
                if (app.isPillShown()) {
                    isPillHidingOrShowing = true

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
            } else {
                needsScheduledHide = true
                hideHandle?.cancel(true)
                hideHandle = null
            }
        }
    }

    private fun animateHide() {
        pill.animate()
                .translationY(pill.height.toFloat() / 2f)
                .alpha(ALPHA_HIDDEN)
                .setInterpolator(ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    pill.translationY = pill.height.toFloat() / 2f

                    isHidden = true

                    isPillHidingOrShowing = false
                }
                .start()
    }

    private fun scheduleHide(time: Long? = parseHideTime()) {
        if (time != null) {
            hideHandle = pool.schedule({
                if (System.currentTimeMillis() - lastTouchTime < time) {
                    scheduleHide()
                } else {
                    if (hiddenPillReasons.isNotEmpty()) {
                        hidePill(true, hiddenPillReasons.getMostRecentReason())
                    }
                }
            }, time, TimeUnit.MILLISECONDS)
        }
    }

    private fun parseHideTime(): Long? {
        val reason = hiddenPillReasons.getMostRecentReason()
        return when (reason) {
            HiddenPillReasonManager.AUTO -> app.prefManager.autoHideTime.toLong()
            HiddenPillReasonManager.FULLSCREEN -> app.prefManager.hideInFullscreenTime.toLong()
            HiddenPillReasonManager.KEYBOARD -> app.prefManager.hideOnKeyboardTime.toLong()
            else -> null
        }
    }

    /**
     * "Show" the pill by moving it back to its normal position
     */
    fun showPill(autoReasonToRemove: String?, forceShow: Boolean = false) {
        handler?.post {
            if (autoReasonToRemove != null) hiddenPillReasons.remove(autoReasonToRemove)
            if (app.isPillShown()) {
                isPillHidingOrShowing = true
                val reallyForceNotAuto = hiddenPillReasons.isEmpty()

                if ((reallyForceNotAuto) && hideHandle != null) {
                    hideHandle?.cancel(true)
                    hideHandle = null
                }

                if (!reallyForceNotAuto) {
                    scheduleHide()
                }

                if (reallyForceNotAuto || forceShow) {
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
                } else isPillHidingOrShowing = false
            }
        }
    }

    private fun animateShow() {
        val animDurScale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
        val time = (getAnimationDurationMs() * animDurScale)

        val navHeight = getAdjustedHomeY()
        val distance = (navHeight - params.y).absoluteValue
        val animator = ValueAnimator.ofInt(params.y, navHeight)

        if (distance == 0) {
            handler?.postDelayed(Runnable {
                isHidden = false
                isPillHidingOrShowing = false
            }, (if (getAnimationDurationMs() < 12) 12 else 0))
        } else {
            animator.interpolator = DecelerateInterpolator()
            animator.addUpdateListener {
                params.y = it.animatedValue.toString().toInt()
                updateLayout(params)
            }
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    handler?.postDelayed(Runnable {
                        isHidden = false
                        isPillHidingOrShowing = false
                    }, (if (getAnimationDurationMs() < 12) 12 else 0))
                }
            })
            animator.duration = (time * distance / 100f).toLong()
            animator.start()
        }
    }

    fun changePillMargins(margins: Rect) {
        handler?.post {
            (pill.layoutParams as FrameLayout.LayoutParams).apply {
                bottomMargin = margins.bottom
                topMargin = margins.top
                marginStart = margins.left
                marginEnd = margins.right

                pill.layoutParams = pill.layoutParams
            }
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
        return Utils.getRealScreenSize(context).y - app.prefManager.homeY - app.prefManager.customHeight
    }

    fun getZeroY(): Int {
        return Utils.getRealScreenSize(context).y - app.prefManager.customHeight
    }

    fun getAdjustedHomeX(): Int {
        val diff = try {
            val screenSize = Utils.getRealScreenSize(context)
            val frame = Rect().apply { getWindowVisibleDisplayFrame(this) }
            (frame.left + frame.right) - screenSize.x
        } catch (e: Exception) {
            0
        }

        return app.prefManager.homeX - if (immersiveNav && !app.prefManager.useTabletMode) (diff / 2f).toInt() else 0
    }

    fun toggleScreenOn(): Boolean {
        val hasScreenOn = params.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON == WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

        if (hasScreenOn) params.flags = params.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.inv()
        else params.flags = params.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

        return try {
            app.wm.updateViewLayout(this, params)
            !hasScreenOn
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Show a toast when the pill is hidden. Only shows once.
     */
    private fun showHiddenToast() {
        if (app.prefManager.showHiddenToast) {
            Toast.makeText(context, resources.getString(R.string.pill_hidden), Toast.LENGTH_LONG).show()
            app.prefManager.showHiddenToast = false
        }
    }
    
    fun updateLayout(params: WindowManager.LayoutParams) {
        handler?.post {
            try {
                wm.updateViewLayout(this, params)
            } catch (e: Exception) {}
        }
    }

    /**
     * Get the user-defined or default time the user must hold a swipe to perform the swipe and hold action
     * @return the time, in ms
     */
    private fun getHoldTime(): Int {
        return app.prefManager.holdTime
    }

    /**
     * Get the user-defined or default duration of the feedback vibration
     * @return the duration, in ms
     */
    private fun getVibrationDuration(): Int {
        return app.prefManager.vibrationDuration
    }

    /**
     * Get the user-defined or default duration of the pill animations
     * @return the duration, in ms
     */
    private fun getAnimationDurationMs(): Long {
        return app.prefManager.animationDurationMs.toLong()
    }

    /**
     * The animation for a single tap on the pill
     */
    private fun jiggleTap() {
        animate()
                .scaleX(SCALE_MID)
                .setInterpolator(ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    animate()
                            .scaleX(SCALE_NORMAL)
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
                .x(-width * (1 - SCALE_SMALL) / 2)
                .setInterpolator(ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    animate()
                            .scaleX(SCALE_NORMAL)
                            .x(0f)
                            .setInterpolator(EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .start()
                    animateActiveLayer(BRIGHTEN_INACTIVE)
                }
        animateActiveLayer(BRIGHTEN_ACTIVE)
    }

    /**
     * The animation for a swipe-right and hold on the pill
     */
    private fun jiggleRightHold() {
        animate()
                .scaleX(SCALE_SMALL)
                .x(width * (1 - SCALE_SMALL) / 2)
                .setInterpolator(ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    animate()
                            .scaleX(SCALE_NORMAL)
                            .x(0f)
                            .setInterpolator(EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .start()
                    animateActiveLayer(BRIGHTEN_INACTIVE)
                }
        animateActiveLayer(BRIGHTEN_ACTIVE)
    }

    private fun jiggleDownHold() {
        animate()
                .scaleY(SCALE_SMALL)
                .y(height * (1 - SCALE_SMALL) / 2)
                .setInterpolator(ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    animate()
                            .scaleY(SCALE_NORMAL)
                            .y(0f)
                            .setInterpolator(EXIT_INTERPOLATOR)
                            .setDuration(getAnimationDurationMs())
                            .start()
                    animateActiveLayer(BRIGHTEN_INACTIVE)
                }
                .start()
        animateActiveLayer(BRIGHTEN_ACTIVE)
    }

    /**
     * The animation for a long-press on the pill
     */
    private fun jiggleHold() {
        animate()
                .scaleX(SCALE_SMALL)
                .setInterpolator(ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    animate()
                            .scaleX(SCALE_NORMAL)
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
                .setInterpolator(ENTER_INTERPOLATOR)
                .setDuration(getAnimationDurationMs())
                .withEndAction {
                    animate()
                            .scaleY(SCALE_NORMAL)
                            .y(0f)
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
     * This is called twice to "flash" the pill when an action is performed
     */
    fun animateActiveLayer(alpha: Float) {
        pill_tap_flash.apply {
            val alphaRatio = Color.alpha(app.prefManager.pillBGColor).toFloat() / 255f
            animate()
                    .setDuration(getAnimationDurationMs())
                    .alpha(alpha * alphaRatio)
                    .start()
        }
    }

    /**
     * Vibrate for the specified duration
     * @param duration the desired duration
     */
    fun vibrate(duration: Long) {
        handler?.post {
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
    }

    /**
     * Manage all the gestures on the pill
     */
    inner class GestureManager {
        val actionMap = HashMap<String, Int>()
        private val tapLock = Any()

        private var isSwipeUp = false
        private var isSwipeLeft = false
        private var isSwipeRight = false
        private var isSwipeDown = false
        private var isOverrideTap = false
        private var wasHidden = false

        var isActing = false

        private var upHoldHandle: ScheduledFuture<*>? = null
        private var leftHoldHandle: ScheduledFuture<*>? = null
        private var rightHoldHandle: ScheduledFuture<*>? = null
        private var downHoldHandle: ScheduledFuture<*>? = null

        private var isRunningLongUp = false
        private var isRunningLongLeft = false
        private var isRunningLongRight = false
        private var isRunningLongDown = false

        private var oldEvent: MotionEvent? = null
        private var oldY = 0F
        private var oldX = 0F

        private var origX = 0F
        private var origY = 0F

        private var origAdjX = 0F
        private var origAdjY = 0F

        private val manager = GestureDetector(context, Detector())

        fun onTouchEvent(ev: MotionEvent?): Boolean {
            return handleTouchEvent(ev) || manager.onTouchEvent(ev)
        }

        private fun handleTouchEvent(ev: MotionEvent?): Boolean {
            val animDurScale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
            val time = (getAnimationDurationMs() * animDurScale)
            var ultimateReturn = false

            when (ev?.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchTime = System.currentTimeMillis()
                    wasHidden = isHidden
                    oldY = ev.rawY
                    oldX = ev.rawX
                    origX = ev.rawX
                    origY = ev.rawY
                    origAdjX = ev.x
                    origAdjY = ev.y
                    beingTouched = true
                    isCarryingOutTouchAction = true
                }

                MotionEvent.ACTION_UP -> {
                    beingTouched = false

                    if (wasHidden) {
                        isSwipeUp = false
                    }

                    upHoldHandle?.cancel(false)
                    upHoldHandle = null

                    leftHoldHandle?.cancel(false)
                    leftHoldHandle = null

                    rightHoldHandle?.cancel(false)
                    rightHoldHandle = null

                    downHoldHandle?.cancel(false)
                    downHoldHandle = null

                    if (isSwipeUp || (isRunningLongUp &&  getSectionedUpHoldAction(origAdjX) == actionHolder.typeNoAction)) {
                        sendAction(actionHolder.actionUp)
                    }

                    if (isSwipeLeft || (isRunningLongLeft && actionMap[actionHolder.actionLeftHold] == actionHolder.typeNoAction)) {
                        sendAction(actionHolder.actionLeft)
                    }

                    if (isSwipeRight || (isRunningLongRight && actionMap[actionHolder.actionRightHold] == actionHolder.typeNoAction)) {
                        sendAction(actionHolder.actionRight)
                    }

                    if (isSwipeDown || (isRunningLongDown && actionMap[actionHolder.actionDown] == actionHolder.typeNoAction)) {
                        sendAction(actionHolder.actionDown)
                    }

                    if (pill.translationX != 0f) {
                        pill.animate()
                                .translationX(0f)
                                .setDuration(getAnimationDurationMs())
                                .withEndAction {
                                    if (params.x == getAdjustedHomeX()) {
                                        isActing = false
                                        isSwipeLeft = false
                                        isSwipeRight = false
                                    }
                                }
                                .start()
                    }

                    if (pill.translationY != 0f && !isHidden && !isPillHidingOrShowing) {
                        pill.animate()
                                .translationY(0f)
                                .setDuration(getAnimationDurationMs())
                                .withEndAction {
                                    if (params.y == getAdjustedHomeY()) {
                                        isActing = false
                                        isSwipeDown = false
                                        isSwipeUp = false
                                    }
                                }
                                .start()
                    }

                    when {
                        params.y != getAdjustedHomeY() && !isHidden && !isPillHidingOrShowing -> {
                            val distance = (params.y - getAdjustedHomeY()).absoluteValue
                            if (yHomeAnimator != null) {
                                yHomeAnimator?.cancel()
                                yHomeAnimator = null
                            }
                            yHomeAnimator = ValueAnimator.ofInt(params.y, getAdjustedHomeY())
                            yHomeAnimator?.interpolator = DecelerateInterpolator()
                            yHomeAnimator?.addUpdateListener {
                                params.y = it.animatedValue.toString().toInt()
                                updateLayout(params)
                            }
                            yHomeAnimator?.addListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator?) {
                                    isActing = false
                                    isSwipeUp = false
                                    isSwipeDown = false
                                    isCarryingOutTouchAction = false

                                    yHomeAnimator = null
                                }

                                override fun onAnimationCancel(animation: Animator?) {
                                    onAnimationEnd(animation)
                                }
                            })
                            yHomeAnimator?.duration = (time * distance / 100f).toLong()
                            yHomeAnimator?.start()
                        }
                        params.x < getAdjustedHomeX() || params.x > getAdjustedHomeX() -> {
                            val distance = (params.x - getAdjustedHomeX()).absoluteValue
                            val animator = ValueAnimator.ofInt(params.x, getAdjustedHomeX())
                            animator.interpolator = DecelerateInterpolator()
                            animator.addUpdateListener {
                                params.x = it.animatedValue.toString().toInt()
                                updateLayout(params)
                            }
                            animator.addListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator?) {
                                    isActing = false
                                    isSwipeLeft = false
                                    isSwipeRight = false
                                    isCarryingOutTouchAction = false
                                }
                            })
                            animator.duration = (time * distance / 100f).toLong()
                            animator.start()
                        }
                        else -> {
                            isActing = false
                            isSwipeUp = false
                            isSwipeLeft = false
                            isSwipeRight = false
                            isSwipeDown = false
                            isCarryingOutTouchAction = false
                        }
                    }

                    isRunningLongRight = false
                    isRunningLongLeft = false
                    isRunningLongUp = false
                    isRunningLongDown = false

                    wasHidden = isHidden
                }
                MotionEvent.ACTION_MOVE -> {
                    ultimateReturn = handlePotentialSwipe(ev)

                    if (isSwipeUp && !isSwipeLeft && !isSwipeRight && !isSwipeDown) {
                        if (!isActing) isActing = true

                        val velocity = (oldY - ev.rawY)
                        oldY = ev.rawY

                        if (params.y > Utils.getRealScreenSize(context).y
                                - Utils.getRealScreenSize(context).y / 6 - app.prefManager.homeY
                                && getAnimationDurationMs() > 0) {
                            params.y -= (velocity / 2).toInt()
                            updateLayout(params)
                        }

                        if (upHoldHandle == null) {
                            upHoldHandle = pool.schedule({
                                isRunningLongUp = true
                                isSwipeUp = false
                                sendAction(actionHolder.actionUpHold)
                                upHoldHandle = null
                            }, getHoldTime().toLong(), TimeUnit.MILLISECONDS)
                        }
                    }

                    if (isSwipeDown && !isSwipeLeft && !isSwipeRight && !isSwipeUp) {
                        if (!isActing) isActing = true

                        val velocity = (oldY - ev.rawY)
                        oldY = ev.rawY

                        if (getAnimationDurationMs() > 0) {
                            params.y -= (velocity / 2).toInt()
                            updateLayout(params)
                        }

                        if (downHoldHandle == null) {
                            downHoldHandle = pool.schedule({
                                isRunningLongDown = true
                                isSwipeDown = false
                                sendAction(actionHolder.actionDownHold)
                                downHoldHandle = null
                            }, getHoldTime().toLong(), TimeUnit.MILLISECONDS)
                        }
                    }

                    if ((isSwipeLeft || isSwipeRight) && !isSwipeUp && !isSwipeDown) {
                        if (!isActing) isActing = true

                        val velocity = ev.rawX - oldX
                        oldX = ev.rawX

                        val halfScreen = Utils.getRealScreenSize(context).x / 2f
                        val leftParam = params.x - app.prefManager.customWidth.toFloat() / 2f
                        val rightParam = params.x + app.prefManager.customWidth.toFloat() / 2f

                        if (getAnimationDurationMs() > 0) {
                            when {
                                leftParam <= -halfScreen && !isSwipeRight -> {
                                    pill.translationX += velocity
                                }
                                rightParam >= halfScreen && !isSwipeLeft -> pill.translationX += velocity
                                else -> {
                                    params.x = params.x + (velocity / 2).toInt()
                                    updateLayout(params)
                                }
                            }
                        }

                        if (isSwipeLeft) {
                            if (leftHoldHandle == null) {
                                leftHoldHandle = pool.schedule({
                                    isRunningLongLeft = true
                                    isSwipeLeft = false
                                    sendAction(actionHolder.actionLeftHold)
                                    leftHoldHandle = null
                                }, getHoldTime().toLong(), TimeUnit.MILLISECONDS)
                            }
                        }

                        if (isSwipeRight) {
                            if (rightHoldHandle == null) {
                                rightHoldHandle = pool.schedule({
                                    isRunningLongRight = true
                                    isSwipeRight = false
                                    sendAction(actionHolder.actionRightHold)
                                    rightHoldHandle = null
                                }, getHoldTime().toLong(), TimeUnit.MILLISECONDS)
                            }
                        }
                    }
                }
            }

            oldEvent = MotionEvent.obtain(ev)

            return ultimateReturn
        }

        private fun handlePotentialSwipe(motionEvent: MotionEvent?): Boolean {
            if (motionEvent == null) return false

            val distanceX = motionEvent.rawX - origX
            val distanceY = motionEvent.rawY - origY
            val xThresh = app.prefManager.xThresholdPx
            val yThresh = app.prefManager.yThresholdPx

            return if (!isHidden && !isActing) {
                when {
                    distanceX < -xThresh && distanceY.absoluteValue <= distanceX.absoluteValue -> { //left swipe
                        isSwipeLeft = true
                        true
                    }
                    distanceX > xThresh && distanceY.absoluteValue <= distanceX.absoluteValue -> { //right swipe
                        isSwipeRight = true
                        true
                    }
                    distanceY > yThresh && distanceY.absoluteValue > distanceX.absoluteValue -> { //down swipe and down hold-swipe
                        isSwipeDown = true
                        true
                    }
                    distanceY < -yThresh && distanceY.absoluteValue > distanceX.absoluteValue -> { //up swipe and up hold-swipe
                        isSwipeUp = true
                        true
                    }
                    else -> false
                }
            } else if (isHidden
                    && !isActing
                    && distanceY < -yThresh
                    && distanceY.absoluteValue > distanceX.absoluteValue) { //up swipe
                if (isHidden && !isPillHidingOrShowing && !beingTouched) {
                    vibrate(getVibrationDuration().toLong())
                    showPill(null, true)
                }
                true
            } else false
        }

        private fun getSectionedUpHoldAction(x: Float): Int? {
            return if (!app.prefManager.sectionedPill) actionMap[actionHolder.actionUpHold]
            else when (getSection(x)) {
                FIRST_SECTION -> actionMap[actionHolder.actionUpHoldLeft]
                SECOND_SECTION -> actionMap[actionHolder.actionUpHoldCenter]
                else -> actionMap[actionHolder.actionUpHoldRight]
            }
        }

        private fun String.isEligible() = arrayListOf(
                actionHolder.actionUp,
                actionHolder.actionUpHold
        ).contains(this) && app.prefManager.sectionedPill

        private fun getSection(x: Float): Int {
            val third = app.prefManager.customWidth / 3f

            return when {
                x < third -> FIRST_SECTION
                x <= (2f * third) -> SECOND_SECTION
                else -> THIRD_SECTION
            }
        }

        private fun sendAction(action: String) {
            if (action.isEligible()) {
                when(getSection(origAdjX)) {
                    FIRST_SECTION -> sendActionInternal("${action}_left")
                    SECOND_SECTION -> sendActionInternal("${action}_center")
                    THIRD_SECTION -> sendActionInternal("${action}_right")
                }
            } else {
                sendActionInternal(action)
            }
        }

        /**
         * Parse the action index and broadcast to {@link com.xda.nobar.services.Actions}
         * @param key one of app.action*
         */
        private fun sendActionInternal(key: String) {
            handler?.post {
                val which = actionMap[key] ?: return@post

                if (which == actionHolder.typeNoAction) return@post

                if (isHidden || isPillHidingOrShowing) return@post

                vibrate(getVibrationDuration().toLong())

                if (key == actionHolder.actionDouble) handler?.postDelayed({ vibrate(getVibrationDuration().toLong()) }, getVibrationDuration().toLong())

                if (which == actionHolder.typeHide) {
                    if (key == actionHolder.actionUp || key == actionHolder.actionUpHold) {
                        yHomeAnimator?.cancel()
                        yHomeAnimator = null
                    }
                    hidePill(false, null, true)
                    return@post
                }

                when (key) {
                    actionHolder.actionDouble -> jiggleDoubleTap()
                    actionHolder.actionHold -> jiggleHold()
                    actionHolder.actionTap -> jiggleTap()
                    actionHolder.actionUpHold -> jiggleHoldUp()
                    actionHolder.actionLeftHold -> jiggleLeftHold()
                    actionHolder.actionRightHold -> jiggleRightHold()
                    actionHolder.actionDownHold -> jiggleDownHold()
                }

                if (key == actionHolder.actionUp || key == actionHolder.actionLeft || key == actionHolder.actionRight) {
                    animate(null, ALPHA_ACTIVE)
                }

                if (Utils.isAccessibilityAction(context, which)) {
                    if (app.prefManager.useRoot && Shell.rootAccess()) {
                        when (which) {
                            actionHolder.typeHome -> rootActions.home()
                            actionHolder.typeRecents -> rootActions.recents()
                            actionHolder.typeBack -> rootActions.back()
                            actionHolder.typeSwitch -> rootActions.switch()
                            actionHolder.typeSplit -> rootActions.split()
                            actionHolder.premTypePower -> Utils.runPremiumAction(context, App.isValidPremium) { rootActions.power() }
                        }
                    } else {
                        if (which == actionHolder.typeHome
                                && app.prefManager.useAlternateHome) {
                            handleAction(which, key)
                        } else {
                            val options = Bundle()
                            options.putInt(Actions.EXTRA_ACTION, which)
                            options.putString(Actions.EXTRA_GESTURE, key)

                            Actions.sendAction(context, Actions.ACTION, options)
                        }
                    }
                } else {
                    handleAction(which, key)
                }
            }
        }

        fun handleAction(which: Int, key: String) {
            GlobalScope.launch {
                when (which) {
                    actionHolder.typeAssist -> {
                        val assist = Intent(RecognizerIntent.ACTION_WEB_SEARCH)
                        assist.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                        try {
                            context.startActivity(assist)
                        } catch (e: Exception) {
                            assist.action = RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE

                            try {
                                context.startActivity(assist)
                            } catch (e: Exception) {
                                assist.action = Intent.ACTION_VOICE_ASSIST

                                try {
                                    context.startActivity(assist)
                                } catch (e: Exception) {
                                    assist.action = Intent.ACTION_VOICE_COMMAND

                                    try {
                                        context.startActivity(assist)
                                    } catch (e: Exception) {
                                        assist.action = Intent.ACTION_ASSIST

                                        try {
                                            context.startActivity(assist)
                                        } catch (e: Exception) {
                                            val searchMan = context.getSystemService(Context.SEARCH_SERVICE) as SearchManager

                                            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                                                try {
                                                    searchMan.launchAssist(null)
                                                } catch (e: Exception) {

                                                    searchMan.launchLegacyAssist(null, UserHandle.USER_CURRENT, null)
                                                }
                                            } else {
                                                val launchAssistAction = searchMan::class.java
                                                        .getMethod("launchAssistAction", Int::class.java, String::class.java, Int::class.java)
                                                launchAssistAction.invoke(searchMan, 1, null, UserHandle.USER_CURRENT)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    actionHolder.typeOhm -> {
                        val ohm = Intent("com.xda.onehandedmode.intent.action.TOGGLE_OHM")
                        ohm.setClassName("com.xda.onehandedmode", "com.xda.onehandedmode.receivers.OHMReceiver")
                        context.sendBroadcast(ohm)
                    }
                    actionHolder.typeHome -> {
                        val homeIntent = Intent(Intent.ACTION_MAIN)
                        homeIntent.addCategory(Intent.CATEGORY_HOME)
                        homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(homeIntent)
                    }
                    actionHolder.premTypePlayPause -> runPremiumAction {
                        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                    }
                    actionHolder.premTypePrev -> runPremiumAction {
                        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                    }
                    actionHolder.premTypeNext -> runPremiumAction {
                        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT))
                        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT))
                    }
                    actionHolder.premTypeSwitchIme -> runPremiumAction {
                        imm.showInputMethodPicker()
                    }
                    actionHolder.premTypeLaunchApp -> runPremiumAction {
                        val launchPackage = app.prefManager.getPackage(key)

                        if (launchPackage != null) {
                            val launch = Intent(Intent.ACTION_MAIN)
                            launch.addCategory(Intent.CATEGORY_LAUNCHER)
                            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            launch.`package` = launchPackage.split("/")[0]
                            launch.component = ComponentName(launch.`package`, launchPackage.split("/")[1])

                            try {
                                context.startActivity(launch)
                            } catch (e: Exception) {}
                        }
                    }
                    actionHolder.premTypeLaunchActivity -> runPremiumAction {
                        val activity = app.prefManager.getActivity(key)

                        val p = activity.split("/")[0]
                        val c = activity.split("/")[1]

                        val launch = Intent()
                        launch.component = ComponentName(p, c)
                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                        try {
                            context.startActivity(launch)
                        } catch (e: Exception) {}
                    }
                    actionHolder.premTypeLockScreen -> runPremiumAction {
                        runSystemSettingsAction {
                            if (app.prefManager.useRoot && Shell.rootAccess()) {
                                rootActions.lock()
                            } else {
                                ActionReceiver.turnScreenOff(context)
                            }
                        }
                    }
                    actionHolder.premTypeScreenshot -> runPremiumAction {
                        val screenshot = Intent(context, ScreenshotActivity::class.java)
                        screenshot.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(screenshot)
                    }
                    actionHolder.premTypeRot -> runPremiumAction {
                        runSystemSettingsAction {
                            orientationEventListener.enable()
                        }
                    }
                    actionHolder.premTypeTaskerEvent -> runPremiumAction {
                        EventConfigureActivity::class.java.requestQuery(context, EventUpdate(key))
                    }
                    actionHolder.typeToggleNav -> {
                        ActionReceiver.toggleNav(context)
                    }
                    actionHolder.premTypeFlashlight -> runPremiumAction {
                        flashlightController.flashlightEnabled = !flashlightController.flashlightEnabled
                    }
                    actionHolder.premTypeVolumePanel -> runPremiumAction {
                        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
                    }
                    actionHolder.premTypeBluetooth -> runPremiumAction {
                        val adapter = BluetoothAdapter.getDefaultAdapter()
                        if (adapter.isEnabled) adapter.disable() else adapter.enable()
                    }
                    actionHolder.premTypeWiFi -> runPremiumAction {
                        wifiManager.isWifiEnabled = !wifiManager.isWifiEnabled
                    }
                    actionHolder.premTypeIntent -> runPremiumAction {
                        val broadcast = IntentSelectorActivity.INTENTS[app.prefManager.getIntentKey(key)]
                        val type = broadcast?.which

                        try {
                            when (type) {
                                IntentSelectorActivity.ACTIVITY -> {
                                    broadcast.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(broadcast)
                                }
                                IntentSelectorActivity.SERVICE -> ContextCompat.startForegroundService(context, broadcast)
                                IntentSelectorActivity.BROADCAST -> context.sendBroadcast(broadcast)
                            }
                        } catch (e: SecurityException) {
                            when (broadcast?.action) {
                                MediaStore.ACTION_VIDEO_CAPTURE,
                                MediaStore.ACTION_IMAGE_CAPTURE -> {
                                    RequestPermissionsActivity.createAndStart(context,
                                            arrayOf(Manifest.permission.CAMERA),
                                            ComponentName(context, BarView::class.java),
                                            Bundle().apply {
                                                putInt(Actions.EXTRA_ACTION, which)
                                                putString(Actions.EXTRA_GESTURE, key)
                                            }
                                    )
                                }
                            }
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, R.string.unable_to_launch, Toast.LENGTH_SHORT).show()
                        }
                    }
                    actionHolder.premTypeBatterySaver -> {
                        runPremiumAction {
                            val current = Settings.Global.getInt(context.contentResolver, Settings.Global.LOW_POWER_MODE, 0)
                            Settings.Global.putInt(context.contentResolver, Settings.Global.LOW_POWER_MODE, if (current == 0) 1 else 0)
                        }
                    }
                    actionHolder.premTypeScreenTimeout -> {
                        runPremiumAction { ActionReceiver.toggleScreenOn(context) }
                    }
                    actionHolder.premTypeNotif -> runPremiumAction {
                        iStatusBarManager.expandNotificationsPanel()
                    }
                    actionHolder.premTypeQs -> runPremiumAction {
                        iStatusBarManager.expandSettingsPanel(null)
                    }
                    actionHolder.premTypeVibe -> {
                        //TODO: Implement
                    }
                    actionHolder.premTypeSilent -> {
                        //TODO: Implement
                    }
                    actionHolder.premTypeMute -> {
                        //TODO: Implement
                    }
                }
            }
        }

        private fun runNougatAction(action: () -> Unit) = Utils.runNougatAction(context, action)
        private fun runPremiumAction(action: () -> Unit) = Utils.runPremiumAction(context, App.isValidPremium, action)
        private fun runSystemSettingsAction(action: () -> Unit) = Utils.runSystemSettingsAction(context, action)

        /**
         * Load the user's custom gesture/action pairings; default values if a pairing doesn't exist
         */
        fun loadActionMap() {
            app.prefManager.getActionsList(actionMap)

            if (actionMap.values.contains(actionHolder.premTypeFlashlight)) {
                if (!flashlightController.isCreated) flashlightController.onCreate()
            } else {
                flashlightController.onDestroy()
            }
        }

        inner class Detector : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(ev: MotionEvent): Boolean {
                return if (actionMap[actionHolder.actionDouble] == actionHolder.typeNoAction && !isActing && !wasHidden) {
                    isOverrideTap = true
                    sendAction(actionHolder.actionTap)
                    isActing = false
                    true
                } else false
            }

            override fun onLongPress(ev: MotionEvent) {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val isPinned = Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1 && am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE

                if (!isHidden && !isActing) {
                    if (isPinned) {
                       if (app.prefManager.shouldUseOverscanMethod) app.showNav()
                    } else {
                        isActing = true
                        sendAction(actionHolder.actionHold)
                    }
                }
            }

            override fun onDoubleTap(ev: MotionEvent): Boolean {
                return if (!isHidden &&!isActing) {
                    isActing = true
                    sendAction(actionHolder.actionDouble)
                    true
                } else false
            }

            override fun onSingleTapConfirmed(ev: MotionEvent): Boolean {
                return if (!isOverrideTap && !isHidden) {
                    isActing = false

                    sendAction(actionHolder.actionTap)
                    true
                } else if (isHidden && !isPillHidingOrShowing) {
                    isOverrideTap = false
                    vibrate(getVibrationDuration().toLong())
                    showPill(null, true)
                    true
                } else {
                    isOverrideTap = false
                    false
                }
            }
        }
    }
}