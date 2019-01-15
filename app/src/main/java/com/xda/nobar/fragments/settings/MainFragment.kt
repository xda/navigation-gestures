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
                "gestures" -> GestureFragment()
                "appearance" -> AppearanceFragment()
                "behavior" -> BehaviorFragment()
                "compatibility" -> CompatibilityFragment()
                "experimental" -> ExperimentalFragment()
                else -> null
            }

            if (whichFrag != null) fragmentManager
                    ?.beginAnimatedTransaction()
                    ?.replace(R.id.content, whichFrag, it.key)
                    ?.addToBackStack(it.key)
                    ?.commit()
            true
        }

        findPreference<Preference>("gestures").onPreferenceClickListener = listener
        findPreference<Preference>("appearance").onPreferenceClickListener = listener
        findPreference<Preference>("behavior").onPreferenceClickListener = listener
        findPreference<Preference>("compatibility").onPreferenceClickListener = listener
        findPreference<Preference>("experimental").onPreferenceClickListener = listener
    }
}