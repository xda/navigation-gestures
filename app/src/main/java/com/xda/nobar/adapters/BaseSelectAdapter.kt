package com.xda.nobar.adapters

import android.support.v7.util.SortedList
import android.support.v7.widget.RecyclerView
import android.view.View

abstract class BaseSelectAdapter<Info : Any>: RecyclerView.Adapter<BaseSelectAdapter.VH>() {
    abstract val apps: SortedList<Info>

    override fun getItemCount() = apps.size()

    fun add(info: Info) {
        notifyItemInserted(apps.add(info))
    }

    fun remove(info: Info) {
        val index = apps.indexOf(info)
        apps.remove(info)
        notifyItemRemoved(index)
    }

    fun add(infos: List<Info>) {
        apps.addAll(infos)
        notifyDataSetChanged()
    }

    fun remove(infos: List<Info>) {
        infos.forEach {
            remove(it)
        }
        notifyDataSetChanged()
    }

    fun replaceAll(models: List<Info>) {
        apps.beginBatchedUpdates()
        for (i in apps.size() - 1 downTo 0) {
            val model = apps.get(i)
            if (!models.contains(model)) {
                apps.remove(model)
            }
        }
        apps.addAll(models)
        apps.endBatchedUpdates()
        notifyDataSetChanged()
    }

    fun clear() {
        val dataSize = apps.size()
        apps.clear()
        notifyItemRangeRemoved(0, dataSize)
    }

    class VH(val view: View) : RecyclerView.ViewHolder(view)
}