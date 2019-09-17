package com.xda.nobar.util.helpers

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.heinrichreimersoftware.materialintro.slide.FragmentSlide
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide
import com.xda.nobar.R
import com.xda.nobar.fragments.intro.DynamicForwardFragmentSlide
import com.xda.nobar.fragments.intro.DynamicForwardSlide
import com.xda.nobar.fragments.intro.WelcomeFragment
import com.xda.nobar.fragments.intro.WriteSecureFragment
import com.xda.nobar.util.*
import eu.chainfire.libsuperuser.Shell
import kotlinx.coroutines.launch

@SuppressLint("InlinedApi")
class IntroSlideHolder(private val activity: Activity) : ContextWrapper(activity) {
    val welcomeSlide: FragmentSlide by lazy {
        FragmentSlide.Builder()
                .background(R.color.slide_1)
                .backgroundDark(R.color.slide_1_dark)
                .fragment(WelcomeFragment())
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
        DynamicForwardSlide(SimpleSlide.Builder()
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
        DynamicForwardSlide(SimpleSlide.Builder()
                .title(R.string.accessibility_service)
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
        DynamicForwardFragmentSlide(
                FragmentSlide.Builder()
                        .background(R.color.slide_4)
                        .backgroundDark(R.color.slide_4_dark)
                        .fragment(WriteSecureFragment())
                        .buttonCtaLabel(R.string.grant)
                        .buttonCtaClickListener {
                            isSuAsync(mainHandler) {
                                if (it) {
                                    app.rootWrapper.onCreate()
                                    if (!activity.isDestroyed) {
                                        MaterialAlertDialogBuilder(this)
                                                .setTitle(R.string.root_found)
                                                .setMessage(R.string.root_found_desc)
                                                .setPositiveButton(R.string.use_root) { _, _ ->
                                                    logicScope.launch {
                                                        Shell.SU.run("pm grant $packageName ${android.Manifest.permission.WRITE_SECURE_SETTINGS}")
                                                    }
                                                }
                                                .setNegativeButton(R.string.non_root) { _, _ ->
                                                    nonRootDialog()
                                                }
                                                .show()
                                    }
                                } else {
                                    nonRootDialog()
                                }
                            }
                        }
        ) {
            prefManager.confirmedSkipWss || hasWss
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
                    launchUrl("https://dontkillmyapp.com/huawei")
                }
                .build()
    }

    val touchWizMSlide: SimpleSlide by lazy {
        SimpleSlide.Builder()
                .background(R.color.slide_6)
                .backgroundDark(R.color.slide_6_dark)
                .title(R.string.touchwiz)
                .description(R.string.touchwiz_desc)
                .buttonCtaLabel(R.string.tell_me_how)
                .buttonCtaClickListener {
                    launchUrl("https://www.xda-developers.com/?page_id=244047")
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
                    try {
                        requestBatteryExemption()
                    } catch (e: Exception) {
                        if (!activity.isDestroyed) {
                            MaterialAlertDialogBuilder(this)
                                    .setTitle(R.string.battery_optimizations)
                                    .setMessage(R.string.unable_to_request_battery_exemption)
                                    .setPositiveButton(R.string.show_me_how) { _, _ ->
                                        launchUrl("https://dontkillmyapp.com/general")
                                    }
                                    .setNegativeButton(R.string.not_now, null)
                                    .show()
                        }
                    }
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
            MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.run_command)
                    .setMessage(R.string.run_command_desc)
                    .setPositiveButton(R.string.got_it, null)
                    .setNegativeButton(R.string.need_help) { _, _ ->
                        launchUrl("https://youtu.be/Yg44Tu6oxnQ")
                    }
                    .show()
        } catch (e: Exception) {}
    }
}