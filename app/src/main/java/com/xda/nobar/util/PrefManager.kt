package com.xda.nobar.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.preference.PreferenceManager
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.xda.nobar.R
import com.xda.nobar.adapters.info.ShortcutInfo
import com.xda.nobar.util.helpers.GsonIntentHandler
import com.xda.nobar.util.helpers.GsonUriHandler
import java.util.*
import kotlin.collections.HashSet
import kotlin.collections.set

class PrefManager private constructor(private val context: Context) {
    companion object {
        /* Booleans */
        const val IS_ACTIVE = "is_active"
        const val HIDE_NAV = "hide_nav"
        const val USE_ROOT = "use_root"
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
        const val PILL_FG = "pill_fg"
        const val PILL_CORNER_RADIUS = "pill_corner_radius"
        const val HOLD_TIME = "hold_time"
        const val VIBRATION_DURATION = "vibration_duration"
        const val NAVIGATIONBAR_COLOR = "navigationbar_color"
        const val NAVIGATIONBAR_CURRENT_COLOR = "navigationbar_current_color"
        const val NAVIGATIONBAR_USE_THEME_DEFAULT = "navigationbar_use_theme_default"
        const val ANIM_DURATION = "anim_duration"
        const val X_THRESHOLD = "x_threshold"
        const val Y_THRESHOLD = "y_threshold"
        const val Y_THRESHOLD_DOWN = "y_threshold_down"
        const val AUTO_HIDE_PILL_PROGRESS = "auto_hide_pill_progress"
        const val HIDE_IN_FULLSCREEN_PROGRESS = "hide_in_fullscreen_progress"
        const val HIDE_PILL_ON_KEYBOARD_PROGRESS = "hide_pill_on_keyboard_progress"

        /* Strings */
        const val CRASHLYTICS_ID = "crashlytics_id"

        /* Lists */
        const val BLACKLISTED_NAV_APPS = "blacklisted_nav_apps"
        const val BLACKLISTED_BAR_APPS = "blacklisted_bar_apps"
        const val BLACKLISTED_IMM_APPS = "blacklisted_imm_apps"
        const val OTHER_WINDOW_APPS = "other_window_apps"

        /* Misc */
        const val SUFFIX_INTENT = "_intent"
        const val SUFFIX_ACTIVITY = "_activity"
        const val SUFFIX_PACKAGE = "_package"
        const val SUFFIX_DISPLAYNAME = "_displayname"
        const val SUFFIX_SHORTCUT = "shortcut"

        @SuppressLint("StaticFieldLeak")
        private var instance: PrefManager? = null

        fun getInstance(context: Context): PrefManager {
            if (instance == null) instance = PrefManager(context.applicationContext)
            return instance!!
        }
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    val crashlyticsIdEnabled: Boolean
        get() = prefs.getBoolean(ENABLE_CRASHLYTICS_ID, false)
    val useAlternateHome: Boolean
        get() = getBoolean(ALTERNATE_HOME, context.resources.getBoolean(R.bool.alternate_home_default))
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
        get() = getBoolean(SHOW_SHADOW, context.resources.getBoolean(R.bool.show_shadow_default))
    val dontMoveForKeyboard: Boolean
        get() = getBoolean(STATIC_PILL, context.resources.getBoolean(R.bool.static_pill_default))
    val useRot270Fix: Boolean
        get() = getBoolean(ROT270_FIX, context.resources.getBoolean(R.bool.rot_fix_default))
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.P
    val useRot180Fix: Boolean
        get() = getBoolean(ROT180_FIX, context.resources.getBoolean(R.bool.rot_fix_default))
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.P
    var useTabletMode: Boolean
        get() = getBoolean(TABLET_MODE, context.resources.getBoolean(R.bool.tablet_mode_default))
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.P
        set(value) {
            putBoolean(TABLET_MODE, value)
        }
    val feedbackSound: Boolean
        get() = getBoolean(AUDIO_FEEDBACK, context.resources.getBoolean(R.bool.feedback_sound_default))
    var firstRun: Boolean
        get() = getBoolean(FIRST_RUN, true)
        set(value) {
            putBoolean(FIRST_RUN, value)
        }
    val useRoot: Boolean
        get() = getBoolean(USE_ROOT, false) && false //TODO: implement at some point
    val hideInFullscreen: Boolean
        get() = getBoolean(HIDE_IN_FULLSCREEN, context.resources.getBoolean(R.bool.hide_in_fullscreen_default))
    val largerHitbox: Boolean
        get() = getBoolean(LARGER_HITBOX, context.resources.getBoolean(R.bool.large_hitbox_default))
    val origBarInFullscreen: Boolean
        get() = getBoolean(ORIG_NAV_IN_IMMERSIVE, context.resources.getBoolean(R.bool.orig_nav_in_immersive_default))
    val enableInCarMode: Boolean
        get() = getBoolean(ENABLE_IN_CAR_MODE, context.resources.getBoolean(R.bool.car_mode_default))
    val usePixelsW: Boolean
        get() = getBoolean(USE_PIXELS_WIDTH, context.resources.getBoolean(R.bool.use_pixels_width_default))
    val usePixelsH: Boolean
        get() = getBoolean(USE_PIXELS_HEIGHT, context.resources.getBoolean(R.bool.use_pixels_height_default))
    val usePixelsX: Boolean
        get() = getBoolean(USE_PIXELS_X, context.resources.getBoolean(R.bool.use_pixels_x_default))
    val usePixelsY: Boolean
        get() = getBoolean(USE_PIXELS_Y, context.resources.getBoolean(R.bool.use_pixels_y_default))
    val sectionedPill: Boolean
        get() = getBoolean(SECTIONED_PILL, context.resources.getBoolean(R.bool.sectioned_pill_default))
    val hidePillWhenKeyboardShown: Boolean
        get() = getBoolean(HIDE_PILL_ON_KEYBOARD, context.resources.getBoolean(R.bool.hide_on_keyboard_default))
    val anchorPill: Boolean
        get() = getBoolean(ANCHOR_PILL, context.resources.getBoolean(R.bool.anchor_pill_default))
    var useImmersiveWhenNavHidden: Boolean
        get() = getBoolean(USE_IMMERSIVE_MODE_WHEN_NAV_HIDDEN, context.resources.getBoolean(R.bool.immersive_nav_default))
        set(value) {
            putBoolean(USE_IMMERSIVE_MODE_WHEN_NAV_HIDDEN, value)
        }
    val autoHide: Boolean
        get() = getBoolean(AUTO_HIDE_PILL, context.resources.getBoolean(R.bool.auto_hide_default))
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
        get() = getBoolean(SHOW_NAV_WITH_KEYBOARD, false)
        set(value) {
            putBoolean(SHOW_NAV_WITH_KEYBOARD, value)
        }
    var confirmedSkipWss: Boolean
        get() = getBoolean(CONFIRMED_SKIP_WSS, false)
        set(value) {
            putBoolean(CONFIRMED_SKIP_WSS, value)
        }

    /**
     * Get the user-defined or default pill color
     * @return the color, as a ColorInt
     */
    val pillBGColor: Int
        get() = getInt(PILL_BG, context.defaultPillBGColor)
    /**
     * Get the user-defined or default pill border color
     * @return the color, as a ColorInt
     */
    val pillFGColor: Int
        get() = getInt(PILL_FG, context.defaultPillFGColor)
    val pillCornerRadiusDp: Int
        get() = getInt(PILL_CORNER_RADIUS, context.resources.getInteger(R.integer.default_corner_radius_dp))
    val pillCornerRadiusPx: Int
        get() = context.dpAsPx(pillCornerRadiusDp)
    val animationDurationMs: Int
        get() = getInt(ANIM_DURATION, context.resources.getInteger(R.integer.default_anim_duration))
    val xThresholdDp: Int
        get() = getInt(X_THRESHOLD, context.resources.getInteger(R.integer.default_x_threshold_dp))
    val yThresholdUpDp: Int
        get() = getInt(Y_THRESHOLD, context.resources.getInteger(R.integer.default_y_threshold_dp))
    val yThresholdDownDp: Int
        get() = getInt(Y_THRESHOLD_DOWN, context.resources.getInteger(R.integer.default_y_threshold_dp))
    val xThresholdPx: Int
        get() = context.dpAsPx(xThresholdDp)
    val yThresholdUpPx: Int
        get() = context.dpAsPx(yThresholdUpDp)
    val yThresholdDownPx: Int
        get() = context.dpAsPx(yThresholdDownDp)
    val autoHideTime: Int
        get() = getInt(AUTO_HIDE_PILL_PROGRESS, context.resources.getInteger(R.integer.default_auto_hide_time))
    val hideInFullscreenTime: Int
        get() = getInt(HIDE_IN_FULLSCREEN_PROGRESS, context.resources.getInteger(R.integer.default_auto_hide_time))
    val hideOnKeyboardTime: Int
        get() = getInt(HIDE_PILL_ON_KEYBOARD_PROGRESS, context.resources.getInteger(R.integer.default_auto_hide_time))
    val homeY: Int
        get() {
            val percent = (homeYPercent / 100f)

            return if (usePixelsY)
                getInt(CUSTOM_Y, defaultY)
            else
                (percent * context.realScreenSize.y).toInt()
        }
    val homeYPercent: Float
        get() = getInt(CUSTOM_Y_PERCENT, defaultYPercentUnscaled) * 0.05f
    val homeX: Int
        get() {
            val percent = (homeXPercent / 100f)
            val screenWidthHalf = context.realScreenSize.x / 2f - customWidth / 2f

            return if (usePixelsX)
                getInt(CUSTOM_X, 0)
            else
                (percent * screenWidthHalf).toInt()
        }
    val homeXPercent: Float
        get() = getInt(CUSTOM_X_PERCENT, context.resources.getInteger(R.integer.default_pill_x_pos_percent)) / 10f
    val customWidth: Int
        get() {
            val percent = (customWidthPercent / 100f)
            val screenWidth = context.realScreenSize.x

            return if (usePixelsW)
                getInt(CUSTOM_WIDTH, context.resources.getDimensionPixelSize(R.dimen.pill_width_default))
            else
                (percent * screenWidth).toInt()
        }
    val customWidthPercent: Float
        get() = getInt(CUSTOM_WIDTH_PERCENT, context.resources.getInteger(R.integer.default_pill_width_percent)) / 10f
    val customHeight: Int
        get() {
            var defHeight = customHeightWithoutHitbox
            if (largerHitbox) defHeight += context.resources.getDimensionPixelSize(R.dimen.pill_large_hitbox_height_increase)

            return defHeight
        }
    val customHeightWithoutHitbox: Int
        get() {
            val percent = (customHeightPercent / 100f)

            return if (usePixelsH)
                getInt(CUSTOM_HEIGHT, context.resources.getDimensionPixelSize(R.dimen.pill_height_default))
            else
                (percent * context.realScreenSize.y).toInt()
        }
    val customHeightPercent: Float
        get() = getInt(CUSTOM_HEIGHT_PERCENT, context.resources.getInteger(R.integer.default_pill_height_percent)) / 10f
    val defaultYPercentUnscaled: Int
        get() = ((context.navBarHeight / 2f - customHeight / 2f) / context.realScreenSize.y * 2000f).toInt()
    val defaultY: Int
        get() = ((context.navBarHeight / 2f - customHeight / 2f)).toInt()
    val holdTime: Int
        get() = getInt(HOLD_TIME, context.resources.getInteger(R.integer.default_hold_time))
    val vibrationDuration: Int
        get() = getInt(VIBRATION_DURATION, context.resources.getInteger(R.integer.default_vibe_time))

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
    var navigationBarColor: String?
        get() = getString(NAVIGATIONBAR_COLOR, Color.BLACK.toString())
        set(value) {
            if (value == null) remove(NAVIGATIONBAR_COLOR)
            else putString(NAVIGATIONBAR_COLOR, value)
        }
    var navigationBarCurrentColor: String?
        get() = getString(NAVIGATIONBAR_CURRENT_COLOR, Color.BLACK.toString())
        set(value) {
            if (value == null) remove(NAVIGATIONBAR_CURRENT_COLOR)
            else putString(NAVIGATIONBAR_CURRENT_COLOR, value)
        }
    var navigationBarUseThemeDefault: String?
        get() = getString(NAVIGATIONBAR_USE_THEME_DEFAULT, Color.BLACK.toString())
        set(value) {
            if (value == null) remove(NAVIGATIONBAR_USE_THEME_DEFAULT)
            else putString(NAVIGATIONBAR_USE_THEME_DEFAULT, value)
        }

    fun getIntentKey(baseKey: String?) = getInt(baseKey + SUFFIX_INTENT, 0)
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

    /**
     * Load the actions corresponding to each gesture
     * @param map the HashMap to fill/update
     */
    fun getActionsList(map: HashMap<String, Int>) {
        try {
            val actionHolder = context.actionHolder

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
        } catch (e: Exception) {
        }
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

    fun getBoolean(key: String, def: Boolean) = prefs.getBoolean(key, def)
    fun getFloat(key: String, def: Float) = prefs.getFloat(key, def)
    fun getInt(key: String, def: Int) = prefs.getInt(key, def)
    fun getString(key: String, def: String? = null) = prefs.getString(key, def)
    fun getStringSet(key: String, def: Set<String>) = prefs.getStringSet(key, def)

    fun remove(key: String) = prefs.edit().remove(key).apply()

    fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    fun putFloat(key: String, value: Float) = prefs.edit().putFloat(key, value).apply()
    fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
    fun putString(key: String, value: String?) = prefs.edit().putString(key, value).apply()
    fun putStringSet(key: String, set: Set<String>) = prefs.edit().putStringSet(key, set).apply()
}