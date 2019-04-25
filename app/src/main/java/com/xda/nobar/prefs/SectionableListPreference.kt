package com.xda.nobar.prefs

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import com.xda.nobar.R
import com.xda.nobar.interfaces.OnItemChosenListener
import com.xda.nobar.util.dpAsPx
import com.xda.nobar.util.helpers.bar.ActionHolder
import com.xda.nobar.views.ItemView

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
class SectionableListPreference(context: Context, attributeSet: AttributeSet) : Preference(context, attributeSet), OnItemChosenListener {
    var defaultValue: String? = null

    private val sections = ArrayList<Section>()
    private var dialog: AlertDialog? = null

    private var tempValue: String? = null

    init {
        val array = context.theme.obtainStyledAttributes(attributeSet, R.styleable.SectionableListPreference, 0, 0)

        val sectionNames = ArrayList<String>()
        val sectionDataNames = ArrayList<ArrayList<String>>()
        val sectionDataValues = ArrayList<ArrayList<String>>()

        for (i in 0 until array.indexCount) {
            val attr = array.getIndex(i)

            when (attr) {
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

            sections.add(Section(name, entryNames, entryValues))
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
        dialog?.dismiss()

        sections.removeAt(index)
    }

    fun removeItemByValue(value: String) {
        dialog?.dismiss()

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
        dialog?.dismiss()

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

    override fun onItemChosen(value: String?) {
        tempValue = value
        dialog?.dismiss()
        saveValue(tempValue)
    }

    override fun onClick() {
        val builder = AlertDialog.Builder(context)
        onPrepareDialogBuilder(builder)

        dialog = builder.show()
    }

    private fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        var whichButton = DialogInterface.BUTTON_NEGATIVE

        builder.setTitle(R.string.actions)
        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            whichButton = DialogInterface.BUTTON_POSITIVE
            tempValue = getPersistedString(tempValue)
        }

        builder.setView(onCreateDialogView())
        builder.setOnDismissListener { onDialogClosed(whichButton == DialogInterface.BUTTON_POSITIVE) }
    }

    private fun onCreateDialogView(): View {
        val topContainer = LinearLayout(context)
        topContainer.orientation = LinearLayout.VERTICAL
        topContainer.setPaddingRelative(0, context.dpAsPx(8), 0, 0)

        val topView = View(context)
        val bottomView = View(context)
        val viewParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dpAsPx(1))
        topView.layoutParams = viewParams
        bottomView.layoutParams = viewParams

        val color = Color.argb(0x30, 0xcc, 0xcc, 0xcc)
        topView.setBackgroundColor(color)
        bottomView.setBackgroundColor(color)

        topContainer.addView(topView)

        val scroller = ScrollView(context)
        scroller.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply { weight = 1f }
        scroller.isFillViewport = true
        topContainer.addView(scroller)

        topView.visibility = if (scroller.canScrollVertically(-1)) View.VISIBLE else View.INVISIBLE
        bottomView.visibility = if (scroller.canScrollVertically(1)) View.VISIBLE else View.INVISIBLE

        topContainer.addView(bottomView)

        scroller.viewTreeObserver.addOnScrollChangedListener {
            topView.visibility = if (scroller.canScrollVertically(-1)) View.VISIBLE else View.INVISIBLE
            bottomView.visibility = if (scroller.canScrollVertically(1)) View.VISIBLE else View.INVISIBLE
        }

        val holder = Holder(context)
        scroller.addView(holder)
        holder.setup(this, sections, scroller)

        return topContainer
    }

    private fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) saveValue(tempValue)
    }

    inner class Section(val title: String?, val entryNames: ArrayList<String>, val entryValues: ArrayList<String>) {
        override fun toString(): String {
            return "Title: $title\nEntry Names: $entryNames\nEntry Values: $entryValues"
        }
    }

    inner class SectionTitleView(context: Context) : AppCompatTextView(context) {
        var name: String?
            get() = text?.toString()
            set(value) {
                text = value
            }

        init {
            isClickable = false
            setTypeface(typeface, Typeface.BOLD)

            gravity = Gravity.CENTER_VERTICAL

            height = context.dpAsPx(48)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)

            setTextColor(ContextCompat.getColor(context, R.color.colorAccent))
            setPaddingRelative(context.dpAsPx(16), 0, 0, 0)
        }
    }

    inner class Holder(context: Context) : LinearLayout(context) {
        init {
            val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            layoutParams = params
            orientation = LinearLayout.VERTICAL
        }

        fun setup(listener: OnItemChosenListener, sections: ArrayList<SectionableListPreference.Section>, scrollView: ScrollView) {
            sections.forEach {
                val sectionView = SectionTitleView(context)
                sectionView.name = it.title
                if (it.title != null && it.title.isNotBlank()) addView(sectionView)

                for (i in 0 until it.entryNames.size) {
                    val itemView = View.inflate(context, R.layout.item_view, null) as ItemView
                    itemView.name = it.entryNames[i]
                    itemView.value = it.entryValues[i]
                    addView(itemView)

                    if (getSavedValue() == itemView.value) {
                        itemView.isChecked = true

                        itemView.viewTreeObserver.addOnGlobalLayoutListener {
                            scrollView.scrollTo(0, itemView.top)
                        }
                    }

                    itemView.setOnClickListener {v ->
                        if (v is ItemView) {
                            v.isChecked = true
                            setAllOthersUnchecked(v.value)
                            listener.onItemChosen(v.value)
                        }
                    }
                }
            }
        }

        fun getSelectedValue(): String? {
            for (i in 0 until childCount) {
                val child = getChildAt(i)

                if (child is ItemView && child.isChecked) return child.value
            }

            return null
        }

        private fun setAllOthersUnchecked(toKeep: String?) {
            for (i in 0 until childCount) {
                val child = getChildAt(i)

                if (child is ItemView && child.value != toKeep && child.isChecked) child.isChecked = false
            }
        }
    }
}
