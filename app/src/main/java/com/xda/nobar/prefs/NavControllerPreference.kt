package com.xda.nobar.prefs

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import com.xda.nobar.R

class NavControllerPreference(context: Context, attrs: AttributeSet) :
    Preference(context, attrs) {

    var action = 0

    init {
        val array = context.theme.obtainStyledAttributes(attrs, R.styleable.NavControllerPreference, 0, 0)

        action = array.getResourceId(R.styleable.NavControllerPreference_preference_action, 0)

        array.recycle()
    }
}