package com.xda.nobar.fragments.settings

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import com.xda.nobar.R
import com.xda.nobar.activities.selectors.BlacklistSelectorActivity
import com.xda.nobar.prefs.SeekBarSwitchPreference
import com.xda.nobar.util.PrefManager
import com.xda.nobar.util.vibrator
import tk.zwander.seekbarpreference.SeekBarPreference

/**
 * Behavior settings
 */
class BehaviorFragment : BasePrefFragment() {
    companion object {
        const val BAR_BLACKLIST = "bar_blacklist"
        const val NAV_BLACKLIST = "nav_blacklist"
    }

    override val resId = R.xml.prefs_behavior

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        setListeners()
    }

    override fun onResume() {
        super.onResume()

        activity?.title = resources.getText(R.string.behavior)

        if (prefManager.overlayNav) {
            val dontMoveWithKb = findPreference<Preference>(PrefManager.STATIC_PILL)!!
            dontMoveWithKb.isEnabled = false
        }
    }

    private fun setListeners() {
        val barBL = findPreference<Preference>(BAR_BLACKLIST)!!
        val navBL = findPreference<Preference>(NAV_BLACKLIST)!!
        val customVibeStrength = findPreference<SwitchPreference>(PrefManager.CUSTOM_VIBRATION_STRENGTH)!!
        val vibeStrength = findPreference<SeekBarPreference>(PrefManager.VIBRATION_STRENGTH)!!

        val autoHide = findPreference<SeekBarSwitchPreference>(PrefManager.AUTO_HIDE_PILL)!!
        val autoFade = findPreference<SeekBarSwitchPreference>(PrefManager.FADE_AFTER_SPECIFIED_DELAY)!!

        val fullscreenHide = findPreference<SeekBarSwitchPreference>(PrefManager.HIDE_IN_FULLSCREEN)!!
        val fullscreenFade = findPreference<SeekBarSwitchPreference>(PrefManager.FADE_IN_FULLSCREEN_APPS)!!

        if (autoHide.isChecked) {
            autoFade.isEnabled = false
            autoFade.isChecked = false
        }
        if (autoFade.isChecked) {
            autoHide.isEnabled = false
            autoHide.isChecked = false
        }

        if (fullscreenHide.isChecked) {
            fullscreenFade.isEnabled = false
            fullscreenFade.isChecked = false
        }
        if (fullscreenFade.isChecked) {
            fullscreenHide.isEnabled = false
            fullscreenHide.isChecked = false
        }

        autoHide.setOnPreferenceChangeListener { _, newValue ->
            val new = newValue.toString().toBoolean()

            autoFade.isEnabled = !new
            autoFade.isChecked = if (new) false else autoFade.isChecked

            true
        }

        autoFade.setOnPreferenceChangeListener { _, newValue ->
            val new = newValue.toString().toBoolean()

            autoHide.isEnabled = !new
            autoHide.isChecked = if (new) false else autoHide.isChecked

            true
        }

        fullscreenHide.setOnPreferenceChangeListener { _, newValue ->
            val new = newValue.toString().toBoolean()

            fullscreenFade.isEnabled = !new
            fullscreenFade.isChecked = if (new) false else fullscreenFade.isChecked

            true
        }

        fullscreenFade.setOnPreferenceChangeListener { _, newValue ->
            val new = newValue.toString().toBoolean()

            fullscreenHide.isEnabled = !new
            fullscreenHide.isChecked = if (new) false else fullscreenHide.isChecked

            true
        }

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
    }
}