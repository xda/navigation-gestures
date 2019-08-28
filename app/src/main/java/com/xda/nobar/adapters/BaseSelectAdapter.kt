package com.xda.nobar.adapters

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList

abstract class BaseSelectAdapter<Info : Any, VH : BaseSelectAdapter.VH>(private val isSorted: Boolean = true) : RecyclerView.Adapter<VH>() {
    abstract val sortedApps: SortedList<Info>

    override fun getItemCount() = sortedApps.size()

    fun add(info: Info) {
        sortedApps.add(info)
    }

    fun remove(info: Info) {
        sortedApps.remove(info)
    }

    fun add(infos: List<Info>) {
        sortedApps.addAll(infos)
    }

    fun remove(infos: List<Info>) {
        infos.forEach {
            remove(it)
        }
    }

    fun replaceAll(models: List<Info>) {
        sortedApps.replaceAll(models)
    }

    fun clear() {
        sortedApps.clear()
    }

    open class VH(val view: View) : RecyclerView.ViewHolder(view)

    open class UnsortedList<Info : Any>(clazz: Class<Info>, private val callback: Callback<Info>) : SortedList<Info>(clazz, callback) {
        private val unsortedData = ArrayList<Info>()

        override fun add(item: Info): Int {
            unsortedData.add(item)
            val index = unsortedData.lastIndex
            callback.onInserted(index, 1)
            return index
        }

        override fun addAll(items: MutableCollection<Info>) {
            val size = items.size
            val start = unsortedData.lastIndex
            unsortedData.addAll(items)
            callback.onInserted(start, size)
        }

        override fun addAll(vararg items: Info) {
            addAll(items, false)
        }

        override fun addAll(items: Array<out Info>, mayModifyInput: Boolean) {
            addAll(items.toCollection(ArrayList<Info>()))
        }

        override fun clear() {
            val oldSize = unsortedData.size
            unsortedData.clear()
            callback.onRemoved(0, oldSize)
        }

        override fun get(index: Int): Info {
            return unsortedData[index]
        }

        override fun indexOf(item: Info): Int {
            return unsortedData.indexOf(item)
        }

        override fun remove(item: Info): Boolean {
            val index = indexOf(item)
            return unsortedData.remove(item).also { if (it) callback.onRemoved(index, 1) }
        }

        override fun removeItemAt(index: Int): Info {
            return unsortedData.removeAt(index).also { callback.onRemoved(index, 1) }
        }

        override fun replaceAll(items: MutableCollection<Info>) {
            val size = size()
            unsortedData.clear()
            callback.onRemoved(0, size)

            unsortedData.addAll(items)
            callback.onInserted(0, items.size)
        }

        override fun replaceAll(vararg items: Info) {
            replaceAll(items, false)
        }

        override fun replaceAll(items: Array<out Info>, mayModifyInput: Boolean) {
            replaceAll(items.toCollection(ArrayList<Info>()))
        }

        override fun size(): Int {
            return unsortedData.size
        }

        override fun updateItemAt(index: Int, item: Info) {
            unsortedData.removeAt(index)
            unsortedData.add(index, item)

            callback.onChanged(index, 1)
        }
    }
}