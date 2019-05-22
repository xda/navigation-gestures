package com.xda.nobar.prefs

import android.content.Context
import android.content.Intent
import android.content.res.TypedArray
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import androidx.preference.Preference
import com.xda.nobar.R
import com.xda.nobar.activities.selectors.ActionSelectorActivity
import com.xda.nobar.adapters.info.ActionInfo
import com.xda.nobar.util.helpers.bar.ActionHolder
import kotlinx.android.parcel.Parcelize
import java.util.*
import kotlin.collections.ArrayList

/**
 * Android's built-in ListPreference has no option to make sections for the items
 * This class is a custom ListPreference that has that option
 *
 * The following custom styleable options are available:
 *     - section_names:
 *         - a String[] representing the titles of each section
 *     - section_data_names
 *         - an array of String[]s representing the titles of the items for each section
 *         - the top-level array holds each String[] of titles
 *         - each String[] corresponds to a section_names index
 *     - section_data_values
 *         - Similar to above, just with the values/keys of each item
 */
class SectionableListPreference(context: Context, attributeSet: AttributeSet) : Preference(context, attributeSet) {
    var defaultValue: String? = null

    private val sections = ArrayList<Section>()

    init {
        val array = context.theme.obtainStyledAttributes(attributeSet, R.styleable.SectionableListPreference, 0, 0)

        val sectionNames = ArrayList<String>()
        val sectionDataNames = ArrayList<ArrayList<String>>()
        val sectionDataValues = ArrayList<ArrayList<String>>()

        for (i in 0 until array.indexCount) {
            when (val attr = array.getIndex(i)) {
                R.styleable.SectionableListPreference_section_names -> {
                    array.getTextArray(attr).forEach {
                        sectionNames.add(it.toString())
                    }
                }

                R.styleable.SectionableListPreference_section_data_names -> {
                    val namesArray = context.resources.obtainTypedArray(array.getResourceId(attr, 0))
                    for (j in 0 until namesArray.length()) {
                        val id = namesArray.getResourceId(j, 0)
                        if (id > 0) {
                            sectionDataNames.add(ArrayList(context.resources.getStringArray(id).toList()))
                        }
                    }
                    namesArray.recycle()
                }

                R.styleable.SectionableListPreference_section_data_values -> {
                    val valuesArray = context.resources.obtainTypedArray(array.getResourceId(attr, 0))
                    for (j in 0 until valuesArray.length()) {
                        val id = valuesArray.getResourceId(j, 0)
                        if (id > 0) {
                            sectionDataValues.add(ArrayList(context.resources.getStringArray(id).toList()))
                        }
                    }
                    valuesArray.recycle()
                }
            }
        }

        for (i in 0 until sectionNames.size) {
            val name = sectionNames[i]
            val entryNames = sectionDataNames[i]
            val entryValues = sectionDataValues[i]

            sections.add(Section(name, name.toLowerCase(Locale.getDefault()), entryNames, entryValues))
        }
    }

    fun saveValue(value: String?) {
        saveValueWithoutListener(value)
        callChangeListener(value)
    }

    fun saveValueWithoutListener(value: String?) {
        persistString(value)
        notifyChanged()
        updateSummary(value)
    }

    fun updateSummary(value: String?) {
        sections.forEach {
            val index = it.entryValues.indexOf(value)
            if (index != -1) {
                summary = it.entryNames[index]
            }
        }
    }

    fun getSavedValue(): String {
        return getPersistedString(defaultValue.toString())
    }

    fun removeSection(index: Int) {
        sections.removeAt(index)
    }

    fun removeItemByValue(value: String) {
        sections.forEach {
            val index = it.entryValues.indexOf(value)
            if (index != -1) {
                it.entryValues.removeAt(index)
                it.entryNames.removeAt(index)
            }
        }
    }

    fun removeItemsByValue(values: Array<String>) {
        values.forEach { removeItemByValue(it) }
    }

    fun removeItemByName(name: String) {
        sections.forEach {
            val index = it.entryNames.indexOf(name)
            if (index != -1) {
                it.entryNames.removeAt(index)
                it.entryValues.removeAt(index)
            }
        }
    }

    fun removeItemsByName(names: Array<String>) {
        names.forEach { removeItemByName(it) }
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): String {
        val def = a.getString(index) ?: ActionHolder.getInstance(context).typeNoAction.toString()
        defaultValue = def
        return def
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        saveValue(
                if (isPersistent) try {
                    getPersistedString(defaultValue.toString())
                } catch (e: ClassCastException) {
                    getPersistedInt(Int.MIN_VALUE).toString()
                }
                else defaultValue.toString()
        )
    }

    override fun onClick() {
        val callback = object : ActionSelectorActivity.IActionSelectedCallback() {
            override fun onActionInfoSelected(info: ActionInfo) {
                saveValue(info.res?.toString())
            }
        }

        val selectorIntent = Intent(context, ActionSelectorActivity::class.java)
        val bundle = Bundle()

        bundle.putParcelableArrayList(ActionSelectorActivity.EXTRA_SECTIONS, sections)
        bundle.putBinder(ActionSelectorActivity.EXTRA_CALLBACK, callback)
        selectorIntent.putExtra(ActionSelectorActivity.EXTRA_BUNDLE, bundle)

        context.startActivity(selectorIntent)
    }

    @Parcelize
    class Section(
            val title: String,
            val key: String?,
            val entryNames: ArrayList<String>,
            val entryValues: ArrayList<String>
    ) : Parcelable {
        override fun toString(): String {
            return "Title: $title\nKey: $key\nEntry Names: $entryNames\nEntry Values: $entryValues"
        }
    }
}
