package com.xda.nobar.adapters.prefs

import android.annotation.SuppressLint
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceGroupAdapter

@SuppressLint("RestrictedApi")
class CollapsiblePreferenceGroupAdapter(group: PreferenceGroup) : PreferenceGroupAdapter(group) {
    override fun onPreferenceVisibilityChange(preference: Preference) {
        onPreferenceHierarchyChange(preference)
    }
}