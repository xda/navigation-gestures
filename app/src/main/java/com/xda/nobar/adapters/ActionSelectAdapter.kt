package com.xda.nobar.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.xda.nobar.R
import com.xda.nobar.adapters.info.ActionInfo
import com.xda.nobar.adapters.info.ActionInfoSorterCallback

class ActionSelectAdapter(private val onItemSelectedListener: (info: ActionInfo) -> Unit) : BaseSelectAdapter<ActionInfo, ActionSelectAdapter.BaseVH>() {
    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM = 1
        const val TYPE_BLANK_HEADER = 2
    }

    override val sortedApps = UnsortedList(ActionInfo::class.java, ActionInfoSorterCallback(this))

    override fun getItemViewType(position: Int): Int {
        val info = sortedApps[position]
        return if (info.isHeader) {
            if (info.label.isBlank()) TYPE_BLANK_HEADER
            else TYPE_HEADER
        } else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseVH {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> {
                HeaderViewHolder(
                        inflater.inflate(R.layout.pref_category, parent, false)
                )
            }
            TYPE_BLANK_HEADER -> {
                BlankHeaderViewHolder(
                        LinearLayout(parent.context)
                )
            }
            TYPE_ITEM -> {
                ActionViewHolder(
                        inflater.inflate(R.layout.pref, parent, false)
                )
            }
            else -> throw IllegalArgumentException("$viewType is not a valid viewType")
        }
    }

    override fun onBindViewHolder(holder: BaseVH, position: Int) {
        val info = sortedApps[position]

        holder.setTitle(info.label)
        holder.itemView.setOnClickListener(
                if (info.isHeader) null else View.OnClickListener {
                    onItemSelectedListener.invoke(sortedApps[holder.adapterPosition])
                }
        )
    }

    class HeaderViewHolder(view: View) : BaseVH(view)
    class BlankHeaderViewHolder(view: View) : BaseVH(view) {
        override fun setTitle(title: CharSequence) {
            //no-op
        }
    }
    class ActionViewHolder(view: View) : BaseVH(view)

    open class BaseVH(view: View) : BaseSelectAdapter.VH(view) {
        private val titleView = itemView.findViewById<TextView>(android.R.id.title)

        open fun setTitle(title: CharSequence) {
            titleView.text = title
        }
    }
}