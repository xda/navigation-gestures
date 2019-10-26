package com.xda.nobar.fragments.settings

import android.content.Intent
import android.content.SharedPreferences
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import com.xda.nobar.R
import com.xda.nobar.activities.selectors.BlacklistSelectorActivity
import com.xda.nobar.util.PrefManager

/**
 * Compatibility settings
 */
class CompatibilityFragment : BasePrefFragment() {
    companion object {
        const val IMMERSIVE_BLACKLIST = "immersive_blacklist"
        const val NAV_HIDING = "nav_hiding"
    }

    override val resId = R.xml.prefs_compatibility
    override val activityTitle by lazy { resources.getText(R.string.compatibility) }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            PrefManager.ROT270_FIX -> {
                updateEnabledStates()
            }
            PrefManager.ROT180_FIX -> {
                updateEnabledStates()
            }
            PrefManager.TABLET_MODE -> {
               updateEnabledStates()
            }
            PrefManager.ORIG_NAV_IN_IMMERSIVE -> {
                val enabled = sharedPreferences.getBoolean(key, false)
                val immNav = findPreference<SwitchPreference>(PrefManager.USE_IMMERSIVE_MODE_WHEN_NAV_HIDDEN)!!

                immNav.isEnabled = !enabled
                immNav.isChecked = if (enabled) false else immNav.isChecked
            }
            PrefManager.USE_IMMERSIVE_MODE_WHEN_NAV_HIDDEN -> {
                val enabled = sharedPreferences.getBoolean(key, false)
                val origNav = findPreference<SwitchPreference>(PrefManager.ORIG_NAV_IN_IMMERSIVE)!!

                origNav.isEnabled = !enabled
                origNav.isChecked = if (enabled) false else origNav.isChecked
            }
        }
    }

    override fun onResume() {
        super.onResume()

        updateEnabledStates()
        setUpImmersiveListeners()
    }

    private fun updateEnabledStates() {
        val rot180Fix = findPreference<SwitchPreference>(PrefManager.ROT180_FIX)!!
        val rot270Fix = findPreference<SwitchPreference>(PrefManager.ROT270_FIX)!!
        val tabletMode = findPreference<SwitchPreference>(PrefManager.TABLET_MODE)!!

        (!rot270Fix.isChecked && !rot180Fix.isChecked).apply {
            tabletMode.isEnabled = this

            if (!this) tabletMode.isChecked = false
        }

        (!tabletMode.isChecked).apply {
            rot270Fix.isEnabled = this
            rot180Fix.isEnabled = this

            if (!this) {
                rot270Fix.isChecked = false
                rot180Fix.isChecked = false
            }
        }
    }

    private fun setUpImmersiveListeners() {
        val origNav = findPreference<SwitchPreference>(PrefManager.ORIG_NAV_IN_IMMERSIVE)!!
        val immNav = findPreference<SwitchPreference>(PrefManager.USE_IMMERSIVE_MODE_WHEN_NAV_HIDDEN)!!
        val immBL = findPreference<Preference>(IMMERSIVE_BLACKLIST)!!

        if (origNav.isChecked) {
            immNav.isChecked = false
            immNav.isEnabled = false
        }

        if (immNav.isChecked) {
            origNav.isChecked = false
            origNav.isEnabled = false
        }

        immBL.setOnPreferenceClickListener {
            val selector = Intent(activity, BlacklistSelectorActivity::class.java)
            selector.putExtra(BlacklistSelectorActivity.EXTRA_WHICH, BlacklistSelectorActivity.FOR_IMM)
            startActivity(selector)
            true
        }
    }
}