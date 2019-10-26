package com.xda.nobar.fragments.settings.appearance

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.preference.Preference
import com.jaredrummler.android.colorpicker.ColorPreferenceCompat
import com.xda.nobar.R
import com.xda.nobar.fragments.settings.BasePrefFragment
import com.xda.nobar.prefs.PixelDPSwitch
import com.xda.nobar.util.*
import tk.zwander.seekbarpreference.SeekBarPreference

class PillAppearanceFragment : BasePrefFragment() {
    override val resId = R.xml.prefs_pill_appearance
    override val activityTitle by lazy { resources.getText(R.string.pill_appearance) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        setListeners()
        setup()
    }

    @SuppressLint("RestrictedApi")
    private fun setup() {
        val pixelsW = findPreference<PixelDPSwitch>(PrefManager.USE_PIXELS_WIDTH)!!
        pixelsW.isChecked = prefManager.usePixelsW

        val pixelsH = findPreference<PixelDPSwitch>(PrefManager.USE_PIXELS_HEIGHT)!!
        pixelsH.isChecked = prefManager.usePixelsH

        val pixelsX = findPreference<PixelDPSwitch>(PrefManager.USE_PIXELS_X)!!
        pixelsX.isChecked = prefManager.usePixelsX

        val pixelsY = findPreference<PixelDPSwitch>(PrefManager.USE_PIXELS_Y)!!
        pixelsY.isChecked = prefManager.usePixelsY

        val widthPercent = findPreference<SeekBarPreference>(PrefManager.CUSTOM_WIDTH_PERCENT)!!
        val heightPercent = findPreference<SeekBarPreference>(PrefManager.CUSTOM_HEIGHT_PERCENT)!!
        val xPercent = findPreference<SeekBarPreference>(PrefManager.CUSTOM_X_PERCENT)!!
        val yPercent = findPreference<SeekBarPreference>(PrefManager.CUSTOM_Y_PERCENT)!!

        val widthPixels = findPreference<SeekBarPreference>(PrefManager.CUSTOM_WIDTH)!!
        val heightPixels = findPreference<SeekBarPreference>(PrefManager.CUSTOM_HEIGHT)!!
        val xPixels = findPreference<SeekBarPreference>(PrefManager.CUSTOM_X)!!
        val yPixels = findPreference<SeekBarPreference>(PrefManager.CUSTOM_Y)!!

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

        val dividerColor = findPreference<ColorPreferenceCompat>(PrefManager.PILL_DIVIDER_COLOR)!!
        dividerColor.isVisible = prefManager.sectionedPill
    }

    private fun setListeners() {
        val pillColor = findPreference<ColorPreferenceCompat>(PrefManager.PILL_BG)!!
        val pillBorderColor = findPreference<ColorPreferenceCompat>(PrefManager.PILL_FG)!!

        pillColor.setDefaultValue(activity!!.defaultPillBGColor)
        pillBorderColor.setDefaultValue(activity!!.defaultPillFGColor)

        pillColor.saveValue(prefManager.pillBGColor)
        pillBorderColor.saveValue(prefManager.pillFGColor)
    }
}