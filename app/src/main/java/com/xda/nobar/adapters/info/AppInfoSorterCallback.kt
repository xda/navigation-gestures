package com.xda.nobar.adapters.info

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList

/**
 * For use by BaseAppSelectActivity
 * Perform certain actions when a search is made
 */
class AppInfoSorterCallback(private val adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>,
                            private val activity: Boolean = false) :
        SortedList.Callback<AppInfo>() {
    override fun areItemsTheSame(item1: AppInfo?, item2: AppInfo?) =
            (if (activity) item1?.activity else item1?.packageName) == (if (activity) item2?.activity else item2?.packageName)
    override fun areContentsTheSame(oldItem: AppInfo?, newItem: AppInfo?) = oldItem == newItem
    override fun onInserted(position: Int, count: Int) = adapter.notifyItemRangeInserted(position, count)
    override fun onMoved(fromPosition: Int, toPosition: Int) = adapter.notifyItemMoved(fromPosition, toPosition)
    override fun onRemoved(position: Int, count: Int) = adapter.notifyItemRangeRemoved(position, count)
    override fun onChanged(position: Int, count: Int) = adapter.notifyItemRangeChanged(position, count)
    override fun compare(o1: AppInfo, o2: AppInfo) = o1.displayName.compareTo(o2.displayName)
}