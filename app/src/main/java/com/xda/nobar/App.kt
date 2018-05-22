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
import android.os.Looper
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.*
import com.xda.nobar.activities.IntroActivity
import com.xda.nobar.services.Actions
import com.xda.nobar.services.ForegroundService
import com.xda.nobar.util.IWindowManager
import com.xda.nobar.util.PremiumHelper
import com.xda.nobar.util.Utils
import com.xda.nobar.util.Utils.getHomeY
import com.xda.nobar.views.BarView
import java.util.*
import kotlin.math.absoluteValue



/**
 * Centralize important stuff in the App class, so we can be sure to have an instance of it
 */
class App : Application(), SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        const val TOGGLE = "${Actions.BASE}.TOGGLE"
    }

    private lateinit var wm: WindowManager
    private lateinit var um: UiModeManager
    private lateinit var kgm: KeyguardManager
    private lateinit var stateHandler: ScreenStateHandler
    private lateinit var carModeHandler: CarModeHandler
    private lateinit var prefs: SharedPreferences
    private lateinit var premiumHelper: PremiumHelper
    private lateinit var premiumInstallListener: PremiumInstallListener
    private lateinit var compatibilityRotationListener: CompatibilityRotationListener

    var isValidPremium: Boolean = false
    private var navHidden = false

    lateinit var immersiveListener: ImmersiveListener
    lateinit var bar: BarView

    private val listeners = ArrayList<ActivationListener>()
    private val handler = Handler(Looper.getMainLooper())

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
    val premTypeSplit: Int
        get() = resources.getString(R.string.prem_type_split).toInt()
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

    /**
     * ***************************************************************
     */

    override fun onCreate() {
        super.onCreate()
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        if (!Utils.canRunHiddenCommands(this)) {
            startActivity(Intent(this, IntroActivity::class.java))
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

        premiumHelper = PremiumHelper(this, object : LicenseCheckListener {
            override fun onResult(valid: Boolean, reason: String) {
                Log.e("NoBar", reason)
                isValidPremium = valid
                prefs.edit().putBoolean("valid_prem", valid).apply()
            }
        })

        isValidPremium = prefs.getBoolean("valid_prem", false)

        if (!isActivated()) Utils.saveBackupImmersive(this)

        prefs.registerOnSharedPreferenceChangeListener(this)

        refreshPremium()
        setDoubleTapToNoActionPreNougat()

        if (isActivated() && !IntroActivity.needsToRun(this)) {
            toggle(false)
        }
        if (isNavBarHidden()) compatibilityRotationListener.enable()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            "use_immersive" -> {
                switchImmersive(!Utils.shouldUseImmersiveInsteadOfOverscan(this))
            }
            "is_active" -> {
                listeners.forEach { it.onChange(sharedPreferences.getBoolean(key, false)) }
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
    fun addActivationListener(listener: ActivationListener) {
        listeners.add(listener)
    }

    /**
     * Remove an activation listener
     */
    fun removeActivationListener(listener: ActivationListener) {
        listeners.remove(listener)
    }

    /**
     * Add the pill to the screen
     */
    fun addBar() {
        params.width = Utils.getCustomWidth(this)
        params.height = Utils.getCustomHeight(this)
        params.gravity = Gravity.CENTER or Gravity.BOTTOM
        params.y = getHomeY(this)
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

        if (Utils.dontMoveForKeyboard(this)) params.flags = params.flags or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN and
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM.inv()

        if (bar.isHidden) {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            params.y = -(Utils.getCustomHeight(this) / 2)
        }

        addBarInternal()
    }

    private fun addBarInternal() {
        try {
            wm.removeView(bar)
        } catch (e: Exception) {}

        wm.addView(bar, params)
        ContextCompat.startForegroundService(this, Intent(this, ForegroundService::class.java))

        bar.show(null)
        bar.setOnSystemUiVisibilityChangeListener(immersiveListener)
        bar.viewTreeObserver.addOnGlobalLayoutListener(immersiveListener)
        immersiveListener.onGlobalLayout()
    }

    /**
     * Remove the pill from the screen
     */
    fun removeBar() {
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
        toggle(isActivated())
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
                showNav()
                removeBar()
                stopService(Intent(this@App, ForegroundService::class.java))
            } else {
                hideNav()
                addBar()
            }

            setState(!state)
        }
    }

    /**
     * Check if NoBar is currently active
     * @return true if active
     */
    fun isActivated(): Boolean {
        return prefs.getBoolean("is_active", false) || isNavBarHidden()
    }

    /**
     * Check if the navbar is currently hidden
     * @return true if hidden
     */
    fun isNavBarHidden(): Boolean {
        return when (wm.defaultDisplay.rotation) {
            Surface.ROTATION_0 -> {
                getOverscan().bottom.absoluteValue > 0
            }
            Surface.ROTATION_90 -> {
                if (Utils.useTabletMode(this)) getOverscan().left.absoluteValue > 0 else getOverscan().bottom.absoluteValue > 0
            }
            Surface.ROTATION_180 -> {
                if (Utils.useTabletMode(this) || Utils.useRot270Fix(this)) getOverscan().top.absoluteValue > 0 else getOverscan().bottom.absoluteValue > 0
            }
            else -> {
                when {
                    Utils.useTabletMode(this) -> getOverscan().right.absoluteValue > 0
                    Utils.useRot270Fix(this) -> getOverscan().top.absoluteValue > 0
                    else -> getOverscan().bottom.absoluteValue > 0
                }
            }
        } || getOverscan().bottom.absoluteValue > 0
    }

    /**
     * Hide the navbar
     */
    fun hideNav() {
        if (!Utils.touchWizNavEnabled(this)) Utils.forceTouchWizNavEnabled(this)

        if (Utils.shouldUseOverscanMethod(this) && !Utils.isInImmersive(this)) {
            IWindowManager.setOverscan(0, 0, 0, -Utils.getNavBarHeight(resources) + 1)
            compatibilityRotationListener.enable()
        } else if (!Utils.isInImmersive(this) && Utils.hasNavBar(this)) {
            try {
                Settings.Global.putString(contentResolver, Settings.Global.POLICY_CONTROL, "immersive.navigation")
            } catch (e: Exception) {}
        }

        navHidden = true
    }

    /**
     * Show the navbar
     */
    fun showNav() {
        if (Utils.shouldUseOverscanMethod(this) && !Utils.isInImmersive(this)) {
            IWindowManager.setOverscan(0, 0, 0, 0)
            compatibilityRotationListener.disable()
        } else try {
            Settings.Global.putString(contentResolver, Settings.Global.POLICY_CONTROL, Utils.getBackupImmersive(this))
        } catch (e: Exception) {}

        navHidden = false
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
     * If the user turns on compatibility mode while NoBar is active, that needs to be handled
     * @param toOverscan if true, compatibility mode has been deactivated
     */
    private fun switchImmersive(toOverscan: Boolean) {
        if (isActivated()) {
            if (toOverscan) {
                Utils.disableNavImmersive(this)
                hideNav()
            } else {
                Utils.enableNavImmersive(this)
                showNav()
            }
        }
    }

    private fun setDoubleTapToNoActionPreNougat() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            prefs.edit().putString("double_tap", "-1").apply()
        }
    }

    /**
     * Listen for changes in the screen state and handle appropriately
     */
    inner class ScreenStateHandler : BroadcastReceiver() {
        init {
            val filter = IntentFilter()
            filter.addAction(Intent.ACTION_SCREEN_ON)
            filter.addAction(Intent.ACTION_USER_PRESENT)
            filter.addAction(TOGGLE)

            registerReceiver(this, filter)
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    if (isActivated() && (kgm.isKeyguardLocked || if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) kgm.isDeviceLocked else false)) {
                        toggle(true)
                        Utils.setOffForRebootOrScreenLock(this@App, true)
                    }
                }
                Intent.ACTION_USER_PRESENT -> {
                    if (Utils.isOffForRebootOrScreenLock(this@App)) {
                        toggle(false)
                        Utils.setOffForRebootOrScreenLock(this@App, false)
                    }
                }
                TOGGLE -> {
                    toggle()
                }
            }
        }

        fun destroy() {
            unregisterReceiver(this)
        }
    }

    inner class CarModeHandler : BroadcastReceiver() {
        private var isDisabledForCarMode = false

        init {
            val filter = IntentFilter()
            filter.addAction(UiModeManager.ACTION_ENTER_CAR_MODE)
            filter.addAction(UiModeManager.ACTION_EXIT_CAR_MODE)

            registerReceiver(this, filter)
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            if (isActivated()) {
                when (intent?.action) {
                    UiModeManager.ACTION_ENTER_CAR_MODE -> {
                        removeBar()
                        showNav()
                        isDisabledForCarMode = true
                    }

                    UiModeManager.ACTION_EXIT_CAR_MODE -> {
                        if (isDisabledForCarMode) {
                            addBar()
                            hideNav()
                            isDisabledForCarMode = false
                        }
                    }
                }
            }
        }

        fun destroy() {
            unregisterReceiver(this)
        }
    }

    inner class ImmersiveListener : ContentObserver(handler), View.OnSystemUiVisibilityChangeListener, ViewTreeObserver.OnGlobalLayoutListener {
        private var isDisabledForContent = false

        init {
            contentResolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.POLICY_CONTROL), true, this)
        }

        override fun onGlobalLayout() {
            val rect = Rect()
            val screenRes = Utils.getRealScreenSize(this@App)

            bar.getWindowVisibleDisplayFrame(rect)

            if (rect.bottom < screenRes.y - 10) {
                onSystemUiVisibilityChange(0)
            }

            if (rect.bottom > screenRes.y && rect.bottom < screenRes.y * 2) {
                onSystemUiVisibilityChange(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            }
        }

        override fun onSystemUiVisibilityChange(visibility: Int) {
            if (isActivated()) {
                if (visibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION != 0) {
                    showNav()
                    isDisabledForContent = true
                } else if (isDisabledForContent) {
                    hideNav()
                    isDisabledForContent = false
                }
            }
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            if (isActivated()) {
                if (uri == Settings.Global.getUriFor(Settings.Global.POLICY_CONTROL)) {
                    val current = Settings.Global.getString(contentResolver, Settings.Global.POLICY_CONTROL)

                    if (current != null && (current.contains("full") || current.contains("nav"))) {
                        showNav()
                        isDisabledForContent = true
                    } else if (isDisabledForContent) {
                        hideNav()
                        isDisabledForContent = false
                    }
                }
            }
        }

        fun destroy() {
            contentResolver.unregisterContentObserver(this)
        }
    }

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

        fun destroy() {
            unregisterReceiver(this)
        }
    }

    inner class CompatibilityRotationListener : OrientationEventListener(this) {
        private var oldRot = Surface.ROTATION_0

        override fun onOrientationChanged(orientation: Int) {
            if (navHidden && (Utils.useRot270Fix(this@App) || Utils.useTabletMode(this@App))) {
                val newRot = wm.defaultDisplay.rotation

                if (oldRot != newRot) {
                    oldRot = newRot

                    if (Utils.useRot270Fix(this@App)) handle270()
                    if (Utils.useTabletMode(this@App)) handleTablet()
                }
            }
        }

        override fun enable() {
            super.enable()
            onOrientationChanged(0)
        }

        private fun handle270() {
            if (oldRot == Surface.ROTATION_270 || oldRot == Surface.ROTATION_180) {
                IWindowManager.setOverscan(0, -Utils.getNavBarHeight(resources) + 1, 0, 0)
            } else {
                IWindowManager.setOverscan(0, 0, 0, -Utils.getNavBarHeight(resources) + 1)
            }
        }

        private fun handleTablet() {
            if (Utils.shouldUseOverscanMethod(this@App) && !Utils.isInImmersive(this@App)) {
                when (oldRot) {
                    Surface.ROTATION_0 -> {
                        IWindowManager.setOverscan(0, 0, 0, -Utils.getNavBarHeight(resources) + 1)
                    }

                    Surface.ROTATION_90 -> {
                        IWindowManager.setOverscan(-Utils.getNavBarHeight(resources) + 1, 0, 0, 0)
                    }

                    Surface.ROTATION_180 -> {
                        IWindowManager.setOverscan(0, -Utils.getNavBarHeight(resources) + 1, 0 ,0)
                    }

                    Surface.ROTATION_270 -> {
                        IWindowManager.setOverscan(0, 0, -Utils.getNavBarHeight(resources) + 1, 0)
                    }
                }
            }
        }
    }

    interface ActivationListener {
        fun onChange(activated: Boolean)
    }

    interface LicenseCheckListener {
        fun onResult(valid: Boolean, reason: String)
    }
}