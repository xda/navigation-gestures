package com.xda.nobar.util

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.preference.PreferenceManager
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.xda.nobar.R
import com.xda.nobar.adapters.info.ShortcutInfo
import com.xda.nobar.data.ColoredAppData
import com.xda.nobar.util.helpers.GsonIntentHandler
import com.xda.nobar.util.helpers.GsonUriHandler
import java.util.*
import kotlin.collections.HashSet
import kotlin.collections.set

class PrefManager private constructor(context: Context) : ContextWrapper(context) {
    companion object {
        /* Booleans */
        const val IS_ACTIVE = "is_active"
        const val HIDE_NAV = "hide_nav"
        const val ROT270_FIX = "rot270_fix"
        const val ROT180_FIX = "rot180_fix"
        const val TABLET_MODE = "tablet_mode"
        const val ENABLE_IN_CAR_MODE = "enable_in_car_mode"
        const val USE_IMMERSIVE_MODE_WHEN_NAV_HIDDEN = "use_immersive_mode_when_nav_hidden"
        const val HIDE_PILL_ON_KEYBOARD = "hide_pill_on_keyboard"
        const val FULL_OVERSCAN = "full_overscan"
        const val VALID_PREM = "valid_prem"
        const val USE_PIXELS_WIDTH = "use_pixels_width"
        const val USE_PIXELS_HEIGHT = "use_pixels_height"
        const val USE_PIXELS_X = "use_pixels_x"
        const val USE_PIXELS_Y = "use_pixels_y"
        const val SHOW_SHADOW = "show_shadow"
        const val STATIC_PILL = "static_pill"
        const val AUDIO_FEEDBACK = "audio_feedback"
        const val LARGER_HITBOX = "larger_hitbox"
        const val AUTO_HIDE_PILL = "auto_hide_pill"
        const val SHOW_HIDDEN_TOAST = "show_hidden_toast"
        const val ENABLE_CRASHLYTICS_ID = "enable_crashlytics_id"
        const val ALTERNATE_HOME = "alternate_home"
        const val FIRST_RUN = "first_run"
        const val HIDE_IN_FULLSCREEN = "hide_in_fullscreen"
        const val ORIG_NAV_IN_IMMERSIVE = "orig_nav_in_immersive"
        const val SECTIONED_PILL = "sectioned_pill"
        const val LOCKSCREEN_OVERSCAN = "lockscreen_overscan"
        const val SHOW_NAV_WITH_KEYBOARD = "keyboard_nav"
        const val CONFIRMED_SKIP_WSS = "has_confirmed_skip_wss"
        const val ANCHOR_PILL = "anchor_pill"
        const val FLASHLIGHT_COMPAT = "flashlight_compat"
        const val CUSTOM_VIBRATION_STRENGTH = "custom_vibration_strength"
        const val FADE_AFTER_SPECIFIED_DELAY = "fade_after_specified_delay"
        const val FADE_IN_FULLSCREEN_APPS = "fade_in_fullscreen_apps"
        const val HIDE_BETA_PROMPT = "hide_beta_prompt"
        const val ENABLE_ANALYTICS = "enable_analytics"
        const val HIDE_ON_LOCKSCREEN = "hide_on_lockscreen"
        const val HIDE_ON_PERMISSIONS = "hide_on_permissions"
        const val HIDE_ON_INSTALLER = "hide_on_installer"
        const val OVERLAY_NAV = "overlay_nav"
        const val KEEP_ALIVE = "keep_alive"
        const val OVERLAY_NAV_BLACKOUT = "overlay_nav_blackout"
        const val IMPROVED_APP_CHANGE_DETECTION = "improved_app_change_detection"
        const val ALLOW_REPEAT_LONG = "allow_repeat_long"
        const val LEFT_SIDE_GESTURE = "left_side_gesture"
        const val RIGHT_SIDE_GESTURE = "right_side_gesture"
        const val SIDE_GESTURE_USE_PILL_COLOR = "side_gesture_use_pill_color"

        /* Numbers */
        const val CUSTOM_WIDTH_PERCENT = "custom_width_percent"
        const val CUSTOM_WIDTH = "custom_width"
        const val CUSTOM_HEIGHT_PERCENT = "custom_height_percent"
        const val CUSTOM_HEIGHT = "custom_height"
        const val CUSTOM_X_PERCENT = "custom_x_percent"
        const val CUSTOM_X = "custom_x"
        const val CUSTOM_Y_PERCENT = "custom_y_percent"
        const val CUSTOM_Y = "custom_y"
        const val PILL_BG = "pill_bg"
        const val AUTO_PILL_BG = "auto_pill_bg"
        const val PILL_DIVIDER_COLOR = "section_divider_color"
        const val PILL_FG = "pill_fg"
        const val PILL_CORNER_RADIUS = "pill_corner_radius"
        const val HOLD_TIME = "hold_time"
        const val VIBRATION_DURATION = "vibration_duration"
        const val ANIM_DURATION = "anim_duration"
        const val X_THRESHOLD = "x_threshold"
        const val Y_THRESHOLD = "y_threshold"
        const val Y_THRESHOLD_DOWN = "y_threshold_down"
        const val AUTO_HIDE_PILL_PROGRESS = "auto_hide_pill_progress"
        const val HIDE_IN_FULLSCREEN_PROGRESS = "hide_in_fullscreen_progress"
        const val HIDE_PILL_ON_KEYBOARD_PROGRESS = "hide_pill_on_keyboard_progress"
        const val VIBRATION_STRENGTH = "vibration_strength"
        const val FADE_AFTER_SPECIFIED_DELAY_PROGRESS = "fade_after_specified_delay_progress"
        const val FADE_IN_FULLSCREEN_APPS_PROGRESS = "fade_in_fullscreen_apps_progress"
        const val FADE_OPACITY = "fade_opacity"
        const val FADE_DURATION = "fade_duration"
        const val BRIGHTNESS_STEP_SIZE = "brightness_step_size"
        const val SAVED_MEDIA_VOLUME = "saved_media_volume"
        const val SWITCH_APPS_DELAY = "switch_app_delay"
        const val ACCESSIBILITY_DELAY = "accessibility_delay"
        const val NAV_WITH_VOLUME = "show_nav_with_volume_dialog_fullscreen"
        const val LEFT_SIDE_GESTURE_HEIGHT = "left_side_gesture_height"
        const val RIGHT_SIDE_GESTURE_HEIGHT = "right_side_gesture_height"
        const val LEFT_SIDE_GESTURE_POSITION = "left_side_gesture_position"
        const val RIGHT_SIDE_GESTURE_POSITION = "right_side_gesture_position"
        const val LEFT_SIDE_GESTURE_WIDTH = "left_side_gesture_width"
        const val RIGHT_SIDE_GESTURE_WIDTH = "right_side_gesture_width"
        const val SIDE_GESTURE_COLOR = "side_gesture_color"

        /* Strings */
        const val CRASHLYTICS_ID = "crashlytics_id"

        /* Lists */
        const val BLACKLISTED_NAV_APPS = "blacklisted_nav_apps"
        const val BLACKLISTED_BAR_APPS = "blacklisted_bar_apps"
        const val BLACKLISTED_IMM_APPS = "blacklisted_imm_apps"
        const val OTHER_WINDOW_APPS = "other_window_apps"
        const val COLORED_APPS = "colored_apps"
        const val HIDE_DIALOG_APPS = "hide_dialog_apps"

        /* Misc */
        const val SUFFIX_INTENT = "_intent"
        const val SUFFIX_ACTIVITY = "_activity"
        const val SUFFIX_PACKAGE = "_package"
        const val SUFFIX_DISPLAYNAME = "_displayname"
        const val SUFFIX_SHORTCUT = "shortcut"
        const val SUFFIX_KEYCODE = "_keycode"

        @SuppressLint("StaticFieldLeak")
        private var instance: PrefManager? = null

        fun getInstance(context: Context): PrefManager {
            if (instance == null) instance = PrefManager(context.applicationContext)
            return instance!!
        }
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(this)

    val crashlyticsIdEnabled: Boolean
        get() = prefs.getBoolean(ENABLE_CRASHLYTICS_ID, false)
    val useAlternateHome: Boolean
        get() = getBoolean(ALTERNATE_HOME, resources.getBoolean(R.bool.alternate_home_default))
    val shouldntKeepOverscanOnLock: Boolean
        get() = getBoolean(LOCKSCREEN_OVERSCAN, false)
    val useFullOverscan: Boolean
        get() = getBoolean(FULL_OVERSCAN, false)
    var shouldUseOverscanMethod: Boolean
        get() = getBoolean(HIDE_NAV, false)
        set(value) {
            putBoolean(HIDE_NAV, value)
        }
    val shouldShowShadow: Boolean
        get() = getBoolean(SHOW_SHADOW, resources.getBoolean(R.bool.show_shadow_default))
    val dontMoveForKeyboard: Boolean
        get() = getBoolean(STATIC_PILL, resources.getBoolean(R.bool.static_pill_default)) || overlayNav
    val useRot270Fix: Boolean
        get() = getBoolean(ROT270_FIX, resources.getBoolean(R.bool.rot_fix_default))
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.P
    val useRot180Fix: Boolean
        get() = getBoolean(ROT180_FIX, resources.getBoolean(R.bool.rot_fix_default))
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.P
    var useTabletMode: Boolean
        get() = getBoolean(TABLET_MODE, resources.getBoolean(R.bool.tablet_mode_default))
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.P
        set(value) {
            putBoolean(TABLET_MODE, value)
        }
    val feedbackSound: Boolean
        get() = getBoolean(AUDIO_FEEDBACK, resources.getBoolean(R.bool.feedback_sound_default))
    var firstRun: Boolean
        get() = getBoolean(FIRST_RUN, true)
        set(value) {
            putBoolean(FIRST_RUN, value)
        }
    val hideInFullscreen: Boolean
        get() = getBoolean(HIDE_IN_FULLSCREEN, resources.getBoolean(R.bool.hide_in_fullscreen_default))
    val autoFade: Boolean
        get() = getBoolean(FADE_AFTER_SPECIFIED_DELAY, resources.getBoolean(R.bool.fade_after_delay_default))
    val fullscreenFade: Boolean
        get() = getBoolean(FADE_IN_FULLSCREEN_APPS, resources.getBoolean(R.bool.fade_in_fullscreen_default))
    val largerHitbox: Boolean
        get() = getBoolean(LARGER_HITBOX, resources.getBoolean(R.bool.large_hitbox_default))
    val origBarInFullscreen: Boolean
        get() = getBoolean(ORIG_NAV_IN_IMMERSIVE, resources.getBoolean(R.bool.orig_nav_in_immersive_default))
    val enableInCarMode: Boolean
        get() = getBoolean(ENABLE_IN_CAR_MODE, resources.getBoolean(R.bool.car_mode_default))
    val usePixelsW: Boolean
        get() = getBoolean(USE_PIXELS_WIDTH, resources.getBoolean(R.bool.use_pixels_width_default))
    val usePixelsH: Boolean
        get() = getBoolean(USE_PIXELS_HEIGHT, resources.getBoolean(R.bool.use_pixels_height_default))
    val usePixelsX: Boolean
        get() = getBoolean(USE_PIXELS_X, resources.getBoolean(R.bool.use_pixels_x_default))
    val usePixelsY: Boolean
        get() = getBoolean(USE_PIXELS_Y, resources.getBoolean(R.bool.use_pixels_y_default))
    var sectionedPill: Boolean
        get() = getBoolean(SECTIONED_PILL, resources.getBoolean(R.bool.sectioned_pill_default))
        set(value) {
            putBoolean(SECTIONED_PILL, value)
        }
    val hidePillWhenKeyboardShown: Boolean
        get() = getBoolean(HIDE_PILL_ON_KEYBOARD, resources.getBoolean(R.bool.hide_on_keyboard_default))
    var anchorPill: Boolean
        get() = getBoolean(ANCHOR_PILL, resources.getBoolean(R.bool.anchor_pill_default))
        set(value) {
            putBoolean(ANCHOR_PILL, value)
        }
    var useImmersiveWhenNavHidden: Boolean
        get() = getBoolean(USE_IMMERSIVE_MODE_WHEN_NAV_HIDDEN, resources.getBoolean(R.bool.immersive_nav_default))
        set(value) {
            putBoolean(USE_IMMERSIVE_MODE_WHEN_NAV_HIDDEN, value)
        }
    val autoHide: Boolean
        get() = getBoolean(AUTO_HIDE_PILL, resources.getBoolean(R.bool.auto_hide_default))
    var validPrem: Boolean
        get() = getBoolean(VALID_PREM, false)
        set(value) {
            putBoolean(VALID_PREM, value)
        }
    var isActive: Boolean
        get() = getBoolean(IS_ACTIVE, false)
        set(value) {
            putBoolean(IS_ACTIVE, value)
        }
    var showHiddenToast: Boolean
        get() = getBoolean(SHOW_HIDDEN_TOAST, true)
        set(value) {
            putBoolean(SHOW_HIDDEN_TOAST, value)
        }
    var showNavWithKeyboard: Boolean
        get() = getBoolean(SHOW_NAV_WITH_KEYBOARD, false) && !overlayNav
        set(value) {
            putBoolean(SHOW_NAV_WITH_KEYBOARD, value)
        }
    var confirmedSkipWss: Boolean
        get() = getBoolean(CONFIRMED_SKIP_WSS, false)
        set(value) {
            putBoolean(CONFIRMED_SKIP_WSS, value)
        }
    val flashlightCompat: Boolean
        get() = getBoolean(FLASHLIGHT_COMPAT, resources.getBoolean(R.bool.flashlight_compat_default))
    val customVibrationStrength: Boolean
        get() = getBoolean(CUSTOM_VIBRATION_STRENGTH, resources.getBoolean(R.bool.custom_vibration_strength_default))
    var hideBetaPrompt: Boolean
        get() = getBoolean(HIDE_BETA_PROMPT, resources.getBoolean(R.bool.hide_beta_prompt_default))
        set(value) {
            putBoolean(HIDE_BETA_PROMPT, value)
        }
    val enableAnalytics: Boolean
        get() = getBoolean(ENABLE_ANALYTICS, resources.getBoolean(R.bool.enable_analytics_default))
    val hideOnLockscreen: Boolean
        get() = getBoolean(HIDE_ON_LOCKSCREEN, resources.getBoolean(R.bool.hide_on_lockscreen_default))
    val hideOnPermissions: Boolean
        get() = getBoolean(HIDE_ON_PERMISSIONS, resources.getBoolean(R.bool.hide_on_permissions_default))
    val hideOnInstaller: Boolean
        get() = getBoolean(HIDE_ON_INSTALLER, resources.getBoolean(R.bool.hide_on_installer_default))
    var overlayNav: Boolean
        get() = getBoolean(OVERLAY_NAV, resources.getBoolean(R.bool.overlay_nav_default))
        set(value) {
            putBoolean(OVERLAY_NAV, value)
        }
    val keepAlive: Boolean
        get() = getBoolean(KEEP_ALIVE, resources.getBoolean(R.bool.keep_alive_default))
    val overlayNavBlackout: Boolean
        get() = getBoolean(OVERLAY_NAV_BLACKOUT, resources.getBoolean(R.bool.overlay_nav_blackout_default)) && overlayNav
    val allowRepeatLong: Boolean
        get() = getBoolean(ALLOW_REPEAT_LONG, resources.getBoolean(R.bool.allow_repeat_long_execute_default))
    val showNavWithVolume: Boolean
        get() = getBoolean(NAV_WITH_VOLUME, resources.getBoolean(R.bool.show_nav_with_volume_dialog_fullscreen_default))
    var leftSideGesture: Boolean
        get() = getBoolean(LEFT_SIDE_GESTURE, resources.getBoolean(R.bool.enable_left_side_gesture_default))
        set(value) {
            putBoolean(LEFT_SIDE_GESTURE, value)
        }
    var rightSideGesture: Boolean
        get() = getBoolean(RIGHT_SIDE_GESTURE, resources.getBoolean(R.bool.enable_right_side_gesture_default))
        set(value) {
            putBoolean(RIGHT_SIDE_GESTURE, value)
        }
    val sideGestureUsePillColor: Boolean
        get() = getBoolean(SIDE_GESTURE_USE_PILL_COLOR, resources.getBoolean(R.bool.side_gesture_use_pill_color_default))

    /**
     * Get the user-defined or default pill color
     * @return the color, as a ColorInt
     */
    var pillBGColor: Int
        get() = getInt(PILL_BG, defaultPillBGColor)
        set(value) = putInt(PILL_BG, value)

    var autoPillBGColor = 0
        set(value) {
            field = value
            putInt(AUTO_PILL_BG, value)
        }
    /**
     * Get the user-defined or default pill border color
     * @return the color, as a ColorInt
     */
    val pillFGColor: Int
        get() = getInt(PILL_FG, defaultPillFGColor)
    val pillCornerRadiusDp: Int
        get() = getInt(PILL_CORNER_RADIUS, resources.getInteger(R.integer.default_corner_radius_dp))
    val pillCornerRadiusPx: Int
        get() = dpAsPx(pillCornerRadiusDp)
    val pillDividerColor: Int
        get() = getInt(PILL_DIVIDER_COLOR, defaultPillDividerColor)
    val animationDurationMs: Int
        get() = getInt(ANIM_DURATION, resources.getInteger(R.integer.default_anim_duration))
    val xThresholdDp: Int
        get() = getInt(X_THRESHOLD, resources.getInteger(R.integer.default_x_threshold_dp))
    val yThresholdUpDp: Int
        get() = getInt(Y_THRESHOLD, resources.getInteger(R.integer.default_y_threshold_dp))
    val yThresholdDownDp: Int
        get() = getInt(Y_THRESHOLD_DOWN, resources.getInteger(R.integer.default_y_threshold_dp))
    val xThresholdPx: Int
        get() = dpAsPx(xThresholdDp)
    val yThresholdUpPx: Int
        get() = dpAsPx(yThresholdUpDp)
    val yThresholdDownPx: Int
        get() = dpAsPx(yThresholdDownDp)
    val autoHideTime: Int
        get() = getInt(AUTO_HIDE_PILL_PROGRESS, resources.getInteger(R.integer.default_auto_hide_time))
    val hideInFullscreenTime: Int
        get() = getInt(HIDE_IN_FULLSCREEN_PROGRESS, resources.getInteger(R.integer.default_auto_hide_time))
    val autoFadeTime: Long
        get() = getInt(FADE_AFTER_SPECIFIED_DELAY_PROGRESS, resources.getInteger(R.integer.default_fade_time)).toLong()
    val fullscreenFadeTime: Long
        get() = getInt(FADE_IN_FULLSCREEN_APPS_PROGRESS, resources.getInteger(R.integer.default_fade_time)).toLong()
    val fadeOpacity: Float
        get() = getInt(FADE_OPACITY, resources.getInteger(R.integer.default_fade_opacity_percent)) / 10f
    val hideOnKeyboardTime: Int
        get() = getInt(HIDE_PILL_ON_KEYBOARD_PROGRESS, resources.getInteger(R.integer.default_auto_hide_time))
    val homeY: Int
        get() {
            val percent = (homeYPercent / 100f)

            return if (usePixelsY)
                getInt(CUSTOM_Y, defaultY)
            else
                (percent * realScreenSize.y).toInt()
        }
    val homeYPercent: Float
        get() = getInt(CUSTOM_Y_PERCENT, defaultYPercentUnscaled) * 0.05f
    val homeX: Int
        get() {
            val percent = (homeXPercent / 100f)
            val screenWidthHalf = realScreenSize.x / 2f - customWidth / 2f

            return if (usePixelsX)
                getInt(CUSTOM_X, 0)
            else
                (percent * screenWidthHalf).toInt()
        }
    val homeXPercent: Float
        get() = getInt(CUSTOM_X_PERCENT, resources.getInteger(R.integer.default_pill_x_pos_percent)) / 10f
    val customWidth: Int
        get() {
            val percent = (customWidthPercent / 100f)
            val screenWidth = realScreenSize.x

            return if (usePixelsW)
                getInt(CUSTOM_WIDTH, resources.getDimensionPixelSize(R.dimen.pill_width_default))
            else
                (percent * screenWidth).toInt()
        }
    val customWidthPercent: Float
        get() = getInt(CUSTOM_WIDTH_PERCENT, resources.getInteger(R.integer.default_pill_width_percent)) / 10f
    val customHeight: Int
        get() {
            var defHeight = customHeightWithoutHitbox
            if (largerHitbox) defHeight += resources.getDimensionPixelSize(R.dimen.pill_large_hitbox_height_increase)

            return defHeight
        }
    val customHeightWithoutHitbox: Int
        get() {
            val percent = (customHeightPercent / 100f)

            return if (usePixelsH)
                getInt(CUSTOM_HEIGHT, resources.getDimensionPixelSize(R.dimen.pill_height_default))
            else
                (percent * realScreenSize.y).toInt()
        }
    val customHeightPercent: Float
        get() = getInt(CUSTOM_HEIGHT_PERCENT, resources.getInteger(R.integer.default_pill_height_percent)) / 10f
    val defaultYPercentUnscaled: Int
        get() = ((navBarHeight / 2f - customHeight / 2f) / realScreenSize.y * 2000f).toInt()
    val defaultY: Int
        get() = ((navBarHeight / 2f - customHeight / 2f)).toInt()
    val holdTime: Int
        get() = getInt(HOLD_TIME, resources.getInteger(R.integer.default_hold_time))
    val vibrationDuration: Int
        get() = getInt(VIBRATION_DURATION, resources.getInteger(R.integer.default_vibe_time))
    val vibrationStrength: Int
        @TargetApi(Build.VERSION_CODES.O)
        get() =
                if (customVibrationStrength) getInt(VIBRATION_STRENGTH, resources.getInteger(R.integer.default_vibe_strength))
                else VibrationEffect.DEFAULT_AMPLITUDE
    val fadeDuration: Long
        get() = getInt(FADE_DURATION, resources.getInteger(R.integer.default_fade_duration)).toLong()
    val brightnessStepSize: Int
        get() = getInt(BRIGHTNESS_STEP_SIZE, resources.getInteger(R.integer.default_brightness_step_size))
    var savedMediaVolume: Int
        get() = getInt(SAVED_MEDIA_VOLUME, 0)
        set(value) {
            putInt(SAVED_MEDIA_VOLUME, value)
        }
    val switchAppDelay: Int
        get() = getInt(SWITCH_APPS_DELAY, resources.getInteger(R.integer.default_switch_app_delay))
    val accessibilityDelay: Int
        get() = getInt(ACCESSIBILITY_DELAY, resources.getInteger(R.integer.default_accessibility_delay))
    val leftSideGestureHeight: Int
        get() = getInt(LEFT_SIDE_GESTURE_HEIGHT, resources.getInteger(R.integer.default_left_side_gesture_height))
    val rightSideGestureHeight: Int
        get() = getInt(RIGHT_SIDE_GESTURE_HEIGHT, resources.getInteger(R.integer.default_right_side_gesture_height))
    val leftSideGesturePosition: Int
        get() = getInt(LEFT_SIDE_GESTURE_POSITION, resources.getInteger(R.integer.default_left_side_gesture_position))
    val rightSideGesturePosition: Int
        get() = getInt(RIGHT_SIDE_GESTURE_POSITION, resources.getInteger(R.integer.default_right_side_gesture_position))
    val leftSideGestureWidth: Int
        get() = getInt(LEFT_SIDE_GESTURE_WIDTH, resources.getInteger(R.integer.default_left_side_gesture_width))
    val rightSideGestureWidth: Int
        get() = getInt(RIGHT_SIDE_GESTURE_WIDTH, resources.getInteger(R.integer.default_right_side_gesture_width))
    val sideGestureColor: Int
        get() = getInt(SIDE_GESTURE_COLOR, defaultSideGestureColor)

    val crashlyticsId: String?
        get() {
            val s = getString(CRASHLYTICS_ID, null)

            return if (s != null) s
            else {
                val new = System.currentTimeMillis().toString()
                putString(CRASHLYTICS_ID, new)
                new
            }
        }

    val all: Map<String, *>
        get() = prefs.all

    fun getIntentKey(baseKey: String?) = getInt(baseKey + SUFFIX_INTENT, -1)
    fun saveIntentKey(baseKey: String?, res: Int) = putInt(baseKey + SUFFIX_INTENT, res)
    fun getPackage(baseKey: String?): String? = getString("$baseKey$SUFFIX_PACKAGE")
    fun getActivity(baseKey: String?): String? = getString("$baseKey$SUFFIX_ACTIVITY")
    fun getDisplayName(baseKey: String?): String? = getString("$baseKey$SUFFIX_DISPLAYNAME")
    fun getShortcut(baseKey: String?): ShortcutInfo? {
        return GsonBuilder()
                .registerTypeAdapter(Uri::class.java, GsonUriHandler())
                .registerTypeAdapter(Intent::class.java, GsonIntentHandler())
                .create()
                .fromJson<ShortcutInfo>(
                        getString((baseKey ?: return null) + SUFFIX_SHORTCUT)
                                ?: return null,
                        object : TypeToken<ShortcutInfo>() {}.type
                )
    }
    fun getKeycode(baseKey: String?) = getInt(baseKey + SUFFIX_KEYCODE, -1)

    fun putPackage(baseKey: String, value: String) =
            putString(baseKey + SUFFIX_PACKAGE, value)

    fun putActivity(baseKey: String, value: String) =
            putString(baseKey + SUFFIX_ACTIVITY, value)

    fun putDisplayName(baseKey: String, value: String) =
            putString(baseKey + SUFFIX_DISPLAYNAME, value)

    fun putShortcut(baseKey: String, shortcutInfo: ShortcutInfo?) =
            putString(baseKey + SUFFIX_SHORTCUT,
                    try {
                        GsonBuilder()
                                .registerTypeAdapter(Uri::class.java, GsonUriHandler())
                                .registerTypeAdapter(Intent::class.java, GsonIntentHandler())
                                .create()
                                .toJson(shortcutInfo)
                    } catch (e: Exception) {
                        null
                    }
            )
    fun putKeycode(baseKey: String, value: Int) = putInt(baseKey + SUFFIX_KEYCODE, value)

    /**
     * Load the actions corresponding to each gesture
     * @param map the HashMap to fill/update
     */
    fun getActionsList(map: HashMap<String, Int>) {
        try {
            map.clear()

            val actionHolder = actionHolder

            val left = getString(actionHolder.actionLeft, actionHolder.typeBack.toString())!!.toInt()
            val right = getString(actionHolder.actionRight, actionHolder.typeRecents.toString())!!.toInt()
            val tap = getString(actionHolder.actionTap, actionHolder.typeHome.toString())!!.toInt()
            val hold = getString(actionHolder.actionHold, actionHolder.typeAssist.toString())!!.toInt()
            val up = getString(actionHolder.actionUp, actionHolder.typeNoAction.toString())!!.toInt()
            val down = getString(actionHolder.actionDown, actionHolder.typeHide.toString())!!.toInt()
            val double = getString(actionHolder.actionDouble, actionHolder.typeNoAction.toString())!!.toInt()
            val holdUp = getString(actionHolder.actionUpHold, actionHolder.typeNoAction.toString())!!.toInt()
            val holdLeft = getString(actionHolder.actionLeftHold, actionHolder.typeNoAction.toString())!!.toInt()
            val holdRight = getString(actionHolder.actionRightHold, actionHolder.typeNoAction.toString())!!.toInt()
            val holdDown = getString(actionHolder.actionDownHold, actionHolder.typeNoAction.toString())!!.toInt()

            val upLeft = getString(actionHolder.actionUpLeft, actionHolder.typeBack.toString())!!.toInt()
            val upHoldLeft = getString(actionHolder.actionUpHoldLeft, actionHolder.typeNoAction.toString())!!.toInt()
            val upCenter = getString(actionHolder.actionUpCenter, actionHolder.typeHome.toString())!!.toInt()
            val upHoldCenter = getString(actionHolder.actionUpHoldCenter, actionHolder.typeRecents.toString())!!.toInt()
            val upRight = getString(actionHolder.actionUpRight, actionHolder.typeBack.toString())!!.toInt()
            val upHoldRight = getString(actionHolder.actionUpHoldRight, actionHolder.typeNoAction.toString())!!.toInt()

            val complexLeftUp = getString(actionHolder.complexActionLeftUp, actionHolder.typeNoAction.toString())!!.toInt()
            val complexRightUp = getString(actionHolder.complexActionRightUp, actionHolder.typeNoAction.toString())!!.toInt()
            val complexLeftDown = getString(actionHolder.complexActionLeftDown, actionHolder.typeNoAction.toString())!!.toInt()
            val complexRightDown = getString(actionHolder.complexActionRightDown, actionHolder.typeNoAction.toString())!!.toInt()
            val complexLongLeftUp = getString(actionHolder.complexActionLongLeftUp, actionHolder.typeNoAction.toString())!!.toInt()
            val complexLongRightUp = getString(actionHolder.complexActionLongRightUp, actionHolder.typeNoAction.toString())!!.toInt()
            val complexLongLeftDown = getString(actionHolder.complexActionLongLeftDown, actionHolder.typeNoAction.toString())!!.toInt()
            val complexLongRightDown = getString(actionHolder.complexActionLongRightDown, actionHolder.typeNoAction.toString())!!.toInt()

            val sideLeftIn = getString(actionHolder.sideLeftIn, actionHolder.typeNoAction.toString())!!.toInt()
            val sideRightIn = getString(actionHolder.sideRightIn, actionHolder.typeNoAction.toString())!!.toInt()
            val sideLeftInLong = getString(actionHolder.sideLeftInLong, actionHolder.typeNoAction.toString())!!.toInt()
            val sideRightInLong = getString(actionHolder.sideRightInLong, actionHolder.typeNoAction.toString())!!.toInt()

            map[actionHolder.actionLeft] = left
            map[actionHolder.actionRight] = right
            map[actionHolder.actionTap] = tap
            map[actionHolder.actionHold] = hold
            map[actionHolder.actionUp] = up
            map[actionHolder.actionDown] = down
            map[actionHolder.actionDouble] = double
            map[actionHolder.actionUpHold] = holdUp
            map[actionHolder.actionLeftHold] = holdLeft
            map[actionHolder.actionRightHold] = holdRight
            map[actionHolder.actionDownHold] = holdDown

            map[actionHolder.actionUpLeft] = upLeft
            map[actionHolder.actionUpHoldLeft] = upHoldLeft
            map[actionHolder.actionUpCenter] = upCenter
            map[actionHolder.actionUpHoldCenter] = upHoldCenter
            map[actionHolder.actionUpRight] = upRight
            map[actionHolder.actionUpHoldRight] = upHoldRight

            map[actionHolder.complexActionLeftUp] = complexLeftUp
            map[actionHolder.complexActionRightUp] = complexRightUp
            map[actionHolder.complexActionLeftDown] = complexLeftDown
            map[actionHolder.complexActionRightDown] = complexRightDown
            map[actionHolder.complexActionLongLeftUp] = complexLongLeftUp
            map[actionHolder.complexActionLongRightUp] = complexLongRightUp
            map[actionHolder.complexActionLongLeftDown] = complexLongLeftDown
            map[actionHolder.complexActionLongRightDown] = complexLongRightDown

            map[actionHolder.sideLeftIn] = sideLeftIn
            map[actionHolder.sideRightIn] = sideRightIn
            map[actionHolder.sideLeftInLong] = sideLeftInLong
            map[actionHolder.sideRightInLong] = sideRightInLong
        } catch (e: Exception) {}
    }

    /**
     * Load the list of apps that should keep the navbar shown
     */
    fun loadBlacklistedNavPackages(packages: ArrayList<String>) =
            packages.addAll(getStringSet(BLACKLISTED_NAV_APPS, HashSet()))

    /**
     * Load the list of apps where the pill shouldn't be shown
     */
    fun loadBlacklistedBarPackages(packages: ArrayList<String>) =
            packages.addAll(getStringSet(BLACKLISTED_BAR_APPS, HashSet()))

    /**
     * Load the list of apps where immersive navigation should be disabled
     */
    fun loadBlacklistedImmPackages(packages: ArrayList<String>) =
            packages.addAll(getStringSet(BLACKLISTED_IMM_APPS, HashSet()))

    fun loadOtherWindowApps(packages: ArrayList<String>) =
            packages.addAll(getStringSet(OTHER_WINDOW_APPS, HashSet()))

    fun loadColoredApps(packages: ArrayList<ColoredAppData>) {
        val list = GsonBuilder()
                .create()
                .fromJson<ArrayList<ColoredAppData>>(
                        getString(COLORED_APPS, null)
                                ?: return,
                        object : TypeToken<ArrayList<ColoredAppData>>() {}.type
                )

        packages.addAll(list)
    }

    fun loadHideDialogApps(packages: ArrayList<String>) {
        val list = getStringSet(HIDE_DIALOG_APPS, HashSet())

        packages.addAll(list)
    }

    /**
     * Save the list of apps that should keep the navbar shown
     */
    fun saveBlacklistedNavPackageList(packages: ArrayList<String>) =
            putStringSet(BLACKLISTED_NAV_APPS, HashSet<String>(packages))

    /**
     * Save the list of apps where the pill shouldn't be shown
     */
    fun saveBlacklistedBarPackages(packages: ArrayList<String>) =
            putStringSet(BLACKLISTED_BAR_APPS, HashSet<String>(packages))

    /**
     * Save the list of apps where immersive navigation should be disabled
     */
    fun saveBlacklistedImmPackages(packages: ArrayList<String>) =
            putStringSet(BLACKLISTED_IMM_APPS, HashSet<String>(packages))

    fun saveOtherWindowApps(packages: ArrayList<String>) =
            putStringSet(OTHER_WINDOW_APPS, HashSet<String>(packages))

    fun saveColoredApps(packages: ArrayList<ColoredAppData>) {
        putString(COLORED_APPS, GsonBuilder()
                .create()
                .toJson(packages))
    }

    fun saveHideDialogApps(packages: ArrayList<String>) {
        putStringSet(HIDE_DIALOG_APPS, HashSet(packages))
    }

    fun getBoolean(key: String, def: Boolean) = prefs.getBoolean(key, def)
    fun getFloat(key: String, def: Float) = prefs.getFloat(key, def)
    fun getInt(key: String, def: Int) = prefs.getInt(key, def)
    fun getString(key: String, def: String? = null): String? = prefs.getString(key, def)
    fun getStringSet(key: String, def: Set<String>): Set<String> = prefs.getStringSet(key, def) ?: HashSet()
    fun get(key: String): Any? {
        return prefs.all[key]
    }

    fun remove(key: String) = prefs.edit().remove(key).apply()

    fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    fun putFloat(key: String, value: Float) = prefs.edit().putFloat(key, value).apply()
    fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
    fun putString(key: String, value: String?) = prefs.edit().putString(key, value).apply()
    fun putStringSet(key: String, set: Set<String>) = prefs.edit().putStringSet(key, set).apply()
    fun put(key: String, value: Any?) {
        if (value == null) {
            remove(key)
            return
        }

        when (value) {
            is Boolean -> putBoolean(key, value)
            is Float -> putFloat(key, value)
            is Int -> putInt(key, value)
            is String -> putString(key, value)
            is Set<*> -> putStringSet(key, value as Set<String>)
        }
    }

    fun clear() {
        prefs.edit().clear().commit()
    }

    fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}