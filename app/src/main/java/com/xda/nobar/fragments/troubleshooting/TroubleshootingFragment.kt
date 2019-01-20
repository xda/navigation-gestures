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
                    findNavController().navigate(
                            R.id.action_troubleshootingFragment_to_compatibilityFragment,
                            Bundle().apply {
                                putString(BasePrefFragment.PREF_KEY_TO_HIGHLIGHT, "nav_hiding")
                            },
                            navOptions
                    )
                    true
                }

        findPreference<Preference>("home_not_working")
                .setOnPreferenceClickListener {
                    findNavController().navigate(
                            R.id.action_troubleshootingFragment_to_compatibilityFragment,
                            Bundle().apply {
                                putString(BasePrefFragment.PREF_KEY_TO_HIGHLIGHT, PrefManager.ALTERNATE_HOME)
                            },
                            navOptions
                    )
                    true
                }

        findPreference<Preference>("force_touch_not_working")
                .setOnPreferenceClickListener {
                    findNavController().navigate(
                            R.id.action_troubleshootingFragment_to_compatibilityFragment,
                            Bundle().apply {
                                putString(BasePrefFragment.PREF_KEY_TO_HIGHLIGHT, PrefManager.USE_IMMERSIVE_MODE_WHEN_NAV_HIDDEN)
                            },
                            navOptions
                    )
                    true
                }

        findPreference<Preference>("other_overlays")
                .setOnPreferenceClickListener {
                    findNavController().navigate(
                            R.id.action_troubleshootingFragment_to_experimentalFragment,
                            Bundle().apply {
                                putString(BasePrefFragment.PREF_KEY_TO_HIGHLIGHT, ExperimentalFragment.WINDOW_FIX)
                            }
                    )
                    true
                }

        findPreference<Preference>("hard_to_hit_pill")
                .setOnPreferenceClickListener {
                    findNavController().navigate(
                            R.id.action_troubleshootingFragment_to_behaviorFragment,
                            Bundle().apply {
                                putString(BasePrefFragment.PREF_KEY_TO_HIGHLIGHT, PrefManager.LARGER_HITBOX)
                            },
                            navOptions
                    )
                    true
                }

        findPreference<Preference>("white_line")
                .setOnPreferenceClickListener {
                    findNavController().navigate(
                            R.id.action_troubleshootingFragment_to_experimentalFragment,
                            Bundle().apply {
                                putString(BasePrefFragment.PREF_KEY_TO_HIGHLIGHT, PrefManager.FULL_OVERSCAN)
                            },
                            navOptions
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
                    AlertDialog.Builder(activity!!)
                            .setTitle(it.summary)
                            .setMessage(R.string.pill_overlaps_content_expl)
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                    true
                }
    }

    override fun onResume() {
        super.onResume()

        activity?.title = resources.getString(R.string.troubleshooting)
    }
}