package com.xda.nobar.prefs

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder
import com.xda.nobar.R

class CollapsiblePreferenceCategory(context: Context, attributeSet: AttributeSet) : PreferenceCategory(context, attributeSet) {

    private val expandedVisibilities = HashMap<String, Boolean>()

    var expanded = false
        set(value) {
            if (!value) {
                setExpandedVisibilities()
                hideAllPrefs()
            } else {
                resetVisibilities()
            }

            field = value
            notifyChanged()
        }

    init {
        layoutResource = R.layout.pref_cat_collapsible
        setIcon(R.drawable.arrow_up)

        val array = context.theme.obtainStyledAttributes(attributeSet, R.styleable.CollapsiblePreferenceCategory, 0, 0)
        expanded = array.getBoolean(R.styleable.CollapsiblePreferenceCategory_default_expanded, expanded)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val icon = holder.itemView.findViewById<ImageView>(android.R.id.icon)
        icon.animate()
                .scaleY(if (expanded) 1f else -1f)
        holder.itemView.setOnClickListener {
            expanded = !expanded
        }
    }

    override fun onAttached() {
        super.onAttached()

        expanded = expanded
    }

    private fun setExpandedVisibilities() {
        expandedVisibilities.clear()

        for (i in 0 until preferenceCount) {
            val pref = getPreference(i)
            expandedVisibilities[pref.key] = pref.isVisible
        }
    }

    private fun hideAllPrefs() {
        for (i in 0 until preferenceCount) {
            getPreference(i).isVisible = false
        }
    }

    private fun resetVisibilities() {
        expandedVisibilities.keys.forEach {
            val vis = expandedVisibilities[it]

            findPreference(it)?.isVisible = vis!!
        }
    }
}