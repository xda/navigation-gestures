package com.xda.nobar.fragments.troubleshooting

import android.provider.Settings
import androidx.preference.Preference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xda.nobar.R
import com.xda.nobar.fragments.settings.BasePrefFragment
import com.xda.nobar.fragments.settings.CompatibilityFragment
import com.xda.nobar.fragments.settings.ExperimentalFragment
import com.xda.nobar.util.*
import kotlinx.coroutines.launch

class TroubleshootingFragment : BasePrefFragment() {
    companion object {
        const val NAV_NOT_HIDING = "nav_not_hiding"
        const val HOME_NOT_WORKING = "home_not_working"
        const val FORCE_TOUCH_NOT_WORKING = "force_touch_not_working"
        const val HARD_TO_HIT_PILL = "hard_to_hit_pill"
        const val LOCKSCREEN_SHORTCUTS = "lockscreen_shortcuts"
        const val OTHER_OVERLAYS = "other_overlays"
        const val WHITE_LINE = "white_line"
        const val PILL_OVERLAPS = "pill_overlaps"
        const val SOMETHING_ELSE = "something_else"
        const val REPORT_ISSUE = "report_issue"
        const val PIXEL_AMBIENT_CUT_OFF = "pixel_ambient_cut_off"
        const val BETA_SIGN_UP = "beta_sign_up"
        const val PILL_NOT_SHOWING = "pill_not_showing"
        const val AUTO_HIDING_NAV = "auto_hiding_nav"
        const val RESTART_PRPBLEMS = "restart_problems"
    }

    override val resId = R.xml.prefs_troubleshooting
    override val activityTitle by lazy { resources.getText(R.string.troubleshooting) }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        return when(preference?.key) {
            NAV_NOT_HIDING -> {
                navigateTo(
                        R.id.action_troubleshootingFragment_to_compatibilityFragment,
                        CompatibilityFragment.NAV_HIDING
                )
                true
            }
            HOME_NOT_WORKING -> {
                navigateTo(
                        R.id.action_troubleshootingFragment_to_compatibilityFragment,
                        PrefManager.ALTERNATE_HOME
                )
                true
            }
            FORCE_TOUCH_NOT_WORKING -> {
                navigateTo(
                        R.id.action_troubleshootingFragment_to_compatibilityFragment,
                        PrefManager.USE_IMMERSIVE_MODE_WHEN_NAV_HIDDEN
                )
                true
            }
            HARD_TO_HIT_PILL -> {
                navigateTo(
                        R.id.action_troubleshootingFragment_to_behaviorFragment,
                        PrefManager.LARGER_HITBOX
                )
                true
            }
            LOCKSCREEN_SHORTCUTS -> {
                navigateTo(
                        R.id.action_troubleshootingFragment_to_behaviorFragment,
                        PrefManager.LOCKSCREEN_OVERSCAN
                )
                true
            }
            PIXEL_AMBIENT_CUT_OFF -> {
                navigateTo(
                        R.id.action_troubleshootingFragment_to_behaviorFragment,
                        PrefManager.LOCKSCREEN_OVERSCAN
                )
                true
            }
            OTHER_OVERLAYS -> {
                navigateTo(
                        R.id.action_troubleshootingFragment_to_experimentalFragment,
                        ExperimentalFragment.WINDOW_FIX
                )
                true
            }
            WHITE_LINE -> {
                navigateTo(
                        R.id.action_troubleshootingFragment_to_experimentalFragment,
                        PrefManager.FULL_OVERSCAN
                )
                true
            }
            PILL_OVERLAPS -> {
                showExplanation(preference.summary, R.string.pill_overlaps_content_expl)
                true
            }
            SOMETHING_ELSE -> {
                navigateTo(
                        R.id.action_troubleshootingFragment_to_settingsActivity,
                        null
                )
                true
            }
            REPORT_ISSUE -> {
                context?.launchUrl("https://support.xda-developers.com")
                true
            }
            BETA_SIGN_UP -> {
                context?.launchUrl("https://play.google.com/apps/testing/com.xda.nobar")
                true
            }
            PILL_NOT_SHOWING -> {
                showExplanation(preference.summary, R.string.pill_not_showing_desc)
                true
            }
            AUTO_HIDING_NAV -> {
                if (context!!.hasWss) {
                    logicScope.launch {
                        Settings.Global.putString(context?.contentResolver, POLICY_CONTROL, null)
                    }
                }
                showExplanation(preference.summary, R.string.fixed)
                true
            }
            RESTART_PRPBLEMS -> {
                context?.restartApp()
                true
            }
            else -> super.onPreferenceTreeClick(preference)
        }
    }

    private fun showExplanation(title: CharSequence, message: Int) {
        MaterialAlertDialogBuilder(activity!!)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
    }
}