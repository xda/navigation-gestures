package com.xda.nobar.fragments.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
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
    }

    override val resId = R.xml.prefs_compatibility

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            PrefManager.ROT270_FIX -> {
                val enabled = sharedPreferences.getBoolean(key, false)
                val tabletMode = findPreference<SwitchPreference>(PrefManager.TABLET_MODE)

                tabletMode.isEnabled = !enabled
                tabletMode.isChecked = if (enabled) false else tabletMode.isChecked
            }
            PrefManager.TABLET_MODE -> {
                val enabled = sharedPreferences.getBoolean(key, false)
                val rot270Fix = findPreference<SwitchPreference>(PrefManager.ROT270_FIX)

                rot270Fix.isEnabled = !enabled
                rot270Fix.isChecked = if (enabled) false else rot270Fix.isChecked
            }
            PrefManager.ORIG_NAV_IN_IMMERSIVE -> {
                val enabled = sharedPreferences.getBoolean(key, false)
                val immNav = findPreference<SwitchPreference>(PrefManager.USE_IMMERSIVE_MODE_WHEN_NAV_HIDDEN)

                immNav.isEnabled = !enabled
                immNav.isChecked = if (enabled) false else immNav.isChecked
            }
            PrefManager.USE_IMMERSIVE_MODE_WHEN_NAV_HIDDEN -> {
                val enabled = sharedPreferences.getBoolean(key, false)
                val origNav = findPreference<SwitchPreference>(PrefManager.ORIG_NAV_IN_IMMERSIVE)

                origNav.isEnabled = !enabled
                origNav.isChecked = if (enabled) false else origNav.isChecked
            }
        }
    }

    override fun onResume() {
        super.onResume()

        activity?.title = resources.getText(R.string.compatibility)

        setUpRotListeners()
        setUpImmersiveListeners()
    }

    override fun onDestroy() {
        super.onDestroy()

        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun setUpRotListeners() {
        val rot180Fix = findPreference<SwitchPreference>(PrefManager.ROT180_FIX)
        val rot270Fix = findPreference<SwitchPreference>(PrefManager.ROT270_FIX)
        val tabletMode = findPreference<SwitchPreference>(PrefManager.TABLET_MODE)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            if (rot270Fix.isChecked) {
                tabletMode.isChecked = false
                tabletMode.isEnabled = false
            }

            if (tabletMode.isChecked) {
                rot270Fix.isChecked = false
                rot270Fix.isEnabled = false
            }
        } else {
            rot180Fix.isEnabled = false
            rot270Fix.isEnabled = false
            tabletMode.isEnabled = false
        }
    }

    private fun setUpImmersiveListeners() {
        val origNav = findPreference<SwitchPreference>(PrefManager.ORIG_NAV_IN_IMMERSIVE)
        val immNav = findPreference<SwitchPreference>(PrefManager.USE_IMMERSIVE_MODE_WHEN_NAV_HIDDEN)
        val immBL = findPreference<Preference>(IMMERSIVE_BLACKLIST)

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