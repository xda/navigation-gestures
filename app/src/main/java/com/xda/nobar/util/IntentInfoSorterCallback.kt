package com.xda.nobar.util

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList

/**
 * For use by BaseAppSelectActivity
 * Perform certain actions when a search is made
 */
class IntentInfoSorterCallback(private val adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>, private val context: Context) :
        SortedList.Callback<IntentInfo>() {
    override fun areItemsTheSame(item1: IntentInfo?, item2: IntentInfo?) = item1 == item2
    override fun areContentsTheSame(oldItem: IntentInfo?, newItem: IntentInfo?) = oldItem == newItem
    override fun onInserted(position: Int, count: Int) = adapter.notifyItemRangeInserted(position, count)
    override fun onMoved(fromPosition: Int, toPosition: Int) = adapter.notifyItemMoved(fromPosition, toPosition)
    override fun onRemoved(position: Int, count: Int) = adapter.notifyItemRangeRemoved(position, count)
    override fun onChanged(position: Int, count: Int) = adapter.notifyItemRangeChanged(position, count)
    override fun compare(o1: IntentInfo, o2: IntentInfo) = context.resources.getString(o1.id).compareTo(context.resources.getString(o2.id))
}