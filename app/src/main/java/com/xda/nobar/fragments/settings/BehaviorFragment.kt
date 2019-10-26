package com.xda.nobar.fragments.settings

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import com.xda.nobar.R
import com.xda.nobar.activities.selectors.AppLaunchSelectActivity
import com.xda.nobar.activities.selectors.BlacklistSelectorActivity
import com.xda.nobar.util.PrefManager
import com.xda.nobar.util.prefManager
import com.xda.nobar.util.vibrator
import tk.zwander.seekbarpreference.SeekBarPreference

/**
 * Behavior settings
 */
class BehaviorFragment : BasePrefFragment() {
    companion object {
        const val BAR_BLACKLIST = "bar_blacklist"
        const val NAV_BLACKLIST = "nav_blacklist"

        const val REQ_HIDE_DIALOG = 104
    }

    override val resId = R.xml.prefs_behavior
    override val activityTitle by lazy { resources.getText(R.string.behavior) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        setListeners()
    }

    override fun onResume() {
        super.onResume()

        if (prefManager.overlayNav) {
            val staticPill = findPreference<Preference>(PrefManager.STATIC_PILL)!!
            staticPill.isEnabled = false

            val showNavWithKb = findPreference<Preference>(PrefManager.SHOW_NAV_WITH_KEYBOARD)!!
            showNavWithKb.isEnabled = false
        }
    }

    private fun setListeners() {
        val barBL = findPreference<Preference>(BAR_BLACKLIST)!!
        val navBL = findPreference<Preference>(NAV_BLACKLIST)!!
        val customVibeStrength = findPreference<SwitchPreference>(PrefManager.CUSTOM_VIBRATION_STRENGTH)!!
        val vibeStrength = findPreference<SeekBarPreference>(PrefManager.VIBRATION_STRENGTH)!!

        val listener = Preference.OnPreferenceClickListener {
            val which = when (it.key) {
                barBL.key -> BlacklistSelectorActivity.FOR_BAR
                navBL.key -> BlacklistSelectorActivity.FOR_NAV
                else -> return@OnPreferenceClickListener false
            }

            val blIntent = Intent(activity, BlacklistSelectorActivity::class.java)
            blIntent.putExtra(BlacklistSelectorActivity.EXTRA_WHICH, which)
            startActivity(blIntent)

            true
        }

        barBL.onPreferenceClickListener = listener
        navBL.onPreferenceClickListener = listener

        customVibeStrength.isVisible = Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1
                && context!!.vibrator.hasAmplitudeControl()
        customVibeStrength.setOnPreferenceChangeListener { _, newValue ->
            vibeStrength.isVisible = newValue.toString().toBoolean()
            true
        }

        vibeStrength.isVisible = customVibeStrength.run { isVisible && isChecked }

        val hideDialogApps = findPreference<Preference>(PrefManager.HIDE_DIALOG_APPS)

        hideDialogApps?.setOnPreferenceClickListener {
            startActivityForResult(
                    Intent(requireActivity(), AppLaunchSelectActivity::class.java).apply {
                        putExtra(AppLaunchSelectActivity.EXTRA_INCLUDE_ALL_APPS, true)
                        putExtra(AppLaunchSelectActivity.EXTRA_TITLE, resources.getString(R.string.hide_dialog_apps))
                        putExtra(AppLaunchSelectActivity.EXTRA_SHOW_UP_AS_CHECK, true)
                        putExtra(AppLaunchSelectActivity.EXTRA_USE_SINGLE_SELECT, false)
                        putExtra(AppLaunchSelectActivity.EXTRA_SELECTED_APPS,
                                ArrayList<String>().apply { requireActivity().prefManager.loadHideDialogApps(this) })
                    },
                    REQ_HIDE_DIALOG)

            true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_HIDE_DIALOG && resultCode == Activity.RESULT_OK) {
            requireActivity()
                    .prefManager
                    .saveHideDialogApps(data!!.getStringArrayListExtra(AppLaunchSelectActivity.EXTRA_SELECTED_APPS))
        }
    }
}