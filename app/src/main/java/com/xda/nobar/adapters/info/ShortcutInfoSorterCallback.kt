package com.xda.nobar.adapters.info

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList

class ShortcutInfoSorterCallback(
        private val adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>
) : SortedList.Callback<ShortcutInfo>() {
    override fun areItemsTheSame(item1: ShortcutInfo?, item2: ShortcutInfo?) =
            item1?.activityInfo?.name == item2?.activityInfo?.name

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        adapter.notifyItemMoved(fromPosition, toPosition)
    }

    override fun onChanged(position: Int, count: Int) {
        adapter.notifyItemRangeChanged(position, count)
    }

    override fun onInserted(position: Int, count: Int) {
        adapter.notifyItemRangeChanged(position, count)
    }

    override fun onRemoved(position: Int, count: Int) {
        adapter.notifyItemRangeRemoved(position, count)
    }

    override fun compare(o1: ShortcutInfo, o2: ShortcutInfo) =
            o1.activityInfo.name.compareTo(o2.activityInfo.name)

    override fun areContentsTheSame(oldItem: ShortcutInfo?, newItem: ShortcutInfo?) =
            areItemsTheSame(oldItem, newItem)

}