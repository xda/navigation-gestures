package com.xda.nobar

import android.animation.Animator
import android.annotation.SuppressLint
import android.app.Application
import android.app.UiModeManager
import android.content.*
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.*
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.crashlytics.android.Crashlytics
import com.github.anrwatchdog.ANRWatchDog
import com.xda.nobar.activities.IntroActivity
import com.xda.nobar.interfaces.OnGestureStateChangeListener
import com.xda.nobar.interfaces.OnLicenseCheckResultListener
import com.xda.nobar.interfaces.OnNavBarHideStateChangeListener
import com.xda.nobar.providers.BaseProvider
import com.xda.nobar.services.ForegroundService
import com.xda.nobar.services.RootService
import com.xda.nobar.util.*
import com.xda.nobar.util.IWindowManager
import com.xda.nobar.views.BarView
import com.xda.nobar.views.ImmersiveHelperView
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlin.math.absoluteValue


/**
 * Centralize important stuff in the App class, so we can be sure to have an instance of it
 */
class App : Application(), SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        const val EDGE_TYPE_ACTIVE = 2
    }

    private lateinit var wm: WindowManager
    private lateinit var um: UiModeManager
    private lateinit var stateHandler: ScreenStateHandler
    private lateinit var carModeHandler: CarModeHandler
    private lateinit var premiumHelper: PremiumHelper
    private lateinit var premiumInstallListener: PremiumInstallListener
    private lateinit var rootServiceIntent: Intent

    lateinit var uiHandler: UIHandler
    lateinit var bar: BarView
    lateinit var immersiveHelperView: ImmersiveHelperView
    lateinit var prefs: SharedPreferences
    lateinit var immersiveHelper: ImmersiveHelper

    private var isInOtherWindowApp = false

    var isValidPremium: Boolean = false
    var rootBinder: RootService.RootBinder? = null

    var navHidden = false
    var pillShown = false

    private val gestureListeners = ArrayList<OnGestureStateChangeListener>()
    private val navbarListeners = ArrayList<OnNavBarHideStateChangeListener>()
    private val licenseCheckListeners = ArrayList<OnLicenseCheckResultListener>()

    val handler = Handler(Looper.getMainLooper())
    private val rootConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            rootBinder = service as RootService.RootBinder
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            rootBinder = null
        }
    }

    val disabledNavReasonManager = DisabledReasonManager()
    val disabledBarReasonManager = DisabledReasonManager()
    val disabledImmReasonManager = DisabledReasonManager()

    /**
     * Actions and Types
     * *********************************************************
     */
    val actionLeft: String
        get() = resources.getString(R.string.action_left)
    val actionRight: String
        get() = resources.getString(R.string.action_right)
    val actionUp: String
        get() = resources.getString(R.string.action_up)
    val actionDown: String
        get() = resources.getString(R.string.action_down)
    val actionDouble: String
        get() = resources.getString(R.string.action_double)
    val actionHold: String
        get() = resources.getString(R.string.action_hold)
    val actionTap: String
        get() = resources.getString(R.string.action_tap)
    val actionUpHold: String
        get() = resources.getString(R.string.action_up_hold)
    val actionLeftHold: String
        get() = resources.getString(R.string.action_left_hold)
    val actionRightHold: String
        get() = resources.getString(R.string.action_right_hold)

    val actionUpHoldLeft: String
        get() = resources.getString(R.string.action_up_hold_left)
    val actionUpLeft: String
        get() = resources.getString(R.string.action_up_left)

    val actionUpCenter: String
        get() = resources.getString(R.string.action_up_center)
    val actionUpHoldCenter: String
        get() = resources.getString(R.string.action_up_hold_center)

    val actionUpRight: String
        get() = resources.getString(R.string.action_up_right)
    val actionUpHoldRight: String
        get() = resources.getString(R.string.action_up_hold_right)

    val typeNoAction: Int
        get() = resources.getString(R.string.type_no_action).toInt()
    val typeBack: Int
        get() = resources.getString(R.string.type_back).toInt()
    val typeOhm: Int
        get() = resources.getString(R.string.type_ohm).toInt()
    val typeRecents: Int
        get() = resources.getString(R.string.type_recents).toInt()
    val typeHide: Int
        get() = resources.getString(R.string.type_hide).toInt()
    val typeSwitch: Int
        get() = resources.getString(R.string.type_switch).toInt()
    val typeAssist: Int
        get() = resources.getString(R.string.type_assist).toInt()
    val typeHome: Int
        get() = resources.getString(R.string.type_home).toInt()
    val premTypeNotif: Int
        get() = resources.getString(R.string.prem_type_notif).toInt()
    val premTypeQs: Int
        get() = resources.getString(R.string.prem_type_qs).toInt()
    val premTypePower: Int
        get() = resources.getString(R.string.prem_type_power).toInt()
    val typeSplit: Int
        get() = resources.getString(R.string.type_split).toInt()
    val premTypeVibe: Int
        get() = resources.getString(R.string.prem_type_vibe).toInt()
    val premTypeSilent: Int
        get() = resources.getString(R.string.prem_type_silent).toInt()
    val premTypeMute: Int
        get() = resources.getString(R.string.prem_type_mute).toInt()
    val premTypePlayPause: Int
        get() = resources.getString(R.string.prem_type_play_pause).toInt()
    val premTypePrev: Int
        get() = resources.getString(R.string.prem_type_prev).toInt()
    val premTypeNext: Int
        get() = resources.getString(R.string.prem_type_next).toInt()
    val premTypeSwitchIme: Int
        get() = resources.getString(R.string.prem_type_switch_ime).toInt()
    val premTypeLaunchApp: Int
        get() = resources.getString(R.string.prem_type_launch_app).toInt()
    val premTypeLockScreen: Int
        get() = resources.getString(R.string.prem_type_lock_screen).toInt()
    val premTypeScreenshot: Int
        get() = resources.getString(R.string.prem_type_screenshot).toInt()

    val typeRootHoldBack: Int
        get() = resources.getString(R.string.type_hold_back).toInt()
    val typeRootForward: Int
        get() = resources.getString(R.string.type_forward).toInt()
    val typeRootMenu: Int
        get() = resources.getString(R.string.type_menu).toInt()
    val typeRootSleep: Int
        get() = resources.getString(R.string.type_sleep).toInt()
    val typeRootVolUp: Int
        get() = resources.getString(R.string.type_vol_up).toInt()
    val typeRootVolDown: Int
        get() = resources.getString(R.string.type_vol_down).toInt()

    /**
     * ***************************************************************
     */

    override fun onCreate() {
        super.onCreate()

        val watchDog = ANRWatchDog()
        watchDog.start()
        watchDog.setANRListener {
            Crashlytics.logException(it)
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        immersiveHelper = ImmersiveHelper(this)

        if (!Utils.canRunHiddenCommands(this)) {
            val intent = Intent(this, IntroActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            return
        }

        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        um = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        bar = BarView(this)
        immersiveHelperView = ImmersiveHelperView(this)
        stateHandler = ScreenStateHandler()
        carModeHandler = CarModeHandler()
        uiHandler = UIHandler()
        premiumInstallListener = PremiumInstallListener()
        rootServiceIntent = Intent(this, RootService::class.java)

        premiumHelper = PremiumHelper(this, OnLicenseCheckResultListener { valid, reason ->
            Log.e("NoBar", reason)
            isValidPremium = valid
            prefs.edit().putBoolean("valid_prem", valid).apply()

            licenseCheckListeners.forEach { it.onResult(valid, reason) }
        })

        isValidPremium = prefs.getBoolean("valid_prem", false)

        prefs.registerOnSharedPreferenceChangeListener(this)

        refreshPremium()

        if (areGesturesActivated() && !IntroActivity.needsToRun(this)) {
            addBar()
        }

        if (Utils.useRot270Fix(this) || Utils.useTabletMode(this)) uiHandler.handleRot()

        if (!IntroActivity.needsToRun(this)) {
            wm.addView(immersiveHelperView, immersiveHelperView.params)
        }

        immersiveHelperView.viewTreeObserver.addOnGlobalLayoutListener(uiHandler)
        uiHandler.onGlobalLayout()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            "is_active" -> {
                gestureListeners.forEach { it.onGestureStateChange(bar, sharedPreferences.getBoolean(key, false)) }
            }
            "hide_nav" -> {
                navbarListeners.forEach { it.onNavStateChange(sharedPreferences.getBoolean(key, false)) }
            }
            "use_root" -> {
                if (Utils.shouldUseRootCommands(this)) {
                    startService(rootServiceIntent)
                    ensureRootServiceBound()
                } else {
                    stopService(rootServiceIntent)
                }
            }
            "rot270_fix" -> {
                if (Utils.useRot270Fix(this)) uiHandler.handleRot()
            }
            "tablet_mode" -> {
                if (Utils.useTabletMode(this)) uiHandler.handleRot()
            }
            "enable_in_car_mode" -> {
                val enabled = Utils.enableInCarMode(this)
                if (um.currentModeType == Configuration.UI_MODE_TYPE_CAR) {
                    if (enabled) {
                        if (Utils.shouldUseOverscanMethod(this)) {
                            disabledNavReasonManager.removeAll(DisabledReasonManager.NavBarReasons.CAR_MODE)
                        }
                        if (areGesturesActivated() && !pillShown) addBar()
                    } else {
                        if (Utils.shouldUseOverscanMethod(this)
                                && disabledNavReasonManager.add(DisabledReasonManager.NavBarReasons.CAR_MODE)) {
                            showNav()
                        }
                        if (areGesturesActivated() && !pillShown) removeBar()
                    }
                }
            }
            "use_immersive_mode_when_nav_hidden" -> {
                if (Utils.shouldUseOverscanMethod(this) && disabledNavReasonManager.isEmpty()) {
                    if (Utils.useImmersiveWhenNavHidden(this)) {
                        if (disabledImmReasonManager.isEmpty()) {
                            Utils.setNavImmersive(this)
                        }
                    } else {
                        Settings.Global.putString(contentResolver, Settings.Global.POLICY_CONTROL, null)
                    }
                }
                BaseProvider.sendUpdate(this)
            }
            "hide_pill_on_keyboard" -> {
                uiHandler.onGlobalLayout()
            }
            "full_overscan" -> {
                if (Utils.shouldUseOverscanMethod(this)) hideNav(false)
            }
        }
    }

    fun refreshPremium() {
        premiumHelper.checkPremium()
    }

    /**
     * Add an activation listener
     * Notifies caller when a change in activation occurs
     */
    fun addGestureActivationListener(listener: OnGestureStateChangeListener) = gestureListeners.add(listener)

    /**
     * Remove an activation listener
     */
    fun removeGestureActivationListener(listener: OnGestureStateChangeListener) = gestureListeners.remove(listener)

    fun addNavBarHideListener(listener: OnNavBarHideStateChangeListener) = navbarListeners.add(listener)

    fun removeNavbarHideListener(listener: OnNavBarHideStateChangeListener) = navbarListeners.remove(listener)

    fun addLicenseCheckListener(listener: OnLicenseCheckResultListener) = licenseCheckListeners.add(listener)

    fun removeLicenseCheckListener(listener: OnLicenseCheckResultListener) = licenseCheckListeners.remove(listener)

    /**
     * Add the pill to the screen
     */
    fun addBar(callListeners: Boolean = true) {
        if (disabledBarReasonManager.isEmpty()) {
            handler.post {
                pillShown = true
                if (callListeners) gestureListeners.forEach { it.onGestureStateChange(bar, true) }

                bar.params.width = Utils.getCustomWidth(this)
                bar.params.height = Utils.getCustomHeight(this)
                bar.params.gravity = Gravity.CENTER or Gravity.BOTTOM
                bar.params.y = bar.getAdjustedHomeY()
                bar.params.x = bar.getAdjustedHomeX()
                bar.params.type =
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1)
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        else
                            WindowManager.LayoutParams.TYPE_PRIORITY_PHONE
                bar.params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
                        WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                bar.params.format = PixelFormat.TRANSLUCENT
                bar.params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE

                if (Utils.dontMoveForKeyboard(this)) {
                    bar.params.flags = bar.params.flags or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN and
                            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM.inv()
                    bar.params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
                }

                if (Utils.largerHitbox(this)) {
                    val margins = bar.getPillMargins()
                    margins.top = resources.getDimensionPixelSize(R.dimen.pill_margin_top_large_hitbox)
                    bar.changePillMargins(margins)
                }

                addBarInternal()
            }
        }
    }

    /**
     * Remove the pill from the screen
     */
    fun removeBar(callListeners: Boolean = true) {
        if (callListeners) gestureListeners.forEach { it.onGestureStateChange(bar, false) }

        pillShown = false
        bar.hide(object : Animator.AnimatorListener {
            override fun onAnimationCancel(animation: Animator?) {}

            override fun onAnimationRepeat(animation: Animator?) {}

            override fun onAnimationStart(animation: Animator?) {}

            override fun onAnimationEnd(animation: Animator?) {
                try {
                    wm.removeView(bar)
                } catch (e: Exception) {}
            }
        })

        if (!navHidden) stopService(Intent(this, ForegroundService::class.java))
    }

    fun toggleGestureBar() {
        if (!IntroActivity.needsToRun(this)) {
            val shown = isPillShown()
            setGestureState(!shown)
            if (shown) removeBar()
            else addBar()

            BaseProvider.sendUpdate(this)
        }
    }

    fun toggleNavState(hidden: Boolean = Utils.shouldUseOverscanMethod(this)) {
        if (IntroActivity.hasWss(this)) {
            setNavState(!hidden)

            if (hidden) showNav()
            else hideNav()

            BaseProvider.sendUpdate(this)
        } else {
            val activity = Intent(this, IntroActivity::class.java)
            activity.putExtra(IntroActivity.EXTRA_WSS_ONLY, true)
            activity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(activity)
        }
    }

    fun toggleImmersiveWhenNavHidden() {
        val enabled = Utils.useImmersiveWhenNavHidden(this)
        prefs.edit().putBoolean("use_immersive_mode_when_nav_hidden", !enabled).apply()
    }

    /**
     * Check if NoBar is currently active
     * @return true if active
     */
    fun areGesturesActivated() = prefs.getBoolean("is_active", false)

    fun isPillShown() = areGesturesActivated() && pillShown

    /**
     * Check if the navbar is currently hidden
     * @return true if hidden
     */
    fun isNavBarHidden(): Boolean {
        val overscan = getOverscan()

        return overscan.bottom < 0 || overscan.top < 0 || overscan.left < 0 || overscan.right < 0
    }

    /**
     * Hide the navbar
     */
    fun hideNav(callListeners: Boolean = true) {
        if (Utils.shouldUseOverscanMethod(this)
                && disabledNavReasonManager.isEmpty()
                && IntroActivity.hasWss(this)) {
            if (Utils.useImmersiveWhenNavHidden(this)) Utils.setNavImmersive(this)

            if (!Utils.useRot270Fix(this) && !Utils.useTabletMode(this))
                IWindowManager.setOverscan(0, 0, 0, -getAdjustedNavBarHeight())
            else {
                uiHandler.handleRot()
            }
            Utils.forceNavBlack(this)
            Utils.forceTouchWizNavEnabled(this)

            if (callListeners) navbarListeners.forEach { it.onNavStateChange(true) }
            navHidden = true

            ContextCompat.startForegroundService(this, Intent(this, ForegroundService::class.java))
        }
    }

    /**
     * Show the navbar
     */
    fun showNav(callListeners: Boolean = true, removeImmersive: Boolean = true) {
        if (IntroActivity.hasWss(this)) {
            if (removeImmersive && Utils.useImmersiveWhenNavHidden(this)) Settings.Global.putString(contentResolver, Settings.Global.POLICY_CONTROL, null)

            if (callListeners) navbarListeners.forEach { it.onNavStateChange(false) }

            IWindowManager.setOverscan(0, 0, 0, 0)
            Utils.clearBlackNav(this)
            Utils.undoForceTouchWizNavEnabled(this)

            navHidden = false

            if (!areGesturesActivated()) stopService(Intent(this, ForegroundService::class.java))
        }
    }

    fun ensureRootServiceBound() = bindService(rootServiceIntent, rootConnection, 0)

    /**
     * Get the current screen overscan
     * @return the overscan as a Rect
     */
    fun getOverscan(): Rect {
        val rect = Rect(0, 0, 0, 0)

        (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getOverscanInsets(rect)

        return rect
    }

    /**
     * Save the current NoBar gesture state to preferences
     */
    fun setGestureState(activated: Boolean) = prefs.edit().putBoolean("is_active", activated).apply()

    fun setNavState(hidden: Boolean) = prefs.edit().putBoolean("hide_nav", hidden).apply()

    fun getAdjustedNavBarHeight() =
            Utils.getNavBarHeight(this) - if (Utils.useFullOverscan(this)) 0 else 1

    private fun addBarInternal() {
        try {
            wm.removeView(bar)
        } catch (e: Exception) {}

        wm.addView(bar, bar.params)
        ContextCompat.startForegroundService(this, Intent(this, ForegroundService::class.java))

        if (Utils.shouldUseRootCommands(this)) {
            startService(rootServiceIntent)
            ensureRootServiceBound()
        }

        bar.show(null)
    }

    fun runAsync(action: () -> Unit) {
        runAsync(action, null)
    }

    fun runAsync(action: () -> Unit, listener: (() -> Unit)?) {
        val thread = Schedulers.newThread()
        Observable.fromCallable(action)
                .subscribeOn(thread)
                .observeOn(thread)
                .subscribe {
                    thread.createWorker().dispose()
                    listener?.invoke()
                }
    }

    /**
     * Listen for changes in the screen state and handle appropriately
     */
    inner class ScreenStateHandler : BroadcastReceiver() {
        init {
            val filter = IntentFilter()
            filter.addAction(Intent.ACTION_BOOT_COMPLETED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                filter.addAction(Intent.ACTION_LOCKED_BOOT_COMPLETED)
            }
            filter.addAction(Intent.ACTION_SCREEN_ON)
            filter.addAction(Intent.ACTION_USER_PRESENT)
            filter.addAction(Intent.ACTION_SCREEN_OFF)
            filter.addAction(Intent.ACTION_REBOOT)
            filter.addAction(Intent.ACTION_SHUTDOWN)

            registerReceiver(this, filter)
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            if (!IntroActivity.needsToRun(this@App)) {
                handler.postDelayed({
                    val action = intent?.action
                    when (action) {
                        Intent.ACTION_REBOOT,
                        Intent.ACTION_SHUTDOWN,
                        Intent.ACTION_SCREEN_OFF -> {
                            if (Utils.shouldUseOverscanMethod(this@App)
                                    && disabledNavReasonManager.add(DisabledReasonManager.NavBarReasons.KEYGUARD)) {
                                showNav()
                            }
                        }
                        Intent.ACTION_SCREEN_ON,
                        Intent.ACTION_BOOT_COMPLETED,
                        Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                            if (Utils.isOnKeyguard(this@App)) {
                                if (Utils.shouldUseOverscanMethod(this@App)
                                        && disabledNavReasonManager.add(DisabledReasonManager.NavBarReasons.KEYGUARD)) {
                                    showNav()
                                }
                            } else {
                                if (Utils.shouldUseOverscanMethod(this@App)) {
                                    disabledNavReasonManager.removeAll(DisabledReasonManager.NavBarReasons.KEYGUARD)
                                }
                                if (areGesturesActivated() && !pillShown) addBar()
                            }
                        }
                        Intent.ACTION_USER_PRESENT -> {
                            if (Utils.shouldUseOverscanMethod(this@App)) {
                                disabledNavReasonManager.removeAll(DisabledReasonManager.NavBarReasons.KEYGUARD)
                            }
                            if (areGesturesActivated() && !pillShown) addBar()
                        }
                    }
                }, 50)
            }
        }
    }

    /**
     * Listen for changes in Car Mode (Android Auto, etc)
     * We want to disable NoBar when Car Mode is active
     */
    inner class CarModeHandler : BroadcastReceiver() {
        init {
            val filter = IntentFilter()
            filter.addAction(UiModeManager.ACTION_ENTER_CAR_MODE)
            filter.addAction(UiModeManager.ACTION_EXIT_CAR_MODE)

            registerReceiver(this, filter)
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                UiModeManager.ACTION_ENTER_CAR_MODE -> {
                    if (Utils.enableInCarMode(this@App)) {
                        if (pillShown) bar.params.height = Utils.getCustomHeight(this@App) * 2
                    } else {
                        if (pillShown) removeBar()
                        if (Utils.shouldUseOverscanMethod(this@App)
                                && disabledNavReasonManager.add(DisabledReasonManager.NavBarReasons.CAR_MODE)) {
                            showNav()
                        }
                    }
                }

                UiModeManager.ACTION_EXIT_CAR_MODE -> {
                    if (Utils.enableInCarMode(this@App)) {
                        if (pillShown) bar.params.height = Utils.getCustomHeight(this@App)
                    } else {
                        if (areGesturesActivated() && !pillShown) addBar()
                        if (Utils.shouldUseOverscanMethod(this@App)) {
                            disabledNavReasonManager.removeAll(DisabledReasonManager.NavBarReasons.CAR_MODE)
                        }
                    }
                }
            }

            if (pillShown) {
                bar.updateLayout(bar.params)
            }

            if (Utils.shouldUseOverscanMethod(this@App)) {
                if (Utils.enableInCarMode(this@App)) hideNav()
            }
        }
    }

    /**
     * Basically does everything that needs to be dynamically managed
     * Listens for changes in Immersive Mode and adjusts appropriately
     * Listens for changes in rotation and adjusts appropriately
     * Listens for TouchWiz navbar hiding and coloring and adjusts appropriately
     * //TODO: More work may be needed on immersive detection
     */
    inner class UIHandler : ContentObserver(handler), View.OnSystemUiVisibilityChangeListener, ViewTreeObserver.OnGlobalLayoutListener {
        private var oldRot = Surface.ROTATION_0
        private var isActing = false

        init {
            contentResolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.POLICY_CONTROL), true, this)
            contentResolver.registerContentObserver(Settings.Global.getUriFor("navigationbar_hide_bar_enabled"), true, this)
            contentResolver.registerContentObserver(Settings.Global.getUriFor("navigationbar_color"), true, this)
            contentResolver.registerContentObserver(Settings.Global.getUriFor("navigationbar_current_color"), true, this)
            contentResolver.registerContentObserver(Settings.Global.getUriFor("navigationbar_use_theme_default"), true, this)

            bar.immersiveNav = Settings.Global.getString(contentResolver, Settings.Global.POLICY_CONTROL)?.contains("navigation") ?: false
        }

        fun setNodeInfoAndUpdate(info: AccessibilityNodeInfo?) {
            onGlobalLayout()
            if (info != null) handleNewNodeInfo(info)
        }

        @SuppressLint("WrongConstant")
        private fun handleNewNodeInfo(info: AccessibilityNodeInfo) {
            val pName = info.packageName?.toString()

            runAsync {
                val navArray = ArrayList<String>().apply { Utils.loadBlacklistedNavPackages(this@App, this) }
                if (navArray.contains(pName)) {
                    if (!disabledNavReasonManager.contains(DisabledReasonManager.NavBarReasons.NAV_BLACKLIST)) {
                        if (Utils.shouldUseOverscanMethod(this@App)
                                && disabledNavReasonManager.add(DisabledReasonManager.NavBarReasons.NAV_BLACKLIST)) {
                            showNav(false, false)
                        }
                        onGlobalLayout()
                    }
                } else {
                    if (Utils.shouldUseOverscanMethod(this@App)) {
                        disabledNavReasonManager.removeAll(DisabledReasonManager.NavBarReasons.NAV_BLACKLIST)

                    }
                    onGlobalLayout()
                }
            }

            runAsync {
                val barArray = ArrayList<String>().apply { Utils.loadBlacklistedBarPackages(this@App, this) }
                if (barArray.contains(pName)) {
                    if (disabledBarReasonManager.add(DisabledReasonManager.PillReasons.BLACKLIST)) {
                        if (areGesturesActivated()) {
                            removeBar(false)
                        }
                    }
                } else {
                    if (areGesturesActivated() && disabledBarReasonManager.removeAll(DisabledReasonManager.PillReasons.BLACKLIST)) {
                        if (!pillShown) addBar(false)
                    }
                }
            }

            runAsync {
                val immArray = ArrayList<String>().apply { Utils.loadBlacklistedImmPackages(this@App, this) }
                if (immArray.contains(pName)) {
                    if (Utils.shouldUseOverscanMethod(this@App)
                            && Utils.useImmersiveWhenNavHidden(this@App)
                            && disabledImmReasonManager.add(DisabledReasonManager.ImmReasons.BLACKLIST)) {
                        Settings.Global.putString(contentResolver, Settings.Global.POLICY_CONTROL, null)
                    }
                } else {
                    if (Utils.shouldUseOverscanMethod(this@App)
                            && Utils.useImmersiveWhenNavHidden(this@App)
                            && disabledImmReasonManager.removeAll(DisabledReasonManager.ImmReasons.BLACKLIST)) {
                        Utils.setNavImmersive(this@App)
                    }
                }
            }

            runAsync {
                if (pName != packageName) {
                    val windowArray = ArrayList<String>().apply { Utils.loadOtherWindowApps(this@App, this) }
                    if (windowArray.contains(pName)) {
                        if (!isInOtherWindowApp) {
                            addBar(false)
                            isInOtherWindowApp = true
                        }
                    } else if (isInOtherWindowApp) isInOtherWindowApp = false
                }
            }
        }

        @SuppressLint("WrongConstant")
        override fun onGlobalLayout() {
            if (packageManager.hasSystemFeature("com.samsung.feature.samsung_experience_mobile")) {
                runAsync {
                    try {
                        val SemCocktailBarManager = Class.forName("com.samsung.android.cocktailbar.SemCocktailBarManager")

                        val manager = getSystemService("CocktailBarService")

                        val getCocktailBarWindowType = SemCocktailBarManager.getMethod("getCocktailBarWindowType")

                        val edgeType = getCocktailBarWindowType.invoke(manager).toString().toInt()

                        if (edgeType == EDGE_TYPE_ACTIVE) {
                            if (Utils.shouldUseOverscanMethod(this@App)
                                    && Utils.useImmersiveWhenNavHidden(this@App)
                                    && disabledImmReasonManager.add(DisabledReasonManager.ImmReasons.EDGE_SCREEN)) {
                                Settings.Global.putString(contentResolver, Settings.Global.POLICY_CONTROL, null)
                            }
                        } else {
                            if (Utils.shouldUseOverscanMethod(this@App)
                                    && Utils.useImmersiveWhenNavHidden(this@App)
                                    && disabledImmReasonManager.removeAll(DisabledReasonManager.ImmReasons.EDGE_SCREEN)) {
                                Utils.setNavImmersive(this@App)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            if (!isNavBarHidden() && Utils.shouldUseOverscanMethod(this@App)) hideNav()

            if (isPillShown()) {
                val rot = wm.defaultDisplay.rotation
                if (oldRot != rot) {
                    runAsync {
                        handleRot()
                    }

                    oldRot = rot
                }

                if (!isActing) {
                    isActing = true
                    handler.postDelayed({
                        runAsync {
                            val screenRes = Utils.getRealScreenSize(this@App)
                            val overscan = getOverscan()

                            val rect = Rect()
                            immersiveHelperView.getWindowVisibleDisplayFrame(rect)

                            val screenHeight = Utils.getRealScreenSize(this@App).y

                            val isKeyboardProbablyShown = rect.bottom <
                                    if (IWindowManager.hasNavigationBar()) screenHeight - Utils.getNavBarHeight(this@App) else screenHeight

                            bar.immersiveNav = Settings.Global.getString(contentResolver, Settings.Global.POLICY_CONTROL)?.contains("navigation") ?: false
                                    && !isKeyboardProbablyShown

                            if (Utils.hidePillWhenKeyboardShown(this@App) && !bar.isCarryingOutTouchAction) {
                                if (isKeyboardProbablyShown) bar.hidePill(true, HiddenPillReasonManager.KEYBOARD)
                                else if (bar.hiddenPillReasons.onlyContains(HiddenPillReasonManager.KEYBOARD)) {
                                    bar.showPill(HiddenPillReasonManager.KEYBOARD)
                                }
                            }

                            val insets = Rect()
                            IWindowManager.getStableInsetsForDefaultDisplay(insets)

                            val height = Point(screenRes.x - rect.left - rect.right,
                                    screenRes.y - rect.top - rect.bottom)

                            val totalOverscan = overscan.left + overscan.top + overscan.right + overscan.bottom

                            val hidden = when {
                                (wm.defaultDisplay.rotation == Surface.ROTATION_270
                                        || wm.defaultDisplay.rotation == Surface.ROTATION_90)
                                        && !Utils.useTabletMode(this@App) -> height.x.absoluteValue == totalOverscan.absoluteValue
                                else -> height.y.absoluteValue == totalOverscan.absoluteValue
                            }

                            handleImmersiveChange(hidden)
                            isActing = false
                        }
                    }, 50)
                }
            }
        }

        override fun onSystemUiVisibilityChange(visibility: Int) {
            handleImmersiveChange(visibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION != 0
                    || visibility and View.SYSTEM_UI_FLAG_FULLSCREEN != 0
                    || visibility and View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN != 0
                    || visibility and 7 != 0)
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            runAsync {
                if (uri == Settings.Global.getUriFor(Settings.Global.POLICY_CONTROL)) {
                    val current = Settings.Global.getString(contentResolver, Settings.Global.POLICY_CONTROL) ?: ""

                    bar.immersiveNav = current.contains("nav")
                    handleImmersiveChange(current.contains("full"))
                }
                if (uri == Settings.Global.getUriFor("navigationbar_hide_bar_enabled")) {
                    if (Utils.shouldUseOverscanMethod(this@App)) {
                        try {
                            val current = Settings.Global.getInt(contentResolver, "navigationbar_hide_bar_enabled")

                            if (current != 0) {
                                Settings.Global.putInt(contentResolver, "navigationbar_hide_bar_enabled", 0)

                                Toast.makeText(this@App, resources.getText(R.string.feature_not_avail), Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Settings.SettingNotFoundException) {}
                    }
                }
                if (uri == Settings.Global.getUriFor("navigationbar_color")
                        || uri == Settings.Global.getUriFor("navigationbar_current_color")
                        || uri == Settings.Global.getUriFor("navigationbar_use_theme_default")) {
                    if (isNavBarHidden()) Utils.forceNavBlack(this@App)
                }
            }
        }

        private fun handleImmersiveChange(isImmersive: Boolean) {
            if (!IntroActivity.needsToRun(this@App)) {
                bar.isImmersive = isImmersive
                val hideInFullScreen = Utils.hideInFullscreen(this@App)
                if (isImmersive) {
                    if (Utils.shouldUseOverscanMethod(this@App)) {
                        if (Utils.origBarInFullscreen(this@App)
                                && disabledNavReasonManager.add(DisabledReasonManager.NavBarReasons.IMMERSIVE)) showNav()
                        else hideNav()
                    }
                    if (hideInFullScreen) bar.hidePill(true, HiddenPillReasonManager.FULLSCREEN)
                } else {
                    if (Utils.shouldUseOverscanMethod(this@App)) {
                        disabledNavReasonManager.removeAll(DisabledReasonManager.NavBarReasons.IMMERSIVE)
                    }
                    if (hideInFullScreen && bar.hiddenPillReasons.onlyContains(HiddenPillReasonManager.FULLSCREEN)) {
                        bar.showPill(HiddenPillReasonManager.FULLSCREEN)
                    }
                }
            }
        }

        fun handleRot() {
            runAsync {
                bar.params.x = bar.getAdjustedHomeX()
                bar.params.y = bar.getAdjustedHomeY()
                bar.params.width = Utils.getCustomWidth(this@App)
                bar.params.height = Utils.getCustomHeight(this@App)
                bar.updateLayout(bar.params)
            }

            if (Utils.shouldUseOverscanMethod(this@App)) {
                if (Utils.useRot270Fix(this@App)) handle270()
                if (Utils.useTabletMode(this@App)) handleTablet()
            }
        }

        private fun handle270() {
            runAsync {
                if (wm.defaultDisplay.rotation == Surface.ROTATION_270 || wm.defaultDisplay.rotation == Surface.ROTATION_180) {
                    IWindowManager.setOverscan(0, -getAdjustedNavBarHeight(), 0, 0)
                } else {
                    IWindowManager.setOverscan(0, 0, 0, -getAdjustedNavBarHeight())
                }
            }
        }

        private fun handleTablet() {
            if (Utils.shouldUseOverscanMethod(this@App)) {
                runAsync {
                    when (wm.defaultDisplay.rotation) {
                        Surface.ROTATION_0 -> {
                            IWindowManager.setOverscan(0, 0, 0, -getAdjustedNavBarHeight())
                        }

                        Surface.ROTATION_90 -> {
                            IWindowManager.setOverscan(-getAdjustedNavBarHeight(), 0, 0, 0)
                        }

                        Surface.ROTATION_180 -> {
                            IWindowManager.setOverscan(0, -getAdjustedNavBarHeight(), 0 ,0)
                        }

                        Surface.ROTATION_270 -> {
                            IWindowManager.setOverscan(0, 0, -getAdjustedNavBarHeight(), 0)
                        }
                    }
                }
            }
        }
    }

    /**
     * Listen to see if the premium add-on has been installed/uninstalled, and refresh the premium state
     */
    inner class PremiumInstallListener : BroadcastReceiver() {
        init {
            val filter = IntentFilter()
            filter.addAction(Intent.ACTION_PACKAGE_ADDED)
            filter.addAction(Intent.ACTION_PACKAGE_CHANGED)
            filter.addAction(Intent.ACTION_PACKAGE_REPLACED)
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED)
            filter.addDataScheme("package")

            registerReceiver(this, filter)
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_PACKAGE_ADDED
                    || intent?.action == Intent.ACTION_PACKAGE_CHANGED
                    || intent?.action == Intent.ACTION_PACKAGE_REPLACED
                    || intent?.action == Intent.ACTION_PACKAGE_REMOVED) {
                if (intent.dataString.contains("com.xda.nobar.premium")) {
                    refreshPremium()
                }
            }
        }
    }
}