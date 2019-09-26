package com.xda.nobar.adapters

import android.content.pm.PackageManager
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.SortedList
import com.rey.material.widget.CheckedImageView
import com.xda.nobar.R
import com.xda.nobar.adapters.info.AppInfo
import com.xda.nobar.adapters.info.AppInfoSorterCallback
import com.xda.nobar.interfaces.OnAppSelectedListener
import com.xda.nobar.util.toBitmapDrawable

/**
 * For use by BaseAppSelectActivity
 * This manages all the selection logic
 * Parses available AppInfo and displays the information
 */
@Suppress("DEPRECATION")
class AppSelectAdapter(val isSingleSelect: Boolean,
                       val showSummary: Boolean,
                       val checkListener: OnAppSelectedListener,
                       val activity: Boolean = false,
                       val isRemote: Boolean = true,
                       val showCheck: Boolean = true) : BaseSelectAdapter<AppInfo, BaseSelectAdapter.VH>() {

    override val sortedApps = SortedList(AppInfo::class.java, AppInfoSorterCallback(this, activity))

    private val selectedApps = ArrayList<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            BaseSelectAdapter.VH(LayoutInflater.from(parent.context)
                    .inflate(if (isSingleSelect) R.layout.app_info_single else R.layout.app_info_multi, parent, false))

    override fun onBindViewHolder(holder: BaseSelectAdapter.VH, position: Int) {
        val app = sortedApps[position]
        val view = holder.view

        val title = view.findViewById<TextView>(R.id.title)
        val summary = view.findViewById<TextView>(R.id.summary)
        val icon = view.findViewById<ImageView>(R.id.icon)
        val check = view.findViewById<CheckedImageView>(R.id.checkmark)

        if (!showCheck) check.visibility = View.GONE

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
            remoteResources.getDrawable(app.icon).toBitmapDrawable(view.context.resources)
                    ?: view.context.resources.getDrawable(R.drawable.blank)
        } catch (e: Resources.NotFoundException) {
            ContextCompat.getDrawable(view.context, R.drawable.blank)
        } catch (e: IllegalStateException) {
            ContextCompat.getDrawable(view.context, R.drawable.blank)
        }

        if (selectedApps.contains(app.packageName)) {
            app.isChecked = true
            check.isChecked = true
        }

        view.setOnClickListener {
            val app = sortedApps[holder.adapterPosition]

            check.isChecked = !check.isChecked || isSingleSelect
            app.isChecked = check.isChecked

            if (isSingleSelect) {
                (0 until sortedApps.size())
                        .map { int -> sortedApps[int] }
                        .filterNot { info -> info == app }
                        .filter { info -> info.isChecked }
                        .forEach { info ->
                            info.isChecked = false
                            notifyItemChanged(sortedApps.indexOf(info))
                        }
            }

            checkListener.onAppSelected(app, app.isChecked)
        }

        check.isChecked = app.isChecked
    }

    fun setSelectedByPackage(packageName: String) {
        (0 until sortedApps.size())
                .map { sortedApps[it] }
                .filter { it.packageName == packageName }
                .forEach {
                    it.isChecked = true
                    notifyItemChanged(sortedApps.indexOf(it))
                }
    }

    fun setInitiallySelectedPackages(packages: ArrayList<String>) {
        selectedApps.clear()
        selectedApps.addAll(packages)
    }
}