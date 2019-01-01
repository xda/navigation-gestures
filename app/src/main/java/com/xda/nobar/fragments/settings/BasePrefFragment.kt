package com.xda.nobar.fragments.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.preference.PreferenceFragmentCompat
import com.xda.nobar.util.prefManager

abstract class BasePrefFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    internal val prefManager by lazy { activity!!.prefManager }

    abstract val resId: Int

    @CallSuper
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(resId, rootKey)
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {}

    override fun onDestroy() {
        super.onDestroy()

        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }
}