package com.xda.nobar.util

import android.support.v7.util.SortedList
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.rey.material.widget.CheckedImageView
import com.xda.nobar.R
import com.xda.nobar.interfaces.OnAppSelectedListener

/**
 * For use by BaseAppSelectActivity
 * This manages all the selection logic
 * Parses available AppInfo and displays the information
 */
class AppSelectAdapter(private val isSingleSelect: Boolean, private val showSummary: Boolean, private val checkListener: OnAppSelectedListener)
    : RecyclerView.Adapter<AppSelectAdapter.VH>() {
    val apps = SortedList<AppInfo>(AppInfo::class.java, AppInfoSorterCallback(this))

    override fun getItemCount() = apps.size()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            AppSelectAdapter.VH(LayoutInflater.from(parent.context)
                    .inflate(if (isSingleSelect) R.layout.app_info_single else R.layout.app_info_multi, parent, false))

    override fun onBindViewHolder(holder: AppSelectAdapter.VH, position: Int) {
        val app = apps[position]
        val view = holder.view

        val title = view.findViewById<TextView>(R.id.title)
        val summary = view.findViewById<TextView>(R.id.summary)
        val icon = view.findViewById<ImageView>(R.id.icon)
        val check = view.findViewById<CheckedImageView>(R.id.checkmark)

        title.text = app.displayName
        if (showSummary) summary.text = app.packageName
        else {
            summary.visibility = View.GONE
        }

        icon.background = Utils.getBitmapDrawable(apps[position].icon, holder.view.context.resources)

        view.setOnClickListener {
            if (isSingleSelect) {
                checkListener.onAppSelected(app)
            } else {
                check.isChecked = !check.isChecked
                app.isChecked = check.isChecked

                checkListener.onAppSelected(app)
            }
        }

        check.isChecked = app.isChecked
    }

    fun add(info: AppInfo) {
        apps.add(info)
    }

    fun remove(info: AppInfo) {
        apps.remove(info)
    }

    fun add(infos: List<AppInfo>) {
        apps.addAll(infos)
    }

    fun remove(infos: List<AppInfo>) {
        infos.forEach {
            remove(it)
        }
    }

    fun replaceAll(models: List<AppInfo>) {
        apps.beginBatchedUpdates()
        for (i in apps.size() - 1 downTo 0) {
            val model = apps.get(i)
            if (!models.contains(model)) {
                apps.remove(model)
            }
        }
        apps.addAll(models)
        apps.endBatchedUpdates()
    }

    class VH(val view: View) : RecyclerView.ViewHolder(view)
}