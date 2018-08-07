package com.xda.nobar.util

import android.content.res.Resources
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
class AppSelectAdapter(val isSingleSelect: Boolean,
                       val showSummary: Boolean,
                       val checkListener: OnAppSelectedListener,
                       val activity: Boolean = false,
                       val isRemote: Boolean = true)
    : RecyclerView.Adapter<AppSelectAdapter.VH>() {
    val apps = SortedList<AppInfo>(AppInfo::class.java, AppInfoSorterCallback(this, activity))

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
        if (showSummary) summary.text = if (activity) app.activity else app.packageName
        else {
            summary.visibility = View.GONE
        }

        val remoteResources = if (isRemote) view.context.packageManager.getResourcesForApplication(app.packageName) else view.context.resources
        icon.background = try {
            Utils.getBitmapDrawable(remoteResources.getDrawable(app.icon), holder.view.context.resources)
                    ?: view.context.resources.getDrawable(android.R.drawable.ic_menu_help)
        } catch (e: Resources.NotFoundException) {
            view.context.resources.getDrawable(android.R.drawable.ic_menu_help)
        }

        view.setOnClickListener { _ ->
            check.isChecked = !check.isChecked || isSingleSelect
            app.isChecked = check.isChecked

            if (isSingleSelect) {
                (0 until apps.size())
                        .map { apps[it] }
                        .filterNot { it == app }
                        .filter { it.isChecked }
                        .forEach {
                            it.isChecked = false
                            notifyItemChanged(apps.indexOf(it))
                        }
            }

            checkListener.onAppSelected(app)
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

    fun clear() {
        val dataSize = apps.size()
        apps.clear()
        notifyItemRangeRemoved(0, dataSize)
    }

    fun setSelectedByPackage(packageName: String) {
        (0 until apps.size())
                .map { apps[it] }
                .filter { it.packageName == packageName }
                .forEach {
                    it.isChecked = true
                    notifyItemChanged(apps.indexOf(it))
                }
    }

    class VH(val view: View) : RecyclerView.ViewHolder(view)
}