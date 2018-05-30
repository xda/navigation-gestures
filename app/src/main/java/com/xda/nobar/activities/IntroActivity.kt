package com.xda.nobar.activities

import android.Manifest
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import com.heinrichreimersoftware.materialintro.app.IntroActivity
import com.heinrichreimersoftware.materialintro.app.OnNavigationBlockedListener
import com.heinrichreimersoftware.materialintro.app.SlideFragment
import com.heinrichreimersoftware.materialintro.slide.FragmentSlide
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide
import com.xda.nobar.R
import com.xda.nobar.util.SuUtils
import com.xda.nobar.util.Utils

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

            return !overlaysGranted || !accessibilityGranted || Utils.isFirstRun(context) || !Utils.canRunHiddenCommands(context)
        }

        fun hasWss(context: Context): Boolean {
            return context.checkCallingOrSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        buttonBackFunction = BUTTON_BACK_FUNCTION_BACK
        isButtonBackVisible = true

        val wssSlide = DynamicForwardFragmentSlide(FragmentSlide.Builder()
                .background(R.color.slide_4)
                .backgroundDark(R.color.slide_4_dark)
                .fragment(WriteSecureFragment())
                .buttonCtaLabel(R.string.grant)
                .buttonCtaClickListener {
                    if (SuUtils.testSudo()) {
                        AlertDialog.Builder(this)
                                .setTitle(R.string.root_found)
                                .setMessage(R.string.root_found_desc)
                                .setPositiveButton(R.string.use_root, { _, _ ->
                                    SuUtils.sudo("pm grant $packageName ${Manifest.permission.WRITE_SECURE_SETTINGS}")
                                })
                                .setNegativeButton(R.string.non_root, { _, _ ->
                                    nonRootDialog()
                                })
                                .show()
                    } else {
                        nonRootDialog()
                    }
                }, { prefs.getBoolean("has_confirmed_skip_wss", false) })

        if (intent.hasExtra(EXTRA_WSS_ONLY)) {
            addSlide(wssSlide)

            addOnNavigationBlockedListener { index, dir ->
                if (index == indexOfSlide(wssSlide)) {
                    AlertDialog.Builder(this)
                            .setTitle(R.string.are_you_sure)
                            .setMessage(R.string.skip_wss_message)
                            .setPositiveButton(android.R.string.yes, { _, _ ->
                                prefs.edit().putBoolean("has_confirmed_skip_wss", true).apply()
                                nextSlide()
                            })
                            .setNegativeButton(android.R.string.no, null)
                            .show()
                }
            }
        } else {
            //Only show the intro if the device is able to run the needed commands. Otherwise, show failure screen
            if (Utils.canRunHiddenCommands(this)) {
                if (Utils.isFirstRun(this)) {
                    addSlide(FragmentSlide.Builder()
                            .background(R.color.slide_1)
                            .backgroundDark(R.color.slide_1_dark)
                            .fragment(WelcomeFragment())
                            .build())
                } else {
                    addSlide(SimpleSlide.Builder()
                            .background(R.color.slide_1)
                            .backgroundDark(R.color.slide_1_dark)
                            .title(R.string.missing_perms)
                            .description(R.string.missing_perms_desc)
                            .build())
                }

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1 && !Settings.canDrawOverlays(this)) {
                    addSlide(DynamicForwardSlide(SimpleSlide.Builder()
                            .title(R.string.draw_over_apps)
                            .description(R.string.draw_over_apps_desc)
                            .image(R.drawable.nav_overlay)
                            .background(R.color.slide_2)
                            .backgroundDark(R.color.slide_2_dark)
                            .buttonCtaLabel(R.string.grant)
                            .buttonCtaClickListener {
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                intent.data = Uri.parse("package:$packageName")

                                try {
                                    startActivity(intent)
                                } catch (e: ActivityNotFoundException) {
                                    intent.data = null
                                    startActivity(intent)
                                }
                            },
                            {Settings.canDrawOverlays(this)}))
                }

                if (!Utils.isAccessibilityEnabled(this)) {
                    addSlide(DynamicForwardSlide(SimpleSlide.Builder()
                            .title(R.string.accessibility)
                            .description(R.string.accessibility_desc)
                            .image(R.drawable.nav_acc)
                            .background(R.color.slide_3)
                            .backgroundDark(R.color.slide_3_dark)
                            .buttonCtaLabel(R.string.grant)
                            .buttonCtaClickListener {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

                                try {
                                    startActivity(intent)
                                } catch (e: ActivityNotFoundException) {
                                    intent.action = Settings.ACTION_SETTINGS
                                    startActivity(intent)
                                    Toast.makeText(this, resources.getText(R.string.accessibility_msg), Toast.LENGTH_LONG).show()
                                }
                            },
                            {Utils.isAccessibilityEnabled(this)}))
                }

                if (checkCallingOrSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                    addSlide(wssSlide)
                }

                if (Utils.isFirstRun(this) && Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                    addSlide(SimpleSlide.Builder()
                            .title(R.string.qs_tile)
                            .description(R.string.nougat_qs_reminder)
                            .image(R.drawable.qs)
                            .background(R.color.slide_6)
                            .backgroundDark(R.color.slide_6_dark)
                            .build())
                }

                addSlide(SimpleSlide.Builder()
                        .title(R.string.warning)
                        .description(R.string.warning_desc)
                        .background(R.color.slide_7)
                        .backgroundDark(R.color.slide_7_dark)
                        .build())

                addSlide(SimpleSlide.Builder()
                        .title(R.string.white_bar)
                        .description(R.string.white_bar_desc)
                        .background(R.color.slide_4)
                        .backgroundDark(R.color.slide_4_dark)
                        .build())

                addSlide(SimpleSlide.Builder()
                        .title(R.string.compatibility)
                        .description(R.string.compatibility_desc)
                        .background(R.color.slide_3)
                        .backgroundDark(R.color.slide_3_dark)
                        .build())

                addSlide(SimpleSlide.Builder()
                        .title(R.string.ready)
                        .description(if (Utils.isFirstRun(this)) R.string.ready_first_run_desc else R.string.ready_desc)
                        .background(R.color.slide_5)
                        .backgroundDark(R.color.slide_5_dark)
                        .build())

                addOnNavigationBlockedListener { index, dir ->
                    if (index == indexOfSlide(wssSlide)) {
                        AlertDialog.Builder(this)
                                .setTitle(R.string.are_you_sure)
                                .setMessage(R.string.skip_wss_message)
                                .setPositiveButton(android.R.string.yes, { _, _ ->
                                    prefs.edit().putBoolean("has_confirmed_skip_wss", true).apply()
                                    nextSlide()
                                })
                                .setNegativeButton(android.R.string.no, null)
                                .show()
                    } else if (dir == OnNavigationBlockedListener.DIRECTION_FORWARD) {
                        Toast.makeText(this, resources.getString(R.string.grant_permission), Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                addSlide(SimpleSlide.Builder()
                        .background(R.color.slide_1)
                        .backgroundDark(R.color.slide_1_dark)
                        .title(R.string.sorry)
                        .description(R.string.sorry_desc)
                        .build())
            }
        }
    }

    override fun onBackPressed() {
        previousSlide()
    }

    override fun onDestroy() {
        super.onDestroy()

        Utils.setFirstRun(this, false)
    }

    private fun nonRootDialog() {
        try {
            AlertDialog.Builder(this)
                    .setTitle(R.string.run_command)
                    .setMessage(R.string.run_command_desc)
                    .setPositiveButton(R.string.got_it, null)
                    .setNegativeButton(R.string.need_help, { _, _ ->
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse("https://youtu.be/Yg44Tu6oxnQ")
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    })
                    .show()
        } catch (e: Exception) {}
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

            val title = view.findViewById<TextView>(R.id.mi_title)
            val desc = view.findViewById<TextView>(R.id.mi_description)

            title.text = resources.getText(R.string.welcome)
            desc.text = resources.getText(R.string.app_purpose)
        }

        override fun onResume() {
            super.onResume()
            val image = view?.findViewById<VideoView>(R.id.mi_image)

            try {
                val uri = Uri.parse("android.resource://${context?.packageName}/${R.raw.nav_gesture}")
                image?.setVideoURI(uri)

                image?.setOnPreparedListener {
                    it.isLooping = true
                }

                image?.start()
            } catch (e: Exception) {
                image?.visibility = View.GONE
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

            val title = view.findViewById<TextView>(R.id.mi_title)
            val desc = view.findViewById<TextView>(R.id.mi_description)

            title.text = resources.getText(R.string.write_secure_settings)
            desc.text = resources.getText(R.string.write_secure_settings_desc)
        }

        override fun onResume() {
            super.onResume()
            val image = view?.findViewById<VideoView>(R.id.mi_image)

            try {
                val uri = Uri.parse("android.resource://${context?.packageName}/${R.raw.hide_nav}")
                image?.setVideoURI(uri)

                image?.setOnPreparedListener {
                    it.isLooping = true
                }

                image?.start()
            } catch (e: Exception) {
                image?.visibility = View.GONE
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
