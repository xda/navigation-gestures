package com.xda.nobar.prefs

import android.content.Context
import android.util.AttributeSet
import androidx.preference.SwitchPreference
import com.xda.nobar.R
import java.util.*

class PixelDPSwitch(context: Context, attributeSet: AttributeSet) : SwitchPreference(context, attributeSet) {

    init {
        val array = context.theme.obtainStyledAttributes(attributeSet, R.styleable.PixelDPSwitch, 0, 0)
        val dim = array.getString(R.styleable.PixelDPSwitch_dimension_name)
                ?: context.resources.getString(R.string.dimension)

        title = String.format(
                Locale.getDefault(),
                context.resources.getString(R.string.use_pixels),
                dim
        )

        summary = String.format(
                Locale.getDefault(),
                context.resources.getString(R.string.use_pixels_desc),
                dim.toLowerCase()
        )
    }

    fun setDimensionType(dim: String) {
        title = String.format(
                Locale.getDefault(),
                context.resources.getString(R.string.use_pixels),
                dim
        )
        summary = String.format(
                Locale.getDefault(),
                context.resources.getString(R.string.use_pixels_desc),
                dim.toLowerCase()
        )
    }
}