package com.xda.nobar.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.SortedList
import com.xda.nobar.R
import com.xda.nobar.adapters.info.ShortcutInfo
import com.xda.nobar.adapters.info.ShortcutInfoSorterCallback
import com.xda.nobar.interfaces.OnShortcutSelectedListener
import com.xda.nobar.util.toBitmapDrawable
import kotlinx.android.synthetic.main.app_info_single.view.*

class ShortcutSelectAdapter(
        val checkListener: OnShortcutSelectedListener
) : BaseSelectAdapter<ShortcutInfo>() {
    override val apps = SortedList(ShortcutInfo::class.java,
            ShortcutInfoSorterCallback(this))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context)
                .inflate(R.layout.app_info_single, parent, false))

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = apps[position]
        val view = holder.view

        view.title.text = item.label
        view.summary.text = "${item.packageName}/${item.clazz}"

        val res = view.context.packageManager.getResourcesForApplication(item.packageName)

        view.icon.background = try {
            ResourcesCompat.getDrawable(res, item.icon, res.newTheme())?.toBitmapDrawable(view.context.resources)
        } catch (e: Exception) {
            ContextCompat.getDrawable(view.context, R.drawable.blank)
        }

        view.setOnClickListener {
            view.checkmark.isChecked = true
            item.isChecked = true

            (0 until apps.size())
                    .map { int -> apps[int] }
                    .filterNot { info -> info == item }
                    .filter { info -> info.isChecked }
                    .forEach { info ->
                        info.isChecked = false
                        notifyItemChanged(apps.indexOf(info))
                    }

            checkListener.onShortcutSelected(item)
        }

        view.checkmark.isChecked = item.isChecked
    }
}