package com.xda.nobar.views

import android.Manifest
import android.animation.Animator
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
import android.util.AttributeSet
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.dynamicanimation.animation.DynamicAnimation
import com.joaomgcd.taskerpluginlibrary.extensions.requestQuery
import com.topjohnwu.superuser.Shell
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

        const val DEF_MARGIN_LEFT_DP = 2
        const val DEF_MARGIN_RIGHT_DP = 2
        const val DEF_MARGIN_BOTTOM_DP = 2

        val ENTER_INTERPOLATOR = DecelerateInterpolator()
        val EXIT_INTERPOLATOR = AccelerateInterpolator()

        private const val FIRST_SECTION = 0
        private const val SECOND_SECTION = 1
        private const val THIRD_SECTION = 2

        private const val MSG_UP_HOLD = 0
        private const val MSG_LEFT_HOLD = 1
        private const val MSG_RIGHT_HOLD = 2
        private const val MSG_DOWN_HOLD = 3

        private const val MSG_HIDE = 0
        private const val MSG_SHOW = 1
    }


    private val actionHolder = ActionHolder.getInstance(context)
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val flashlightController =
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) FlashlightControllerMarshmallow(context)
        else FlashlightControllerLollipop(context)

    var view: View = View.inflate(context, R.layout.pill, this)
    var lastTouchTime = -1L
    var shouldReAddOnDetach = false

    val params = WindowManager.LayoutParams()
    val hiddenPillReasons = HiddenPillReasonManager()
    val gestureDetector = GestureManager()

    private val wm: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val animator = BarAnimator(this)

    private val hideThread = HandlerThread("NoBar-Hide").apply { start() }
    private val hideHandler = HideHandler(hideThread.looper)

    var isHidden = false
    var beingTouched = false
    var isCarryingOutTouchAction = false
    var isPillHidingOrShowing = false
    var isImmersive = false
    var immersiveNav = false

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
        isSoundEffectsEnabled = context.app.prefManager.feedbackSound

        val layers = pill.background as LayerDrawable
        (layers.findDrawableByLayerId(R.id.background) as GradientDrawable).apply {
            setColor(context.app.prefManager.pillBGColor)
            cornerRadius = context.app.prefManager.pillCornerRadiusPx.toFloat()
        }
        (layers.findDrawableByLayerId(R.id.foreground) as GradientDrawable).apply {
            setStroke(Utils.dpAsPx(context, 1), context.app.prefManager.pillFGColor)
            cornerRadius = context.app.prefManager.pillCornerRadiusPx.toFloat()
        }

        (pill_tap_flash.background as GradientDrawable).apply {
            cornerRadius = context.app.prefManager.pillCornerRadiusPx.toFloat()
        }

        pill.elevation = Utils.dpAsPx(context, if (context.app.prefManager.shouldShowShadow) 2 else 0).toFloat()
        (pill.layoutParams as FrameLayout.LayoutParams).apply {
            val shadow = context.app.prefManager.shouldShowShadow
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

        if (context.app.prefManager.largerHitbox) {
            val margins = getPillMargins()
            margins.top = resources.getDimensionPixelSize(R.dimen.pill_margin_top_large_hitbox)
            changePillMargins(margins)
        }

        context.app.pillShown = true

        show(null)

        if (context.app.prefManager.autoHide) {
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
            if (!context.app.prefManager.isActive) {
                flashlightController.onDestroy()
            }
        }

        if (key != null && key.contains("use_pixels")) {
            params.width = context.app.prefManager.customWidth
            params.height = context.app.prefManager.customHeight
            params.x = getAdjustedHomeX()
            params.y = getAdjustedHomeY()
            updateLayout(params)
        }
        if (key == PrefManager.CUSTOM_WIDTH_PERCENT || key == PrefManager.CUSTOM_WIDTH) {
            params.width = context.app.prefManager.customWidth
            params.x = getAdjustedHomeX()
            updateLayout(params)
        }
        if (key == PrefManager.CUSTOM_HEIGHT_PERCENT || key == PrefManager.CUSTOM_HEIGHT) {
            params.height = context.app.prefManager.customHeight
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
                setColor(context.app.prefManager.pillBGColor)
            }
            (layers.findDrawableByLayerId(R.id.foreground) as GradientDrawable).apply {
                setStroke(Utils.dpAsPx(context, 1), context.app.prefManager.pillFGColor)
            }
        }
        if (key == PrefManager.SHOW_SHADOW) {
            val shadow = context.app.prefManager.shouldShowShadow
            pill.elevation = Utils.dpAsPx(context, if (shadow) 2 else 0).toFloat()

            (pill.layoutParams as FrameLayout.LayoutParams).apply {
                marginEnd = if (shadow) Utils.dpAsPx(context, DEF_MARGIN_RIGHT_DP) else 0
                marginStart = if (shadow) Utils.dpAsPx(context, DEF_MARGIN_LEFT_DP) else 0
                bottomMargin = if (shadow) Utils.dpAsPx(context, DEF_MARGIN_BOTTOM_DP) else 0

                pill.layoutParams = this
            }
        }
        if (key == PrefManager.STATIC_PILL) {
            if (context.app.prefManager.dontMoveForKeyboard) {
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
            isSoundEffectsEnabled = context.app.prefManager.feedbackSound
        }
        if (key == PrefManager.PILL_CORNER_RADIUS) {
            val layers = pill.background as LayerDrawable
            (layers.findDrawableByLayerId(R.id.background) as GradientDrawable).apply {
                cornerRadius = Utils.dpAsPx(context, context.app.prefManager.pillCornerRadiusDp).toFloat()
            }
            (layers.findDrawableByLayerId(R.id.foreground) as GradientDrawable).apply {
                cornerRadius = Utils.dpAsPx(context, context.app.prefManager.pillCornerRadiusDp).toFloat()
            }
            (pill_tap_flash.background as GradientDrawable).apply {
                cornerRadius = context.app.prefManager.pillCornerRadiusPx.toFloat()
            }
        }
        if (key == PrefManager.LARGER_HITBOX) {
            val enabled = context.app.prefManager.largerHitbox
            val margins = getPillMargins()
            params.height = context.app.prefManager.customHeight
            params.y = getAdjustedHomeY()
            margins.top = resources.getDimensionPixelSize((if (enabled) R.dimen.pill_margin_top_large_hitbox else R.dimen.pill_margin_top_normal))
            updateLayout(params)
            changePillMargins(margins)
        }
        if (key == PrefManager.AUTO_HIDE_PILL) {
            if (context.app.prefManager.autoHide) {
                hiddenPillReasons.add(HiddenPillReasonManager.AUTO)
                if (!isHidden) scheduleHide()
            } else {
                if (isHidden) hideHandler.show(HiddenPillReasonManager.AUTO)
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
        handler?.post {
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
                scheduleHide()
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
            hideHandler.hide(time)
        }
    }

    fun scheduleHide(reason: String) {
        hideHandler.hide(reason)
    }

    private fun parseHideTime(): Long? {
        val reason = hiddenPillReasons.getMostRecentReason()
        return parseHideTimeNoThrow(reason)
    }

    private fun parseHideTimeNoThrow(reason: String) =
            try {
                parseHideTime(reason)
            } catch (e: IllegalArgumentException) {
                null
            }

    private fun parseHideTime(reason: String) =
            when (reason) {
                HiddenPillReasonManager.AUTO -> context.app.prefManager.autoHideTime.toLong()
                HiddenPillReasonManager.FULLSCREEN -> context.app.prefManager.hideInFullscreenTime.toLong()
                HiddenPillReasonManager.KEYBOARD -> context.app.prefManager.hideOnKeyboardTime.toLong()
                else -> throw IllegalArgumentException("$reason is not a valid hide reason")
            }

    fun showPill(reason: String?, forceShow: Boolean = false) {
        hideHandler.show(reason, forceShow)
    }

    /**
     * "Show" the pill by moving it back to its normal position
     */
    private fun showPillInternal(autoReasonToRemove: String?, forceShow: Boolean = false) {
        if (isHidden) {
            handler?.post {
                if (autoReasonToRemove != null) hiddenPillReasons.remove(autoReasonToRemove)
                if (context.app.isPillShown()) {
                    isPillHidingOrShowing = true
                    val reallyForceNotAuto = hiddenPillReasons.isEmpty()

                    if (reallyForceNotAuto) {
                        hideHandler.removeMessages(MSG_HIDE)
                    }

                    if (reallyForceNotAuto || forceShow) {
                        pill.animate()
                                .translationY(0f)
                                .alpha(ALPHA_ACTIVE)
                                .setInterpolator(EXIT_INTERPOLATOR)
                                .setDuration(getAnimationDurationMs())
                                .withEndAction {
                                    pill.translationY = 0f

                                    animateShow(!reallyForceNotAuto)
                                }
                                .start()
                    } else isPillHidingOrShowing = false
                }
            }
        }
    }

    private fun animateShow(rehide: Boolean) {
        animator.show(DynamicAnimation.OnAnimationEndListener { _, _, _, _ ->
            handler?.postDelayed(Runnable {
                if (rehide) scheduleHide()
                isHidden = false
                isPillHidingOrShowing = false
            }, (if (getAnimationDurationMs() < 12) 12 else 0))
        })
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
        return Utils.getRealScreenSize(context).y - context.app.prefManager.homeY - context.app.prefManager.customHeight
    }

    fun getZeroY(): Int {
        return Utils.getRealScreenSize(context).y - context.app.prefManager.customHeight
    }

    fun getAdjustedHomeX(): Int {
        val diff = try {
            val screenSize = Utils.getRealScreenSize(context)
            val frame = Rect().apply { getWindowVisibleDisplayFrame(this) }
            (frame.left + frame.right) - screenSize.x
        } catch (e: Exception) {
            0
        }

        return context.app.prefManager.homeX - if (immersiveNav && !context.app.prefManager.useTabletMode) (diff / 2f).toInt() else 0
    }

    fun toggleScreenOn(): Boolean {
        val hasScreenOn = params.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON == WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

        if (hasScreenOn) params.flags = params.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.inv()
        else params.flags = params.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

        return try {
            context.app.wm.updateViewLayout(this, params)
            !hasScreenOn
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Show a toast when the pill is hidden. Only shows once.
     */
    private fun showHiddenToast() {
        if (context.app.prefManager.showHiddenToast) {
            Toast.makeText(context, resources.getString(R.string.pill_hidden), Toast.LENGTH_LONG).show()
            context.app.prefManager.showHiddenToast = false
        }
    }
    
    fun updateLayout(params: WindowManager.LayoutParams = this.params) {
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
        return context.app.prefManager.holdTime
    }

    /**
     * Get the user-defined or default duration of the feedback vibration
     * @return the duration, in ms
     */
    private fun getVibrationDuration(): Int {
        return context.app.prefManager.vibrationDuration
    }

    /**
     * Get the user-defined or default duration of the pill animations
     * @return the duration, in ms
     */
    fun getAnimationDurationMs(): Long {
        return context.app.prefManager.animationDurationMs.toLong()
    }

    /**
     * This is called twice to "flash" the pill when an action is performed
     */
    private fun animateActiveLayer(alpha: Float) {
        pill_tap_flash.apply {
            val alphaRatio = Color.alpha(context.app.prefManager.pillBGColor).toFloat() / 255f
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
            if (isSoundEffectsEnabled) {
                playSoundEffect(SoundEffectConstants.CLICK)
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

    /**
     * Manage all the gestures on the pill
     */
    inner class GestureManager {
        val actionMap = HashMap<String, Int>()

        private var isSwipeUp = false
        private var isSwipeLeft = false
        private var isSwipeRight = false
        private var isSwipeDown = false
        private var isOverrideTap = false
        private var wasHidden = false

        var isActing = false

        private var isRunningLongUp = false
        private var isRunningLongLeft = false
        private var isRunningLongRight = false
        private var isRunningLongDown = false

        private var sentLongUp = false
        private var sentLongLeft = false
        private var sentLongRight = false
        private var sentLongDown = false

        private var oldEvent: MotionEvent? = null
        private var oldY = 0F
        private var oldX = 0F

        private var origX = 0F
        private var origY = 0F

        private var origAdjX = 0F
        private var origAdjY = 0F

        private val manager = GestureDetector(context, Detector())

        private val gestureThread = HandlerThread("NoBar-Gesture").apply { start() }
        private val gestureHandler = GestureHandler(gestureThread.looper)

        fun onTouchEvent(ev: MotionEvent?): Boolean {
            return handleTouchEvent(ev) || manager.onTouchEvent(ev)
        }

        private fun handleTouchEvent(ev: MotionEvent?): Boolean {
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
                    lastTouchTime = -1L

                    if (wasHidden) {
                        isSwipeUp = false
                    }

                    gestureHandler.removeMessages(MSG_UP_HOLD)
                    gestureHandler.removeMessages(MSG_LEFT_HOLD)
                    gestureHandler.removeMessages(MSG_RIGHT_HOLD)
                    gestureHandler.removeMessages(MSG_DOWN_HOLD)

                    if (isSwipeUp && !isRunningLongUp) {
                        sendAction(actionHolder.actionUp)
                    }

                    if (isSwipeLeft && !isRunningLongLeft) {
                        sendAction(actionHolder.actionLeft)
                    }

                    if (isSwipeRight && !isRunningLongRight) {
                        sendAction(actionHolder.actionRight)
                    }

                    if (isSwipeDown && !isRunningLongDown) {
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
                                        isCarryingOutTouchAction = false
                                    }
                                }
                                .start()
                    }

                    when {
                        params.y != getAdjustedHomeY() && !isHidden && !isPillHidingOrShowing -> {
                            animator.homeY(DynamicAnimation.OnAnimationEndListener { _, _, _, _ ->
                                isActing = false
                                isCarryingOutTouchAction = false
                            })
                        }
                        params.x < getAdjustedHomeX() || params.x > getAdjustedHomeX() -> {
                            animator.homeX(DynamicAnimation.OnAnimationEndListener { _, _, _, _ ->
                                isActing = false
                                isCarryingOutTouchAction = false
                            })
                        }
                        else -> {
                            isActing = false
                            isCarryingOutTouchAction = false
                        }
                    }

                    isRunningLongRight = false
                    isRunningLongLeft = false
                    isRunningLongUp = false
                    isRunningLongDown = false

                    sentLongRight = false
                    sentLongLeft = false
                    sentLongUp = false
                    sentLongDown = false

                    isSwipeUp = false
                    isSwipeLeft = false
                    isSwipeRight = false
                    isSwipeDown = false

                    wasHidden = isHidden
                }
                MotionEvent.ACTION_MOVE -> {
                    ultimateReturn = handlePotentialSwipe(ev)

                    if (isSwipeUp && !isSwipeLeft && !isSwipeRight && !isSwipeDown) {
                        if (!isActing) isActing = true

                        val velocity = (oldY - ev.rawY)
                        oldY = ev.rawY

                        if (params.y > Utils.getRealScreenSize(context).y
                                - Utils.getRealScreenSize(context).y / 6 - context.app.prefManager.homeY
                                && getAnimationDurationMs() > 0) {
                            params.y -= (velocity / 2).toInt()
                            updateLayout(params)
                        }

                        if (!sentLongUp) {
                            sentLongUp = true
                            gestureHandler.sendEmptyMessageAtTime(MSG_UP_HOLD,
                                    SystemClock.uptimeMillis() + getHoldTime().toLong())
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

                        if (!sentLongDown) {
                            sentLongDown = true
                            gestureHandler.sendEmptyMessageAtTime(MSG_DOWN_HOLD,
                                    SystemClock.uptimeMillis() + getHoldTime().toLong())
                        }
                    }

                    if ((isSwipeLeft || isSwipeRight) && !isSwipeUp && !isSwipeDown) {
                        if (!isActing) isActing = true

                        val velocity = ev.rawX - oldX
                        oldX = ev.rawX

                        val halfScreen = Utils.getRealScreenSize(context).x / 2f
                        val leftParam = params.x - context.app.prefManager.customWidth.toFloat() / 2f
                        val rightParam = params.x + context.app.prefManager.customWidth.toFloat() / 2f

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

                        if (isSwipeLeft && !sentLongLeft) {
                            sentLongLeft = true
                            gestureHandler.sendEmptyMessageAtTime(MSG_LEFT_HOLD,
                                    SystemClock.uptimeMillis() + getHoldTime().toLong())
                        }

                        if (isSwipeRight && !sentLongRight) {
                            sentLongRight = true
                            gestureHandler.sendEmptyMessageAtTime(MSG_RIGHT_HOLD,
                                    SystemClock.uptimeMillis() + getHoldTime().toLong())
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
            val xThresh = context.app.prefManager.xThresholdPx
            val yThresh = context.app.prefManager.yThresholdPx

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
                    hiddenPillReasons.remove(HiddenPillReasonManager.MANUAL)
                    hideHandler.show(null, true)
                }
                true
            } else false
        }

        private fun getSectionedUpHoldAction(x: Float): Int? {
            return if (!context.app.prefManager.sectionedPill) actionMap[actionHolder.actionUpHold]
            else when (getSection(x)) {
                FIRST_SECTION -> actionMap[actionHolder.actionUpHoldLeft]
                SECOND_SECTION -> actionMap[actionHolder.actionUpHoldCenter]
                else -> actionMap[actionHolder.actionUpHoldRight]
            }
        }

        private fun String.isEligible() = arrayListOf(
                actionHolder.actionUp,
                actionHolder.actionUpHold
        ).contains(this) && context.app.prefManager.sectionedPill

        private fun getSection(x: Float): Int {
            val third = context.app.prefManager.customWidth / 3f

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
         * @param key one of ActionHolder's variables
         */
        private fun sendActionInternal(key: String) {
            handler?.post {
                val which = actionMap[key] ?: return@post

                if (which == actionHolder.typeNoAction) return@post

                if (isHidden || isPillHidingOrShowing) return@post

                vibrate(getVibrationDuration().toLong())

                if (key == actionHolder.actionDouble) handler?.postDelayed({ vibrate(getVibrationDuration().toLong()) }, getVibrationDuration().toLong())

                if (which == actionHolder.typeHide) {
                    hidePill(false, null, true)
                    return@post
                }

                when (key) {
                    actionHolder.actionDouble -> animator.jiggleDoubleTap()
                    actionHolder.actionHold -> animator.jiggleHold()
                    actionHolder.actionTap -> animator.jiggleTap()
                    actionHolder.actionUpHold -> animator.jiggleHoldUp()
                    actionHolder.actionLeftHold -> animator.jiggleLeftHold()
                    actionHolder.actionRightHold -> animator.jiggleRightHold()
                    actionHolder.actionDownHold -> animator.jiggleDownHold()
                }

                if (key == actionHolder.actionUp || key == actionHolder.actionLeft || key == actionHolder.actionRight) {
                    animate(null, ALPHA_ACTIVE)
                }

                if (Utils.isAccessibilityAction(context, which)) {
                    if (context.app.prefManager.useRoot && Shell.rootAccess()) {
                        when (which) {
                            actionHolder.typeHome -> context.app.rootWrapper.actions?.goHome()
                            actionHolder.typeRecents -> context.app.rootWrapper.actions?.openRecents()
                            actionHolder.typeBack -> context.app.rootWrapper.actions?.goBack()

                            actionHolder.typeSwitch -> runNougatAction {
                                context.app.rootWrapper.actions?.switchApps()
                            }
                            actionHolder.typeSplit -> runNougatAction {
                                context.app.rootWrapper.actions?.splitScreen()
                            }

                            actionHolder.premTypePower -> runPremiumAction {
                                context.app.rootWrapper.actions?.openPowerMenu()
                            }
                            actionHolder.premTypeLockScreen -> runPremiumAction {
                                context.app.rootWrapper.actions?.lockScreen()
                            }
                        }
                    } else {
                        if (which == actionHolder.typeHome
                                && context.app.prefManager.useAlternateHome) {
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
                                assist.action = "android.intent.action.VOICE_ASSIST"

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
                                                    searchMan.launchAssist()
                                                } catch (e: Exception) {

                                                    searchMan.launchLegacyAssist()
                                                }
                                            } else {
                                                val launchAssistAction = searchMan::class.java
                                                        .getMethod("launchAssistAction", Int::class.java, String::class.java, Int::class.java)
                                                launchAssistAction.invoke(searchMan, 1, null, -2)
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
                        val launchPackage = context.app.prefManager.getPackage(key)

                        if (launchPackage != null) {
                            val launch = Intent(Intent.ACTION_MAIN)
                            launch.addCategory(Intent.CATEGORY_LAUNCHER)
                            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            launch.`package` = launchPackage.split("/")[0]
                            launch.component = ComponentName(launch.`package`!!, launchPackage.split("/")[1])

                            try {
                                context.startActivity(launch)
                            } catch (e: Exception) {}
                        }
                    }
                    actionHolder.premTypeLaunchActivity -> runPremiumAction {
                        val activity = context.app.prefManager.getActivity(key) ?: return@runPremiumAction

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
                            if (context.app.prefManager.useRoot
                                    && Shell.rootAccess()) {
                                context.app.rootWrapper.actions?.lockScreen()
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
                        val broadcast = IntentSelectorActivity.INTENTS[context.app.prefManager.getIntentKey(key)]
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
                            val current = Settings.Global.getInt(context.contentResolver, "low_power", 0)
                            Settings.Global.putInt(context.contentResolver, "low_power", if (current == 0) 1 else 0)
                        }
                    }
                    actionHolder.premTypeScreenTimeout -> {
                        runPremiumAction { ActionReceiver.toggleScreenOn(context) }
                    }
                    actionHolder.premTypeNotif -> runPremiumAction {
                        expandNotificationsPanel()
                    }
                    actionHolder.premTypeQs -> runPremiumAction {
                        expandSettingsPanel()
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
        private fun runPremiumAction(action: () -> Unit) = Utils.runPremiumAction(context, context.app.isValidPremium, action)
        private fun runSystemSettingsAction(action: () -> Unit) = Utils.runSystemSettingsAction(context, action)

        /**
         * Load the user's custom gesture/action pairings; default values if a pairing doesn't exist
         */
        fun loadActionMap() {
            context.app.prefManager.getActionsList(actionMap)

            if (actionMap.values.contains(actionHolder.premTypeFlashlight)) {
                if (!flashlightController.isCreated) flashlightController.onCreate()
            } else {
                flashlightController.onDestroy()
            }
        }

        @SuppressLint("HandlerLeak")
        private inner class GestureHandler(looper: Looper) : Handler(looper) {
            override fun handleMessage(msg: Message?) {
                when (msg?.what) {
                    MSG_UP_HOLD -> {
                        if (getSectionedUpHoldAction(origAdjX) != actionHolder.typeNoAction) {
                            isRunningLongUp = true
                            sendAction(actionHolder.actionUpHold)
                        }
                    }

                    MSG_LEFT_HOLD -> {
                        if (actionMap[actionHolder.actionLeftHold] != actionHolder.typeNoAction) {
                            isRunningLongLeft = true
                            sendAction(actionHolder.actionLeftHold)
                        }
                    }

                    MSG_RIGHT_HOLD -> {
                        if (actionMap[actionHolder.actionRightHold] != actionHolder.typeNoAction) {
                            isRunningLongRight = true
                            sendAction(actionHolder.actionRightHold)
                        }
                    }

                    MSG_DOWN_HOLD -> {
                        if (actionMap[actionHolder.actionDownHold] != actionHolder.typeNoAction) {
                            isRunningLongDown = true
                            sendAction(actionHolder.actionDownHold)
                        }
                    }
                }
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
                       if (context.app.prefManager.shouldUseOverscanMethod) context.app.showNav()
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
                    hideHandler.show(null, true)
                    true
                } else {
                    isOverrideTap = false
                    false
                }
            }
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

        fun hide(reason: String) {
            val alreadyContains = hiddenPillReasons.contains(reason)
            hiddenPillReasons.add(reason)
            val msg = Message.obtain(this)
            msg.what = MSG_HIDE
            msg.obj = reason
            if (!hasMessages(MSG_HIDE, reason)
                    && !isHidden
                    && !alreadyContains) sendMessageAtTime(msg, SystemClock.uptimeMillis() + parseHideTime(reason))
        }

        fun hide(time: Long) {
            sendEmptyMessageAtTime(MSG_HIDE, SystemClock.uptimeMillis() + time)
        }

        fun show(reason: String?, forceShow: Boolean = false) {
            val msg = Message.obtain(this)
            msg.what = MSG_SHOW
            msg.arg1 = if (forceShow) 1 else 0
            msg.obj = reason

            if (!hasMessages(MSG_SHOW, reason)
                    && isHidden) sendMessage(msg)
        }
    }
}