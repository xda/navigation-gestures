package com.xda.nobar.prefs

import android.content.Context
import android.util.AttributeSet
import androidx.preference.SwitchPreference
import com.xda.nobar.R
import java.util.*

class PixelDPSwitch(context: Context, attributeSet: AttributeSet) : SwitchPreference(context, attributeSet) {
    var dimensionType: String
        get() = throw IllegalArgumentException("not a gettable value")
        set(value) {
            title = String.format(
                    Locale.getDefault(),
                    context.resources.getString(R.string.use_pixels),
                    value.capitalize()
            )
            summary = String.format(
                    Locale.getDefault(),
                    context.resources.getString(R.string.use_pixels_desc),
                    value.toLowerCase()
            )
        }

    init {
        val array = context.theme.obtainStyledAttributes(attributeSet, R.styleable.PixelDPSwitch, 0, 0)
        dimensionType = array.getString(R.styleable.PixelDPSwitch_dimension_name)
                ?: context.resources.getString(R.string.dimension)
    }
}