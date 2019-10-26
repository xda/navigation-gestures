package com.xda.nobar.fragments.settings.gestures

import android.graphics.Color
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xda.nobar.R
import com.xda.nobar.util.PrefManager
import com.xda.nobar.util.dpAsPx
import com.xda.nobar.util.minPillHeightPx

class PillGestureFragment : GestureFragment() {
    companion object {
        const val SECTION_GESTURES = "section_gestures"
    }

    override val resId = R.xml.prefs_pill_gestures
    override val activityTitle by lazy { resources.getText(R.string.pill_gestures) }

    private val sectionedCategory by lazy { findPreference<PreferenceCategory>(SECTION_GESTURES) }

    private val swipeUp by lazy { findPreference<Preference>(actionHolder.actionUp) }
    private val swipeUpHold by lazy { findPreference<Preference>(actionHolder.actionUpHold) }
    private val sectionedPill by lazy { findPreference<SwitchPreference>(PrefManager.SECTIONED_PILL) }

    private val sectionedPillListener = Preference.OnPreferenceChangeListener { _, newValue ->
        val new = newValue.toString().toBoolean()

        updateSplitPill(new)

        if (new) {
            refreshListPrefs()

            updateSummaries()

            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.use_recommended_settings)
                .setView(R.layout.use_recommended_settings_dialog_message_view)
                .setPositiveButton(android.R.string.yes) { _, _ -> setSectionedSettings() }
                .setNegativeButton(R.string.no, null)
                .show()
        } else {
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.back_to_default)
                .setMessage(R.string.back_to_default_desc)
                .setPositiveButton(android.R.string.yes) { _, _ -> resetSectionedSettings() }
                .setNegativeButton(R.string.no, null)
                .show()

            refreshListPrefs()
        }

        true
    }

    override fun onResume() {
        super.onResume()

        sectionedPill?.onPreferenceChangeListener = sectionedPillListener

        sectionedCategory?.isVisible = sectionedPill?.isChecked == true
        swipeUp?.isVisible = sectionedPill?.isChecked == false
        swipeUpHold?.isVisible = sectionedPill?.isChecked == false
    }

    private fun setSectionedSettings() {
        preferenceManager.sharedPreferences.edit().apply {
            putBoolean(PrefManager.USE_PIXELS_WIDTH, false)
            putBoolean(PrefManager.USE_PIXELS_HEIGHT, true)
            putBoolean(PrefManager.USE_PIXELS_Y, false)
            putBoolean(PrefManager.LARGER_HITBOX, false)
            putBoolean(PrefManager.HIDE_IN_FULLSCREEN, false)
            putBoolean(PrefManager.STATIC_PILL, true)
            putBoolean(PrefManager.HIDE_PILL_ON_KEYBOARD, false)
            putBoolean(PrefManager.AUTO_HIDE_PILL, false)

            putInt(PrefManager.CUSTOM_WIDTH_PERCENT, 1000)
            putInt(PrefManager.CUSTOM_HEIGHT, activity!!.minPillHeightPx + activity!!.dpAsPx(7))
            putInt(PrefManager.CUSTOM_Y_PERCENT, 0)
            putInt(PrefManager.PILL_CORNER_RADIUS, 0)
            putInt(PrefManager.PILL_BG, Color.TRANSPARENT)
            putInt(PrefManager.PILL_FG, Color.TRANSPARENT)
            putInt(PrefManager.ANIM_DURATION, 0)
            putInt(PrefManager.HOLD_TIME, 500)
        }.apply()

        updateSummaries()
    }

    private fun resetSectionedSettings() {
        preferenceManager.sharedPreferences.edit().apply {
            remove(PrefManager.USE_PIXELS_WIDTH)
            remove(PrefManager.USE_PIXELS_HEIGHT)
            remove(PrefManager.USE_PIXELS_Y)
            remove(PrefManager.LARGER_HITBOX)
            remove(PrefManager.HIDE_IN_FULLSCREEN)
            remove(PrefManager.STATIC_PILL)
            remove(PrefManager.HIDE_PILL_ON_KEYBOARD)
            remove(PrefManager.AUTO_HIDE_PILL)

            remove(PrefManager.CUSTOM_WIDTH_PERCENT)
            remove(PrefManager.CUSTOM_HEIGHT)
            remove(PrefManager.CUSTOM_Y_PERCENT)
            remove(PrefManager.PILL_CORNER_RADIUS)
            remove(PrefManager.PILL_BG)
            remove(PrefManager.PILL_FG)
            remove(PrefManager.ANIM_DURATION)
            remove(PrefManager.HOLD_TIME)
        }.apply()

        updateSummaries()
    }

    private fun updateSplitPill(enabled: Boolean) {
        sectionedCategory?.isVisible = enabled
        swipeUp?.isVisible = !enabled
        swipeUpHold?.isVisible = !enabled
    }
}