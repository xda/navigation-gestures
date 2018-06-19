package com.xda.nobar.util

import android.support.v7.util.SortedList
import android.support.v7.widget.RecyclerView

class AppInfoSorterCallback(private val adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>) : SortedList.Callback<AppInfo>() {
    override fun areItemsTheSame(item1: AppInfo?, item2: AppInfo?) = item1?.packageName == item2?.packageName
    override fun areContentsTheSame(oldItem: AppInfo?, newItem: AppInfo?) = oldItem == newItem
    override fun onInserted(position: Int, count: Int) = adapter.notifyItemRangeInserted(position, count)
    override fun onMoved(fromPosition: Int, toPosition: Int) = adapter.notifyItemMoved(fromPosition, toPosition)
    override fun onRemoved(position: Int, count: Int) = adapter.notifyItemRangeRemoved(position, count)
    override fun onChanged(position: Int, count: Int) = adapter.notifyItemRangeChanged(position, count)
    override fun compare(o1: AppInfo, o2: AppInfo) = o1.displayName.compareTo(o2.displayName)
}