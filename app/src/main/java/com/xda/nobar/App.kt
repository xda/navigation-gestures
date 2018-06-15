package com.xda.nobar

import android.animation.Animator
import android.app.Application
import android.app.KeyguardManager
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
import android.widget.Toast
import com.xda.nobar.activities.IntroActivity
import com.xda.nobar.services.ForegroundService
import com.xda.nobar.services.RootService
import com.xda.nobar.util.IWindowManager
import com.xda.nobar.util.PremiumHelper
import com.xda.nobar.util.Utils
import com.xda.nobar.util.Utils.getCustomHeight
import com.xda.nobar.util.Utils.getCustomWidth
import com.xda.nobar.util.Utils.getHomeX
import com.xda.nobar.views.BarView
import com.xda.nobar.views.ImmersiveHelperView
import kotlin.math.absoluteValue


/**
 * Centralize important stuff in the App class, so we can be sure to have an instance of it
 */
class App : Application(), SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var wm: WindowManager
    private lateinit var um: UiModeManager
    private lateinit var kgm: KeyguardManager
    private lateinit var stateHandler: ScreenStateHandler
    private lateinit var carModeHandler: CarModeHandler
    private lateinit var premiumHelper: PremiumHelper
    private lateinit var premiumInstallListener: PremiumInstallListener
    private lateinit var rootServiceIntent: Intent

    var isValidPremium: Boolean = false
    var rootBinder: RootService.RootBinder? = null

    private var navHidden = false
    private var pillShown = false

    lateinit var uiHandler: UIHandler
    lateinit var bar: BarView
    lateinit var immersiveHelperView: ImmersiveHelperView
    lateinit var prefs: SharedPreferences

    private val gestureListeners = ArrayList<GestureActivationListener>()
    private val navbarListeners = ArrayList<NavBarHideListener>()
    private val handler = Handler(Looper.getMainLooper())
    private val rootConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            rootBinder = service as RootService.RootBinder
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            rootBinder = null
        }
    }

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

    val typeRootHoldBack: Int
        get() = resources.getString(R.string.type_hold_back).toInt()
    val typeRootForward: Int
        get() = resources.getString(R.string.type_forward).toInt()
    val typeRootMenu: Int
        get() = resources.getString(R.string.type_menu).toInt()
    val typeRootSleep: Int
        get() = resources.getString(R.string.type_sleep).toInt()
    val premTypeRootVolUp: Int
        get() = resources.getString(R.string.prem_type_vol_up).toInt()
    val premTypeRootVolDown: Int
        get() = resources.getString(R.string.prem_type_vol_down).toInt()
    val premTypeRootScreenshot: Int
        get() = resources.getString(R.string.prem_type_screenshot).toInt()

    /**
     * ***************************************************************
     */

    override fun onCreate() {
        super.onCreate()
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        if (!Utils.canRunHiddenCommands(this)) {
            val intent = Intent(this, IntroActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            return
        }

        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        um = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        kgm = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        bar = BarView(this)
        immersiveHelperView = ImmersiveHelperView(this)
        stateHandler = ScreenStateHandler()
        carModeHandler = CarModeHandler()
        uiHandler = UIHandler()
        premiumInstallListener = PremiumInstallListener()
        rootServiceIntent = Intent(this, RootService::class.java)

        premiumHelper = PremiumHelper(this, object : LicenseCheckListener {
            override fun onResult(valid: Boolean, reason: String) {
                Log.e("NoBar", reason)
                isValidPremium = valid
                prefs.edit().putBoolean("valid_prem", valid).apply()
            }
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
                gestureListeners.forEach { it.onChange(sharedPreferences.getBoolean(key, false)) }
            }
            "hide_nav" -> {
                navbarListeners.forEach { it.onNavChange(sharedPreferences.getBoolean(key, false)) }
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
            "use_car_mode" -> {
                val enabled = Utils.enableInCarMode(this)
                if (um.currentModeType == Configuration.UI_MODE_TYPE_CAR) {
                    if (enabled) {
                        if (Utils.shouldUseOverscanMethod(this)) hideNav()
                        if (areGesturesActivated()) addBar()
                    } else {
                        if (Utils.shouldUseOverscanMethod(this)) showNav()
                        if (areGesturesActivated()) removeBar()
                    }
                }
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
    fun addGestureActivationListener(listener: GestureActivationListener) {
        gestureListeners.add(listener)
    }

    /**
     * Remove an activation listener
     */
    fun removeGestureActivationListener(listener: GestureActivationListener) {
        gestureListeners.remove(listener)
    }

    fun addNavBarHideListener(listener: NavBarHideListener) {
        navbarListeners.add(listener)
    }

    fun removeNavbarHideListener(listener: NavBarHideListener) {
        navbarListeners.remove(listener)
    }

    /**
     * Add the pill to the screen
     */
    fun addBar() {
        pillShown = true
        gestureListeners.forEach { it.onChange(true) }

        bar.params.width = Utils.getCustomWidth(this)
        bar.params.height = Utils.getCustomHeight(this)
        bar.params.gravity = Gravity.CENTER or Gravity.BOTTOM
        bar.params.y = bar.getAdjustedHomeY()
        bar.params.x = getHomeX(this)
        bar.params.type =
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE
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
//        bar.setOnSystemUiVisibilityChangeListener(uiHandler)
    }

    /**
     * Remove the pill from the screen
     */
    fun removeBar() {
        pillShown = false
        gestureListeners.forEach { it.onChange(false) }
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
    }

    /**
     * Toggle whether the pill is shown or hidden
     */
    fun toggle() {
        toggle(areGesturesActivated())
    }

    /**
     * Toggle whether the pill is shown or hidden
     * @param currentState the current state of the pill; if 'true' is passed, NoBar will deactivate; 'false' will activate it
     */
    fun toggle(currentState: Boolean) {
        if (IntroActivity.needsToRun(this)) {
            val intent = Intent(this, IntroActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } else {
            val inCarMode = um.currentModeType == Configuration.UI_MODE_TYPE_CAR
            val state = inCarMode || currentState

            if (state) {
                if (!Utils.shouldUseOverscanMethod(this)) showNav()
                removeBar()
                stopService(Intent(this@App, ForegroundService::class.java))
                stopService(rootServiceIntent)
            } else {
                hideNav()
                addBar()
            }

            setGestureState(!state)
        }
    }

    fun toggleGestureBar() {
        val shown = isPillShown()
        setGestureState(!shown)
        if (shown) removeBar()
        else addBar()
    }

    /**
     * Check if NoBar is currently active
     * @return true if active
     */
    fun areGesturesActivated(): Boolean {
        return prefs.getBoolean("is_active", false)
    }

    fun isPillShown(): Boolean {
        return areGesturesActivated() && pillShown
    }

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
    fun hideNav() {
        Utils.saveBackupImmersive(this)
        if (Utils.isInImmersive(this) && Utils.origBarInFullscreen(this)) {
            Utils.disableNavImmersive(this)
        }

        if (Utils.shouldUseOverscanMethod(this)) {
            if (!Utils.useRot270Fix(this) && !Utils.useTabletMode(this)) IWindowManager.setOverscan(0, 0, 0, -Utils.getNavBarHeight(this) + 1)
            else {
                uiHandler.handleRot()
            }
            Utils.forceNavBlack(this)
            Utils.forceTouchWizNavEnabled(this)

            navbarListeners.forEach { it.onNavChange(true) }
            navHidden = true
        }

    }

    /**
     * Show the navbar
     */
    fun showNav() {
        if (!Utils.isInImmersive(this) && Utils.origBarInFullscreen(this))
            Settings.Global.putString(contentResolver, Settings.Global.POLICY_CONTROL, Utils.getBackupImmersive(this))

        navbarListeners.forEach { it.onNavChange(false) }

        IWindowManager.setOverscan(0, 0, 0, 0)
        Utils.clearBlackNav(this)
        Utils.undoForceTouchWizNavEnabled(this)

        navHidden = false
    }

    fun ensureRootServiceBound(): Boolean {
        return bindService(rootServiceIntent, rootConnection, 0)
    }

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
    fun setGestureState(activated: Boolean) {
        prefs.edit().putBoolean("is_active", activated).apply()
    }

    /**
     * Listen for changes in the screen state and handle appropriately
     */
    inner class ScreenStateHandler : BroadcastReceiver() {
        init {
            val filter = IntentFilter()
            filter.addAction(Intent.ACTION_SCREEN_ON)
            filter.addAction(Intent.ACTION_USER_PRESENT)

            registerReceiver(this, filter)
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            if (!IntroActivity.needsToRun(this@App)) {
                handler.postDelayed({
                    when (intent?.action) {
                        Intent.ACTION_SCREEN_ON -> {
                            if (Utils.shouldUseOverscanMethod(this@App)) {
                                if (kgm.inKeyguardRestrictedInputMode()
                                        || kgm.isKeyguardLocked
                                        || (if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) kgm.isDeviceLocked else false)) {
                                    showNav()
                                } else {
                                    hideNav()
                                }
                            }
                        }
                        Intent.ACTION_USER_PRESENT -> {
                            if (areGesturesActivated()) addBar()
                            if (Utils.shouldUseOverscanMethod(this@App)) hideNav()
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
            if (areGesturesActivated()) {
                when (intent?.action) {
                    UiModeManager.ACTION_ENTER_CAR_MODE -> {
                        if (Utils.enableInCarMode(this@App)) bar.params.height = Utils.getCustomHeight(this@App) * 2
                        else {
                            removeBar()
                            if (Utils.shouldUseOverscanMethod(this@App)) showNav()
                        }
                    }

                    UiModeManager.ACTION_EXIT_CAR_MODE -> {
                        if (Utils.enableInCarMode(this@App)) bar.params.height = Utils.getCustomHeight(this@App)
                        else {
                            addBar()
                            if (Utils.shouldUseOverscanMethod(this@App)) hideNav()
                        }
                    }
                }

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
        private var isDisabledForContent = false
        private var oldRot = Surface.ROTATION_0

        private var isActing = false

        init {
            contentResolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.POLICY_CONTROL), true, this)
            contentResolver.registerContentObserver(Settings.Global.getUriFor("navigationbar_hide_bar_enabled"), true, this)
            contentResolver.registerContentObserver(Settings.Global.getUriFor("navigationbar_color"), true, this)
            contentResolver.registerContentObserver(Settings.Global.getUriFor("navigationbar_current_color"), true, this)
            contentResolver.registerContentObserver(Settings.Global.getUriFor("navigationbar_use_theme_default"), true, this)
        }

        override fun onGlobalLayout() {
            if (isPillShown()) {
                val rot = wm.defaultDisplay.rotation
                if (oldRot != rot) {
                    bar.params.x = getHomeX(this@App)
                    bar.params.y = bar.getAdjustedHomeY()
                    bar.params.width = getCustomWidth(this@App)
                    bar.params.height = getCustomHeight(this@App)
                    bar.updateLayout(bar.params)
                    oldRot = rot
                    handleRot()
                }

                if (!isActing) handler.postDelayed({
                    val screenRes = Utils.getRealScreenSize(this@App)
                    val overscan = getOverscan()

                    val rect = Rect()
                    immersiveHelperView.getWindowVisibleDisplayFrame(rect)

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
                }, 50)
            }
        }

        override fun onSystemUiVisibilityChange(visibility: Int) {
            handleImmersiveChange(visibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION != 0
                    || visibility and View.SYSTEM_UI_FLAG_FULLSCREEN != 0
                    || visibility and View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN != 0
                    || visibility and 7 != 0)
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
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

        private fun handleImmersiveChange(isImmersive: Boolean) {
            if (!IntroActivity.needsToRun(this@App)) {
                bar.isImmersive = isImmersive
                val hideInFullScreen = Utils.hideInFullscreen(this@App)
                if (isImmersive) {
                    if (!isDisabledForContent) {
                        if (Utils.shouldUseOverscanMethod(this@App) && Utils.origBarInFullscreen(this@App)) showNav()
                        if (hideInFullScreen) bar.hidePill(true)
                        isDisabledForContent = true
                    }
                } else if (isDisabledForContent) {
                    if (Utils.shouldUseOverscanMethod(this@App)) hideNav()
                    if (hideInFullScreen && bar.isAutoHidden) {
                        bar.isAutoHidden = false
                        bar.showPill(true)
                    }
                    isDisabledForContent = false
                }
            }
        }

        fun handleRot() {
            if (Utils.useRot270Fix(this@App)) handle270()
            if (Utils.useTabletMode(this@App)) handleTablet()
        }

        private fun handle270() {
            if (wm.defaultDisplay.rotation == Surface.ROTATION_270 || wm.defaultDisplay.rotation == Surface.ROTATION_180) {
                IWindowManager.setOverscan(0, -Utils.getNavBarHeight(this@App) + 1, 0, 0)
            } else {
                IWindowManager.setOverscan(0, 0, 0, -Utils.getNavBarHeight(this@App) + 1)
            }
        }

        private fun handleTablet() {
            if (Utils.shouldUseOverscanMethod(this@App) && !Utils.isInImmersive(this@App)) {
                when (wm.defaultDisplay.rotation) {
                    Surface.ROTATION_0 -> {
                        IWindowManager.setOverscan(0, 0, 0, -Utils.getNavBarHeight(this@App) + 1)
                    }

                    Surface.ROTATION_90 -> {
                        IWindowManager.setOverscan(-Utils.getNavBarHeight(this@App) + 1, 0, 0, 0)
                    }

                    Surface.ROTATION_180 -> {
                        IWindowManager.setOverscan(0, -Utils.getNavBarHeight(this@App) + 1, 0 ,0)
                    }

                    Surface.ROTATION_270 -> {
                        IWindowManager.setOverscan(0, 0, -Utils.getNavBarHeight(this@App) + 1, 0)
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

    /**
     * Used to listen for changes in activation
     */
    interface GestureActivationListener {
        fun onChange(activated: Boolean)
    }

    interface NavBarHideListener {
        fun onNavChange(hidden: Boolean)
    }

    /**
     * Used to listen for changes in premium state
     */
    interface LicenseCheckListener {
        fun onResult(valid: Boolean, reason: String)
    }
}