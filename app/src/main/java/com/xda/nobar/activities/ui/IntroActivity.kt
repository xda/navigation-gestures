package com.xda.nobar.activities.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import com.heinrichreimersoftware.materialintro.app.IntroActivity
import com.xda.nobar.R
import com.xda.nobar.activities.MainActivity
import com.xda.nobar.util.*
import com.xda.nobar.util.helpers.IntroSlideHolder
import kotlinx.coroutines.launch

/**
 * Introduction activity for Navigation Gestures
 * Lead user through setup process
 */
class IntroActivity : IntroActivity() {
    companion object {
        const val EXTRA_WSS_ONLY = "wss"

        fun needsToRun(context: Context): Boolean {
            val overlaysGranted = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) Settings.canDrawOverlays(context) else true
            val accessibilityGranted = context.app.accessibilityConnected

            return !overlaysGranted || !accessibilityGranted || context.prefManager.firstRun
        }

        fun start(context: Context, extras: Bundle = Bundle()) {
            val launch = Intent(context, com.xda.nobar.activities.ui.IntroActivity::class.java)
            launch.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK)
            launch.putExtras(extras)

            context.startActivity(launch)
        }

        fun startForWss(context: Context) {
            start(context, Bundle().apply { putBoolean(EXTRA_WSS_ONLY, true) })
        }

        fun needsToRunAsync(context: Context, listener: (Boolean) -> Unit) {
            logicScope.launch {
                val needsToRun = needsToRun(context)

                mainScope.launch { listener.invoke(needsToRun) }
            }
        }
    }

    private var didntNeedToRun = false

    private val slides by lazy { IntroSlideHolder(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        app.introRunning = true

        if (!needsToRun(this) && hasWss) {
            didntNeedToRun = true
            finish()
            return
        }

        buttonBackFunction = BUTTON_BACK_FUNCTION_BACK
        isButtonBackVisible = true

        if (intent.hasExtra(EXTRA_WSS_ONLY)) { //The following logic will be used if the user tries to hide the navbar, but didn't grant WSS during the initial setup
            addSlide(slides.wssSlide)
        } else {
            //Only show the intro if the device is able to run the needed commands. Otherwise, show failure screen
            if (prefManager.firstRun) {
                addSlide(slides.welcomeSlide)
            } else {
                addSlide(slides.missingPermsSlide)
            }

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1
                    && !Settings.canDrawOverlays(this)) {
                addSlide(slides.overlaySlide)
            }

            if (!isAccessibilityEnabled) {
                addSlide(slides.accessibilitySlide)
            }

            if (!hasWss) {
                addSlide(slides.wssSlide)
            }

            if (prefManager.firstRun) {
                if (isTouchWiz && Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1)
                    addSlide(slides.touchWizMSlide)

                if (checkEMUI())
                    addSlide(slides.emuiSlide)

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                    addSlide(slides.batterySlide)
                }

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                    addSlide(slides.qsSlide)
                }

                addSlide(slides.warnSlide)
                addSlide(slides.whiteBarSlide)
                addSlide(slides.compatibilitySlide)
            }

            addSlide(slides.allSetSlide)
        }

        addOnNavigationBlockedListener { index, _ ->
            if (index == indexOfSlide(slides.wssSlide)) {
                AlertDialog.Builder(this)
                        .setTitle(R.string.are_you_sure)
                        .setMessage(R.string.skip_wss_message)
                        .setPositiveButton(android.R.string.yes) { _, _ ->
                            prefManager.confirmedSkipWss = true
                            nextSlide()
                        }
                        .setNegativeButton(android.R.string.no, null)
                        .show()
            }
        }

        if (prefManager.firstRun) { //If the user is using a tablet, we want to turn Tablet Mode on automatically
            if (getSystemProperty("ro.build.characteristics")?.contains("tablet") == true) {
                prefManager.useTabletMode = true
            }
        }
    }

    override fun onBackPressed() {
        previousSlide()
    }

    override fun onDestroy() {
        prefManager.firstRun = false
        if (!didntNeedToRun && !needsToRun(this)) MainActivity.start(this)

        super.onDestroy()

        app.introRunning = false
    }
}
