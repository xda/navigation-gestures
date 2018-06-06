package com.xda.nobar.prefs

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.preference.DialogPreference
import android.preference.Preference
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.interfaces.Interfaces
import com.xda.nobar.util.Utils
import com.xda.nobar.views.ItemView

class SectionableListPreference(context: Context, attributeSet: AttributeSet) : DialogPreference(context, attributeSet), Interfaces.ItemChosenListener {
    val defaultValue: Any?
        get() {
            val mDefaultProgress = Preference::class.java.getDeclaredField("mDefaultValue")
            mDefaultProgress.isAccessible = true

            return mDefaultProgress.get(this)
        }
    val app = context.applicationContext as App

    private val sections = ArrayList<Section>()

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
        persistString(value)
        notifyChanged()
        callChangeListener(value)
        updateSummary(value)
    }

    fun updateSummary(value: String?) {
        val map = HashMap<String, Int>()
        Utils.getActionList(context, map)

        summary = Utils.actionToName(context, value?.toInt() ?: return)
    }

    fun getSavedValue(): String {
        return sharedPreferences.getString(key, (defaultValue ?: app.typeNoAction).toString())
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

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        saveValue(if (restorePersistedValue) sharedPreferences.getString(key, app.typeNoAction.toString()) else defaultValue.toString())
    }

    override fun onItemChosen(value: String?) {
        tempValue = value
        dialog?.dismiss()
        saveValue(tempValue)
    }

    override fun onCreateDialogView(): View {
        val topContainer = LinearLayout(context)
        topContainer.orientation = LinearLayout.VERTICAL
        topContainer.setPaddingRelative(0, Utils.dpAsPx(context, 8), 0, 0)

        val topView = View(context)
        val bottomView = View(context)
        val viewParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.dpAsPx(context, 1f))
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

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) saveValue(tempValue)
    }

    inner class Section(val title: String?, val entryNames: ArrayList<String>, val entryValues: ArrayList<String>) {
        override fun toString(): String {
            return "Title: $title\nEntry Names: $entryNames\nEntry Values: $entryValues"
        }
    }

    inner class SectionTitleView(context: Context) : TextView(context) {
        var name: String? = null
            set(value) {
                text = value
            }

        init {
            isClickable = false
            setTypeface(typeface, Typeface.BOLD)

            gravity = Gravity.CENTER_VERTICAL

            height = Utils.dpAsPx(context, 48)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)

            setTextColor(resources.getColor(R.color.colorAccent))
            setPaddingRelative(Utils.dpAsPx(context, 16), 0, 0, 0)
        }
    }

    inner class Holder(context: Context) : LinearLayout(context) {
        init {
            val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            layoutParams = params
            orientation = LinearLayout.VERTICAL
        }

        fun setup(listener: Interfaces.ItemChosenListener, sections: ArrayList<Section>, scrollView: ScrollView) {
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

                    itemView.setOnClickListener {
                        if (it is ItemView) {
                            if (!it.isChecked) {
                                it.isChecked = true
                                setAllOthersUnchecked(it.value)
                                listener.onItemChosen(it.value)
                            }
                        }
                    }
                }
            }
        }

        private fun setAllOthersUnchecked(toKeep: String?) {
            for (i in 0 until childCount) {
                val child = getChildAt(i)

                if (child is ItemView && child.value != toKeep && child.isChecked) child.isChecked = false
            }
        }
    }

//    class SectionsAdapter(private val context: Context,
//                          private val sections: ArrayList<Section>,
//                          private val listener: Interfaces.ItemChosenListener) : RecyclerView.Adapter<SectionsAdapter.VH>() {
//        val titles = ArrayList<SectionView>()
//        val sectionItems = ArrayList<ItemView>()
//        val orderedItems = ArrayList<View>()
//
//        init {
//            sections.forEach {
//                val sectionView = SectionView(context)
//                sectionView.name = it.title
//                titles.add(sectionView)
//
//                orderedItems.add(sectionView)
//
//                for (i in 0 until it.entryNames.size) {
//                    val itemView = ItemView(context)
//                    itemView.name = it.entryNames[i]
//                    itemView.value = it.entryValues[i]
//                    sectionItems.add(itemView)
//
//                    orderedItems.add(itemView)
//                }
//            }
//        }
//
//        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
//            return VH(ItemView(parent.context))
//        }
//
//        override fun onBindViewHolder(holder: VH, position: Int) {
//            holder.view.
//        }
//
//        override fun getItemCount(): Int {
//            return orderedItems.size
//        }
//
//        inner class VH(val view: View) : RecyclerView.ViewHolder(view) {
//            init {
//                if (view is ItemView) {
//                    view.setOnCheckedChangeListener { buttonView, isChecked ->
//                        sectionItems.forEach {
//                            if (it != view) it.isChecked = false
//                        }
//
//                    }
//                }
//            }
//        }
//    }
}