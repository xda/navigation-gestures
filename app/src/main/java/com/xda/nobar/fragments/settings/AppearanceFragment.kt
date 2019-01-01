package com.xda.nobar.fragments.settings

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.preference.Preference
import com.jaredrummler.android.colorpicker.ColorPreferenceCompat
import com.xda.nobar.R
import com.xda.nobar.prefs.PixelDPSwitch
import com.xda.nobar.util.*
import tk.zwander.seekbarpreference.SeekBarPreference

/**
 * Appearance settings
 */
class AppearanceFragment : BasePrefFragment() {
    override val resId = R.xml.prefs_appearance

    override fun onResume() {
        super.onResume()

        activity?.title = resources.getText(R.string.appearance)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        setListeners()
        setup()
    }

    private fun setListeners() {
        val pillColor = findPreference(PrefManager.PILL_BG) as ColorPreferenceCompat
        val pillBorderColor = findPreference(PrefManager.PILL_FG) as ColorPreferenceCompat

        pillColor.setDefaultValue(activity!!.defaultPillBGColor)
        pillBorderColor.setDefaultValue(activity!!.defaultPillFGColor)

        pillColor.saveValue(prefManager.pillBGColor)
        pillBorderColor.saveValue(prefManager.pillFGColor)
    }

    @SuppressLint("RestrictedApi")
    private fun setup() {
        val pixelsW = findPreference("use_pixels_width") as PixelDPSwitch
        val pixelsH = findPreference("use_pixels_height") as PixelDPSwitch
        val pixelsX = findPreference("use_pixels_x") as PixelDPSwitch
        val pixelsY = findPreference("use_pixels_y") as PixelDPSwitch

        val widthPercent = findPreference("custom_width_percent") as SeekBarPreference
        val heightPercent = findPreference("custom_height_percent") as SeekBarPreference
        val xPercent = findPreference("custom_x_percent") as SeekBarPreference
        val yPercent = findPreference("custom_y_percent") as SeekBarPreference

        val widthPixels = findPreference("custom_width") as SeekBarPreference
        val heightPixels = findPreference("custom_height") as SeekBarPreference
        val xPixels = findPreference("custom_x") as SeekBarPreference
        val yPixels = findPreference("custom_y") as SeekBarPreference

        heightPixels.minValue = activity!!.minPillHeightPx
        widthPixels.minValue = activity!!.minPillWidthPx
        xPixels.minValue = activity!!.minPillXPx
        yPixels.minValue = activity!!.minPillYPx

        heightPixels.maxValue = activity!!.maxPillHeightPx
        widthPixels.maxValue = activity!!.maxPillWidthPx
        xPixels.maxValue = activity!!.maxPillXPx
        yPixels.maxValue = activity!!.maxPillYPx

        yPercent.setDefaultValue(prefManager.defaultYPercentUnscaled)
        yPixels.setDefaultValue(prefManager.defaultY)
        widthPixels.setDefaultValue(activity!!.defPillWidthPx)
        heightPixels.setDefaultValue(activity!!.defPillHeightPx)

        val listener = Preference.OnPreferenceChangeListener { pref, newValue ->
            val new = newValue.toString().toBoolean()

            when (pref) {
                pixelsW -> {
                    widthPixels.isVisible = new
                    widthPercent.isVisible = !new
                }

                pixelsH -> {
                    heightPixels.isVisible = new
                    heightPercent.isVisible = !new
                }

                pixelsX -> {
                    xPixels.isVisible = new
                    xPercent.isVisible = !new
                }

                pixelsY -> {
                    yPixels.isVisible = new
                    yPercent.isVisible = !new
                }
            }

            true
        }

        listener.onPreferenceChange(pixelsW, pixelsW.isChecked)
        listener.onPreferenceChange(pixelsH, pixelsH.isChecked)
        listener.onPreferenceChange(pixelsX, pixelsX.isChecked)
        listener.onPreferenceChange(pixelsY, pixelsY.isChecked)

        pixelsW.onPreferenceChangeListener = listener
        pixelsH.onPreferenceChangeListener = listener
        pixelsX.onPreferenceChangeListener = listener
        pixelsY.onPreferenceChangeListener = listener
    }
}