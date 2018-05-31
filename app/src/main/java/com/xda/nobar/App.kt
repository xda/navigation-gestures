package com.xda.nobar

import android.animation.Animator
import android.app.Application
import android.app.KeyguardManager
import android.app.UiModeManager
import android.content.*
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.PixelFormat
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
import com.xda.nobar.activities.IntroActivity
import com.xda.nobar.services.ForegroundService
import com.xda.nobar.services.RootService
import com.xda.nobar.util.IWindowManager
import com.xda.nobar.util.PremiumHelper
import com.xda.nobar.util.Utils
import com.xda.nobar.util.Utils.getHomeX
import com.xda.nobar.util.Utils.getHomeY
import com.xda.nobar.views.BarView
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
    private lateinit var prefs: SharedPreferences
    private lateinit var premiumHelper: PremiumHelper
    private lateinit var premiumInstallListener: PremiumInstallListener
    private lateinit var compatibilityRotationListener: CompatibilityRotationListener
    private lateinit var rootServiceIntent: Intent

    var isValidPremium: Boolean = false
    var rootBinder: RootService.RootBinder? = null

    private var navHidden = false
    private var pillShown = false

    lateinit var immersiveListener: ImmersiveListener
    lateinit var bar: BarView

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

    val params = WindowManager.LayoutParams()
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
        stateHandler = ScreenStateHandler()
        carModeHandler = CarModeHandler()
        immersiveListener = ImmersiveListener()
        premiumInstallListener = PremiumInstallListener()
        compatibilityRotationListener = CompatibilityRotationListener()
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
        if (isNavBarHidden()) compatibilityRotationListener.enable()
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

        params.width = Utils.getCustomWidth(this)
        params.height = Utils.getCustomHeight(this)
        params.gravity = Gravity.CENTER or Gravity.BOTTOM
        params.y = getHomeY(this)
        params.x = getHomeX(this)
        params.type =
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
                WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
        params.format = PixelFormat.TRANSLUCENT
        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE

        if (Utils.dontMoveForKeyboard(this)) {
            params.flags = params.flags or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN and
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM.inv()
            params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
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

        wm.addView(bar, params)
        ContextCompat.startForegroundService(this, Intent(this, ForegroundService::class.java))

        if (Utils.shouldUseRootCommands(this)) {
            startService(rootServiceIntent)
            ensureRootServiceBound()
        }

        bar.show(null)
        bar.setOnSystemUiVisibilityChangeListener(immersiveListener)
        bar.viewTreeObserver.addOnGlobalLayoutListener(immersiveListener)
        immersiveListener.onGlobalLayout()
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

            setState(!state)
        }
    }

    fun toggleGestureBar() {
        if (isPillShown()) removeBar()
        else addBar()
    }

    fun toggleNavState() {
        if (isNavBarHidden()) showNav()
        else hideNav()
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
        if (Utils.shouldUseOverscanMethod(this) && !Utils.isInImmersive(this)) {
            if (!Utils.useRot270Fix(this) && !Utils.useTabletMode(this)) IWindowManager.setOverscan(0, 0, 0, -Utils.getNavBarHeight(this) + 1)
            compatibilityRotationListener.enable()
            Utils.forceNavBlack(this)

            navbarListeners.forEach { it.onNavChange(true) }
            navHidden = true
        }

    }

    /**
     * Show the navbar
     */
    fun showNav() {
        navbarListeners.forEach { it.onNavChange(false) }

        IWindowManager.setOverscan(0, 0, 0, 0)
        compatibilityRotationListener.disable()
        Utils.clearBlackNav(this)

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
     * Save the current NoBar state to preferences
     */
    fun setState(activated: Boolean) {
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
                            if (isNavBarHidden() && kgm.isKeyguardLocked) {
                                showNav()
                            } else {
                                hideNav()
                            }
                        }
                        Intent.ACTION_USER_PRESENT -> {
                            if (areGesturesActivated()) addBar()
                            hideNav()
                        }
                    }
                }, 300)
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
                        params.height = Utils.getCustomHeight(this@App) * 2
                    }

                    UiModeManager.ACTION_EXIT_CAR_MODE -> {
                        params.height = Utils.getCustomHeight(this@App)
                    }
                }

                wm.updateViewLayout(bar, params)
            }

            if (Utils.shouldUseOverscanMethod(this@App)) {
                hideNav()
            }
        }
    }

    /**
     * Handle changes in Immersive Mode
     * We need to deactivate overscan when nav immersive is active, to avoid cut-off content
     * //TODO: More work may be needed on detection
     */
    inner class ImmersiveListener : ContentObserver(handler), View.OnSystemUiVisibilityChangeListener, ViewTreeObserver.OnGlobalLayoutListener {
        private var isDisabledForContent = false

        init {
            contentResolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.POLICY_CONTROL), true, this)
        }

        override fun onGlobalLayout() {
            if (isPillShown()) {
                val rect = Rect()
                val screenRes = Utils.getRealScreenSize(this@App)

                bar.getWindowVisibleDisplayFrame(rect)

                val hidden = ((rect.top - rect.bottom).absoluteValue >= screenRes.y && (rect.left - rect.right).absoluteValue >= screenRes.x)

                if (hidden) {
                    onSystemUiVisibilityChange(View.SYSTEM_UI_FLAG_FULLSCREEN)
                } else {
                    onSystemUiVisibilityChange(0)
                }
            }
        }

        override fun onSystemUiVisibilityChange(visibility: Int) {
//            Log.e("NoBar", visibility.toString())
            if (Utils.shouldUseOverscanMethod(this@App)) {
                handleImmersiveChange(visibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION != 0
                        || visibility and View.SYSTEM_UI_FLAG_FULLSCREEN != 0
                        || visibility and View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN != 0
                        || visibility and 7 != 0)
            }
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            if (Utils.shouldUseOverscanMethod(this@App)) {
                if (uri == Settings.Global.getUriFor(Settings.Global.POLICY_CONTROL)) {
                    val current = Settings.Global.getString(contentResolver, Settings.Global.POLICY_CONTROL)

                    handleImmersiveChange(current != null && (current.contains("full") || current.contains("nav")))
                }
            }
        }

        private fun handleImmersiveChange(isImmersive: Boolean) {
            if (!IntroActivity.needsToRun(this@App)) {
                val hideInFullScreen = Utils.hideInFullscreen(this@App)
                if (isImmersive) {
                    if (!isDisabledForContent) {
                        showNav()
                        if (hideInFullScreen) {
                            bar.hidePill(true)
                        }
                        isDisabledForContent = true
                    }
                } else if (isDisabledForContent) {
                    hideNav()

                    if (hideInFullScreen && bar.isAutoHidden) {
                        bar.isAutoHidden = false
                        bar.showPill(true)
                    }
                    isDisabledForContent = false
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
     * Listen for changes in rotation, and handle appropriately
     */
    inner class CompatibilityRotationListener : OrientationEventListener(this) {
        private var oldRot = Surface.ROTATION_0

        override fun onOrientationChanged(orientation: Int) {
            if (Utils.shouldUseOverscanMethod(this@App) && (Utils.useRot270Fix(this@App) || Utils.useTabletMode(this@App))) {
                val newRot = wm.defaultDisplay.rotation

                if (oldRot != newRot) {
                    oldRot = newRot

                    handle()
                }
            }
        }

        override fun enable() {
            handle()

            super.enable()
        }

        private fun handle() {
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