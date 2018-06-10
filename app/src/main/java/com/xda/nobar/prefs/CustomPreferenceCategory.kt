package com.xda.nobar.prefs

import android.content.Context
import android.preference.PreferenceGroup
import android.util.AttributeSet
import com.xda.nobar.R

class CustomPreferenceCategory : PreferenceGroup {
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    init {
        layoutResource = R.layout.zero_height_pref
    }
}