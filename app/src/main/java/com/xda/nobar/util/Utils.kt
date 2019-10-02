@file:Suppress("DEPRECATION", "UNCHECKED_CAST")

package com.xda.nobar.util

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Display
import android.view.DisplayInfo
import android.view.Surface
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.activities.MainActivity
import com.xda.nobar.activities.helpers.DialogActivity
import com.xda.nobar.activities.ui.HelpAboutActivity
import com.xda.nobar.activities.ui.IntroActivity
import com.xda.nobar.fragments.settings.BasePrefFragment
import com.xda.nobar.interfaces.OnDialogChoiceMadeListener
import com.xda.nobar.receivers.StartupReceiver
import com.xda.nobar.util.helpers.bar.ActionHolder
import com.xda.nobar.util.helpers.bar.ActionManager
import com.xda.nobar.views.BarView
import eu.chainfire.libsuperuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

val mainHandler = Handler(Looper.getMainLooper())

val logicThread = HandlerThread("NoBar-logic", Process.THREAD_PRIORITY_FOREGROUND).apply { start() }
val logicHandler = LogicHandler(logicThread.looper)

val mainScope = CoroutineScope(Dispatchers.Main)
val logicScope = CoroutineScope(Dispatchers.IO)

/* Context */

val Context.actionHolder: ActionHolder
    get() = ActionHolder.getInstance(applicationContext)

val Context.adjustedNavBarHeight
    get() = navBarHeight - if (prefManager.useFullOverscan) 0 else 1

val Context.app: App
    get() = applicationContext as App

val Context.defaultPillBGColor: Int
    get() = ContextCompat.getColor(this, R.color.pill_color)

val Context.defaultPillFGColor: Int
    get() = ContextCompat.getColor(this, R.color.pill_border_color)

val Context.defaultPillDividerColor: Int
    get() = ContextCompat.getColor(this, R.color.pill_divider_color)

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

val isLandscape: Boolean
    get() = cachedRotation.run { this == Surface.ROTATION_90 || this == Surface.ROTATION_270 }

/**
 * Check if the navbar is currently hidden
 * @return true if hidden
 */
val Context.isNavBarHidden: Boolean
    get() {
        val overscan = Rect(0, 0, 0, 0)

        app.wm.defaultDisplay.getOverscanInsets(overscan)

        return overscan.bottom < 0 || overscan.top < 0 || overscan.left < 0 || overscan.right < 0
    }

/**
 * Check if the device is on the KeyGuard (lockscreen)
 */
val Context.isOnKeyguard: Boolean
    get() {
        val kgm = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        return kgm.isKeyguardLocked
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
        val halfScreen = realScreenSize.x / 2f
        val halfWidth = prefManager.customWidth / 2f
        return if (halfScreen == halfWidth) -halfScreen.toInt() else -(halfScreen - halfWidth).toInt()
    }

val Context.minPillYDp: Int
    get() = resources.getInteger(R.integer.min_pill_y_dp)

val Context.minPillYPx: Int
    get() = dpAsPx(minPillYDp)

var uiMode = Configuration.UI_MODE_TYPE_NORMAL

//fun Context.refreshUiMode(): Int {
//    uiMode = (getSystemService(Context.UI_MODE_SERVICE) as UiModeManager)
//            .currentModeType
//
//    return uiMode
//}

private val navLock = Any()

@Volatile
private var cachedCarModeNavHeight: Int? = null
@Volatile
private var cachedNavHeight: Int? = null

fun Context.refreshNavHeights() {
    synchronized(navLock) {
        try {
            cachedCarModeNavHeight = resources.getDimensionPixelSize(resources.getIdentifier("navigation_bar_height_car_mode", "dimen", "android"))
        } catch (e: Exception) {}
        try {
            cachedNavHeight = resources.getDimensionPixelSize(resources.getIdentifier("navigation_bar_height", "dimen", "android"))
        } catch (e: Exception) {}
    }
}

/**
 * Get the height of the navigation bar
 * @return the height of the navigation bar
 */
val Context.navBarHeight: Int
    get() {
        synchronized(navLock) {
            if (cachedCarModeNavHeight == null
                    || cachedNavHeight == null) refreshNavHeights()

            return if (uiMode == Configuration.UI_MODE_TYPE_CAR && prefManager.enableInCarMode) cachedCarModeNavHeight ?: cachedNavHeight!!
            else cachedNavHeight!!
        }
    }

val Context.prefManager: PrefManager
    get() = PrefManager.getInstance(applicationContext)

private var cachedScreenSize: Point? = null

/**
 * Try not to call this from the main Thread
 */
fun Context.refreshScreenSize(): Point {
    val display = app.wm.defaultDisplay

    val temp = Point().apply { display.getRealSize(this) }
    cachedScreenSize = temp

    return cachedScreenSize!!
}

/**
 * Get the device's screen size
 * @return device's resolution (in px) as a Point
 */
val Context.realScreenSize: Point
    get() {
        val temp = unadjustedRealScreenSize

        return cachedRotation.run {
            if (prefManager.anchorPill
                    && (this == Surface.ROTATION_90 || this == Surface.ROTATION_270))
                Point(temp.y, temp.x) else temp
        }
    }

val Context.unadjustedRealScreenSize: Point
    get() = Point(cachedScreenSize ?: refreshScreenSize())

var cachedRotation = Integer.MIN_VALUE

val Context.rotation: Int
    get() {
        return app.wm.defaultDisplay.rotation.also { cachedRotation = it }
    }

var Context.touchWizNavEnabled: Boolean
    get() = Settings.Global.getInt(contentResolver, "navigationbar_hide_bar_enabled", 0) == 0
    set(value) {
        if (hasWss)
            Settings.Global.putString(contentResolver, "navigationbar_hide_bar_enabled", if (value) "0" else null)
    }

val Context.vibrator: Vibrator
    get() = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

fun Context.checkNavHiddenAsync(listener: (Boolean) -> Unit) {
    logicScope.launch {
        val hidden = isNavBarHidden

        mainHandler.post {
            listener.invoke(hidden)
        }
    }
}

/**
 * Convert a certain DP value to its equivalent in px
 * @param dpVal the chosen DP value
 * @return the DP value in terms of px
 */
fun Context.dpAsPx(dpVal: Number) =
        Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpVal.toFloat(), resources.displayMetrics))

fun <T> Context.getSystemServiceCast(name: String): T? {
    return getSystemService(name) as T?
}

fun Context.launchUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    try {
        startActivity(intent)
    } catch (e: Exception) {
        val cbm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cbm.primaryClip = ClipData.newPlainText(url, url)

        Toast.makeText(this, R.string.url_copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }
}

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
    return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
        action.invoke()
        true
    } else {
        DialogActivity.Builder(this).apply {
            title = R.string.nougat_required
            message = R.string.nougat_required_desc
            yesRes = android.R.string.ok
            start()
        }
        false
    }
}

fun Context.runOreoAction(action: () -> Unit): Boolean {
    return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
        action.invoke()
        true
    } else {
        DialogActivity.Builder(this).apply {
            title = R.string.nougat_required
            message = R.string.nougat_required_desc
            yesRes = android.R.string.ok
            start()
        }
        false
    }
}

fun Context.runPieAction(action: () -> Unit): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        action.invoke()
        true
    } else {
        DialogActivity.Builder(this).apply {
            title = R.string.pie_required
            message = R.string.pie_required_desc
            yesRes = android.R.string.ok
            start()
        }
        false
    }
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
                launchUrl("https://play.google.com/store/apps/details?id=com.xda.nobar.premium")
            }
            start()
        }
    }

    return app.isValidPremium
}

fun Context.runSecureSettingsAction(action: () -> Unit) {
    if (hasWss) {
        action.invoke()
    } else {
        IntroActivity.startForWss(this)
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

private var accessibilityActions = ArrayList<Int>()

fun BarView.isAccessibilityAction(action: Int): Boolean {
    if (accessibilityActions.isEmpty()) {
        accessibilityActions.addAll(arrayListOf(
                actionHolder.typeHome,
                actionHolder.typeRecents,
                actionHolder.typeBack,
                actionHolder.typeSwitch,
                actionHolder.typeSplit,
                actionHolder.premTypePower
        ))

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
            accessibilityActions.add(actionHolder.premTypeScreenshot)
            accessibilityActions.add(actionHolder.premTypeLockScreen)
        }
    }

    return accessibilityActions.contains(action)
}

private val rootActions = ArrayList<Int>()

fun BarView.isRootAction(action: Int): Boolean {
    if (rootActions.isEmpty()) {
        rootActions.addAll(arrayListOf(
                actionHolder.typeRootForward,
                actionHolder.typeRootHoldBack,
                actionHolder.typeRootMenu,
                actionHolder.premTypeScreenshot,
                actionHolder.premTypeLockScreen,
                actionHolder.typeRootKeycode,
                actionHolder.typeRootDoubleKeycode,
                actionHolder.typeRootLongKeycode,
                actionHolder.typeRootKillCurrentApp,
                actionHolder.typeRootAccessibilityMenu,
                actionHolder.typeRootChooseAccessibilityMenu
        ))
    }

    return rootActions.contains(action)
}

/* Other */

val navOptions: NavOptions
    get() {
        val builder = NavOptions.Builder()
        builder.setEnterAnim(android.R.anim.fade_in)
        builder.setExitAnim(android.R.anim.fade_out)
        builder.setPopEnterAnim(android.R.anim.fade_in)
        builder.setPopExitAnim(android.R.anim.fade_out)

        return builder.build()
    }

private var cachedSu = false

/**
 * Check if root is available
 *
 * This checks a cached value, set by isSuAsync(), so it may not be up-to-date
 */
val isSu: Boolean
    get() = cachedSu

/**
 * Check for root access asynchronously
 *
 * By default, the result will be posted on the main Thread.
 * This can also be used to refresh the cached value.
 */
fun isSuAsync(resultHandler: Handler, listener: ((Boolean) -> Unit)? = null) {
    isSuAsync { su ->
        listener?.let { resultHandler.post { it.invoke(su) } }
    }
}

fun isSuAsync(listener: ((Boolean) -> Unit)? = null) {
    logicScope.launch {
        refreshSu()

        listener?.invoke(cachedSu)
    }
}

fun refreshSu(): Boolean {
    cachedSu = Shell.SU.available()

    return cachedSu
}

fun Throwable.logStack() {
    val writer = StringWriter()
    val printer = PrintWriter(writer)

    printStackTrace(printer)

    Log.e("NoBar", writer.toString())
}

fun Fragment.navigateTo(action: Int, highlightKey: String? = null) {
    findNavController().run {
        if (currentDestination?.getAction(action) != null) {
            navigate(
                    action,
                    Bundle().apply {
                        putString(BasePrefFragment.PREF_KEY_TO_HIGHLIGHT, highlightKey ?: return@apply)
                    },
                    navOptions
            )
        }
    }
}

fun Context.relaunch(isForCrashlytics: Boolean = false, isForMainActivity: Boolean = false) {
    val startup = when {
        isForCrashlytics -> PendingIntent.getActivity(
                this,
                11,
                Intent(this, HelpAboutActivity::class.java),
                PendingIntent.FLAG_ONE_SHOT
        )
        isForMainActivity -> PendingIntent.getActivity(
                this,
                12,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_ONE_SHOT
        )
        else -> PendingIntent.getBroadcast(
                this,
                10,
                Intent(this, StartupReceiver::class.java).apply { action = StartupReceiver.ACTION_RELAUNCH },
                PendingIntent.FLAG_ONE_SHOT
        )
    }

    val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 100, startup)

    Process.killProcess(Process.myPid())
}

val Display.cachedDisplayInfo: DisplayInfo
    get() = run {
        Display::class.java.getDeclaredField("mDisplayInfo")
                .apply { isAccessible = true }
                .get(this) as DisplayInfo
    }

fun Context.restartApp() {
    val mainActivity = Intent(this, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(this, 100, mainActivity, PendingIntent.FLAG_CANCEL_CURRENT)
    val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager

    am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 100, pendingIntent)
    exitProcess(0)
}

val Context.actionManager: ActionManager
    get() = ActionManager.getInstance(app)

val Context.hasUsage: Boolean
    get() {
        val aom = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

        val hasAppOps = aom.checkOpNoThrow(AppOpsManager.OP_GET_USAGE_STATS, Process.myUid(), packageName) ==
                                AppOpsManager.MODE_ALLOWED
        val hasPerm = checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) ==
                                PackageManager.PERMISSION_GRANTED

        return hasAppOps || hasPerm
    }
