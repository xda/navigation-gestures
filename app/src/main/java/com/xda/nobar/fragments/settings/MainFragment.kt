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
        const val BACKUP_RESTORE = "backup_and_restore"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.prefs_main)
    }

    override fun onResume() {
        super.onResume()

        activity?.title = resources.getText(R.string.settings)
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        val (frag, ret) = when (preference?.key) {
            GESTURES -> GestureFragment() to true
            APPEARANCE -> AppearanceFragment() to true
            BEHAVIOR -> BehaviorFragment() to true
            COMPATIBILITY -> CompatibilityFragment() to true
            EXPERIMENTAL -> ExperimentalFragment() to true
            BACKUP_RESTORE -> BackupRestoreFragment() to true
            else -> null to super.onPreferenceTreeClick(preference)
        }

        if (frag != null) {
            fragmentManager
                    ?.beginAnimatedTransaction()
                    ?.replace(R.id.content, frag, preference?.key)
                    ?.addToBackStack(preference?.key)
                    ?.commit()
        }

        return ret
    }
}