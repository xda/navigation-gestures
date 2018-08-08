package com.xda.nobar.activities

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.os.PersistableBundle
import android.preference.PreferenceManager
import com.xda.nobar.R
import com.xda.nobar.adapters.AppSelectAdapter
import com.xda.nobar.interfaces.OnAppSelectedListener
import com.xda.nobar.util.AppInfo
import java.util.*
import kotlin.collections.ArrayList

/**
 * Selector activity for choosing an app to launch
 * This will be opened when the user chooses the "Launch App" action for a gesture
 */
class AppLaunchSelectActivity : BaseAppSelectActivity<Any, AppInfo>() {
    companion object {
        const val EXTRA_RESULT_DISPLAY_NAME = "name"
        const val CHECKED_PACKAGE = "checked_package"
        const val CHECKED_ACTIVITY = "checked_activity"

        const val FOR_ACTIVITY_SELECT = "activity_select"

        const val ACTIVITY_REQ = 1002
    }

    override val adapter = AppSelectAdapter(true, true, OnAppSelectedListener { info ->
        if (isForActivitySelect()) {
            val intent = Intent(this, ActivityLaunchSelectActivity::class.java)
            intent.putExtra(CHECKED_ACTIVITY, this.intent.getStringExtra(CHECKED_ACTIVITY))
            intent.putExtra(EXTRA_KEY, this.intent.getStringExtra(EXTRA_KEY))
            passAppInfo(intent, info)
            startActivityForResult(intent, ACTIVITY_REQ)
        } else {
            PreferenceManager.getDefaultSharedPreferences(this@AppLaunchSelectActivity)
                    .edit()
                    .putString("${intent.getStringExtra(EXTRA_KEY)}_package", "${info.packageName}/${info.activity}")
                    .putString("${intent.getStringExtra(EXTRA_KEY)}_displayname", info.displayName)
                    .apply()

            val resultIntent = Intent()
            resultIntent.putExtra(EXTRA_KEY, intent.getStringExtra(EXTRA_KEY))
            resultIntent.putExtra(EXTRA_RESULT_DISPLAY_NAME, info.displayName)
            resultIntent.putExtra(FOR_ACTIVITY_SELECT, false)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = resources.getText(if (isForActivitySelect()) R.string.prem_launch_activity_no_format else R.string.prem_launch_app_no_format)
    }

    override fun canRun() = intent != null && intent.hasExtra(EXTRA_KEY)

    override fun showUpAsCheckMark() = false

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        if (isForActivitySelect()) title = resources.getText(R.string.prem_launch_activity_no_format)
    }

    override fun loadAppList(): ArrayList<Any> {
        return if (isForActivitySelect()) {
            val list = packageManager.getInstalledApplications(0)
            ArrayList(list)
        } else {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_LAUNCHER)

            val list = packageManager.queryIntentActivities(intent, 0)
            Collections.sort(list, ResolveInfo.DisplayNameComparator(packageManager))

            ArrayList(list)
        }
    }

    override fun loadAppInfo(info: Any): AppInfo {
        return if (isForActivitySelect()) {
            info as ApplicationInfo
            AppInfo(info.packageName,
                    "",
                    info.loadLabel(packageManager).toString(),
                    info.icon, info.packageName == intent.getStringExtra(CHECKED_PACKAGE))
        } else {
            info as ResolveInfo
            AppInfo(info.activityInfo.packageName,
                    info.activityInfo.name,
                    info.loadLabel(packageManager).toString(),
                    info.iconResource, info.activityInfo.packageName == intent.getStringExtra(CHECKED_PACKAGE))
        }
    }

    override fun shouldAddInfo(appInfo: AppInfo) =
            if (isForActivitySelect()) {
                val activities = packageManager.getPackageInfo(appInfo.packageName, PackageManager.GET_ACTIVITIES).activities
                (activities != null && activities.isNotEmpty())
            } else true

    override fun onBackPressed() {
        val resultIntent = Intent()
        resultIntent.putExtra(EXTRA_KEY, intent.getStringExtra(EXTRA_KEY))
        setResult(Activity.RESULT_CANCELED, resultIntent)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ACTIVITY_REQ) {
            if (resultCode == Activity.RESULT_OK) {
                data?.putExtra(FOR_ACTIVITY_SELECT, true)
                setResult(resultCode, data)
                finish()
            }
        }
    }

    private fun isForActivitySelect() = intent.getBooleanExtra(FOR_ACTIVITY_SELECT, false)

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
