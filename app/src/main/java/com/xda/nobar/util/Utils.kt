package com.xda.nobar.util

import android.app.KeyguardManager
import android.app.UiModeManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.TypedValue
import android.view.WindowManager
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.activities.DialogActivity
import com.xda.nobar.activities.IntroActivity
import com.xda.nobar.prefs.PrefManager
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

    fun getScreenSize(context: Context): Point {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val size = Point()

        display.getSize(size)

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
     * Make sure the TouchWiz navbar is not hidden
     * @param context a context object
     */
    fun forceTouchWizNavEnabled(context: Context) =
            if (IntroActivity.hasWss(context)) Settings.Global.putInt(context.contentResolver, "navigationbar_hide_bar_enabled", 0) else false

    fun forceTouchWizNavNotEnabled(context: Context) =
            if (IntroActivity.hasWss(context)) Settings.Global.putInt(context.contentResolver, "navigationbar_hide_bar_enabled", 1) else false

    fun undoForceTouchWizNavEnabled(context: Context) =
            if (IntroActivity.hasWss(context)) Settings.Global.putString(context.contentResolver, "navigationbar_hide_bar_enabled", null) else false


    fun getDefaultPillBGColor(context: Context) =
            context.resources.getColor(R.color.pill_color)

    fun getDefaultPillFGColor(context: Context) =
            context.resources.getColor(R.color.pill_border_color)

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
     * Force the navigation bar black, to mask the white line people are complaining so much about
     */
    fun forceNavBlack(context: Context) {
        val prefManager = PrefManager.getInstance(context)
        prefManager.navigationBarColor = Settings.Global.getString(context.contentResolver, PrefManager.NAVIGATIONBAR_COLOR)
        prefManager.navigationBarCurrentColor = Settings.Global.getString(context.contentResolver, PrefManager.NAVIGATIONBAR_CURRENT_COLOR)
        prefManager.navigationBarUseThemeDefault = Settings.Global.getString(context.contentResolver, PrefManager.NAVIGATIONBAR_USE_THEME_DEFAULT)

        val color = Color.argb(0xff, 0x00, 0x00, 0x00)
        if (!IntroActivity.needsToRun(context) && prefManager.shouldUseOverscanMethod && Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            Settings.Global.putInt(context.contentResolver, PrefManager.NAVIGATIONBAR_COLOR, color)
            Settings.Global.putInt(context.contentResolver, PrefManager.NAVIGATIONBAR_CURRENT_COLOR, color)
            Settings.Global.putInt(context.contentResolver, PrefManager.NAVIGATIONBAR_USE_THEME_DEFAULT, 0)
        }
    }

    /**
     * Clear the navigation bar color
     * Used when showing the software nav
     */
    fun clearBlackNav(context: Context) {
        val prefManager = PrefManager.getInstance(context)
        if (!IntroActivity.needsToRun(context) && prefManager.shouldUseOverscanMethod && Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            Settings.Global.putString(context.contentResolver, PrefManager.NAVIGATIONBAR_COLOR, prefManager.navigationBarColor) or
                    Settings.Global.putString(context.contentResolver, PrefManager.NAVIGATIONBAR_CURRENT_COLOR, prefManager.navigationBarCurrentColor) or
                    Settings.Global.putString(context.contentResolver, PrefManager.NAVIGATIONBAR_USE_THEME_DEFAULT, prefManager.navigationBarUseThemeDefault)
        }
    }

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
            useImmersiveWhenNavHiddenInternal(context)

    private fun useImmersiveWhenNavHiddenInternal(context: Context) =
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
     * Save the list of apps where immersive navigation should be disabled
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
     * Stolen from HalogenOS
     * https://github.com/halogenOS/android_frameworks_base/blob/XOS-8.1/packages/SystemUI/src/com/android/systemui/tuner/LockscreenFragment.java
     */
    fun getBitmapDrawable(drawable: Drawable, resources: Resources): BitmapDrawable? {
        if (drawable is BitmapDrawable) return drawable

        val canvas = Canvas()
        canvas.drawFilter = PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG, Paint.FILTER_BITMAP_FLAG)

        return try {
            val bmp = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            canvas.setBitmap(bmp)

            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            BitmapDrawable(resources, bmp)
        } catch (e: IllegalArgumentException) {
            null
        }
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

    fun checkTouchWiz(context: Context) = context.packageManager.hasSystemFeature("com.samsung.feature.samsung_experience_mobile")

    /**
     * Check for valid premium and run the action if possible
     * Otherwise show a warning dialog
     */
    fun runPremiumAction(context: Context, validPrem: Boolean, action: () -> Unit): Boolean {
        if (validPrem) action.invoke()
        else {
            DialogActivity.Builder(context).apply {
                title = R.string.premium_required
                message = R.string.premium_required_desc
                yesAction = DialogInterface.OnClickListener { _, _ ->
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://play.google.com/store/apps/details?id=com.xda.nobar.premium")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
                start()
            }
        }

        return validPrem
    }

    /**
     * Run action if device is on Nougat or later
     * Otherwise show a warning dialog
     */
    fun runNougatAction(context: Context, action: () -> Unit): Boolean {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            action.invoke()
        } else {
            DialogActivity.Builder(context).apply {
                title = R.string.nougat_required
                message = R.string.nougat_required_desc
                yesRes = android.R.string.ok
                start()
            }
        }

        return Build.VERSION.SDK_INT > Build.VERSION_CODES.M
    }

    /**
     * Run an action that requires WRITE_SETTINGS
     * Otherwise show a dialog prompting for permission
     */
    fun runSystemSettingsAction(context: Context, action: () -> Unit): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(context)) {
            action.invoke()
        } else {
            DialogActivity.Builder(context).apply {
                title = R.string.grant_write_settings
                message = R.string.grant_write_settings_desc
                yesRes = android.R.string.ok
                noRes = android.R.string.cancel

                yesAction = DialogInterface.OnClickListener { _, _ ->
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                    intent.data = Uri.parse("package:${context.packageName}")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }

                start()
            }
        }

        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(context)
    }
}