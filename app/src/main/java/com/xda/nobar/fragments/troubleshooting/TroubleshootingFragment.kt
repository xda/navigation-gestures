package com.xda.nobar.fragments.troubleshooting

import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import com.xda.nobar.R
import com.xda.nobar.fragments.settings.BasePrefFragment
import com.xda.nobar.fragments.settings.CompatibilityFragment
import com.xda.nobar.fragments.settings.ExperimentalFragment
import com.xda.nobar.util.PrefManager
import com.xda.nobar.util.launchUrl
import com.xda.nobar.util.navigateTo

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
        const val REPORT_TO_GITHUB = "report_to_github"
        const val PIXEL_AMBIENT_CUT_OFF = "pixel_ambient_cut_off"
        const val BETA_SIGN_UP = "beta_sign_up"
        const val PILL_NOT_SHOWING = "pill_not_showing"
    }

    override val resId = R.xml.prefs_troubleshooting

    override fun onResume() {
        super.onResume()

        activity?.title = resources.getString(R.string.troubleshooting)
    }

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
                findNavController().navigate(
                        R.id.action_troubleshootingFragment_to_settingsActivity
                )
                true
            }
            REPORT_TO_GITHUB -> {
                context?.launchUrl("https://github.com/zacharee/nobar-issues")
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
            else -> super.onPreferenceTreeClick(preference)
        }
    }

    private fun showExplanation(title: CharSequence, message: Int) {
        AlertDialog.Builder(activity!!)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
    }
}