package com.xda.nobar.fragments.settings.search

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.xda.nobar.R
import com.xda.nobar.activities.ui.SettingsSearchActivity
import com.xda.nobar.data.SettingsIndex
import com.xda.nobar.util.navigateTo

class SearchFragment : PreferenceFragmentCompat(), SettingsSearchActivity.ListUpdateListener {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.prefs_search, rootKey)

        (requireActivity() as SettingsSearchActivity).addListUpdateListener(this)
    }

    override fun onListUpdate(newList: List<SettingsIndex.SettingsItem>) {
        preferenceScreen.removeAll()

        newList.forEach { item ->
            val newPref = Preference(requireActivity())
            newPref.title = item.preference.title
            newPref.summary = item.preference.summary
            newPref.key = item.preference.key
            newPref.icon = item.preference.icon

            newPref.setOnPreferenceClickListener {
                navigateTo(item.pageAction, it.key)
                true
            }

            preferenceScreen.addPreference(newPref)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        (requireActivity() as SettingsSearchActivity).removeListUpdateListener(this)
    }
}