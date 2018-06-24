package com.xda.nobar.util

import android.app.KeyguardManager
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.TypedValue
import android.view.WindowManager
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.activities.IntroActivity
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import kotlin.collections.HashSet


/**
 * General utility functions for OHM
 */
object Utils {
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
    fun dpAsPx(context: Context, dpVal: Int) =
            dpAsPx(context, dpVal.toFloat())

    fun dpAsPx(context: Context, dpVal: Float) =
            Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpVal, context.resources.displayMetrics))

    /**
     * Retrieve the OHM handler
     * @param context context object
     * @return the OHM handler instance
     * @throws IllegalStateException if the application context is not correct
     */
    fun getHandler(context: Context): App {
        val app = context.applicationContext
        return app as? App ?: throw IllegalStateException("Bad app context: ${app.javaClass.simpleName}")
    }

    /**
     * Get the height of the navigation bar
     * @param context context object
     * @return the height of the navigation bar
     */
    fun getNavBarHeight(context: Context): Int {
        val uim = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return if (uim.currentModeType == Configuration.UI_MODE_TYPE_CAR && enableInCarMode(context)) {
            context.resources.getDimensionPixelSize(context.resources.getIdentifier("navigation_bar_height_car_mode", "dimen", "android"))
        } else context.resources.getDimensionPixelSize(context.resources.getIdentifier("navigation_bar_height", "dimen", "android"))
    }

    /**
     * Load the actions corresponding to each gesture
     * @param context a context object
     * @param map the HashMap to fill/update
     */
    fun getActionsList(context: Context, map: HashMap<String, Int>) {
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

            val upLeft = prefs.getString(app.actionUpLeft, app.typeBack.toString()).toInt()
            val upHoldLeft = prefs.getString(app.actionUpHoldLeft, app.typeNoAction.toString()).toInt()
            val upCenter = prefs.getString(app.actionUpCenter, app.typeHome.toString()).toInt()
            val upHoldCenter = prefs.getString(app.actionUpHoldCenter, app.typeRecents.toString()).toInt()
            val upRight = prefs.getString(app.actionUpRight, app.typeBack.toString()).toInt()
            val upHoldRight = prefs.getString(app.actionUpHoldRight, app.typeNoAction.toString()).toInt()

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

            map[app.actionUpLeft] = upLeft
            map[app.actionUpHoldLeft] = upHoldLeft
            map[app.actionUpCenter] = upCenter
            map[app.actionUpHoldCenter] = upHoldCenter
            map[app.actionUpRight] = upRight
            map[app.actionUpHoldRight] = upHoldRight
        } catch (e: Exception) {}
    }

    /**
     * Check to see if overscan should be used
     * @param context a context object
     * @return true if device has a navigation bar and is below P
     */
    fun shouldUseOverscanMethod(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context).
                    getBoolean("hide_nav", false)

    /**
     * Make sure the TouchWiz navbar is not hidden
     * @param context a context object
     */
    fun forceTouchWizNavEnabled(context: Context) =
            Settings.Global.putInt(context.contentResolver, "navigationbar_hide_bar_enabled", 0)

    fun undoForceTouchWizNavEnabled(context: Context) =
            Settings.Global.putString(context.contentResolver, "navigationbar_hide_bar_enabled", null)

    /**
     * Get the user-defined or default vertical position of the pill
     * @param context a context object
     * @return the position, in pixels, from the bottom of the screen
     */
    fun getHomeY(context: Context): Int {
        val percent = (getHomeYPercent(context) / 100f)

        return if (usePixelsY(context))
            PreferenceManager.getDefaultSharedPreferences(context).getInt("custom_y", getDefaultY(context))
        else
            (percent * getRealScreenSize(context).y).toInt()
    }

    fun getHomeYPercent(context: Context) =
            (PreferenceManager.getDefaultSharedPreferences(context)
                    .getInt("custom_y_percent", getDefaultYPercent(context)) * 0.05f)

    /**
     * Get the default vertical position
     * @param context a context object
     * @return the default position, in pixels, from the bottom of the screen
     */
    fun getDefaultYPercent(context: Context) =
            ((getNavBarHeight(context) / 2f - getCustomHeight(context) / 2f) / getRealScreenSize(context).y * 2000f).toInt()

    fun getDefaultY(context: Context) =
            ((getNavBarHeight(context) / 2f - getCustomHeight(context) / 2f)).toInt()

    /**
     * Get the user-defined or default horizontal position of the pill
     * @param context a context object
     * @return the position, in pixels, from the horizontal center of the screen
     */
    fun getHomeX(context: Context): Int {
        val percent = ((getHomeXPercent(context)) / 100f)
        val screenWidthHalf = getRealScreenSize(context).x / 2f - getCustomWidth(context) / 2f

        return if (usePixelsX(context))
            PreferenceManager.getDefaultSharedPreferences(context).getInt("custom_x", 0)
        else
            (percent * screenWidthHalf).toInt()
    }

    fun getHomeXPercent(context: Context) =
            (PreferenceManager.getDefaultSharedPreferences(context)
                    .getInt("custom_x_percent", context.resources.getInteger(R.integer.default_pill_x_pos_percent)) / 10f)

    /**
     * Get the user-defined or default width of the pill
     * @param context a context object
     * @return the width, in pixels
     */
    fun getCustomWidth(context: Context): Int {
        val percent = (getCustomWidthPercent(context) / 100f)
        val screenWidth = getRealScreenSize(context).x

        return if (usePixelsW(context))
            PreferenceManager.getDefaultSharedPreferences(context).getInt("custom_width", context.resources.getDimensionPixelSize(R.dimen.pill_width_default))
        else
            (percent * screenWidth).toInt()
    }

    fun getCustomWidthPercent(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context)
                    .getInt("custom_width_percent", context.resources.getInteger(R.integer.default_pill_width_percent)) / 10f

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
        val percent = (getCustomHeightPercent(context) / 100f)

        return if (usePixelsH(context))
            PreferenceManager.getDefaultSharedPreferences(context).getInt("custom_height", context.resources.getDimensionPixelSize(R.dimen.pill_height_default))
        else
            (percent * getRealScreenSize(context).y).toInt()
    }

    fun getCustomHeightPercent(context: Context) =
            (PreferenceManager.getDefaultSharedPreferences(context)
                    .getInt("custom_height_percent", context.resources.getInteger(R.integer.default_pill_height_percent)) / 10f)

    /**
     * Get the user-defined or default pill color
     * @param context a context object
     * @return the color, as a ColorInt
     */
    @android.support.annotation.ColorInt
    fun getPillBGColor(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context)
                    .getInt("pill_bg", getDefaultPillBGColor(context))

    fun getDefaultPillBGColor(context: Context) =
            context.resources.getColor(R.color.pill_color)

    /**
     * Get the user-defined or default pill border color
     * @param context a context object
     * @return the color, as a ColorInt
     */
    @android.support.annotation.ColorInt
    fun getPillFGColor(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context).getInt("pill_fg", getDefaultPillFGColor(context))

    fun getDefaultPillFGColor(context: Context) =
            context.resources.getColor(R.color.pill_border_color)

    /**
     * Get the user-defined or default pill corner radius
     * @param context a context object
     * @return the corner radius, in dp
     */
    fun getPillCornerRadiusInDp(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context)
                    .getInt("pill_corner_radius", context.resources.getInteger(R.integer.default_corner_radius_dp))

    /**
     * Get the user-defined or default pill corner radius
     * @param context a context object
     * @return the corner radius, in px
     */
    fun getPillCornerRadiusInPx(context: Context) =
            dpAsPx(context, getPillCornerRadiusInDp(context))

    /**
     * Whether or not the pill should have a shadow
     * @param context a context object
     * @return true if the pill should be elevated
     */
    fun shouldShowShadow(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("show_shadow", context.resources.getBoolean(R.bool.show_shadow_default))

    /**
     * Whether or not to move the pill with the input method
     * @param context a context object
     * @return true if the pill should NOT move (should stay at the bottom of the screen
     */
    fun dontMoveForKeyboard(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("static_pill", context.resources.getBoolean(R.bool.static_pill_default))

    /**
     * Whether or not to change the overscan to the top in rotation 270 (top of device on the right)
     * This isn't needed for all devices, so it's an option
     * @param context a context object
     * @return true to dynamically change the overscan
     */
    fun useRot270Fix(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("rot270_fix", context.resources.getBoolean(R.bool.rot_fix_default))

    /**
     * Tablets usually have the software nav on the bottom, which isn't always the physical bottom.
     * @param context a context object
     * @return true to dynamically change the overscan to hide the navbar
     */
    fun useTabletMode(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("tablet_mode", context.resources.getBoolean(R.bool.table_mode_default))

    /**
     * Whether or not to provide audio feedback for taps
     * @param context a context object
     * @return true if audio feedback is enabled
     * //TODO: add a user-facing option for this
     */
    fun feedbackSound(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("audio_feedback", context.resources.getBoolean(R.bool.feedback_sound_default))

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
    fun isFirstRun(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("first_run", true)

    /**
     * Set whether or not the next start should be counted as the first run
     * @param context a context object
     * @param isFirst true to "reset" app to first run
     */
    fun setFirstRun(context: Context, isFirst: Boolean) =
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("first_run", isFirst).apply()

    fun setNavImmersive(context: Context) =
            Settings.Global.putString(context.contentResolver, Settings.Global.POLICY_CONTROL, "immersive.navigation=*")

    /**
     * Check if the current device can use the necessary hidden APIs
     * @param context a context object
     * @return true if this app can be used
     */
    fun canRunHiddenCommands(context: Context) =
            try {
                (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getOverscanInsets(Rect())
                true
            } catch (e: Throwable) {
                false
            } && IWindowManager.canRunCommands()

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
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit()
                .putString("navigationbar_color", Settings.Global.getString(context.contentResolver, "navigationbar_color"))
                .putString("navigationbar_current_color", Settings.Global.getString(context.contentResolver, "navigationbar_current_color"))
                .putString("navigationbar_use_theme_default", Settings.Global.getString(context.contentResolver, "navigationbar_use_theme_default"))
                .apply()
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
    fun clearBlackNav(context: Context) =
            if (!IntroActivity.needsToRun(context) && shouldUseOverscanMethod(context) && Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                Settings.Global.putString(context.contentResolver, "navigationbar_color", prefs.getString("navigationbar_color", null)) ||
                Settings.Global.putString(context.contentResolver, "navigationbar_current_color", prefs.getString("navigationbar_current_color", null)) ||
                Settings.Global.putString(context.contentResolver, "navigationbar_use_theme_default", prefs.getString("navigationbar_use_theme_default", null))
            } else {
                false
            }

    /**
     * Whether the pill should "hide" in fullscreen apps
     */
    fun hideInFullscreen(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("hide_in_fullscreen", context.resources.getBoolean(R.bool.hide_in_fullscreen_default))
                    && !autoHide(context)

    /**
     * Whether the pill should have a larger hitbox
     * (12dp above the visible pill vs 4dp)
     */
    fun largerHitbox(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("larger_hitbox", context.resources.getBoolean(R.bool.large_hitbox_default))

    /**
     * Whether overscan should be removed in fullscreen apps
     * If enabled, this allows the user to swipe the original navbar onscreen
     */
    fun origBarInFullscreen(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("orig_nav_in_immersive", context.resources.getBoolean(R.bool.orig_nav_in_immersive_default))

    /**
     * Whether NoBar should stay enabled in Car Mode
     * This is an option because TouchWiz devices ignore the Car Mode navbar height
     */
    fun enableInCarMode(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("enable_in_car_mode", context.resources.getBoolean(R.bool.car_mode_default))

    /**
     * Whether the pill should use the pixel dimension value
     * False for percentages
     */
    fun usePixelsW(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("use_pixels_width", context.resources.getBoolean(R.bool.use_pixels_width_default))

    fun usePixelsH(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("use_pixels_height", context.resources.getBoolean(R.bool.use_pixels_height_default))

    fun usePixelsX(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("use_pixels_x", context.resources.getBoolean(R.bool.use_pixels_x_default))

    fun usePixelsY(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("use_pixels_y", context.resources.getBoolean(R.bool.use_pixels_y_default))

    /**
     * Whether the user has enabled Split Pill
     */
    fun sectionedPill(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("sectioned_pill", context.resources.getBoolean(R.bool.sectioned_pill_default))

    /**
     * Whether the pill should enter "hide" mode when the keyboard is shown
     */
    fun hidePillWhenKeyboardShown(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("hide_pill_on_keyboard", context.resources.getBoolean(R.bool.hide_on_keyboard_default))
                    && !autoHide(context)

    /**
     * Whether immersive navigation should be enabled when overscan is active
     * Fixes alignment issues and Force Touch on TouchWiz
     */
    fun useImmersiveWhenNavHidden(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("use_immersive_mode_when_nav_hidden", context.resources.getBoolean(R.bool.immersive_nav_default))

    /**
     * Load the list of apps that should keep the navbar shown
     */
    fun loadBlacklistedNavPackages(context: Context, packages: ArrayList<String>) =
            packages.addAll(PreferenceManager.getDefaultSharedPreferences(context).getStringSet("blacklisted_nav_apps", HashSet<String>()))

    /**
     * Load the list of apps where the pill shouldn't be shown
     */
    fun loadBlacklistedBarPackages(context: Context, packages: ArrayList<String>) =
            packages.addAll(PreferenceManager.getDefaultSharedPreferences(context).getStringSet("blacklisted_bar_apps", HashSet<String>()))

    /**
     * Load the list of apps where immersive navigation should be disabled
     */
    fun loadBlacklistedImmPackages(context: Context, packages: ArrayList<String>) =
            packages.addAll(PreferenceManager.getDefaultSharedPreferences(context).getStringSet("blacklisted_imm_apps", HashSet<String>()))

    fun loadOtherWindowApps(context: Context, packages: ArrayList<String>) =
            packages.addAll(PreferenceManager.getDefaultSharedPreferences(context).getStringSet("other_window_apps", HashSet<String>()))

    /**
     * Save the list of apps that should keep the navbar shown
     */
    fun saveBlacklistedNavPackageList(context: Context, packages: ArrayList<String>) =
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putStringSet("blacklisted_nav_apps", HashSet<String>(packages))
                    .apply()

    /**
     * Save the list of apps where the pill shouldn't be shown
     */
    fun saveBlacklistedBarPackages(context: Context, packages: ArrayList<String>) =
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putStringSet("blacklisted_bar_apps", HashSet<String>(packages))
                    .apply()

    /**
     * Save the list of appps where immersive navigation should be disabled
     */
    fun saveBlacklistedImmPackages(context: Context, packages: ArrayList<String>) =
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putStringSet("blacklisted_imm_apps", HashSet<String>(packages))
                    .apply()

    fun saveOtherWindowApps(context: Context, packages: ArrayList<String>) =
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putStringSet("other_window_apps", HashSet<String>(packages))
                    .apply()

    /**
     * Get the user-defined (or default) animation duration in ms
     */
    fun getAnimationDurationMs(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context)
                    .getInt("anim_duration", context.resources.getInteger(R.integer.default_anim_duration)).toLong()

    /**
     * Check if the device is on the KeyGuard (lockscreen)
     */
    fun isOnKeyguard(context: Context): Boolean {
        val kgm = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        return kgm.inKeyguardRestrictedInputMode()
                || kgm.isKeyguardLocked
                || (if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) kgm.isDeviceLocked else false)
    }

    /**
     * Run commands for a result
     */
    fun runCommand(vararg strings: String): String? {
        try {
            val comm = Runtime.getRuntime().exec("sh")
            val outputStream = DataOutputStream(comm.outputStream)

            for (s in strings) {
                outputStream.writeBytes(s + "\n")
                outputStream.flush()
            }

            outputStream.writeBytes("exit\n")
            outputStream.flush()

            val inputReader = BufferedReader(InputStreamReader(comm.inputStream))
            val errorReader = BufferedReader(InputStreamReader(comm.errorStream))

            var ret = ""
            var line: String?

            do {
                line = inputReader.readLine()
                if (line == null) break
                ret = ret + line + "\n"
            } while (true)

            do {
                line = errorReader.readLine()
                if (line == null) break
                ret = ret + line + "\n"
            } while (true)

            try {
                comm.waitFor()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            outputStream.close()

            return ret
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Stolen from HalogenOS
     * https://github.com/halogenOS/android_frameworks_base/blob/XOS-8.1/packages/SystemUI/src/com/android/systemui/tuner/LockscreenFragment.java
     */
    fun getBitmapDrawable(drawable: Drawable, resources: Resources): BitmapDrawable {
        if (drawable is BitmapDrawable) return drawable

        val canvas = Canvas()
        canvas.drawFilter = PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG, Paint.FILTER_BITMAP_FLAG)

        val bmp = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        canvas.setBitmap(bmp)

        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return BitmapDrawable(resources, bmp)
    }

    fun getXThresholdDp(context: Context) =
            (PreferenceManager.getDefaultSharedPreferences(context)
                    .getInt("x_threshold", context.resources.getInteger(R.integer.default_x_threshold_dp)))

    fun getYThresholdDp(context: Context) =
            (PreferenceManager.getDefaultSharedPreferences(context)
                    .getInt("y_threshold", context.resources.getInteger(R.integer.default_y_threshold_dp)))

    fun getXThresholdPx(context: Context) = dpAsPx(context, getXThresholdDp(context))

    fun getYThresholdPx(context: Context) = dpAsPx(context, getYThresholdDp(context))

    fun autoHide(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("auto_hide_pill", context.resources.getBoolean(R.bool.auto_hide_default))

    fun autoHideTime(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context)
                    .getInt("auto_hide_pill_progress", context.resources.getInteger(R.integer.default_auto_hide_time)).toLong()

    fun hideInFullscreenTime(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context)
                    .getInt("hide_in_fullscreen_progress", context.resources.getInteger(R.integer.default_auto_hide_time)).toLong()

    fun hideOnKeyboardTime(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context)
                    .getInt("hide_pill_on_keyboard_progress", context.resources.getInteger(R.integer.default_auto_hide_time)).toLong()

    fun minPillWidthPx(context: Context) =
            dpAsPx(context, minPillWidthDp(context))

    fun minPillWidthDp(context: Context) =
            context.resources.getInteger(R.integer.min_pill_width_dp)

    fun minPillHeightPx(context: Context) =
            dpAsPx(context, minPillHeightDp(context))

    fun minPillHeightDp(context: Context) =
            context.resources.getInteger(R.integer.min_pill_height_dp)

    fun minPillXPx(context: Context) =
            -(getRealScreenSize(context).x.toFloat() / 2f - getCustomWidth(context).toFloat() / 2f).toInt()

    fun minPillYPx(context: Context) =
            dpAsPx(context, minPillYDp(context))

    fun minPillYDp(context: Context) =
            context.resources.getInteger(R.integer.min_pill_y_dp)

    fun maxPillWidthPx(context: Context) =
            getRealScreenSize(context).x

    fun maxPillHeightPx(context: Context) =
            dpAsPx(context, maxPillHeightDp(context))

    fun maxPillHeightDp(context: Context) =
            context.resources.getInteger(R.integer.max_pill_height_dp)

    fun maxPillXPx(context: Context) =
            -minPillXPx(context)

    fun maxPillYPx(context: Context) =
            getRealScreenSize(context).y

    fun defPillWidthPx(context: Context) =
            dpAsPx(context, defPillWidthDp(context))

    fun defPillWidthDp(context: Context) =
            context.resources.getInteger(R.integer.default_pill_width_dp)

    fun defPillHeightPx(context: Context) =
            dpAsPx(context, defPillHeightDp(context))

    fun defPillHeightDp(context: Context) =
            context.resources.getInteger(R.integer.default_pill_height_dp)

    fun useFullOverscan(context: Context) =
            PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("full_overscan", false)
}