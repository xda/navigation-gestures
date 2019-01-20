package com.xda.nobar.fragments.troubleshooting

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.xda.nobar.R
import com.xda.nobar.fragments.settings.BasePrefFragment
import com.xda.nobar.fragments.settings.ExperimentalFragment
import com.xda.nobar.util.PrefManager
import com.xda.nobar.util.navOptions

class TroubleshootingFragment : BasePrefFragment() {
    override val resId = R.xml.prefs_troubleshooting

    override fun onStart() {
        super.onStart()

        findPreference<Preference>("nav_not_hiding")
                .setOnPreferenceClickListener {
                    navigateTo(
                            R.id.action_troubleshootingFragment_to_compatibilityFragment,
                            "nav_hiding"
                    )
                    true
                }

        findPreference<Preference>("home_not_working")
                .setOnPreferenceClickListener {
                    navigateTo(
                            R.id.action_troubleshootingFragment_to_compatibilityFragment,
                            PrefManager.ALTERNATE_HOME
                    )
                    true
                }

        findPreference<Preference>("force_touch_not_working")
                .setOnPreferenceClickListener {
                    navigateTo(
                            R.id.action_troubleshootingFragment_to_compatibilityFragment,
                            PrefManager.USE_IMMERSIVE_MODE_WHEN_NAV_HIDDEN
                    )
                    true
                }

        findPreference<Preference>("hard_to_hit_pill")
                .setOnPreferenceClickListener {
                    navigateTo(
                            R.id.action_troubleshootingFragment_to_behaviorFragment,
                            PrefManager.LARGER_HITBOX
                    )
                    true
                }

        findPreference<Preference>("other_overlays")
                .setOnPreferenceClickListener {
                    navigateTo(
                            R.id.action_troubleshootingFragment_to_experimentalFragment,
                            ExperimentalFragment.WINDOW_FIX
                    )
                    true
                }

        findPreference<Preference>("white_line")
                .setOnPreferenceClickListener {
                    navigateTo(
                            R.id.action_troubleshootingFragment_to_experimentalFragment,
                            PrefManager.FULL_OVERSCAN
                    )
                    true
                }

        findPreference<Preference>("something_else")
                .setOnPreferenceClickListener {
                    findNavController().navigate(
                            R.id.action_troubleshootingFragment_to_settingsActivity
                    )
                    true
                }

        findPreference<Preference>("pill_overlaps")
                .setOnPreferenceClickListener {
                    showExplanation(it.summary, R.string.pill_overlaps_content_expl)
                    true
                }
    }

    override fun onResume() {
        super.onResume()

        activity?.title = resources.getString(R.string.troubleshooting)
    }

    private fun navigateTo(action: Int, highlightKey: String? = null) {
        findNavController().navigate(
                action,
                Bundle().apply {
                    putString(BasePrefFragment.PREF_KEY_TO_HIGHLIGHT, highlightKey ?: return@apply)
                },
                navOptions
        )
    }

    private fun showExplanation(title: CharSequence, message: Int) {
        AlertDialog.Builder(activity!!)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
    }
}