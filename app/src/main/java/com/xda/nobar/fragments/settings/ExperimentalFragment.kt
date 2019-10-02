package com.xda.nobar.fragments.settings

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.preference.Preference
import com.xda.nobar.R
import com.xda.nobar.activities.selectors.BlacklistSelectorActivity
import com.xda.nobar.util.PrefManager
import com.xda.nobar.util.hasUsage

/**
 * Experimental, but mostly working settings
 */
class ExperimentalFragment : BasePrefFragment() {
    companion object {
        const val WINDOW_FIX = "window_fix"
    }

    override val resId = R.xml.prefs_experimental

    override fun onResume() {
        super.onResume()

        activity?.title = resources.getText(R.string.experimental_prefs)

        setListeners()
    }

    private fun setListeners() {
        val winFix = findPreference<Preference>(WINDOW_FIX)!!
        winFix.setOnPreferenceClickListener {
            val blIntent = Intent(activity, BlacklistSelectorActivity::class.java)
            blIntent.putExtra(BlacklistSelectorActivity.EXTRA_WHICH, BlacklistSelectorActivity.FOR_WIN)
            startActivity(blIntent)

            true
        }

        val overlayNav = findPreference<Preference>(PrefManager.OVERLAY_NAV)!!
        overlayNav.isEnabled = !prefManager.shouldUseOverscanMethod

        val improvedAppChangeDetection = findPreference<Preference>(PrefManager.IMPROVED_APP_CHANGE_DETECTION)
        improvedAppChangeDetection?.setOnPreferenceClickListener {
            Toast.makeText(
                    requireContext(),
                    if (requireActivity().hasUsage) R.string.improved_app_change_detection_already_granted else R.string.improved_app_change_detection_grant,
                    Toast.LENGTH_LONG
            ).show()

            try {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            } catch (e: Exception) {}
            true
        }
    }
}