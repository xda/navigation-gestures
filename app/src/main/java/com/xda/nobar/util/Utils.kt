@file:Suppress("DEPRECATION")

package com.xda.nobar.util

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.fragment.app.FragmentManager
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.activities.helpers.DialogActivity
import com.xda.nobar.activities.ui.IntroActivity
import com.xda.nobar.interfaces.OnDialogChoiceMadeListener
import com.xda.nobar.views.BarView

/* Context */

val Context.app: App
    get() = applicationContext as App

var Context.blackNav: Boolean
    get() = throw IllegalAccessException("This field has no read value")
    set(value) {
        val prefManager = PrefManager.getInstance(this)

        if (!IntroActivity.needsToRun(this)
                && hasWss
                && prefManager.shouldUseOverscanMethod
                && Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (value) {
                val color = Color.BLACK
                val nColor = Settings.Global.getString(contentResolver, PrefManager.NAVIGATIONBAR_COLOR)
                val nCurrentColor = Settings.Global.getString(contentResolver, PrefManager.NAVIGATIONBAR_CURRENT_COLOR)
                val nUTD = Settings.Global.getString(contentResolver, PrefManager.NAVIGATIONBAR_USE_THEME_DEFAULT)

                if (nColor != color.toString()) prefManager.navigationBarColor = nColor
                if (nCurrentColor != color.toString()) prefManager.navigationBarCurrentColor = nCurrentColor
                if (nUTD != "0") prefManager.navigationBarUseThemeDefault = nUTD

                Settings.Global.putInt(contentResolver, PrefManager.NAVIGATIONBAR_COLOR, color)
                Settings.Global.putInt(contentResolver, PrefManager.NAVIGATIONBAR_CURRENT_COLOR, color)
                Settings.Global.putInt(contentResolver, PrefManager.NAVIGATIONBAR_USE_THEME_DEFAULT, 0)
            } else {
                Settings.Global.putString(contentResolver, PrefManager.NAVIGATIONBAR_COLOR, prefManager.navigationBarColor)
                Settings.Global.putString(contentResolver, PrefManager.NAVIGATIONBAR_CURRENT_COLOR, prefManager.navigationBarCurrentColor)
                Settings.Global.putString(contentResolver, PrefManager.NAVIGATIONBAR_USE_THEME_DEFAULT, prefManager.navigationBarUseThemeDefault)
            }
        }
    }

val Context.defaultPillBGColor: Int
    get() = ContextCompat.getColor(this, R.color.pill_color)

val Context.defaultPillFGColor: Int
    get() = ContextCompat.getColor(this, R.color.pill_border_color)

val Context.defPillHeightDp: Int
    get() = resources.getInteger(R.integer.default_pill_height_dp)

val Context.defPillHeightPx: Int
    get() = dpAsPx(defPillHeightDp)

val Context.defPillWidthDp: Int
    get() = resources.getInteger(R.integer.default_pill_width_dp)

val Context.defPillWidthPx: Int
    get() = dpAsPx(defPillWidthDp)

val Context.hasWss: Boolean
    get() =
        checkCallingOrSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
                PackageManager.PERMISSION_GRANTED

/**
 * Check if the accessibility service is currently enabled
 * @return true if accessibility is running
 */
val Context.isAccessibilityEnabled: Boolean
    get() {
        val services = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)

        return services != null && services.contains(packageName)
    }

/**
 * Check if the device is on the KeyGuard (lockscreen)
 */
val Context.isOnKeyguard: Boolean
    get() {
        val kgm = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        return kgm.inKeyguardRestrictedInputMode()
                || kgm.isKeyguardLocked
                || (if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) kgm.isDeviceLocked else false)
    }

val Context.isTouchWiz: Boolean
    get() = packageManager.hasSystemFeature("com.samsung.feature.samsung_experience_mobile")

val Context.minPillHeightDp: Int
    get() = resources.getInteger(R.integer.min_pill_height_dp)

val Context.minPillHeightPx: Int
    get() = dpAsPx(minPillHeightDp)

val Context.maxPillHeightDp: Int
    get() = resources.getInteger(R.integer.max_pill_height_dp)

val Context.maxPillHeightPx: Int
    get() = dpAsPx(maxPillHeightDp)

val Context.maxPillWidthPx: Int
    get() = realScreenSize.x

val Context.maxPillXPx: Int
    get() = -minPillXPx

val Context.maxPillYPx: Int
    get() = realScreenSize.y

val Context.minPillWidthDp: Int
    get() = resources.getInteger(R.integer.min_pill_width_dp)

val Context.minPillWidthPx: Int
    get() = dpAsPx(minPillWidthDp)

val Context.minPillXPx: Int
    get() {
        val prefManager = PrefManager.getInstance(this)
        return -(realScreenSize.x.toFloat() / 2f - prefManager.customWidth.toFloat() / 2f).toInt()
    }

val Context.minPillYDp: Int
    get() = resources.getInteger(R.integer.min_pill_y_dp)

val Context.minPillYPx: Int
    get() = dpAsPx(minPillYDp)

/**
 * Get the height of the navigation bar
 * @return the height of the navigation bar
 */
val Context.navBarHeight: Int
    get() {
        val uim = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val prefManager = PrefManager.getInstance(this)
        return if (uim.currentModeType == Configuration.UI_MODE_TYPE_CAR && prefManager.enableInCarMode) {
            resources.getDimensionPixelSize(resources.getIdentifier("navigation_bar_height_car_mode", "dimen", "android"))
        } else resources.getDimensionPixelSize(resources.getIdentifier("navigation_bar_height", "dimen", "android"))
    }

val Context.prefManager: PrefManager
    get() = PrefManager.getInstance(this)

/**
 * Get the device's screen size
 * @return device's resolution (in px) as a Point
 */
val Context.realScreenSize: Point
    get() {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val size = Point()

        display.getRealSize(size)

        return size
    }

var Context.touchWizNavEnabled: Boolean
    get() = Settings.Global.getInt(contentResolver, "navigationbar_hide_bar_enabled", 0) == 0
    set(value) {
        if (hasWss)
            Settings.Global.putString(contentResolver, "navigationbar_hide_bar_enabled", if (value) "1" else null)
    }


fun Context.allowHiddenMethods() {
    if (hasWss) Settings.Global.putInt(contentResolver, "hidden_api_policy_p_apps", 1)
}

/**
 * Convert a certain DP value to its equivalent in px
 * @param dpVal the chosen DP value
 * @return the DP value in terms of px
 */
fun Context.dpAsPx(dpVal: Float) =
        Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpVal, resources.displayMetrics))

fun Context.dpAsPx(dpVal: Int) =
        dpAsPx(dpVal.toFloat())

@SuppressLint("BatteryLife")
fun Context.requestBatteryExemption() {
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
        val batt = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
        batt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(batt)
    }
}

/**
 * Run action if device is on Nougat or later
 * Otherwise show a warning dialog
 */
fun Context.runNougatAction(action: () -> Unit): Boolean {
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
        action.invoke()
    } else {
        DialogActivity.Builder(this).apply {
            title = R.string.nougat_required
            message = R.string.nougat_required_desc
            yesRes = android.R.string.ok
            start()
        }
    }

    return Build.VERSION.SDK_INT > Build.VERSION_CODES.M
}

/**
 * Check for valid premium and run the action if possible
 * Otherwise show a warning dialog
 */
fun Context.runPremiumAction(action: () -> Unit): Boolean {
    if (app.isValidPremium) action.invoke()
    else {
        DialogActivity.Builder(this).apply {
            title = R.string.premium_required
            message = R.string.premium_required_desc
            yesAction = OnDialogChoiceMadeListener {
                Log.e("NoBar", "launch")
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://play.google.com/store/apps/details?id=com.xda.nobar.premium")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                applicationContext.startActivity(intent)
            }
            start()
        }
    }

    return app.isValidPremium
}

fun Context.runSecureSettingsAction(action: () -> Boolean): Boolean {
    return if (hasWss) {
        action.invoke()
    } else {
        IntroActivity.startForWss(this)
        false
    }
}

/**
 * Run an action that requires WRITE_SETTINGS
 * Otherwise show a dialog prompting for permission
 */
fun Context.runSystemSettingsAction(action: () -> Unit): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(this)) {
        action.invoke()
    } else {
        DialogActivity.Builder(this).apply {
            title = R.string.grant_write_settings
            message = R.string.grant_write_settings_desc
            yesRes = android.R.string.ok
            noRes = android.R.string.cancel

            yesAction = OnDialogChoiceMadeListener {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }

            start()
        }
    }

    return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(this)
}

/* FragmentManager */

fun FragmentManager.beginAnimatedTransaction() =
        beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                        android.R.anim.fade_in, android.R.anim.fade_out)

/* Drawable */

/**
 * Stolen from HalogenOS
 * https://github.com/halogenOS/android_frameworks_base/blob/XOS-8.1/packages/SystemUI/src/com/android/systemui/tuner/LockscreenFragment.java
 */
fun Drawable.toBitmapDrawable(resources: Resources): BitmapDrawable? {
    if (this is BitmapDrawable) return this

    val canvas = Canvas()
    canvas.drawFilter = PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG, Paint.FILTER_BITMAP_FLAG)

    return try {
        val bmp = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
        canvas.setBitmap(bmp)

        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)

        BitmapDrawable(resources, bmp)
    } catch (e: IllegalArgumentException) {
        null
    }
}

/* BarView */

fun BarView.isAccessibilityAction(action: Int): Boolean {
    val actionHolder = ActionHolder.getInstance(context)
    return arrayListOf(
            actionHolder.typeHome,
            actionHolder.typeRecents,
            actionHolder.typeBack,
            actionHolder.typeSwitch,
            actionHolder.typeSplit,
            actionHolder.premTypePower
    ).contains(action)
}