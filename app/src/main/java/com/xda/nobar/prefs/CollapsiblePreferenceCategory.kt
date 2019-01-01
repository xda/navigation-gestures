package com.xda.nobar.prefs

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.widget.ImageView
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceViewHolder
import com.xda.nobar.R

class CollapsiblePreferenceCategory(context: Context, attributeSet: AttributeSet) : PreferenceCategory(context, attributeSet) {
    var expanded = false
        set(value) {
            field = value

            if (!value) {
                generateSummary()
                super.removeAll()
            } else {
                summary = null
                wrappedPrefs.forEach {
                    super.addPreference(it)
                }
            }
        }

    private val wrappedPrefs = ArrayList<Preference>()

    init {
        layoutResource = R.layout.pref_cat_collapsible
        setIcon(R.drawable.arrow_up)

        val array = context.theme.obtainStyledAttributes(attributeSet, R.styleable.CollapsiblePreferenceCategory, 0, 0)
        expanded = array.getBoolean(R.styleable.CollapsiblePreferenceCategory_default_expanded, expanded)
    }

    override fun onAttached() {
        super.onAttached()

        expanded = expanded
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

    override fun addPreference(preference: Preference): Boolean {
        wrappedPrefs.add(preference)
        return if (expanded) super.addPreference(preference)
        else false
    }

    override fun removePreference(preference: Preference): Boolean {
        wrappedPrefs.remove(preference)
        return if (expanded) super.removePreference(preference)
        else false
    }

    override fun getPreference(index: Int): Preference {
        return wrappedPrefs[index]
    }

    override fun findPreference(key: CharSequence): Preference? {
        wrappedPrefs.forEach {
            if (it.key == key) return it
            else if (it is PreferenceGroup) {
                val pref = it.findPreference(key)
                if (pref != null) return pref
            }
        }

        return null
    }

    override fun getPreferenceCount(): Int {
        return if (expanded) super.getPreferenceCount() else 0
    }

    override fun removeAll() {
        wrappedPrefs.clear()
        super.removeAll()
    }

    private fun generateSummary() {
        val children = wrappedPrefs.map { it.title }

        summary = TextUtils.join(", ", children)
    }
}