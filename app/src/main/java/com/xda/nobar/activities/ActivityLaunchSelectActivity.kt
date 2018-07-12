package com.xda.nobar.activities

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import com.xda.nobar.interfaces.OnAppSelectedListener
import com.xda.nobar.util.AppInfo
import com.xda.nobar.util.AppSelectAdapter
import java.util.*
import kotlin.collections.ArrayList

class ActivityLaunchSelectActivity : BaseAppSelectActivity() {
    override val adapter = AppSelectAdapter(true, true, OnAppSelectedListener { info ->
        PreferenceManager.getDefaultSharedPreferences(this@ActivityLaunchSelectActivity)
                .edit()
                .putString("${intent.getStringExtra(AppLaunchSelectActivity.EXTRA_KEY)}_activity", "${info.packageName}/${info.activity}")
                .putString("${intent.getStringExtra(AppLaunchSelectActivity.EXTRA_KEY)}_displayname", "${getPassedAppInfo()?.displayName}/${info.displayName}")
                .apply()

        val resultIntent = Intent()
        resultIntent.putExtra(AppLaunchSelectActivity.EXTRA_KEY, intent.getStringExtra(AppLaunchSelectActivity.EXTRA_KEY))
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

    override fun loadAppList(): ArrayList<*> {
        val info = packageManager.getPackageInfo(getPassedAppInfo()?.packageName, PackageManager.GET_ACTIVITIES)

        Log.e("NoBar", info.activities.size.toString())

        return ArrayList(info.activities.toList())
    }

    override fun loadAppInfo(info: Any): AppInfo? {
        info as ActivityInfo

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
}