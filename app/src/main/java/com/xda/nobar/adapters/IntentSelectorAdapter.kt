package com.xda.nobar.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.SortedList
import com.rey.material.widget.CheckedImageView
import com.xda.nobar.R
import com.xda.nobar.interfaces.OnIntentSelectedListener
import com.xda.nobar.util.IntentInfo
import com.xda.nobar.util.IntentInfoSorterCallback

class IntentSelectorAdapter(private val callback: OnIntentSelectedListener, private val context: Context) : BaseSelectAdapter<IntentInfo>() {
    override val apps = SortedList<IntentInfo>(IntentInfo::class.java, IntentInfoSorterCallback(this, context))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(LayoutInflater.from(parent.context).inflate(R.layout.app_info_single, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val info = apps[position]
        val view = holder.view

        val title = view.findViewById<TextView>(R.id.title)
        val summary = view.findViewById<TextView>(R.id.summary)
        val icon = view.findViewById<ImageView>(R.id.icon)
        val check = view.findViewById<CheckedImageView>(R.id.checkmark)

        title.text = context.resources.getString(info.id)
        summary.visibility = View.GONE

        icon.background = ContextCompat.getDrawable(view.context, R.drawable.blank)

        view.setOnClickListener { _ ->
            check.isChecked = true
            info.isChecked = check.isChecked

            (0 until apps.size())
                    .map { apps[it] }
                    .filterNot { it == info }
                    .filter { it.isChecked }
                    .forEach {
                        it.isChecked = false
                        notifyItemChanged(apps.indexOf(it))
                    }

            callback.onIntentSelected(info)
        }

        check.isChecked = info.isChecked
    }

}