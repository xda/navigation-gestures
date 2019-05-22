package com.xda.nobar.adapters.info

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList

class ActionInfoSorterCallback(private val adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>) :
        SortedList.Callback<ActionInfo>() {
    override fun areItemsTheSame(item1: ActionInfo?, item2: ActionInfo?) = false
    override fun areContentsTheSame(oldItem: ActionInfo?, newItem: ActionInfo?) = false
    override fun onInserted(position: Int, count: Int) = adapter.notifyItemRangeInserted(position, count)
    override fun onMoved(fromPosition: Int, toPosition: Int) = adapter.notifyItemMoved(fromPosition, toPosition)
    override fun onRemoved(position: Int, count: Int) = adapter.notifyItemRangeRemoved(position, count)
    override fun onChanged(position: Int, count: Int) = adapter.notifyItemRangeChanged(position, count)
    override fun compare(o1: ActionInfo, o2: ActionInfo) = 0
}