package com.xda.nobar

import android.Manifest
import android.animation.Animator
import android.annotation.SuppressLint
import android.app.*
import android.app.usage.UsageStatsManager
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import com.github.anrwatchdog.ANRWatchDog
import com.google.firebase.analytics.FirebaseAnalytics
import com.xda.nobar.activities.helpers.RequestPermissionsActivity
import com.xda.nobar.activities.ui.IntroActivity
import com.xda.nobar.data.ColoredAppData
import com.xda.nobar.interfaces.OnGestureStateChangeListener
import com.xda.nobar.interfaces.OnLicenseCheckResultListener
import com.xda.nobar.interfaces.OnNavBarHideStateChangeListener
import com.xda.nobar.providers.BaseProvider
import com.xda.nobar.root.RootWrapper
import com.xda.nobar.services.Actions
import com.xda.nobar.services.KeepAliveService
import com.xda.nobar.util.*
import com.xda.nobar.util.IWindowManager
import com.xda.nobar.util.helpers.*
import com.xda.nobar.views.BarView
import com.xda.nobar.views.NavBlackout
import io.fabric.sdk.android.Fabric
import io.fabric.sdk.android.InitializationCallback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.reflect.Method
import java.util.*
import kotlin.collections.ArrayList


/**
 * Centralize important stuff in the App class, so we can be sure to have an instance of it
 */
@Suppress("DEPRECATION")
class App : Application(), SharedPreferences.OnSharedPreferenceChangeListener, AppOpsManager.OnOpChangedListener {
    companion object {
        const val EDGE_TYPE_ACTIVE = 2

        private const val ACTION_MINIVIEW_SETTINGS_CHANGED = "com.lge.android.intent.action.MINIVIEW_SETTINGS_CHANGED"
    }

    val wm by lazy { getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    val um by lazy { getSystemService(Context.UI_MODE_SERVICE) as UiModeManager }
    val appOps by lazy { getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager }
    val nm by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    val imm by lazy { getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }
    val am by lazy { getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager }
    val immersiveHelperManager by lazy { ImmersiveHelperManager(this, uiHandler) }
    val usm by lazy { getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager }

    val rootWrapper by lazy { RootWrapper(this) }
    val blackout by lazy { NavBlackout(this) }
    val actionsBinder by lazy { ActionsInterface() }

    val accessibilityEnabled: Boolean
        get() = run {
            val services = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                    ?: ""
            services.contains(packageName)
        }
    var introRunning = false
    var accessibilityConnected = false
        set(value) {
            field = value
            uiHandler.accessibilityChanged(field)
        }
    private val stateHandler = ScreenStateHandler()
    private val carModeHandler = CarModeHandler()
    private val premiumHelper by lazy {
        PremiumHelper(this, OnLicenseCheckResultListener { valid, reason ->
            isValidPremium = valid
            prefManager.validPrem = valid

            licenseCheckListeners.forEach { it.onResult(valid, reason) }
        })
    }
    private val analytics by lazy {
        FirebaseAnalytics.getInstance(this)
    }

    private val premiumInstallListener = PremiumInstallListener()
    private val permissionListener = PermissionReceiver()
    private val displayChangeListener = DisplayChangeListener()
    private val miniViewListener = MiniViewListener()

    private val prefChangeListeners = ArrayList<SharedPreferences.OnSharedPreferenceChangeListener>()

    val screenOffHelper by lazy { ScreenOffHelper(this) }

    private var isInOtherWindowApp = false

    var navHidden = false
    var pillShown = false
    var helperAdded = false
    var keyboardShown = false
    var isValidPremium = false
        get() = field || BuildConfig.DEBUG

    private val gestureListeners = ArrayList<OnGestureStateChangeListener>()
    private val navbarListeners = ArrayList<OnNavBarHideStateChangeListener>()
    private val licenseCheckListeners = ArrayList<OnLicenseCheckResultListener>()
    private val rotationWatchers = ArrayList<IRotationWatcher>()

    val uiHandler = UIHandler()

    val bar by lazy { BarView(this) }

    val disabledNavReasonManager = DisabledReasonManager()
    val disabledBarReasonManager = DisabledReasonManager()
    val disabledImmReasonManager = DisabledReasonManager()

    private val isMainProcess: Boolean
        get() {
            val myPid = Process.myPid()

            return am.runningAppProcesses
                    ?.filter { it.pid == myPid }
                    ?.map { it.processName }
                    ?.any { it == packageName } == true
        }

    private val uncolorable = mapOf(
            "android" to arrayOf(),
            "com.google.android.apps.nexuslauncher" to arrayOf("android.widget"),
            "com.facebook.orca" to arrayOf("android.", "com.facebook.messaging.chatheads", "com.facebook.ui"),
            "com.android.systemui" to arrayOf(),
            "com.google.android.googlequicksearchbox" to arrayOf("android.widget.FrameLayout", "android.inputmethodservice.SoftInputWindow"),
            "com.teslacoilsw.launcher" to arrayOf("android.widget"),
            "com.actionlauncher.playstore" to arrayOf("android.widget"),
            "com.android.launcher" to arrayOf("android.widget"),
            "com.sonymobile.runtimeskinning.effects" to arrayOf(),
            "pl.damianpiwowarski.navbarapps" to arrayOf("android."),
            "pl.damianpiwowarski.keyboardetection" to arrayOf("android."),
            "com.motorola.frameworks.singlehand" to arrayOf(),
            "com.motorola.motodisplay" to arrayOf(),
            "com.motorola.aon" to arrayOf(),
            "com.motorola.actions" to arrayOf(),
            "com.motorola.audiomonitor" to arrayOf(),
            "com.google.android.apps.walletnfcrel" to arrayOf("!com.google.commerce.tapandpay.android.cardlist.CardListActivity"),
            "com.samsung.android.MtpApplication" to arrayOf(),
            "com.google.android.gms" to arrayOf(),
            "com.samsung.android.app.aodservice" to arrayOf(),
            "com.android.systemui.navigationbar" to arrayOf(),
            "com.sec.android.easyonehand" to arrayOf(),
            "com.samsung.android.server.iris" to arrayOf(),
            "com.samsung.android.bio.face.service" to arrayOf(),
            "com.samsung.android.spay" to arrayOf(),
            "com.samsung.android.app.spage" to arrayOf(),
            "com.samsung.android.app.multiwindow" to arrayOf(),
            "com.samsung.android.app.cocktailbarservice" to arrayOf(),
            "com.samsung.android.app.smartcapture" to arrayOf(),
            "com.sec.android.app.clockpackage" to arrayOf("android."),
            "com.sec.android.inputmethod" to arrayOf(),
            "com.android.stk" to arrayOf()
    )

    private val badColors = arrayOf(
            Color.parseColor("#212121"),
            Color.parseColor("#222222"),
            Color.parseColor("#f5f5f5"),
            -1,
            0,
            -16777216
    )

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val forName = Class::class.java.getDeclaredMethod("forName", String::class.java)
            val getDeclaredMethod = Class::class.java.getDeclaredMethod("getDeclaredMethod", String::class.java, arrayOf<Class<*>>()::class.java)

            val vmRuntimeClass = forName.invoke(null, "dalvik.system.VMRuntime") as Class<*>
            val getRuntime = getDeclaredMethod.invoke(vmRuntimeClass, "getRuntime", null) as Method
            val setHiddenApiExemptions = getDeclaredMethod.invoke(vmRuntimeClass, "setHiddenApiExemptions", arrayOf(arrayOf<String>()::class.java)) as Method

            val vmRuntime = getRuntime.invoke(null)

            setHiddenApiExemptions.invoke(vmRuntime, arrayOf("L"))
        }

        if (isMainProcess) {
            if (BuildConfig.DEBUG) {
                val crashHandler = CrashHandler(null, this@App)
                Thread.setDefaultUncaughtExceptionHandler(crashHandler)
            }

            handleKeepAlive()

            val core = CrashlyticsCore.Builder()
                    .disabled(BuildConfig.DEBUG)
                    .build()

            Fabric.with(
                    Fabric.Builder(this).kits(
                            Crashlytics.Builder()
                                    .core(core)
                                    .build()
                    ).initializationCallback(object : InitializationCallback<Fabric> {
                        override fun success(p0: Fabric?) {
                            val crashHandler = CrashHandler(Thread.getDefaultUncaughtExceptionHandler(), this@App)
                            Thread.setDefaultUncaughtExceptionHandler(crashHandler)
                        }

                        override fun failure(p0: Exception?) {}
                    }).build()
            )

            analytics.setAnalyticsCollectionEnabled(prefManager.enableAnalytics)

            if (prefManager.crashlyticsIdEnabled)
                Crashlytics.setUserIdentifier(prefManager.crashlyticsId)

            if (!prefManager.firstRun) {
                isSuAsync(mainHandler) {
                    if (it) rootWrapper.onCreate()
                }
            }

            logicScope.launch {
                val overscan = Rect().apply { wm.defaultDisplay.getOverscanInsets(this) }
                IWindowManager.leftOverscan = overscan.left
                IWindowManager.topOverscan = overscan.top
                IWindowManager.rightOverscan = overscan.right
                IWindowManager.bottomOverscan = overscan.bottom
            }

            val watchDog = ANRWatchDog()
            watchDog.setReportMainThreadOnly()
            watchDog.start()
            watchDog.setANRListener {
                Crashlytics.logException(it)
            }

            cachedRotation = rotation
            actionsBinder.register()
            stateHandler.register()
            uiHandler.register()
            carModeHandler.register()
            premiumInstallListener.register()
            permissionListener.register()
            IWindowManager.watchRotation(displayChangeListener, Display.DEFAULT_DISPLAY)
            miniViewListener.register()

            //Make sure lazy init happens in main Thread
            bar.onCreate()

            mainHandler.post {
                refreshScreenSize()
                refreshNavHeights()
            }

            isValidPremium = prefManager.validPrem

            prefManager.registerOnSharedPreferenceChangeListener(this)

            refreshPremium()

            if (prefManager.isActive
                    && !IntroActivity.needsToRun(this)) {
                addBar()

                if (wm.defaultDisplay.state != Display.STATE_ON) {
                    disabledBarReasonManager.add(DisabledReasonManager.PillReasons.SCREEN_OFF)
                    uiHandler.updateBlacklists()
                }
            }

            if (prefManager.useRot270Fix
                    || prefManager.useRot180Fix
                    || prefManager.useTabletMode
                    || Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                uiHandler.handleRot()

            if (!IntroActivity.needsToRun(this)) {
                addImmersiveHelper()
                uiHandler.onGlobalLayout()
            }

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                appOps.startWatchingMode(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, packageName, this)
            }

            mainHandler.post {
                if (prefManager.isActive && IntroActivity.needsToRun(this)) {
                    IntroActivity.start(this)
                }
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            PrefManager.IS_ACTIVE -> {
                gestureListeners.forEach { it.onGestureStateChange(bar, prefManager.isActive) }
            }
            PrefManager.HIDE_NAV -> {
                navbarListeners.forEach { it.onNavStateChange(prefManager.shouldUseOverscanMethod) }
            }
            PrefManager.ROT270_FIX -> {
                uiHandler.handleRot()
            }
            PrefManager.ROT180_FIX -> {
                uiHandler.handleRot()
            }
            PrefManager.TABLET_MODE -> {
                uiHandler.handleRot()
            }
            PrefManager.ENABLE_IN_CAR_MODE -> {
                val enabled = prefManager.enableInCarMode
                if (um.currentModeType == Configuration.UI_MODE_TYPE_CAR) {
                    if (enabled) {
                        disabledNavReasonManager.remove(DisabledReasonManager.NavBarReasons.CAR_MODE)
                        disabledImmReasonManager.remove(DisabledReasonManager.NavBarReasons.CAR_MODE)
                        disabledBarReasonManager.remove(DisabledReasonManager.PillReasons.CAR_MODE)
                    } else {
                        disabledNavReasonManager.add(DisabledReasonManager.NavBarReasons.CAR_MODE)
                        disabledImmReasonManager.add(DisabledReasonManager.NavBarReasons.CAR_MODE)
                        disabledBarReasonManager.add(DisabledReasonManager.PillReasons.CAR_MODE)
                    }
                    uiHandler.updateBlacklists()
                }
            }
            PrefManager.USE_IMMERSIVE_MODE_WHEN_NAV_HIDDEN -> {
                BaseProvider.sendUpdate(this)
            }
            PrefManager.HIDE_PILL_ON_KEYBOARD -> {
                uiHandler.onGlobalLayout()
            }
            PrefManager.FULL_OVERSCAN -> {
                if (prefManager.shouldUseOverscanMethod) hideNav(false)
            }
            PrefManager.ENABLE_ANALYTICS -> {
                analytics.setAnalyticsCollectionEnabled(prefManager.enableAnalytics)
            }
            PrefManager.KEEP_ALIVE -> {
                handleKeepAlive()
            }
        }

        prefChangeListeners.forEach { it.onSharedPreferenceChanged(sharedPreferences, key) }
    }

    override fun onOpChanged(op: String?, packageName: String?) {
        if (packageName == this.packageName) {
            when (op) {
                AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW -> {
                    val mode = appOps.checkOpNoThrow(op, Process.myUid(), packageName)
                    if ((Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1
                                    && mode == AppOpsManager.MODE_DEFAULT
                                    && checkSelfPermission(Manifest.permission.SYSTEM_ALERT_WINDOW) == PackageManager.PERMISSION_GRANTED)
                            || mode == AppOpsManager.MODE_ALLOWED)
                        IntroActivity.start(this)
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
    fun addGestureActivationListener(listener: OnGestureStateChangeListener) = gestureListeners.add(listener)

    /**
     * Remove an activation listener
     */
    fun removeGestureActivationListener(listener: OnGestureStateChangeListener) = gestureListeners.remove(listener)

    fun addNavBarHideListener(listener: OnNavBarHideStateChangeListener) = navbarListeners.add(listener)

    fun removeNavBarHideListener(listener: OnNavBarHideStateChangeListener) = navbarListeners.remove(listener)

    fun addLicenseCheckListener(listener: OnLicenseCheckResultListener) = licenseCheckListeners.add(listener)

    fun removeLicenseCheckListener(listener: OnLicenseCheckResultListener) = licenseCheckListeners.remove(listener)

    fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefChangeListeners.add(listener)
    }

    fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefChangeListeners.remove(listener)
    }

    fun addRotationWatcher(watcher: IRotationWatcher) {
        rotationWatchers.add(watcher)
    }

    fun removeRotationWatcher(watcher: IRotationWatcher) {
        rotationWatchers.remove(watcher)
    }

    /**
     * Add the pill to the screen
     */
    fun addBar(callListeners: Boolean = true) {
        mainScope.launch {
            if (disabledBarReasonManager.isEmpty() && !pillShown) {
                if (callListeners) gestureListeners.forEach { it.onGestureStateChange(bar, true) }

                addBarInternal(false)
            }
        }
    }

    private val addHelperAction: (Actions.IActionsBinderImpl) -> Unit = {
        it.addImmersiveHelper()
    }

    fun addImmersiveHelper() {
        if (!actionsBinder.contains(addHelperAction)) actionsBinder.post(addHelperAction)
    }

    /**
     * Remove the pill from the screen
     */
    fun removeBar(callListeners: Boolean = true) {
        mainScope.launch {
            if (callListeners) gestureListeners.forEach { it.onGestureStateChange(bar, false) }

            bar.hide(object : Animator.AnimatorListener {
                override fun onAnimationCancel(animation: Animator?) {}

                override fun onAnimationRepeat(animation: Animator?) {}

                override fun onAnimationStart(animation: Animator?) {}

                override fun onAnimationEnd(animation: Animator?) {
                    try {
                        bar.shouldReAddOnDetach = false
                        postAction { it.remBarAndBlackout() }
                    } catch (e: Exception) {
                    }

                    if (!navHidden) removeImmersiveHelper()
                }
            })
        }
    }

    fun removeImmersiveHelper() {
        postAction { it.removeImmersiveHelper() }
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

    fun toggleNavState(hidden: Boolean = prefManager.shouldUseOverscanMethod) {
        runSecureSettingsAction {
            prefManager.shouldUseOverscanMethod = !hidden

            if (hidden) showNav()
            else hideNav()

            BaseProvider.sendUpdate(this)
        }
    }

    fun toggleImmersiveWhenNavHidden() {
        prefManager.useImmersiveWhenNavHidden = !prefManager.useImmersiveWhenNavHidden
    }

    fun isPillShown() = prefManager.isActive && pillShown

    /**
     * Hide the navbar
     */
    fun hideNav(callListeners: Boolean = true) {
        if (prefManager.shouldUseOverscanMethod
                && disabledNavReasonManager.isEmpty()
                && hasWss) {
            addImmersiveHelper()

            uiHandler.handleRot()

            val fullOverscan = prefManager.useFullOverscan
            if (!fullOverscan) postAction {
                it.addBlackout()
            }
            else postAction { it.remBlackout() }

            if (isTouchWiz && !prefManager.useImmersiveWhenNavHidden) {
                touchWizNavEnabled = true
            }

            navHidden = true
        }

        mainScope.launch { if (callListeners) navbarListeners.forEach { it.onNavStateChange(true) } }
    }

    /**
     * Show the navbar
     */
    fun showNav(callListeners: Boolean = true, removeImmersive: Boolean = true) {
        if (hasWss) {
            if (removeImmersive && prefManager.useImmersiveWhenNavHidden)
                immersiveHelperManager.exitNavImmersive()

            mainScope.launch { if (callListeners) navbarListeners.forEach { it.onNavStateChange(false) } }

            IWindowManager.setOverscanAsync(0, 0, 0, 0)

            if (!prefManager.overlayNavBlackout) {
                postAction { it.remBlackout() }
            }

            if (isTouchWiz) {
                touchWizNavEnabled = false
            }

            navHidden = false

            if (!prefManager.isActive) {
                removeImmersiveHelper()
            }
        }
    }

    /**
     * Save the current NoBar gesture state to preferences
     */
    fun setGestureState(activated: Boolean) {
        prefManager.isActive = activated
    }

    fun addBarInternal(isRefresh: Boolean = true) {
        try {
            bar.shouldReAddOnDetach = isRefresh
            if (isRefresh) postAction { it.remBar() }
            else addBarInternalUnconditionally()
        } catch (e: Exception) {
            addBarInternalUnconditionally()
        }

        addImmersiveHelper()
    }

    private val screenOnNotif by lazy {
        NotificationCompat.Builder(this, "nobar-screen-on")
                .setContentTitle(resources.getText(R.string.screen_timeout))
                .setContentText(resources.getText(R.string.screen_timeout_msg))
                .setSmallIcon(R.drawable.ic_navgest)
                .setPriority(if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) NotificationCompat.PRIORITY_MIN else NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
    }

    fun toggleScreenOn() {
        val hasScreenOn = bar.toggleScreenOn()

        if (hasScreenOn) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
                nm.createNotificationChannel(NotificationChannel("nobar-screen-on", resources.getString(R.string.screen_timeout), NotificationManager.IMPORTANCE_LOW))
            }

            nm.notify(100, screenOnNotif.build())
        } else {
            nm.cancel(100)
        }
    }

    fun postAction(action: (Actions.IActionsBinderImpl) -> Unit) {
        actionsBinder.post(action)
    }

    private fun addBarInternalUnconditionally() {
        postAction {
            it.addBarAndBlackout()
        }
    }

    private fun handleKeepAlive() {
        val serviceIntent = Intent(this, KeepAliveService::class.java)

        if (prefManager.keepAlive) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            stopService(serviceIntent)
        }
    }

    /**
     * Listen for changes in the screen state and handle appropriately
     */
    inner class ScreenStateHandler : BroadcastReceiver() {
        fun register() {
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
                mainHandler.postDelayed({
                    when (intent?.action) {
                        Intent.ACTION_REBOOT,
                        Intent.ACTION_SHUTDOWN,
                        Intent.ACTION_SCREEN_OFF -> {
                            if (prefManager.shouldntKeepOverscanOnLock) disabledNavReasonManager.add(DisabledReasonManager.NavBarReasons.KEYGUARD)
                            disabledBarReasonManager.add(DisabledReasonManager.PillReasons.SCREEN_OFF)
                            bar.forceActionUp()

                            uiHandler.updateBlacklists()
                        }
                        Intent.ACTION_SCREEN_ON,
                        Intent.ACTION_BOOT_COMPLETED,
                        Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                            disabledBarReasonManager.remove(DisabledReasonManager.PillReasons.SCREEN_OFF)
                            if (isOnKeyguard) {
                                if (prefManager.shouldntKeepOverscanOnLock) disabledNavReasonManager.add(DisabledReasonManager.NavBarReasons.KEYGUARD)
                                if (prefManager.hideOnLockscreen) disabledBarReasonManager.add(DisabledReasonManager.PillReasons.LOCK_SCREEN)
                            } else {
                                disabledNavReasonManager.remove(DisabledReasonManager.NavBarReasons.KEYGUARD)
                                disabledBarReasonManager.remove(DisabledReasonManager.PillReasons.LOCK_SCREEN)
                            }

                            uiHandler.updateBlacklists()
                        }
                        Intent.ACTION_USER_PRESENT -> {
                            disabledNavReasonManager.remove(DisabledReasonManager.NavBarReasons.KEYGUARD)

                            uiHandler.updateBlacklists()
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
        fun register() {
            val filter = IntentFilter()
            filter.addAction(UiModeManager.ACTION_ENTER_CAR_MODE)
            filter.addAction(UiModeManager.ACTION_EXIT_CAR_MODE)

            registerReceiver(this, filter)
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                UiModeManager.ACTION_ENTER_CAR_MODE -> {
                    uiMode = Configuration.UI_MODE_TYPE_CAR

                    if (prefManager.enableInCarMode) {
                        if (pillShown) {
                            bar.params.height = prefManager.customHeight * 2
                            bar.updateLayout()
                        }
                    } else {
                        disabledBarReasonManager.add(DisabledReasonManager.PillReasons.CAR_MODE)
                        disabledNavReasonManager.add(DisabledReasonManager.NavBarReasons.CAR_MODE)
                        disabledImmReasonManager.add(DisabledReasonManager.NavBarReasons.CAR_MODE)

                        uiHandler.updateBlacklists()
                    }
                }

                UiModeManager.ACTION_EXIT_CAR_MODE -> {
                    uiMode = Configuration.UI_MODE_TYPE_NORMAL

                    if (prefManager.enableInCarMode) {
                        if (pillShown) {
                            bar.params.height = prefManager.customHeight
                            bar.updateLayout()
                        }
                    } else {
                        disabledBarReasonManager.remove(DisabledReasonManager.PillReasons.CAR_MODE)
                        disabledNavReasonManager.remove(DisabledReasonManager.NavBarReasons.CAR_MODE)
                        disabledImmReasonManager.remove(DisabledReasonManager.NavBarReasons.CAR_MODE)

                        uiHandler.updateBlacklists()
                    }
                }
            }

            if (pillShown) {
                bar.updateLayout()
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
    @SuppressLint("WrongConstant")
    inner class UIHandler : ContentObserver(logicHandler), ViewTreeObserver.OnGlobalLayoutListener, (Boolean) -> Unit, SharedPreferences.OnSharedPreferenceChangeListener {
        private var rotLock = Any()

        private val navArray = ArrayList<String>()
        private val barArray = ArrayList<String>()
        private val immArray = ArrayList<String>()
        private val windowArray = ArrayList<String>()
        private val coloredArray = ArrayList<ColoredAppData>()
        private val hideDialogApps = ArrayList<String>()

        private var useOverscan = false
        private var immersiveWhenNavHidden = false
        private var hidePermissions = false
        private var hideInstaller = false
        private var hideLockscreen = false
        private var active = false
        private var origInFullscreen = false

        fun register() {
            contentResolver.registerContentObserver(Settings.Global.getUriFor(POLICY_CONTROL), true, this)
            contentResolver.registerContentObserver(Settings.Global.getUriFor("navigationbar_hide_bar_enabled"), true, this)
            contentResolver.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES), true, this)
            contentResolver.registerContentObserver(Settings.Secure.getUriFor("navigation_mode"), true, this)

            prefManager.apply {
                loadBlacklistedNavPackages(navArray)
                loadBlacklistedBarPackages(barArray)
                loadBlacklistedImmPackages(immArray)
                loadOtherWindowApps(windowArray)
                loadColoredApps(coloredArray)
                loadHideDialogApps(hideDialogApps)

                useOverscan = shouldUseOverscanMethod
                immersiveWhenNavHidden = useImmersiveWhenNavHidden
                hidePermissions = hideOnPermissions
                hideInstaller = hideOnInstaller
                hideLockscreen = hideOnLockscreen
                active = isActive
                origInFullscreen = origBarInFullscreen
            }

            registerOnSharedPreferenceChangeListener(this)
        }

        fun setNodeInfoAndUpdate(info: AccessibilityEvent?) {
            handleNewEvent(info ?: return)
        }

        private var oldPName: String? = null

        private val installers = arrayOf(
                "com.google.android.packageinstaller",
                "com.android.packageinstaller"
        )

        private fun isPackageInstaller(pName: String?) = installers.contains(pName)

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            when (key) {
                PrefManager.BLACKLISTED_NAV_APPS -> {
                    navArray.clear()
                    prefManager.loadBlacklistedNavPackages(navArray)
                }
                PrefManager.BLACKLISTED_BAR_APPS -> {
                    barArray.clear()
                    prefManager.loadBlacklistedBarPackages(barArray)
                }
                PrefManager.BLACKLISTED_IMM_APPS -> {
                    immArray.clear()
                    prefManager.loadBlacklistedImmPackages(immArray)
                }
                PrefManager.OTHER_WINDOW_APPS -> {
                    windowArray.clear()
                    prefManager.loadOtherWindowApps(windowArray)
                }
                PrefManager.COLORED_APPS -> {
                    coloredArray.clear()
                    prefManager.loadColoredApps(coloredArray)
                }
                PrefManager.HIDE_DIALOG_APPS -> {
                    hideDialogApps.clear()
                    prefManager.loadHideDialogApps(hideDialogApps)
                }
                PrefManager.HIDE_NAV -> {
                    useOverscan = prefManager.shouldUseOverscanMethod
                }
                PrefManager.USE_IMMERSIVE_MODE_WHEN_NAV_HIDDEN -> {
                    immersiveWhenNavHidden = prefManager.useImmersiveWhenNavHidden
                }
                PrefManager.HIDE_ON_PERMISSIONS -> {
                    hidePermissions = prefManager.hideOnPermissions
                }
                PrefManager.HIDE_ON_INSTALLER -> {
                    hideInstaller = prefManager.hideOnInstaller
                }
                PrefManager.HIDE_ON_LOCKSCREEN -> {
                    hideLockscreen = prefManager.hideOnLockscreen
                }
                PrefManager.IS_ACTIVE -> {
                    active = prefManager.isActive
                }
                PrefManager.ORIG_NAV_IN_IMMERSIVE -> {
                    origInFullscreen = prefManager.origBarInFullscreen
                }
            }
        }

        private val eventLock = Any()

        private val systemUIPackage = "com.android.systemui"
        private val dialog = "dialog"
        private val recentsActivity = "RecentsActivity"
        private val managePermissionsActivity = "ManagerPermissionsActivity"
        private val grantPermissionsActivity = "GrantPermissionsActivity"
        private val packageInstallerActivity = "PackageInstallerActivity"

        @SuppressLint("WrongConstant")
        private fun handleNewEvent(info: AccessibilityEvent) {
            logicScope.launch {
                synchronized(eventLock) {
                    val hasUsage = this@App.hasUsage

                    var pName = info.packageName?.toString()
                    val className = info.className?.toString()

                    if (pName != oldPName) {
                        oldPName = pName

                        if (hasUsage) {
                            val time = System.currentTimeMillis()
                            val appStats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time)

                            if (appStats != null && appStats.isNotEmpty()) {
                                pName = Collections.max(appStats) { o1, o2 -> compareValues(o1.lastTimeUsed, o2.lastTimeUsed) }.packageName
                            }
                        }

                        runNewNodeInfo(pName)

                        if (hasUsage || (info.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                                || info.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
                                || pName != systemUIPackage)) {
                            processColor(pName)
                        }
                    } else {
                        updateBlacklists()
                    }

                    if (info.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                            || info.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {

                        if (origInFullscreen) {
                            if (immersiveHelperManager.isFullImmersive()) {
                                disabledNavReasonManager.add(DisabledReasonManager.NavBarReasons.FULLSCREEN)
                            } else {
                                disabledNavReasonManager.remove(DisabledReasonManager.NavBarReasons.FULLSCREEN)
                            }
                        }

                        if (useOverscan
                                && immersiveWhenNavHidden) {
                            if (pName == systemUIPackage && className?.contains(recentsActivity) == true) {
                                immersiveHelperManager.tempForcePolicyControlForRecents()
                            } else {
                                immersiveHelperManager.putBackOldImmersive()
                            }
                        }

                        if (hidePermissions
                                && isPackageInstaller(pName)
                                && (className?.contains(managePermissionsActivity) == true
                                        || className?.contains(grantPermissionsActivity) == true)) {
                            disabledBarReasonManager.add(DisabledReasonManager.PillReasons.PERMISSIONS)
                        } else {
                            disabledBarReasonManager.remove(DisabledReasonManager.PillReasons.PERMISSIONS)
                        }

                        if (hideInstaller
                                && isPackageInstaller(pName) && className?.contains(packageInstallerActivity) == true) {
                            disabledBarReasonManager.add(DisabledReasonManager.PillReasons.INSTALLER)
                        } else {
                            disabledBarReasonManager.remove(DisabledReasonManager.PillReasons.INSTALLER)
                        }

                        if (hideDialogApps.contains(pName) && className?.toLowerCase(Locale.getDefault())?.contains(dialog) == true) {
                            disabledBarReasonManager.add(DisabledReasonManager.PillReasons.HIDE_DIALOG)
                        } else {
                            disabledBarReasonManager.remove(DisabledReasonManager.PillReasons.HIDE_DIALOG)
                        }
                    }

                    if (hideLockscreen && isOnKeyguard) {
                        disabledBarReasonManager.add(DisabledReasonManager.PillReasons.LOCK_SCREEN)
                    } else {
                        disabledBarReasonManager.remove(DisabledReasonManager.PillReasons.LOCK_SCREEN)
                    }

                    if (isTouchWiz) {
                        try {
                            if (edgeType == EDGE_TYPE_ACTIVE) {
                                disabledImmReasonManager.add(DisabledReasonManager.ImmReasons.EDGE_SCREEN)
                            } else {
                                disabledImmReasonManager.remove(DisabledReasonManager.ImmReasons.EDGE_SCREEN)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    updateKeyboardFlagState()
                }
            }
        }

        private fun processColor(pName: String?) {
            if (coloredArray.isNotEmpty()) {
                val info = coloredArray.filter { it.packageName == pName }

                if (info.isNotEmpty()) {
                    prefManager.autoPillBGColor = info[0].color
                } else {
                    prefManager.autoPillBGColor = 0
                }
            } else {
                prefManager.autoPillBGColor = 0
            }
        }

        @SuppressLint("ResourceType")
        private fun runNewNodeInfo(pName: String?) {
            if (pName != null) {
                if (navArray.contains(pName)) {
                    disabledNavReasonManager.add(DisabledReasonManager.NavBarReasons.NAV_BLACKLIST)
                } else {
                    disabledNavReasonManager.remove(DisabledReasonManager.NavBarReasons.NAV_BLACKLIST)
                }

                if (barArray.contains(pName)) {
                    disabledBarReasonManager.add(DisabledReasonManager.PillReasons.BLACKLIST)
                } else {
                    disabledBarReasonManager.remove(DisabledReasonManager.PillReasons.BLACKLIST)
                }

                if (immArray.contains(pName)) {
                    disabledImmReasonManager.add(DisabledReasonManager.ImmReasons.BLACKLIST)
                } else {
                    disabledImmReasonManager.remove(DisabledReasonManager.ImmReasons.BLACKLIST)
                }

                if (windowArray.contains(pName)) {
                    if (!isInOtherWindowApp && active) {
                        addBar(false)
                        isInOtherWindowApp = true
                    }
                } else if (isInOtherWindowApp) isInOtherWindowApp = false

//                try {
//                    if (checkGoodPackage(pName, className)) {
//                        val packageRes = packageManager.getResourcesForApplication(pName)
//                        val theme = packageRes.newTheme()
//                        val arr = intArrayOf(packageRes.getIdentifier("colorPrimary", "attr", pName), android.R.attr.colorPrimary)
//
//                        var color = 0
//
//                        try {
//                            theme.applyStyle(
//                                    packageManager.getActivityInfo(
//                                            ComponentName(pName, className),
//                                            0
//                                    ).theme,
//                                    true
//                            )
//
//                            val attrs = theme.obtainStyledAttributes(arr)
//                            color = attrs.getColor(0, attrs.getColor(1, 0))
//
//                            attrs.recycle()
//                        } catch (e: Exception) {}
//
//                        val luminance = ColorUtils.calculateLuminance(color)
//
//                        Log.e("NoBar", "luminance: $luminance")
//
//                        prefManager.autoPillBGColor = Color.parseColor(if (luminance < 0.5) "#f5f5f5" else "#212121")
//
//                        if (badColors.contains(color)) {
//                            val icon = packageManager.getApplicationIcon(pName)
//                            val bmp = Bitmap.createBitmap(icon.intrinsicWidth, icon.intrinsicHeight, Bitmap.Config.ARGB_8888)
//                            val canvas = Canvas(bmp)
//
//                            icon.setBounds(0, 0, canvas.width, canvas.height)
//                            icon.draw(canvas)
//
//                            val palette = Palette.from(bmp).generate()
//                            bmp.recycle()
//
//                            val vibrant = palette.getVibrantColor(0)
//                            val darkVibrant = palette.getDarkVibrantColor(0)
//
//                            prefManager.autoPillBGColor = if (vibrant != 0) vibrant else darkVibrant
//                        } else {
//                            prefManager.autoPillBGColor = color
//                        }
//                    } else {
//                        prefManager.autoPillBGColor = 0
//                    }
//                } catch (e: Exception) {}

                updateBlacklists()
            }
        }

        private fun checkGoodPackage(pName: String, className: String?): Boolean {
            uncolorable.forEach { (badPkg, classes) ->
                if (badPkg == pName) {
                    if (classes.isEmpty())
                        return false
                    classes.forEach {
                        if (it.startsWith("!")) {
                            val parsed = it.substring(1)

                            if (className?.startsWith(parsed) == false)
                                return false
                        } else if (className?.startsWith(it) == true)
                            return false
                    }
                }
            }

            return true
        }

        private fun updateKeyboardFlagState() {
            val kbHeight = try {
                imm.inputMethodWindowVisibleHeight
            } catch (e: Exception) {
                0
            }
            keyboardShown = kbHeight > 0

            if (prefManager.showNavWithKeyboard) {
                if (keyboardShown) {
                    disabledNavReasonManager.add(DisabledReasonManager.NavBarReasons.KEYBOARD)
                } else if (prefManager.shouldUseOverscanMethod) {
                    disabledNavReasonManager.remove(DisabledReasonManager.NavBarReasons.KEYBOARD)
                }
            }

            if (!prefManager.dontMoveForKeyboard) {
                var changed = false

                if (keyboardShown) {
                    if (bar.params.flags and WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN != 0) {
                        bar.params.flags = bar.params.flags and
                                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN.inv()
                        changed = true
                    }
                } else {
                    if (bar.params.flags and WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN == 0) {
                        bar.params.flags = bar.params.flags or
                                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        changed = true
                    }
                }

                if (changed) bar.updateLayout()
            }

            if (isPillShown()) {
                try {
                    if (!immersiveWhenNavHidden)
                        immersiveHelperManager.exitNavImmersive()

                    if (prefManager.hidePillWhenKeyboardShown) {
                        if (keyboardShown) bar.scheduleHide(HiddenPillReasonManagerNew.KEYBOARD)
                        else bar.showPill(HiddenPillReasonManagerNew.KEYBOARD)
                    }
                } catch (e: NullPointerException) {}
            }
        }

        fun updateBlacklists() {
            if (disabledImmReasonManager.isEmpty()) {
                if (useOverscan
                        && immersiveWhenNavHidden)
                    immersiveHelperManager.enterNavImmersive()
            } else {
                immersiveHelperManager.exitNavImmersive()
            }

            if (disabledBarReasonManager.isEmpty()) {
                if (active
                        && !pillShown) addBar(false)
                if (!immersiveHelperManager.helperAdded) addImmersiveHelper()
            } else {
                removeBar(false)
                if (disabledBarReasonManager.run {
                            contains(DisabledReasonManager.PillReasons.INSTALLER)
                                    || contains(DisabledReasonManager.PillReasons.PERMISSIONS)
                                    || contains(DisabledReasonManager.PillReasons.HIDE_DIALOG)
                        }) {
                    removeImmersiveHelper()
                } else {
                    addImmersiveHelper()
                }
            }

            if (useOverscan) {
                if (disabledNavReasonManager.isEmpty()) {
                    hideNav()
                } else {
                    showNav()
                }
            } else {
                showNav()
            }
        }

        override fun invoke(isImmersive: Boolean) {
            handleImmersiveChange(isImmersive)
        }

        private val semCocktailBarManagerClass by lazy {
            try {
                Class.forName("com.samsung.android.cocktailbar.SemCocktailBarManager")
            } catch (e: Exception) {
                null
            }
        }

        private val managerInstance by lazy {
            try {
                getSystemService("CocktailBarService")
            } catch (e: Exception) {
                null
            }
        }

        private val getCocktailBarWindowType by lazy {
            try {
                semCocktailBarManagerClass?.getMethod("getCocktailBarWindowType")
            } catch (e: Exception) {
                null
            }
        }

        private val edgeType: Int?
            get() = try {
                getCocktailBarWindowType?.invoke(managerInstance)?.toString()?.toInt()
            } catch (e: Exception) {
                null
            }

        @SuppressLint("WrongConstant")
        override fun onGlobalLayout() {
            if (pillShown) {
                bar.updatePositionAndDimens()
            }
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            logicScope.launch {
                when (uri) {
                    Settings.Global.getUriFor(POLICY_CONTROL) -> {
                        handleImmersiveChange(immersiveHelperManager.isFullImmersive())
                    }

                    Settings.Global.getUriFor("navigationbar_hide_bar_enabled") -> {
                        if (prefManager.isActive) {
                            touchWizNavEnabled = !immersiveHelperManager.isNavImmersive()
                        }
                    }

                    Settings.Secure.getUriFor("navigation_mode") -> {
                        refreshNavHeights()
                    }
                }
            }
        }

        private val immersiveLock = Any()
        private var oldImmersive = false

        private fun handleImmersiveChange(isImmersive: Boolean) {
            logicScope.launch {
                synchronized(immersiveLock) {
                    if (isImmersive != oldImmersive) {
                        oldImmersive = isImmersive

                        if (!IntroActivity.needsToRun(this@App)) {
                            bar.updatePositionAndDimens()

                            val hideInFullScreen = prefManager.hideInFullscreen
                            val fadeInFullScreen = prefManager.fullscreenFade

                            if (isImmersive) {
                                if (hideInFullScreen && !fadeInFullScreen) bar.scheduleHide(HiddenPillReasonManagerNew.FULLSCREEN)
                                if (fadeInFullScreen && !hideInFullScreen) bar.scheduleFade(prefManager.fullscreenFadeTime)
                            } else {
                                bar.showPill(HiddenPillReasonManagerNew.FULLSCREEN)
                                bar.scheduleUnfade()
                            }
                        }
                    }
                }
            }
        }

        private var prevRot = cachedRotation

        fun handleRot(rot: Int = cachedRotation) {
            logicScope.launch {
                Mutex().withLock(rotLock) {
                    delay(100L)

                    if (prefManager.shouldUseOverscanMethod) {
                        when {
                            prefManager.useRot270Fix ||
                                    prefManager.useRot180Fix -> handle180AndOr270(rot)
                            prefManager.useTabletMode -> handleTablet(rot)
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> handlePie(rot)
                            else -> handle0()
                        }
                    }

                    postAction { it.addBlackout() }

                    if (prevRot != rot) {
                        prevRot = rot

                        if (pillShown) {
                            bar.handleRotationOrAnchorUpdate()
                            bar.forceActionUp()
                        }
                    }
                }
            }
        }

        fun accessibilityChanged(enabled: Boolean) {
            mainHandler.post {
                if (enabled && prefManager.isActive) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                            || Settings.canDrawOverlays(this@App)) {
                        addBar(false)
                    }
                }

                if (enabled && introRunning) IntroActivity.start(this@App)
            }
        }

        private fun handle0() {
            IWindowManager.setOverscan(0, 0, 0, -adjustedNavBarHeight)
        }

        private fun handle180AndOr270(rotation: Int) {
            when (rotation) {
                Surface.ROTATION_180 -> if (prefManager.useRot180Fix) handle180() else handle0()
                Surface.ROTATION_270 -> if (prefManager.useRot270Fix) handle270() else handle0()
                else -> handle0()
            }
        }

        private fun handle270() {
            IWindowManager.setOverscan(0, -adjustedNavBarHeight, 0, 0)
        }

        private fun handle180() {
            IWindowManager.setOverscan(0, -adjustedNavBarHeight, 0, 0)
        }

        private fun handleTablet(rotation: Int) {
            if (prefManager.shouldUseOverscanMethod) {
                when (rotation) {
                    Surface.ROTATION_0 -> {
                        IWindowManager.setOverscan(0, 0, 0, -adjustedNavBarHeight)
                    }

                    Surface.ROTATION_90 -> {
                        IWindowManager.setOverscan(-adjustedNavBarHeight, 0, 0, 0)
                    }

                    Surface.ROTATION_180 -> {
                        IWindowManager.setOverscan(0, -adjustedNavBarHeight, 0, 0)
                    }

                    Surface.ROTATION_270 -> {
                        IWindowManager.setOverscan(0, 0, -adjustedNavBarHeight, 0)
                    }
                }
            }
        }

        private fun handlePie(rotation: Int) {
            if (prefManager.shouldUseOverscanMethod) {
                when (IWindowManager.getNavBarPosition()) {
                    IWindowManager.NAV_BAR_LEFT -> {
                        when (rotation) {
                            Surface.ROTATION_0 -> {
                                IWindowManager.setOverscanAsync(-adjustedNavBarHeight, 0, 0, 0)
                            }

                            Surface.ROTATION_90 -> {
                                IWindowManager.setOverscanAsync(0, -adjustedNavBarHeight, 0, 0)
                            }

                            Surface.ROTATION_180 -> {
                                IWindowManager.setOverscanAsync(0, 0, -adjustedNavBarHeight, 0)
                            }

                            Surface.ROTATION_270 -> {
                                IWindowManager.setOverscanAsync(0, 0, 0, -adjustedNavBarHeight)
                            }
                        }
                    }

                    IWindowManager.NAV_BAR_RIGHT -> {
                        when (rotation) {
                            Surface.ROTATION_0 -> {
                                IWindowManager.setOverscanAsync(0, 0, -adjustedNavBarHeight, 0)
                            }

                            Surface.ROTATION_90 -> {
                                IWindowManager.setOverscanAsync(0, 0, 0, -adjustedNavBarHeight)
                            }

                            Surface.ROTATION_180 -> {
                                IWindowManager.setOverscanAsync(-adjustedNavBarHeight, 0, 0, 0)
                            }

                            Surface.ROTATION_270 -> {
                                IWindowManager.setOverscanAsync(0, -adjustedNavBarHeight, 0, 0)
                            }
                        }
                    }

                    IWindowManager.NAV_BAR_BOTTOM -> {
                        when (rotation) {
                            Surface.ROTATION_0 -> {
                                IWindowManager.setOverscanAsync(0, 0, 0, -adjustedNavBarHeight)
                            }

                            Surface.ROTATION_90 -> {
                                IWindowManager.setOverscanAsync(-adjustedNavBarHeight, 0, 0, 0)
                            }

                            Surface.ROTATION_180 -> {
                                IWindowManager.setOverscanAsync(0, -adjustedNavBarHeight, 0, 0)
                            }

                            Surface.ROTATION_270 -> {
                                IWindowManager.setOverscanAsync(0, 0, -adjustedNavBarHeight, 0)
                            }
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
        fun register() {
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
                if (intent.dataString?.contains("com.xda.nobar.premium") == true) {
                    refreshPremium()
                }
            }
        }
    }

    inner class PermissionReceiver : BroadcastReceiver() {
        fun register() {
            val filter = IntentFilter()
            filter.addAction(RequestPermissionsActivity.ACTION_RESULT)
            LocalBroadcastManager.getInstance(this@App)
                    .registerReceiver(this, filter)
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                RequestPermissionsActivity.ACTION_RESULT -> {
                    val className = intent.getParcelableExtra<ComponentName>(RequestPermissionsActivity.EXTRA_CLASS_NAME)

                    when (className) {
                        ComponentName(this@App, BarView::class.java) -> {
                            val which = intent.getIntExtra(Actions.EXTRA_ACTION, -1)
                            val key = intent.getStringExtra(Actions.EXTRA_GESTURE) ?: return

                            actionManager.actionHandler.handleAction(which, key)
                        }
                    }
                }
            }
        }
    }

    inner class DisplayChangeListener : IRotationWatcher.Stub() {
        override fun onRotationChanged(rotation: Int) {
            cachedRotation = rotation

            rotationWatchers.forEach { it.onRotationChanged(rotation) }
            uiHandler.handleRot(rotation)
            handleDisplayChange()
        }

        private fun handleDisplayChange() {
            refreshScreenSize()
            refreshNavHeights()
        }
    }

    inner class MiniViewListener : BroadcastReceiver() {
        fun register() {
            val filter = IntentFilter()
            filter.addAction(ACTION_MINIVIEW_SETTINGS_CHANGED)

            registerReceiver(this, filter)
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_MINIVIEW_SETTINGS_CHANGED -> {
                    bar.forceActionUp()
                }
            }
        }
    }

    inner class ActionsInterface : BroadcastReceiver() {
        private val queuedPosts = ArrayList<(Actions.IActionsBinderImpl) -> Unit>()

        private var binder: Actions.IActionsBinderImpl? = null

        fun register() {
            val filter = IntentFilter()
            filter.addAction(Actions.ACTIONS_STARTED)
            filter.addAction(Actions.ACTIONS_STOPPED)

            LocalBroadcastManager.getInstance(this@App).registerReceiver(this, filter)
        }

        fun post(action: (Actions.IActionsBinderImpl) -> Unit) {
            synchronized(queuedPosts) {
                if (binder != null && binder!!.isBinderAlive) {
                    action.invoke(binder!!)
                } else {
                    queuedPosts.add(action)
                }
            }
        }

        fun contains(action: (Actions.IActionsBinderImpl) -> Unit) = queuedPosts.contains(action)

        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Actions.ACTIONS_STARTED -> {
                    val bundle = intent.getBundleExtra(Actions.EXTRA_BUNDLE)
                    val binder = bundle.getBinder(Actions.EXTRA_BINDER) as Actions.IActionsBinderImpl
                    onBound(binder)
                }
                Actions.ACTIONS_STOPPED -> {
                    val bundle = intent.getBundleExtra(Actions.EXTRA_BUNDLE)
                    val binder = bundle.getBinder(Actions.EXTRA_BINDER) as Actions.IActionsBinderImpl
                    onUnbound(binder)
                }
            }
        }

        private fun onBound(binder: Actions.IActionsBinderImpl) {
            synchronized(queuedPosts) {
                if (binder.isBinderAlive) {
                    this.binder = binder
                    accessibilityConnected = true

                    queuedPosts.forEach { it.invoke(binder) }
                }
            }
        }

        private fun onUnbound(binder: Actions.IActionsBinderImpl) {
            synchronized(queuedPosts) {
                this.binder = null
                accessibilityConnected = false
            }
        }
    }
}