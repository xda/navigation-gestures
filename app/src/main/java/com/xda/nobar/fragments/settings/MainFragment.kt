package com.xda.nobar.fragments.settings

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.xda.nobar.R
import com.xda.nobar.util.beginAnimatedTransaction

/**
 * Main settings page
 */
class MainFragment : PreferenceFragmentCompat() {
    companion object {
        const val GESTURES = "gestures"
        const val APPEARANCE = "appearance"
        const val BEHAVIOR = "behavior"
        const val COMPATIBILITY = "compatibility"
        const val EXPERIMENTAL = "experimental"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.prefs_main)
    }

    override fun onResume() {
        super.onResume()

        activity?.title = resources.getText(R.string.settings)

        setListeners()
    }

    private fun setListeners() {
        val listener = Preference.OnPreferenceClickListener {
            val whichFrag = when (it.key) {
                GESTURES -> GestureFragment()
                APPEARANCE -> AppearanceFragment()
                BEHAVIOR -> BehaviorFragment()
                COMPATIBILITY -> CompatibilityFragment()
                EXPERIMENTAL -> ExperimentalFragment()
                else -> null
            }

            if (whichFrag != null) fragmentManager
                    ?.beginAnimatedTransaction()
                    ?.replace(R.id.content, whichFrag, it.key)
                    ?.addToBackStack(it.key)
                    ?.commit()
            true
        }

        findPreference<Preference>(GESTURES)?.onPreferenceClickListener = listener
        findPreference<Preference>(APPEARANCE)?.onPreferenceClickListener = listener
        findPreference<Preference>(BEHAVIOR)?.onPreferenceClickListener = listener
        findPreference<Preference>(COMPATIBILITY)?.onPreferenceClickListener = listener
        findPreference<Preference>(EXPERIMENTAL)?.onPreferenceClickListener = listener
    }
}