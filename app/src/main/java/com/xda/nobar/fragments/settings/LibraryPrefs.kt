package com.xda.nobar.fragments.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.xda.nobar.R

/**
 * Fragment for LibraryActivity
 */
class LibraryPrefs : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.prefs_lib)
    }

    override fun onResume() {
        super.onResume()

        activity?.title = resources.getString(R.string.libraries)
    }
}