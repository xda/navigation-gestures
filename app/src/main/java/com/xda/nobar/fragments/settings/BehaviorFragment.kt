package com.xda.nobar.fragments.settings

import android.content.Intent
import androidx.preference.Preference
import com.xda.nobar.R
import com.xda.nobar.activities.selectors.BlacklistSelectorActivity

/**
 * Behavior settings
 */
class BehaviorFragment : BasePrefFragment() {
    override val resId = R.xml.prefs_behavior

    override fun onResume() {
        super.onResume()

        activity?.title = resources.getText(R.string.behavior)

        setBlacklistListeners()
    }

    private fun setBlacklistListeners() {
        val barBL = findPreference<Preference>("bar_blacklist")
        val navBL = findPreference<Preference>("nav_blacklist")

        val listener = Preference.OnPreferenceClickListener {
            val which = when (it.key) {
                barBL.key -> BlacklistSelectorActivity.FOR_BAR
                navBL.key -> BlacklistSelectorActivity.FOR_NAV
                else -> return@OnPreferenceClickListener false
            }

            val blIntent = Intent(activity, BlacklistSelectorActivity::class.java)
            blIntent.putExtra(BlacklistSelectorActivity.EXTRA_WHICH, which)
            startActivity(blIntent)

            true
        }

        barBL.onPreferenceClickListener = listener
        navBL.onPreferenceClickListener = listener
    }
}