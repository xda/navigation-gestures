package com.xda.nobar.fragments.settings

import androidx.preference.Preference
import com.xda.nobar.R
import com.xda.nobar.util.navigateTo

/**
 * Main settings page
 */
class MainFragment : BasePrefFragment() {
    companion object {
        const val GESTURES = "gestures"
        const val APPEARANCE = "appearance"
        const val BEHAVIOR = "behavior"
        const val COMPATIBILITY = "compatibility"
        const val EXPERIMENTAL = "experimental"
        const val BACKUP_RESTORE = "backup_and_restore"
    }

    override val resId = R.xml.prefs_main

    override fun onResume() {
        super.onResume()

        activity?.title = resources.getText(R.string.settings)
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        val (action, ret) = when (preference?.key) {
            GESTURES -> R.id.action_mainFragment_to_gestureFragment to true
            APPEARANCE -> R.id.action_mainFragment_to_appearanceFragment to true
            BEHAVIOR -> R.id.action_mainFragment_to_behaviorFragment to true
            COMPATIBILITY -> R.id.action_mainFragment_to_compatibilityFragment to true
            EXPERIMENTAL -> R.id.action_mainFragment_to_experimentalFragment to true
            BACKUP_RESTORE -> R.id.action_mainFragment_to_backupRestoreFragment to true
            else -> null to super.onPreferenceTreeClick(preference)
        }

        if (action != null) {
            navigateTo(action)
        }

        return ret
    }
}