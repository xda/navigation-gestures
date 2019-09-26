package com.xda.nobar.activities.selectors

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import com.xda.nobar.adapters.AppSelectAdapter
import com.xda.nobar.adapters.info.AppInfo
import com.xda.nobar.interfaces.OnAppSelectedListener
import com.xda.nobar.util.prefManager
import java.util.*
import kotlin.collections.ArrayList

class ActivityLaunchSelectActivity : BaseAppSelectActivity<ActivityInfo, AppInfo>() {
    override val adapter = AppSelectAdapter(isSingleSelect = true, showSummary = true, checkListener = OnAppSelectedListener { info, _ ->
        prefManager.apply {
            putActivity(key!!, "${info.packageName}/${info.activity}")
            putDisplayName(key!!, "${getPassedAppInfo()?.displayName}/${info.displayName}")
        }

        val resultIntent = Intent()
        resultIntent.putExtras(intent)
        resultIntent.putExtra(AppLaunchSelectActivity.EXTRA_RESULT_DISPLAY_NAME,
                "${getPassedAppInfo()?.displayName}/${info.displayName}")
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }, activity = true)

    override fun canRun() = intent.hasExtra(APPINFO) && key != null

    override fun showUpAsCheckMark() = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = getPassedAppInfo()?.displayName
    }

    override fun loadAppList(): ArrayList<ActivityInfo> {
        val info = packageManager.getPackageInfo(getPassedAppInfo()?.packageName, PackageManager.GET_ACTIVITIES)
        val activities = ArrayList(info.activities.toList())
        val shortcuts = packageManager.queryIntentActivities(Intent(Intent.ACTION_CREATE_SHORTCUT), PackageManager.GET_RESOLVED_FILTER)
                .map { it.activityInfo }
                .map { it.name }

        activities.removeAll { shortcuts.contains(it.name) }

        return activities
    }

    override fun loadAppInfo(info: ActivityInfo): AppInfo? {
        return if (info.exported) {
            AppInfo(info.packageName,
                    info.name,
                    info.loadLabel(packageManager).toString(),
                    info.iconResource,
                    info.name == intent.getStringExtra(AppLaunchSelectActivity.CHECKED_ACTIVITY))
        } else null
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)

        super.onBackPressed()
    }

    override fun filter(query: String): ArrayList<AppInfo> {
        val lowercase = query.toLowerCase(Locale.getDefault())

        val filteredList = ArrayList<AppInfo>()

        ArrayList(origAppSet).forEach {
            val title = it.displayName.toLowerCase(Locale.getDefault())
            val summary = if (adapter.activity) it.activity else it.packageName
            if (title.contains(lowercase) || summary.contains(lowercase)) {
                filteredList.add(it)
            }
        }

        return filteredList
    }
}