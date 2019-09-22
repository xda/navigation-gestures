package com.xda.nobar.dev

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xda.nobar.R
import kotlinx.android.synthetic.main.preference_view_layout.view.*
import java.util.*

class PreferenceViewerAdapter : RecyclerView.Adapter<PreferenceViewerAdapter.VH>() {
    private val fullList = object : ArrayList<Data>() {
        override fun add(element: Data): Boolean {
            visibleList.add(element)
            return super.add(element)
        }

        override fun add(index: Int, element: Data) {
            visibleList.add(element)
            super.add(index, element)
        }

        override fun addAll(elements: Collection<Data>): Boolean {
            visibleList.addAll(elements)
            return super.addAll(elements)
        }

        override fun addAll(index: Int, elements: Collection<Data>): Boolean {
            visibleList.addAll(elements)
            return super.addAll(index, elements)
        }

        override fun remove(element: Data): Boolean {
            visibleList.remove(element)
            return super.remove(element)
        }

        override fun removeAll(elements: Collection<Data>): Boolean {
            elements.forEach {
                visibleList.remove(it)
            }
            return super.removeAll(elements)
        }

        override fun removeAt(index: Int): Data {
            return super.removeAt(index).also {
                visibleList.remove(it)
            }
        }
    }
    private val visibleList = SortedList<Data>(Data::class.java, object : SortedList.Callback<Data>() {
        override fun areItemsTheSame(item1: Data, item2: Data): Boolean {
            return item1 == item2
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            notifyItemMoved(fromPosition, toPosition)
        }

        override fun onChanged(position: Int, count: Int) {
            notifyItemRangeChanged(position, count)
        }

        override fun onInserted(position: Int, count: Int) {
            notifyItemRangeInserted(position, count)
        }

        override fun onRemoved(position: Int, count: Int) {
            notifyItemRangeRemoved(position, count)
        }

        override fun compare(o1: Data, o2: Data): Int {
            return o1.compareTo(o2)
        }

        override fun areContentsTheSame(oldItem: Data, newItem: Data): Boolean {
            return oldItem == newItem
        }
    })

    override fun getItemCount(): Int {
        return visibleList.size()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(LayoutInflater.from(parent.context)
                .inflate(R.layout.preference_view_layout, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.onBind(visibleList.get(position))
    }

    fun setItems(data: HashMap<String, Any?>) {
        fullList.clear()
        fullList.addAll(
                data.map {
                    Data(it.key, it.value?.toString())
                }
        )
    }

    fun onSearch(newText: String?) {
        val lowercase = newText?.toLowerCase(Locale.getDefault())

        val newList = fullList.filter {
            lowercase == null
                    || it.key.toLowerCase(Locale.getDefault()).contains(lowercase)
                    || it.value?.toLowerCase(Locale.getDefault())?.contains(lowercase) == true
        }

        visibleList.clear()
        visibleList.addAll(newList)
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        fun onBind(data: Data) {
            itemView.apply {
                key.text = data.key
                value.text = data.value.toString()

                setOnClickListener {
                    val newData = visibleList.get(adapterPosition)

                    val dialog = MaterialAlertDialogBuilder(context)
                            .setTitle(newData.key)
                            .setMessage(newData.value)
                            .create()

                    dialog.setOnShowListener {
                        dialog.findViewById<TextView>(android.R.id.message)
                                ?.setTextIsSelectable(true)
                    }

                    dialog.show()
                }
            }
        }
    }

    data class Data(
            val key: String,
            val value: String?
    ) : Comparable<Data> {
        override fun compareTo(other: Data): Int {
            return key.compareTo(other.key)
        }

        override fun equals(other: Any?): Boolean {
            return other is Data && other.key == key
        }

        override fun hashCode(): Int {
            return key.hashCode()
        }
    }
}