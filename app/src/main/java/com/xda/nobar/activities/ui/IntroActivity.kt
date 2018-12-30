package com.xda.nobar.activities.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.heinrichreimersoftware.materialintro.app.IntroActivity
import com.heinrichreimersoftware.materialintro.app.SlideFragment
import com.heinrichreimersoftware.materialintro.slide.FragmentSlide
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide
import com.xda.nobar.R
import com.xda.nobar.activities.MainActivity
import com.xda.nobar.util.*
import com.xda.nobar.util.helpers.IntroSlideHolder
import kotlinx.android.synthetic.main.slide_welcome.*

/**
 * Introduction activity for Navigation Gestures
 * Lead user through setup process
 */
class IntroActivity : IntroActivity() {
    companion object {
        const val EXTRA_WSS_ONLY = "wss"

        fun needsToRun(context: Context): Boolean {
            val overlaysGranted = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) Settings.canDrawOverlays(context) else true
            val accessibilityGranted = context.isAccessibilityEnabled

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
    }

    private var didntNeedToRun = false

    private val slides by lazy { IntroSlideHolder(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                    addSlide(slides.qsSlide)
                }

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                    addSlide(slides.batterySlide)
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
        super.onDestroy()

        prefManager.firstRun = false
        if (!didntNeedToRun) MainActivity.start(this)
    }

    /**
     * The first slide: show a welcome
     * Uses a custom layout to show a video instead of an image
     */
    class WelcomeFragment : SlideFragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.slide_welcome, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            mi_title.text = resources.getText(R.string.welcome)
            mi_description.text = resources.getText(R.string.app_purpose)
        }

        override fun onResume() {
            super.onResume()
            try {
                val uri = Uri.parse("android.resource://${context?.packageName}/${R.raw.nav_gesture}")
                mi_image?.setVideoURI(uri)

                mi_image?.setOnPreparedListener {
                    it.isLooping = true
                }

                mi_image?.start()
            } catch (e: Exception) {
                mi_image?.visibility = View.GONE
            }
        }
    }

    /**
     * Similar to WelcomeFragment but with a different video
     */
    class WriteSecureFragment : SlideFragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.slide_welcome, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            mi_title.text = resources.getText(R.string.write_secure_settings)
            mi_description.text = resources.getText(R.string.write_secure_settings_desc)
        }

        override fun onResume() {
            super.onResume()
            try {
                val uri = Uri.parse("android.resource://${context?.packageName}/${R.raw.hide_nav}")
                mi_image?.setVideoURI(uri)

                mi_image?.setOnPreparedListener {
                    it.isLooping = true
                }

                mi_image?.start()
            } catch (e: Exception) {
                mi_image?.visibility = View.GONE
            }
        }
    }

    /**
     * The library only checks once if the user can go forward in the simple builder
     * so we need to wrap that builder
     */
    class DynamicForwardSlide(builder: SimpleSlide.Builder, private val action: () -> Boolean) : SimpleSlide(builder) {
        override fun canGoForward(): Boolean {
            return action.invoke()
        }
    }

    /**
     * Same as DynamicForwardSlide but for FragmentSlides
     */
    class DynamicForwardFragmentSlide(builder: FragmentSlide.Builder, private val action: () -> Boolean) : FragmentSlide(builder) {
        override fun canGoForward(): Boolean {
            return action.invoke()
        }
    }
}
