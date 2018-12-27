package com.xda.nobar.util.helpers

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.heinrichreimersoftware.materialintro.slide.FragmentSlide
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide
import com.topjohnwu.superuser.Shell
import com.xda.nobar.R
import com.xda.nobar.activities.ui.IntroActivity
import com.xda.nobar.util.*

@SuppressLint("InlinedApi")
class IntroSlideHolder(context: Context) : ContextWrapper(context) {
    val welcomeSlide: FragmentSlide by lazy {
        FragmentSlide.Builder()
                .background(R.color.slide_1)
                .backgroundDark(R.color.slide_1_dark)
                .fragment(IntroActivity.WelcomeFragment())
                .build()
    }

    val missingPermsSlide: SimpleSlide by lazy {
        SimpleSlide.Builder()
                .background(R.color.slide_1)
                .backgroundDark(R.color.slide_1_dark)
                .title(R.string.missing_perms)
                .description(R.string.missing_perms_desc)
                .build()
    }

    val overlaySlide by lazy {
        IntroActivity.DynamicForwardSlide(SimpleSlide.Builder()
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
    }

    val accessibilitySlide by lazy {
        IntroActivity.DynamicForwardSlide(SimpleSlide.Builder()
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
        ) { isAccessibilityEnabled }
    }

    //Write Secure Settings slide: prompt the user to grant this permission; used for hiding the navbar and some other stuff
    val wssSlide by lazy {
        IntroActivity.DynamicForwardFragmentSlide(FragmentSlide.Builder()
                .background(R.color.slide_4)
                .backgroundDark(R.color.slide_4_dark)
                .fragment(IntroActivity.WriteSecureFragment())
                .buttonCtaLabel(R.string.grant)
                .buttonCtaClickListener {
                    if (Shell.rootAccess()) {
                        app.rootWrapper.onCreate()
                        AlertDialog.Builder(this)
                                .setTitle(R.string.root_found)
                                .setMessage(R.string.root_found_desc)
                                .setPositiveButton(R.string.use_root) { _, _ ->
                                    app.rootWrapper.actions?.grantPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
                                }
                                .setNegativeButton(R.string.non_root) { _, _ ->
                                    nonRootDialog()
                                }
                                .show()
                    } else {
                        nonRootDialog()
                    }
                }) {
            prefManager.confirmedSkipWss || (if (hasWss) {
                allowHiddenMethods()
                true
            } else false)
        }
    }

    val emuiSlide: SimpleSlide by lazy {
        SimpleSlide.Builder()
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
    }

    val touchWizMSlide: SimpleSlide by lazy {
        SimpleSlide.Builder()
                .background(R.color.slide_6)
                .backgroundDark(R.color.slide_6_dark)
                .title(R.string.touchwiz)
                .description(R.string.touchwiz_desc)
                .buttonCtaLabel(R.string.disable)
                .buttonCtaClickListener {
                    //TODO: Instructions
                }
                .build()
    }

    val batterySlide: SimpleSlide by lazy {
        SimpleSlide.Builder()
                .background(R.color.slide_4)
                .backgroundDark(R.color.slide_4_dark)
                .title(R.string.battery_optimizations)
                .description(R.string.battery_optimizations_desc)
                .buttonCtaLabel(R.string.disable)
                .buttonCtaClickListener {
                    requestBatteryExemption()
                }
                .build()
    }

    val qsSlide: SimpleSlide by lazy {
        SimpleSlide.Builder()
                .title(R.string.qs_tile)
                .description(R.string.nougat_qs_reminder)
                .image(R.drawable.qs)
                .background(R.color.slide_6)
                .backgroundDark(R.color.slide_6_dark)
                .build()
    }

    val warnSlide: SimpleSlide by lazy {
        SimpleSlide.Builder()
                .title(R.string.warning)
                .description(R.string.warning_desc)
                .background(R.color.slide_7)
                .backgroundDark(R.color.slide_7_dark)
                .build()
    }

    val whiteBarSlide: SimpleSlide by lazy {
        SimpleSlide.Builder()
                .title(R.string.white_bar)
                .description(R.string.white_bar_desc)
                .background(R.color.slide_4)
                .backgroundDark(R.color.slide_4_dark)
                .build()
    }

    val compatibilitySlide: SimpleSlide by lazy {
        SimpleSlide.Builder()
                .title(R.string.compatibility)
                .description(R.string.compatibility_desc)
                .background(R.color.slide_3)
                .backgroundDark(R.color.slide_3_dark)
                .build()
    }

    val allSetSlide: SimpleSlide by lazy {
        SimpleSlide.Builder()
                .title(R.string.ready)
                .description(if (prefManager.firstRun) R.string.ready_first_run_desc else R.string.ready_desc)
                .background(R.color.slide_5)
                .backgroundDark(R.color.slide_5_dark)
                .build()
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
}