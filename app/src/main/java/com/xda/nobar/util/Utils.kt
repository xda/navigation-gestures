package com.xda.nobar.util

import android.app.KeyguardManager
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.activities.DialogActivity
import com.xda.nobar.activities.IntroActivity
import com.xda.nobar.interfaces.OnDialogChoiceMadeListener
import com.xda.nobar.prefs.PrefManager


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
        val prefManager = PrefManager.getInstance(context)
        return if (uim.currentModeType == Configuration.UI_MODE_TYPE_CAR && prefManager.enableInCarMode) {
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
            ContextCompat.getColor(context, R.color.pill_color)

    fun getDefaultPillFGColor(context: Context) =
            ContextCompat.getColor(context, R.color.pill_border_color)

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
        val color = Color.argb(0xff, 0x00, 0x00, 0x00)
        val nColor = Settings.Global.getString(context.contentResolver, PrefManager.NAVIGATIONBAR_COLOR)
        val nCurrentColor = Settings.Global.getString(context.contentResolver, PrefManager.NAVIGATIONBAR_CURRENT_COLOR)
        val nUTD = Settings.Global.getString(context.contentResolver, PrefManager.NAVIGATIONBAR_USE_THEME_DEFAULT)

        if (nColor != color.toString()) prefManager.navigationBarColor = nColor
        if (nCurrentColor != color.toString()) prefManager.navigationBarCurrentColor = nCurrentColor
        if (nUTD != "0") prefManager.navigationBarUseThemeDefault = nUTD

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
     * Check if the device is on the KeyGuard (lockscreen)
     */
    @Suppress("DEPRECATION")
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

    fun minPillWidthPx(context: Context) =
            dpAsPx(context, minPillWidthDp(context))

    fun minPillWidthDp(context: Context) =
            context.resources.getInteger(R.integer.min_pill_width_dp)

    fun minPillHeightPx(context: Context) =
            dpAsPx(context, minPillHeightDp(context))

    fun minPillHeightDp(context: Context) =
            context.resources.getInteger(R.integer.min_pill_height_dp)

    fun minPillXPx(context: Context): Int {
        val prefManager = PrefManager.getInstance(context)
        return -(getRealScreenSize(context).x.toFloat() / 2f - prefManager.customWidth.toFloat() / 2f).toInt()
    }

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
                yesAction = OnDialogChoiceMadeListener {
                    Log.e("NoBar", "launch")
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://play.google.com/store/apps/details?id=com.xda.nobar.premium")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.applicationContext.startActivity(intent)
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

                yesAction = OnDialogChoiceMadeListener {
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

    fun isAccessibilityAction(context: Context, action: Int): Boolean {
        val actionHolder = ActionHolder.getInstance(context)
        return arrayListOf(
                actionHolder.typeHome,
                actionHolder.typeRecents,
                actionHolder.typeBack,
                actionHolder.typeSwitch,
                actionHolder.typeSplit,
//                actionHolder.premTypeNotif,
//                actionHolder.premTypeQs,
                actionHolder.premTypePower
        ).contains(action)
    }
}
