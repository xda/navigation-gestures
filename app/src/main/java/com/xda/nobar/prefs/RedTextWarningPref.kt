package com.xda.nobar.prefs

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

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

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val titleView = holder.itemView.findViewById<TextView>(context.resources.getIdentifier("title", "id", "android"))
        val summaryView = holder.itemView.findViewById<TextView>(context.resources.getIdentifier("summary", "id", "android"))

        titleView.setTextColor(Color.RED)
        summaryView.setTextColor(Color.RED)
    }
}