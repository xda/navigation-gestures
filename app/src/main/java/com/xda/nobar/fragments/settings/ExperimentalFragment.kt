package com.xda.nobar.fragments.settings

import android.content.Intent
import com.xda.nobar.R
import com.xda.nobar.activities.selectors.BlacklistSelectorActivity

/**
 * Experimental, but mostly working settings
 */
class ExperimentalFragment : BasePrefFragment() {
    override val resId = R.xml.prefs_experimental

    override fun onResume() {
        super.onResume()

        activity?.title = resources.getText(R.string.experimental_prefs)

        setListeners()
    }

    private fun setListeners() {
        val winFix = findPreference("window_fix")
        winFix.setOnPreferenceClickListener {
            val blIntent = Intent(activity, BlacklistSelectorActivity::class.java)
            blIntent.putExtra(BlacklistSelectorActivity.EXTRA_WHICH, BlacklistSelectorActivity.FOR_WIN)
            startActivity(blIntent)

            true
        }
    }
}