package com.xda.nobar.prefs

import android.content.Context
import android.graphics.Color
import android.preference.Preference
import android.util.AttributeSet
import android.view.View
import android.widget.TextView

/**
 * Simple preference that sets both the title and summary text color to red
 * Useful for warnings
 */
class RedTextWarningPref : Preference {
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context) : super(context)

    init {
        isSelectable = false
        isEnabled = false
    }

    override fun onBindView(view: View) {
        super.onBindView(view)

        val titleView = view.findViewById<TextView>(com.android.internal.R.id.title)
        val summaryView = view.findViewById<TextView>(com.android.internal.R.id.summary)

        titleView.setTextColor(Color.RED)
        summaryView.setTextColor(Color.RED)
    }
}