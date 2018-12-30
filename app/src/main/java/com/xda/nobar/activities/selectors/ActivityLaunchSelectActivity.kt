package com.xda.nobar.activities.selectors

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import com.xda.nobar.adapters.AppSelectAdapter
import com.xda.nobar.adapters.info.AppInfo
import com.xda.nobar.interfaces.OnAppSelectedListener

class ActivityLaunchSelectActivity : BaseAppSelectActivity<ActivityInfo, AppInfo>() {
    override val adapter = AppSelectAdapter(true, true, OnAppSelectedListener { info ->
        PreferenceManager.getDefaultSharedPreferences(this@ActivityLaunchSelectActivity)
                .edit()
                .putString("${intent.getStringExtra(EXTRA_KEY)}_activity", "${info.packageName}/${info.activity}")
                .putString("${intent.getStringExtra(EXTRA_KEY)}_displayname", "${getPassedAppInfo()?.displayName}/${info.displayName}")
                .apply()

        val resultIntent = Intent()
        resultIntent.putExtra(EXTRA_KEY, intent.getStringExtra(EXTRA_KEY))
        resultIntent.putExtra(AppLaunchSelectActivity.EXTRA_RESULT_DISPLAY_NAME, "${getPassedAppInfo()?.displayName}/${info.displayName}")
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }, true)

    override fun canRun() = intent.hasExtra(APPINFO)

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
        val lowercase = query.toLowerCase()

        val filteredList = ArrayList<AppInfo>()

        ArrayList(origAppSet).forEach {
            val title = it.displayName.toLowerCase()
            val summary = if (adapter.activity) it.activity else it.packageName
            if (title.contains(lowercase) || summary.contains(lowercase)) {
                filteredList.add(it)
            }
        }

        return filteredList
    }
}