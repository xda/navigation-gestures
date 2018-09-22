package com.xda.nobar.prefs

import android.content.Context
import android.graphics.Color
import android.preference.Preference
import android.preference.PreferenceGroup
import android.preference.PreferenceScreen
import android.text.TextUtils
import com.xda.nobar.R
import com.xda.nobar.util.ActionHolder
import com.xda.nobar.util.Utils
import net.grandcentrix.tray.TrayPreferences
import net.grandcentrix.tray.core.SharedPreferencesImport
import java.util.ArrayList
import java.util.HashMap
import kotlin.collections.HashSet
import kotlin.collections.set

class PrefManager private constructor(context: Context) : TrayPreferences(context, "prefs", 1) {
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

        const val OLD_NAME = "com.xda.nobar_preferences"

        private var instance: PrefManager? = null

        fun getInstance(context: Context): PrefManager {
            if (instance == null) instance = PrefManager(context)
            return instance!!
        }
    }

    val actionHolder by lazy { ActionHolder(context) }

    val crashlyticsIdEnabled: Boolean
        get() = getBoolean(ENABLE_CRASHLYTICS_ID, false)
    val useAlternateHome: Boolean
        get() = getBoolean(ALTERNATE_HOME, context.resources.getBoolean(R.bool.alternate_home_default))
    val shouldntKeepOverscanOnLock: Boolean
        get() = getBoolean(LOCKSCREEN_OVERSCAN, false)
    val useFullOverscan: Boolean
        get() = getBoolean(FULL_OVERSCAN, false)
    var shouldUseOverscanMethod: Boolean
        get() = getBoolean(HIDE_NAV, false)
        set(value) {
            put(HIDE_NAV, value)
        }
    val shouldShowShadow: Boolean
        get() = getBoolean(SHOW_SHADOW, context.resources.getBoolean(R.bool.show_shadow_default))
    val dontMoveForKeyboard: Boolean
        get() = getBoolean(STATIC_PILL, context.resources.getBoolean(R.bool.static_pill_default))
    val useRot270Fix: Boolean
        get() = getBoolean(ROT270_FIX, context.resources.getBoolean(R.bool.rot_fix_default))
    val useRot180Fix: Boolean
        get() = getBoolean(ROT180_FIX, context.resources.getBoolean(R.bool.rot_fix_default))
    val useTabletMode: Boolean
        get() = getBoolean(TABLET_MODE, context.resources.getBoolean(R.bool.tablet_mode_default))
    val feedbackSound: Boolean
        get() = getBoolean(AUDIO_FEEDBACK, context.resources.getBoolean(R.bool.feedback_sound_default))
    var firstRun: Boolean
        get() = getBoolean(FIRST_RUN, true)
        set(value) {
            put(FIRST_RUN, value)
        }
    var useRoot: Boolean = false
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
    var useImmersiveWhenNavHidden: Boolean
        get() = getBoolean(USE_IMMERSIVE_MODE_WHEN_NAV_HIDDEN, context.resources.getBoolean(R.bool.immersive_nav_default))
        set(value) {
            put(USE_IMMERSIVE_MODE_WHEN_NAV_HIDDEN, value)
        }
    val autoHide: Boolean
        get() = getBoolean(AUTO_HIDE_PILL, context.resources.getBoolean(R.bool.auto_hide_default))
    var validPrem: Boolean
        get() = getBoolean(VALID_PREM, false)
        set(value) {
            put(VALID_PREM, value)
        }
    var isActive: Boolean
        get() = getBoolean(IS_ACTIVE, false)
        set(value) {
            put(IS_ACTIVE, value)
        }
    val navHidden: Boolean
        get() = getBoolean(HIDE_NAV, false)
    var showHiddenToast: Boolean
        get() = getBoolean(SHOW_HIDDEN_TOAST, true)
        set(value) {
            put(SHOW_HIDDEN_TOAST, value)
        }

    /**
     * Get the user-defined or default pill color
     * @return the color, as a ColorInt
     */
    val pillBGColor: Int
        get() = getInt(PILL_BG, Utils.getDefaultPillBGColor(context))
    /**
     * Get the user-defined or default pill border color
     * @return the color, as a ColorInt
     */
    val pillFGColor: Int
        get() = getInt(PILL_FG, Utils.getDefaultPillFGColor(context))
    val pillCornerRadiusDp: Int
        get() = getFloat(PILL_CORNER_RADIUS, context.resources.getInteger(R.integer.default_corner_radius_dp).toFloat()).toInt()
    val pillCornerRadiusPx: Int
        get() = Utils.dpAsPx(context, pillCornerRadiusDp)
    val animationDurationMs: Int
        get() = getFloat(ANIM_DURATION, context.resources.getInteger(R.integer.default_anim_duration).toFloat()).toInt()
    val xThresholdDp: Int
        get() = getFloat(X_THRESHOLD, context.resources.getInteger(R.integer.default_x_threshold_dp).toFloat()).toInt()
    val yThresholdDp: Int
        get() = getFloat(Y_THRESHOLD, context.resources.getInteger(R.integer.default_y_threshold_dp).toFloat()).toInt()
    val xThresholdPx: Int
        get() = Utils.dpAsPx(context, xThresholdDp)
    val yThresholdPx: Int
        get() = Utils.dpAsPx(context, yThresholdDp)
    val autoHideTime: Int
        get() = getFloat(AUTO_HIDE_PILL_PROGRESS, context.resources.getInteger(R.integer.default_auto_hide_time).toFloat()).toInt()
    val hideInFullscreenTime: Int
        get() = getFloat(HIDE_IN_FULLSCREEN_PROGRESS, context.resources.getInteger(R.integer.default_auto_hide_time).toFloat()).toInt()
    val hideOnKeyboardTime: Int
        get() = getFloat(HIDE_PILL_ON_KEYBOARD_PROGRESS, context.resources.getInteger(R.integer.default_auto_hide_time).toFloat()).toInt()
    val homeY: Int
        get() {
            val percent = (homeYPercent / 100f)

            return if (usePixelsY)
                getInt(CUSTOM_Y, defaultY)
            else
                (percent * Utils.getRealScreenSize(context).y).toInt()
        }
    val homeYPercent: Int
        get() = (getFloat(CUSTOM_Y_PERCENT, defaultYPercent.toFloat()) * 0.05f).toInt()
    val homeX: Int
        get() {
            val percent = (homeXPercent / 100f)
            val screenWidthHalf = Utils.getRealScreenSize(context).x / 2f - customWidth / 2f

            return if (usePixelsX)
                getFloat(CUSTOM_X, 0f).toInt()
            else
                (percent * screenWidthHalf).toInt()
        }
    val homeXPercent: Int
        get() = (getFloat(CUSTOM_X_PERCENT, context.resources.getInteger(R.integer.default_pill_x_pos_percent).toFloat()) / 10f).toInt()
    val customWidth: Int
        get() {
            val percent = (customWidthPercent / 100f)
            val screenWidth = Utils.getRealScreenSize(context).x

            return if (usePixelsW)
                getFloat(CUSTOM_WIDTH, context.resources.getDimensionPixelSize(R.dimen.pill_width_default).toFloat()).toInt()
            else
                (percent * screenWidth).toInt()
        }
    val customWidthPercent: Int
        get() = (getFloat(CUSTOM_WIDTH_PERCENT, context.resources.getInteger(R.integer.default_pill_width_percent).toFloat()) / 10f).toInt()
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
                getFloat(CUSTOM_HEIGHT, context.resources.getDimensionPixelSize(R.dimen.pill_height_default).toFloat()).toInt()
            else
                (percent * Utils.getRealScreenSize(context).y).toInt()
        }
    val customHeightPercent: Int
        get() = (getFloat(CUSTOM_HEIGHT_PERCENT, context.resources.getInteger(R.integer.default_pill_height_percent).toFloat()) / 10f).toInt()
    val defaultYPercent: Int
        get() = ((Utils.getNavBarHeight(context) / 2f - customHeight / 2f) / Utils.getRealScreenSize(context).y * 2000f).toInt()
    val defaultY: Int
        get() = ((Utils.getNavBarHeight(context) / 2f - customHeight / 2f)).toInt()
    val holdTime: Int
        get() = getFloat(HOLD_TIME, context.resources.getInteger(R.integer.default_hold_time).toFloat()).toInt()
    val vibrationDuration: Int
        get() = getFloat(VIBRATION_DURATION, context.resources.getInteger(R.integer.default_vibe_time).toFloat()).toInt()

    val crashlyticsId: String?
        get() = getString(CRASHLYTICS_ID, null)
    var navigationBarColor: String?
        get() = getString(NAVIGATIONBAR_COLOR, Color.BLACK.toString())
        set(value) {
            put(NAVIGATIONBAR_COLOR, value)
        }
    var navigationBarCurrentColor: String?
        get() = getString(NAVIGATIONBAR_CURRENT_COLOR, Color.BLACK.toString())
        set(value) {
            put(NAVIGATIONBAR_CURRENT_COLOR, value)
        }
    var navigationBarUseThemeDefault: String?
        get() = getString(NAVIGATIONBAR_USE_THEME_DEFAULT, Color.BLACK.toString())
        set(value) {
            put(NAVIGATIONBAR_USE_THEME_DEFAULT, value)
        }

    override fun onCreate(initialVersion: Int) {
        super.onCreate(initialVersion)

        //Migrate Booleans
        migrate(
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        IS_ACTIVE,
                        IS_ACTIVE
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        HIDE_NAV,
                        HIDE_NAV
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        USE_ROOT,
                        USE_ROOT
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        ROT270_FIX,
                        ROT270_FIX
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        ROT180_FIX,
                        ROT180_FIX
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        TABLET_MODE,
                        TABLET_MODE
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        ENABLE_IN_CAR_MODE,
                        ENABLE_IN_CAR_MODE
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        USE_IMMERSIVE_MODE_WHEN_NAV_HIDDEN,
                        USE_IMMERSIVE_MODE_WHEN_NAV_HIDDEN
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        HIDE_PILL_ON_KEYBOARD,
                        HIDE_PILL_ON_KEYBOARD
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        FULL_OVERSCAN,
                        FULL_OVERSCAN
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        VALID_PREM,
                        VALID_PREM
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        USE_PIXELS_WIDTH,
                        USE_PIXELS_WIDTH
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        USE_PIXELS_HEIGHT,
                        USE_PIXELS_HEIGHT
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        USE_PIXELS_X,
                        USE_PIXELS_X
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        USE_PIXELS_Y,
                        USE_PIXELS_Y
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        SHOW_SHADOW,
                        SHOW_SHADOW
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        STATIC_PILL,
                        STATIC_PILL
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        AUDIO_FEEDBACK,
                        AUDIO_FEEDBACK
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        LARGER_HITBOX,
                        LARGER_HITBOX
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        AUTO_HIDE_PILL,
                        AUTO_HIDE_PILL
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        SHOW_HIDDEN_TOAST,
                        SHOW_HIDDEN_TOAST
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        ENABLE_CRASHLYTICS_ID,
                        ENABLE_CRASHLYTICS_ID
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        ALTERNATE_HOME,
                        ALTERNATE_HOME
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        FIRST_RUN,
                        FIRST_RUN
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        HIDE_IN_FULLSCREEN,
                        HIDE_IN_FULLSCREEN
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        ORIG_NAV_IN_IMMERSIVE,
                        ORIG_NAV_IN_IMMERSIVE
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        SECTIONED_PILL,
                        SECTIONED_PILL
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        LOCKSCREEN_OVERSCAN,
                        LOCKSCREEN_OVERSCAN
                )
        )

        //Migrate Numbers
        migrate(
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        CUSTOM_WIDTH_PERCENT,
                        CUSTOM_WIDTH_PERCENT
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        CUSTOM_WIDTH,
                        CUSTOM_WIDTH
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        CUSTOM_HEIGHT_PERCENT,
                        CUSTOM_HEIGHT_PERCENT
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        CUSTOM_HEIGHT,
                        CUSTOM_HEIGHT
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        CUSTOM_X_PERCENT,
                        CUSTOM_X_PERCENT
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        CUSTOM_X,
                        CUSTOM_X
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        CUSTOM_Y_PERCENT,
                        CUSTOM_Y_PERCENT
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        CUSTOM_Y,
                        CUSTOM_Y
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        PILL_BG,
                        PILL_BG
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        PILL_FG,
                        PILL_FG
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        PILL_CORNER_RADIUS,
                        PILL_CORNER_RADIUS
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        HOLD_TIME,
                        HOLD_TIME
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        VIBRATION_DURATION,
                        VIBRATION_DURATION
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        NAVIGATIONBAR_COLOR,
                        NAVIGATIONBAR_COLOR
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        NAVIGATIONBAR_CURRENT_COLOR,
                        NAVIGATIONBAR_CURRENT_COLOR
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        NAVIGATIONBAR_USE_THEME_DEFAULT,
                        NAVIGATIONBAR_USE_THEME_DEFAULT
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        ANIM_DURATION,
                        ANIM_DURATION
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        X_THRESHOLD,
                        X_THRESHOLD
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        Y_THRESHOLD,
                        Y_THRESHOLD
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        AUTO_HIDE_PILL_PROGRESS,
                        AUTO_HIDE_PILL_PROGRESS
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        HIDE_IN_FULLSCREEN_PROGRESS,
                        HIDE_IN_FULLSCREEN_PROGRESS
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        HIDE_PILL_ON_KEYBOARD_PROGRESS,
                        HIDE_PILL_ON_KEYBOARD_PROGRESS
                )
        )

        //Migrate Strings
        migrate(
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        CRASHLYTICS_ID,
                        CRASHLYTICS_ID
                )
        )

        //Migrate Lists
        migrateList(
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        BLACKLISTED_NAV_APPS,
                        BLACKLISTED_NAV_APPS
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        BLACKLISTED_BAR_APPS,
                        BLACKLISTED_BAR_APPS
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        BLACKLISTED_IMM_APPS,
                        BLACKLISTED_IMM_APPS
                ),
                SharedPreferencesImport(
                        context,
                        OLD_NAME,
                        OTHER_WINDOW_APPS,
                        OTHER_WINDOW_APPS
                )
        )

        //Actions, Intents, Apps, Activities
        ActionHolder(context).actionsList.forEach {
            migrate(
                    SharedPreferencesImport(
                            context,
                            OLD_NAME,
                            it,
                            it
                    ),
                    SharedPreferencesImport(
                            context,
                            OLD_NAME,
                            it + SUFFIX_INTENT,
                            it + SUFFIX_INTENT
                    ),
                    SharedPreferencesImport(
                            context,
                            OLD_NAME,
                            it + SUFFIX_ACTIVITY,
                            it + SUFFIX_ACTIVITY
                    ),
                    SharedPreferencesImport(
                            context,
                            OLD_NAME,
                            it + SUFFIX_PACKAGE,
                            it + SUFFIX_PACKAGE
                    ),
                    SharedPreferencesImport(
                            context,
                            OLD_NAME,
                            it + SUFFIX_DISPLAYNAME,
                            it + SUFFIX_DISPLAYNAME
                    )
            )
        }
    }

    fun put(key: String, list: Set<*>) {
        val string = TextUtils.join(",", list)
        put(key, string)
    }

    fun put(key: String, obj: Any?) {
        if (obj == null) remove(key)
        else when (obj) {
            is String -> put(key, obj)
            is Int -> put(key, obj)
            is Float -> put(key, obj)
            is Long -> put(key, obj)
            is Boolean -> put(key, obj)
            is Set<*> -> put(key, obj)
        }
    }

    fun getStringSet(key: String, def: Set<String>): Set<String> {
        val saved = getString(key, null) ?: return def
        return HashSet<String>(saved.split(","))
    }

    fun migrateList(vararg migration: SharedPreferencesImport) {
        migration.forEach {
            val data = it.data as? Set<*> ?: return@forEach

            val key = it.trayKey
            val migrationKey = it.previousKey
            // save into tray
            storage.put(key, migrationKey, data)

            // return the saved data.
            val item = storage.get(key)
            it.onPostMigrate(item)
        }
    }

    fun getIntentKey(baseKey: String?) = getInt(baseKey + SUFFIX_INTENT, 0)
    fun saveIntentKey(baseKey: String?, res: Int) = put(baseKey + SUFFIX_INTENT, res)

    /**
     * Load the actions corresponding to each gesture
     * @param map the HashMap to fill/update
     */
    fun getActionsList(map: HashMap<String, Int>) {
        try {
            val actionHolder = ActionHolder(context)

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

    /**
     * Save the list of apps that should keep the navbar shown
     */
    fun saveBlacklistedNavPackageList(packages: ArrayList<String>) =
                    put(BLACKLISTED_NAV_APPS, HashSet<String>(packages))

    /**
     * Save the list of apps where the pill shouldn't be shown
     */
    fun saveBlacklistedBarPackages(packages: ArrayList<String>) =
            put(BLACKLISTED_BAR_APPS, HashSet<String>(packages))

    /**
     * Save the list of apps where immersive navigation should be disabled
     */
    fun saveBlacklistedImmPackages(packages: ArrayList<String>) =
                    put(BLACKLISTED_IMM_APPS, HashSet<String>(packages))

    fun saveOtherWindowApps(packages: ArrayList<String>) =
            put(OTHER_WINDOW_APPS, HashSet<String>(packages))

    fun setPreferenceListeners(screen: PreferenceScreen) {
//        val list = ArrayList<Preference>()
//        for (i in 0 until screen.preferenceCount) {
//            val pref = screen.getPreference(i)
//
//            if (pref is PreferenceGroup) list.addAll(loopPreferenceGroup(pref))
//            else list.add(pref)
//        }
//
//        list.forEach {
//            it.setOnPreferenceChangeListener { preference, newValue ->
//                put(preference.key, newValue)
//                true
//            }
//        }
    }

    private fun loopPreferenceGroup(group: PreferenceGroup): ArrayList<Preference> {
        val list = ArrayList<Preference>()
        for (i in 0 until group.preferenceCount) {
            val pref = group.getPreference(i)

            if (pref is PreferenceGroup) list.addAll(loopPreferenceGroup(pref))
            else list.add(pref)
        }

        return list
    }
}