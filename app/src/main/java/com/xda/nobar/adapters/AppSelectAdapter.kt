package com.xda.nobar.adapters

import android.content.pm.PackageManager
import android.content.res.Resources
import android.support.v7.util.SortedList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.rey.material.widget.CheckedImageView
import com.xda.nobar.R
import com.xda.nobar.interfaces.OnAppSelectedListener
import com.xda.nobar.util.AppInfo
import com.xda.nobar.util.AppInfoSorterCallback
import com.xda.nobar.util.Utils

/**
 * For use by BaseAppSelectActivity
 * This manages all the selection logic
 * Parses available AppInfo and displays the information
 */
class AppSelectAdapter(val isSingleSelect: Boolean,
                       val showSummary: Boolean,
                       val checkListener: OnAppSelectedListener,
                       val activity: Boolean = false,
                       val isRemote: Boolean = true) : BaseSelectAdapter<AppInfo>() {

    override val apps = SortedList<AppInfo>(AppInfo::class.java, AppInfoSorterCallback(this, activity))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context)
                    .inflate(if (isSingleSelect) R.layout.app_info_single else R.layout.app_info_multi, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
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

        val remoteResources = try {
            if (isRemote) view.context.packageManager.getResourcesForApplication(app.packageName) else view.context.resources
        } catch (e: PackageManager.NameNotFoundException) {
            view.context.resources
        }
        icon.background = try {
            Utils.getBitmapDrawable(remoteResources.getDrawable(app.icon), holder.view.context.resources)
                    ?: view.context.resources.getDrawable(R.drawable.blank)
        } catch (e: Resources.NotFoundException) {
            view.context.resources.getDrawable(R.drawable.blank)
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

    fun setSelectedByPackage(packageName: String) {
        (0 until apps.size())
                .map { apps[it] }
                .filter { it.packageName == packageName }
                .forEach {
                    it.isChecked = true
                    notifyItemChanged(apps.indexOf(it))
                }
    }
}