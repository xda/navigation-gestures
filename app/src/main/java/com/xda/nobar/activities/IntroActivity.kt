package com.xda.nobar.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.heinrichreimersoftware.materialintro.app.IntroActivity
import com.heinrichreimersoftware.materialintro.app.SlideFragment
import com.heinrichreimersoftware.materialintro.slide.FragmentSlide
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide
import com.topjohnwu.superuser.Shell
import com.xda.nobar.R
import com.xda.nobar.prefs.PrefManager
import com.xda.nobar.util.Utils
import com.xda.nobar.util.allowHiddenMethods
import com.xda.nobar.util.checkEMUI
import com.xda.nobar.util.getSystemProperty
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
            val accessibilityGranted = Utils.isAccessibilityEnabled(context)

            return !overlaysGranted || !accessibilityGranted || PrefManager.getInstance(context).firstRun
        }

        fun hasWss(context: Context): Boolean {
            return context.checkCallingOrSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
        }

        fun start(context: Context, extras: Bundle = Bundle()) {
            Exception().printStackTrace()
            val launch = Intent(context, com.xda.nobar.activities.IntroActivity::class.java)
            launch.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK)
            launch.putExtras(extras)

            context.startActivity(launch)
        }
    }

    private var didntNeedToRun = false
    private val prefManager by lazy { PrefManager.getInstance(this) }

    private val welcomeSlide = FragmentSlide.Builder()
            .background(R.color.slide_1)
            .backgroundDark(R.color.slide_1_dark)
            .fragment(WelcomeFragment())
            .build()

    private val missingPermsSlide = SimpleSlide.Builder()
            .background(R.color.slide_1)
            .backgroundDark(R.color.slide_1_dark)
            .title(R.string.missing_perms)
            .description(R.string.missing_perms_desc)
            .build()

    @RequiresApi(Build.VERSION_CODES.M)
    private val overlaySlide = DynamicForwardSlide(SimpleSlide.Builder()
            .title(R.string.draw_over_apps)
            .description(R.string.draw_over_apps_desc)
            .image(R.drawable.nav_overlay)
            .background(R.color.slide_2)
            .backgroundDark(R.color.slide_2_dark)
            .buttonCtaLabel(R.string.grant)
            .buttonCtaClickListener {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    intent.data = null
                    startActivity(intent)
                }
            }
    ) { Settings.canDrawOverlays(this) }

    private val accessibilitySlide = DynamicForwardSlide(SimpleSlide.Builder()
            .title(R.string.accessibility)
            .description(R.string.accessibility_desc)
            .image(R.drawable.nav_acc)
            .background(R.color.slide_3)
            .backgroundDark(R.color.slide_3_dark)
            .buttonCtaLabel(R.string.grant)
            .buttonCtaClickListener {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    intent.action = Settings.ACTION_SETTINGS
                    startActivity(intent)
                    Toast.makeText(this, resources.getText(R.string.accessibility_msg), Toast.LENGTH_LONG).show()
                }
            }
    ) { Utils.isAccessibilityEnabled(this) }

    //Write Secure Settings slide: prompt the user to grant this permission; used for hiding the navbar
    private val wssSlide = DynamicForwardFragmentSlide(FragmentSlide.Builder()
            .background(R.color.slide_4)
            .backgroundDark(R.color.slide_4_dark)
            .fragment(WriteSecureFragment())
            .buttonCtaLabel(R.string.grant)
            .buttonCtaClickListener {
                if (Shell.rootAccess()) {
                    AlertDialog.Builder(this)
                            .setTitle(R.string.root_found)
                            .setMessage(R.string.root_found_desc)
                            .setPositiveButton(R.string.use_root) { _, _ ->
                                Shell.su("pm grant $packageName ${Manifest.permission.WRITE_SECURE_SETTINGS}").submit()
                            }
                            .setNegativeButton(R.string.non_root) { _, _ ->
                                nonRootDialog()
                            }
                            .show()
                } else {
                    nonRootDialog()
                }
            }) {
        prefManager.confirmedSkipWss || (if (hasWss(this)) {
            allowHiddenMethods()
            true
        } else false)
    }

    private val emuiSlide = SimpleSlide.Builder()
            .background(R.color.slide_4)
            .backgroundDark(R.color.slide_4_dark)
            .title(R.string.emui)
            .description(R.string.emui_desc)
            .buttonCtaLabel(R.string.show_me_how)
            .buttonCtaClickListener {
                val emuiBatt = Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://support.doubletwist.com/hc/en-us/articles/360001504071-How-to-turn-off-battery-optimization-on-Huawei-devices"))
                emuiBatt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(emuiBatt)
            }
            .build()

    @SuppressLint("BatteryLife")
    @RequiresApi(Build.VERSION_CODES.M)
    private val touchwizMSlide = SimpleSlide.Builder()
            .background(R.color.slide_6)
            .backgroundDark(R.color.slide_6_dark)
            .title(R.string.touchwiz)
            .description(R.string.touchwiz_desc)
            .buttonCtaLabel(R.string.disable)
            .buttonCtaClickListener {
                val batt = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
                batt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(batt)
            }
            .build()

    private val qsSlide = SimpleSlide.Builder()
            .title(R.string.qs_tile)
            .description(R.string.nougat_qs_reminder)
            .image(R.drawable.qs)
            .background(R.color.slide_6)
            .backgroundDark(R.color.slide_6_dark)
            .build()

    private val warnSlide = SimpleSlide.Builder()
            .title(R.string.warning)
            .description(R.string.warning_desc)
            .background(R.color.slide_7)
            .backgroundDark(R.color.slide_7_dark)
            .build()

    private val whiteBarSlide = SimpleSlide.Builder()
            .title(R.string.white_bar)
            .description(R.string.white_bar_desc)
            .background(R.color.slide_4)
            .backgroundDark(R.color.slide_4_dark)
            .build()

    private val compatibilitySlide = SimpleSlide.Builder()
            .title(R.string.compatibility)
            .description(R.string.compatibility_desc)
            .background(R.color.slide_3)
            .backgroundDark(R.color.slide_3_dark)
            .build()

    private val allSetSlide = SimpleSlide.Builder()
            .title(R.string.ready)
            .description(if (prefManager.firstRun) R.string.ready_first_run_desc else R.string.ready_desc)
            .background(R.color.slide_5)
            .backgroundDark(R.color.slide_5_dark)
            .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!needsToRun(this) && hasWss(this)) {
            didntNeedToRun = true
            finish()
            return
        }

        buttonBackFunction = BUTTON_BACK_FUNCTION_BACK
        isButtonBackVisible = true

        if (intent.hasExtra(EXTRA_WSS_ONLY)) { //The following logic will be used if the user tries to hide the navbar, but didn't grant WSS during the initial setup
            addSlide(wssSlide)
        } else {
            //Only show the intro if the device is able to run the needed commands. Otherwise, show failure screen
            if (prefManager.firstRun) {
                addSlide(welcomeSlide)
            } else {
                addSlide(missingPermsSlide)
            }

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1
                    && !Settings.canDrawOverlays(this)) {
                addSlide(overlaySlide)
            }

            if (!Utils.isAccessibilityEnabled(this)) {
                addSlide(accessibilitySlide)
            }

            if (checkCallingOrSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                addSlide(wssSlide)
            }

            if (prefManager.firstRun && Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                if (Utils.checkTouchWiz(this) && Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1)
                    addSlide(touchwizMSlide)

                if (checkEMUI())
                    addSlide(emuiSlide)

                addSlide(qsSlide)
                addSlide(warnSlide)
                addSlide(whiteBarSlide)
                addSlide(compatibilitySlide)
            }

            addSlide(allSetSlide)
        }

        addOnNavigationBlockedListener { index, _ ->
            if (index == indexOfSlide(wssSlide)) {
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

    private fun nonRootDialog() {
        try {
            AlertDialog.Builder(this)
                    .setTitle(R.string.run_command)
                    .setMessage(R.string.run_command_desc)
                    .setPositiveButton(R.string.got_it, null)
                    .setNegativeButton(R.string.need_help) { _, _ ->
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse("https://youtu.be/Yg44Tu6oxnQ")
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                    .show()
        } catch (e: Exception) {
        }
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
