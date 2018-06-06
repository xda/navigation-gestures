package com.xda.nobar.util

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.TypedValue
import android.view.WindowManager
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.activities.IntroActivity


/**
 * General utility functions for OHM
 */
object Utils {
    fun disableNavImmersive(context: Context) {
        val currentImmersive = Settings.Global.getString(context.contentResolver, Settings.Global.POLICY_CONTROL)

        currentImmersive?.apply {
            when {
                currentImmersive.contains("immersive.navigation") -> {
                    saveBackupImmersive(context)
                    Settings.Global.putString(context.contentResolver, Settings.Global.POLICY_CONTROL, null)
                }

                currentImmersive.contains("immersive.full") -> {
                    val split = currentImmersive.split("=")
                    val insert = try { split[1] } catch (e: Exception) { "*" }

                    saveBackupImmersive(context)
                    Settings.Global.putString(context.contentResolver, Settings.Global.POLICY_CONTROL, "immersive.status=$insert")
                }

                else -> {
                    resetBackupImmersive(context)
                }
            }

            return
        }

        resetBackupImmersive(context)
    }

    fun isInImmersive(context: Context): Boolean {
        val policy = Settings.Global.getString(context.contentResolver, Settings.Global.POLICY_CONTROL) ?: ""
        return policy.contains("immersive.navigation") || policy.contains("immersive.full")
    }

    /**
     * Get the device's screen size
     * @param context context object
     * @return device's resolution (in px) as a Point
     */
    fun getRealScreenSize(context: Context): Point {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val size = Point()

        display.getRealSize(size)

        return size
    }

    /**
     * Convert a certain DP value to its equivalent in px
     * @param context context object
     * @param dpVal the chosen DP value
     * @return the DP value in terms of px
     */
    fun dpAsPx(context: Context, dpVal: Int): Int {
        return dpAsPx(context, dpVal.toFloat())
    }

    fun dpAsPx(context: Context, dpVal: Float): Int {
        val r = context.resources
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpVal, r.displayMetrics))
    }

    /**
     * Retrieve the OHM handler
     * @param context context object
     * @return the OHM handler instance
     * @throws IllegalStateException if the application context is not correct
     */
    fun getHandler(context: Context): App {
        val app = context.applicationContext
        if (app is App) {
            return app
        }
        throw IllegalStateException("Bad app context: ${app.javaClass.simpleName}")
    }

    /**
     * Get the height of the navigation bar
     * @param context context object
     * @return the height of the navigation bar
     */
    fun getNavBarHeight(context: Context): Int {
        val uim = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return if (uim.currentModeType == Configuration.UI_MODE_TYPE_CAR) {
            context.resources.getDimensionPixelSize(context.resources.getIdentifier("navigation_bar_height_car_mode", "dimen", "android"))
        } else context.resources.getDimensionPixelSize(context.resources.getIdentifier("navigation_bar_height", "dimen", "android"))
    }

    /**
     * A special "off" state for NoBar. When active, NoBar will automatically re-enable when the device is unlocked or finishes rebooting
     * @param context context object
     * @param off if true, this mode is active
     */
    fun setOffForRebootOrScreenLock(context: Context, off: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("special_off", off).apply()
    }

    /**
     * Check if the special "off" state is currently active
     * @param context context object
     * @return true if this mode is active
     */
    fun isOffForRebootOrScreenLock(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("special_off", false)
    }

    /**
     * Check to see if user has "compatibility mode" enabled; ie, use Immersive Mode instead of overscans
     * @param context context object
     * @return true if Immersive Mode should be used
     */
    fun shouldUseImmersiveInsteadOfOverscan(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("use_immersive", false)
    }

    /**
     * For use during the set-up process; save the chosen action pack
     * @param context context object
     * @param map the chosen action pack: Strings are keys, Ints are actions
     * See {@link com.xda.nobar.views.BarView#TYPE_*} for action values
     */
    fun saveActionSet(context: Context, map: HashMap<String, Int>) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().let {
            map.forEach { t, u ->  it.putString(t, u.toString())}
            it.apply()
        }
    }

    /**
     * Load the actions corresponding to each gesture
     * @param context a context object
     * @param map the HashMap to fill/update
     */
    fun getActionList(context: Context, map: HashMap<String, Int>) {
        try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val app = getHandler(context)

            val left = prefs.getString(app.actionLeft, app.typeBack.toString()).toInt()
            val right = prefs.getString(app.actionRight, app.typeRecents.toString()).toInt()
            val tap = prefs.getString(app.actionTap, app.typeHome.toString()).toInt()
            val hold = prefs.getString(app.actionHold, app.typeAssist.toString()).toInt()
            val up = prefs.getString(app.actionUp, app.typeNoAction.toString()).toInt()
            val down = prefs.getString(app.actionDown, app.typeHide.toString()).toInt()
            val double = prefs.getString(app.actionDouble, app.typeNoAction.toString()).toInt()
            val holdUp = prefs.getString(app.actionUpHold, app.typeNoAction.toString()).toInt()
            val holdLeft = prefs.getString(app.actionLeftHold, app.typeNoAction.toString()).toInt()
            val holdRight = prefs.getString(app.actionRightHold, app.typeNoAction.toString()).toInt()

            map[app.actionLeft] = left
            map[app.actionRight] = right
            map[app.actionTap] = tap
            map[app.actionHold] = hold
            map[app.actionUp] = up
            map[app.actionDown] = down
            map[app.actionDouble] = double
            map[app.actionUpHold] = holdUp
            map[app.actionLeftHold] = holdLeft
            map[app.actionRightHold] = holdRight
        } catch (e: Exception) {}
    }

    fun actionToName(context: Context, action: Int): String {
        val app = context.applicationContext as App
        return context.resources.getString(when (action) {
            app.typeNoAction -> R.string.nothing
            app.typeBack -> R.string.back
            app.typeOhm -> R.string.ohm
            app.typeRecents -> R.string.recents
            app.typeHide -> R.string.hide
            app.typeSwitch -> R.string.switch_apps
            app.typeAssist -> R.string.assist
            app.typeHome -> R.string.home
            app.premTypeNotif -> R.string.prem_notif
            app.premTypeQs -> R.string.prem_qs
            app.premTypePower -> R.string.prem_power
            app.typeSplit -> R.string.split
            app.premTypeVibe -> android.R.string.untitled
            app.premTypeSilent -> android.R.string.untitled
            app.premTypeMute -> android.R.string.untitled
            app.premTypePlayPause -> R.string.prem_play_pause
            app.premTypeNext -> R.string.prem_next
            app.premTypePrev -> R.string.prem_prev
            app.typeRootHoldBack -> R.string.hold_back
            app.typeRootForward -> R.string.forward
            app.typeRootMenu -> R.string.menu
            app.typeRootSleep -> R.string.sleep
            app.premTypeRootVolUp -> R.string.prem_vol_up
            app.premTypeRootVolDown -> R.string.prem_vol_down
            app.premTypeRootScreenshot -> R.string.prem_type_screenshot
            app.premTypeSwitchIme -> R.string.prem_switch_ime
            app.premTypeLaunchApp -> R.string.prem_launch_app
            else -> android.R.string.untitled
        })
    }

    /**
     * Check to see if overscan should be used
     * @param context a context object
     * @return true if device has a navigation bar and is below P
     */
    fun shouldUseOverscanMethod(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("hide_nav", false)
    }

    fun setShouldUseOverscanMethod(context: Context, use: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("hide_nav", use).apply()
    }

//    /**
//     * Check to see if device has a software navigation bar
//     * @param context a context object
//     * @return true if the device has a soft navbar
//     */
//    fun hasNavBar(context: Context): Boolean {
//        val id = context.resources.getIdentifier("config_showNavigationBar", "bool", "android")
//        return context.resources.getBoolean(id)
//                || Build.MODEL.contains("Android SDK built for x86")
//
////        return context.resources.getBoolean(com.android.internal.R.bool.config_showNavigationBar) || Build.MODEL.contains("Android SDK")
//    }

//    /**
//     * Special function for TouchWiz devices, some of which can hide the navigation bar
//     * @param context a context object
//     * @return true if the navigation bar is currently hidden by TouchWiz
//     */
//    fun touchWizHideNavEnabled(context: Context): Boolean {
//        return Settings.Global.getInt(context.contentResolver, "navigationbar_hide_bar_enabled", 0) == 0
//    }

    /**
     * Make sure the TouchWiz navbar is not hidden
     * @param context a context object
     */
    fun forceTouchWizNavEnabled(context: Context) {
        Settings.Global.putInt(context.contentResolver, "navigationbar_hide_bar_enabled", 0)
    }

    fun undoForceTouchWizNavEnabled(context: Context) {
        Settings.Global.putString(context.contentResolver, "navigationbar_hide_bar_enabled", null)
    }

    /**
     * Get the user-defined or default vertical position of the pill
     * @param context a context object
     * @return the position, in pixels, from the bottom of the screen
     */
    fun getHomeY(context: Context): Int {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt("custom_y", getDefaultY(context))
    }

    /**
     * Get the default vertical position
     * @param context a context object
     * @return the default position, in pixels, from the bottom of the screen
     */
    fun getDefaultY(context: Context): Int {
        return (getNavBarHeight(context) / 2 - context.resources.getDimensionPixelSize(R.dimen.pill_height) / 2)
    }

    /**
     * Get the user-defined or default horizontal position of the pill
     * @param context a context object
     * @return the position, in pixels, from the horizontal center of the screen
     */
    fun getHomeX(context: Context): Int {
        val percent = ((getHomeXPercent(context)) / 100f)
        val screenWidthHalf = getRealScreenSize(context).x / 2f - getCustomWidth(context) / 2f

        return (percent * screenWidthHalf).toInt()
    }

    fun getHomeXPercent(context: Context): Int {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt("custom_x_percent", context.resources.getInteger(R.integer.pill_x_pos_percent))
    }

    /**
     * Get the user-defined or default width of the pill
     * @param context a context object
     * @return the width, in pixels
     */
    fun getCustomWidth(context: Context): Int {
        val percent = (getCustomWidthPercent(context) / 100f)
        val screenWidth = getRealScreenSize(context).x

        return (percent * screenWidth).toInt()
    }

    fun getCustomWidthPercent(context: Context): Int {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt("custom_width_percent", context.resources.getInteger(R.integer.pill_width_percent))
    }

    /**
     * Get the user-defined or default height of the pill
     * @param context a context object
     * @return the height, in pixels
     */
    fun getCustomHeight(context: Context): Int {
        var defHeight = getCustomHeightWithoutHitbox(context)
        if (largerHitbox(context)) defHeight += context.resources.getDimensionPixelSize(R.dimen.pill_large_hitbox_height_increase)

        return defHeight
    }

    fun getCustomHeightWithoutHitbox(context: Context): Int {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt("custom_height", context.resources.getDimensionPixelSize(R.dimen.pill_height))
    }

    /**
     * Get the user-defined or default pill color
     * @param context a context object
     * @return the color, as a ColorInt
     */
    @android.support.annotation.ColorInt
    fun getPillBGColor(context: Context): Int {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt("pill_bg", getDefaultPillBGColor())
    }

    fun getDefaultPillBGColor(): Int {
        return Color.argb(0xee, 0xcc, 0xcc, 0xcc)
    }

    /**
     * Get the user-defined or default pill border color
     * @param context a context object
     * @return the color, as a ColorInt
     */
    @android.support.annotation.ColorInt
    fun getPillFGColor(context: Context): Int {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt("pill_fg", getDefaultPillFGColor())
    }

    fun getDefaultPillFGColor(): Int {
        return Color.argb(0x32, 0x22, 0x22, 0x22)
    }

    /**
     * Get the user-defined or default pill corner radius
     * @param context a context object
     * @return the corner radius, in dp
     */
    fun getPillCornerRadiusInDp(context: Context): Int {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt("pill_corner_radius", 8)
    }

    /**
     * Get the user-defined or default pill corner radius
     * @param context a context object
     * @return the corner radius, in px
     */
    fun getPillCornerRadiusInPx(context: Context): Int {
        return dpAsPx(context, getPillCornerRadiusInDp(context))
    }

    /**
     * Whether or not the pill should have a shadow
     * @param context a context object
     * @return true if the pill should be elevated
     */
    fun shouldShowShadow(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("show_shadow", true)
    }

    /**
     * Whether or not to move the pill with the input method
     * @param context a context object
     * @return true if the pill should NOT move (should stay at the bottom of the screen
     */
    fun dontMoveForKeyboard(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("static_pill", false)
    }

    /**
     * Whether or not to change the overscan to the top in rotation 270 (top of device on the right)
     * This isn't needed for all devices, so it's an option
     * @param context a context object
     * @return true to dynamically change the overscan
     */
    fun useRot270Fix(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("rot270_fix", false)
    }

    /**
     * Tablets usually have the software nav on the bottom, which isn't always the physical bottom.
     * @param context a context object
     * @return true to dynamically change the overscan to hide the navbar
     */
    fun useTabletMode(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("tablet_mode", false)
    }

    /**
     * Whether or not to provide audio feedback for taps
     * @param context a context object
     * @return true if audio feedback is enabled
     * //TODO: add a user-facing option for this
     */
    fun feedbackSound(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("audio_feedback", true)
    }

    /**
     * Check if the accessibility service is currently enabled
     * @param context a context object
     * @return true if accessibility is running
     */
    fun isAccessibilityEnabled(context: Context): Boolean {
        val services = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)

        return services != null && services.contains(context.packageName)
    }

    /**
     * Check if this is the app's first run
     * @param context a context object
     * @return true if this is the first run
     */
    fun isFirstRun(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("first_run", true)
    }

    /**
     * Set whether or not the next start should be counted as the first run
     * @param context a context object
     * @param isFirst true to "reset" app to first run
     */
    fun setFirstRun(context: Context, isFirst: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("first_run", isFirst).apply()
    }

    /**
     * Save the current immersive policy, to restore on deactivation
     * @param context a context object
     */
    fun saveBackupImmersive(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString("def_imm",
                Settings.Global.getString(context.contentResolver, "policy_control")).apply()
    }

    fun resetBackupImmersive(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().remove("def_imm").apply()
    }

    /**
     * Get the saved immersive policy for restoration
     * @param context a context object
     * @return the saved immersive policy
     */
    fun getBackupImmersive(context: Context): String {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("def_imm", "immersive.none")
    }

    /**
     * Check if the current device can use the necessary hidden APIs
     * @param context a context object
     * @return true if this app can be used
     */
    fun canRunHiddenCommands(context: Context): Boolean {
        return try {
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getOverscanInsets(Rect())
            true
        } catch (e: Throwable) {
            false
        } && IWindowManager.canRunCommands()
    }

    /**
     * Get the package name of the default launcher
     * @param context a context object
     * @return the package name, eg com.android.launcher3
     * //TODO: this doesn't seem to work properly
     */
    fun getLauncherPackage(context: Context): ArrayList<String> {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)

        val info = context.packageManager.queryIntentActivities(intent, 0)
        val ret = ArrayList<String>()

        info.forEach { ret.add(it.activityInfo.packageName) }

        return ret
    }

    /**
     * Check whether or not the pill should be hidden on the launcher
     * @param context a context object
     * @return true if the pill should be hidden
     * TODO: re-enable this once it's fixed
     */
    fun hideOnLauncher(context: Context): Boolean {
//        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("hide_on_launcher", false)
        return false
    }

    /**
     * Check if the supplemental root actions should be allowed
     * @param context a context object
     * @return true to show root actions
     * //TODO: re-enable when this is fixed
     */
    fun shouldUseRootCommands(context: Context): Boolean {
//        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("use_root", false)
        return false
    }

    /**
     * Force the navigation bar black, to mask the white line people are complaining so much about
     * @param context a context object
     */
    fun forceNavBlack(context: Context) {
        val color = Color.argb(0xff, 0x00, 0x00, 0x00)
        if (!IntroActivity.needsToRun(context) && shouldUseOverscanMethod(context) && Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            Settings.Global.putInt(context.contentResolver, "navigationbar_color", color)
            Settings.Global.putInt(context.contentResolver, "navigationbar_current_color", color)
            Settings.Global.putInt(context.contentResolver, "navigationbar_use_theme_default", 0)
        }
    }

    /**
     * Clear the navigation bar color
     * Used when showing the software nav
     * @param context a context object
     */
    fun clearBlackNav(context: Context) {
        if (!IntroActivity.needsToRun(context) && shouldUseOverscanMethod(context) && Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            Settings.Global.putString(context.contentResolver, "navigationbar_color", null)
            Settings.Global.putString(context.contentResolver, "navigationbar_current_color", null)
            Settings.Global.putString(context.contentResolver, "navigation_bar_use_theme_default", null)
        }
    }

    fun hideInFullscreen(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("hide_in_fullscreen", true)
    }

    fun largerHitbox(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("larger_hitbox", true)
    }
}