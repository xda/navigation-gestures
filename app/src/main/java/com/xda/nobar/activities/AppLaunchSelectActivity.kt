package com.xda.nobar.activities

import android.app.Activity
import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.MenuItem
import com.xda.nobar.R
import com.xda.nobar.interfaces.OnAppSelectedListener
import com.xda.nobar.util.AppInfo
import com.xda.nobar.util.AppSelectAdapter
import java.util.*
import kotlin.collections.ArrayList

/**
 * Selector activity for choosing an app to launch
 * This will be opened when the user chooses the "Launch App" action for a gesture
 */
class AppLaunchSelectActivity : BaseAppSelectActivity() {
    companion object {
        const val EXTRA_KEY = "key"
        const val EXTRA_RESULT_DISPLAY_NAME = "name"
        const val CHECKED_PACKAGE = "checked"
    }

    override val adapter = AppSelectAdapter(true, true, OnAppSelectedListener { info ->
        PreferenceManager.getDefaultSharedPreferences(this@AppLaunchSelectActivity)
                .edit()
                .putString("${intent.getStringExtra(EXTRA_KEY)}_package", "${info.packageName}/${info.activity}")
                .apply()

        val resultIntent = Intent()
        resultIntent.putExtra(EXTRA_KEY, intent.getStringExtra(EXTRA_KEY))
        resultIntent.putExtra(EXTRA_RESULT_DISPLAY_NAME, info.displayName)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = resources.getText(R.string.prem_launch_app_no_format)
    }

    override fun canRun(): Boolean {
        return intent != null && intent.hasExtra(EXTRA_KEY)
    }

    override fun loadAppList(): ArrayList<*> {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        val list = packageManager.queryIntentActivities(intent, 0)
        Collections.sort(list, ResolveInfo.DisplayNameComparator(packageManager))

        return ArrayList(list)
    }

    override fun loadAppInfo(info: Any): AppInfo {
        info as ResolveInfo
        return AppInfo(info.activityInfo.packageName,
                info.activityInfo.name,
                info.loadLabel(packageManager).toString(),
                info.loadIcon(packageManager), info.activityInfo.packageName == intent.getStringExtra(CHECKED_PACKAGE))
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        val resultIntent = Intent()
        resultIntent.putExtra(EXTRA_KEY, intent.getStringExtra(EXTRA_KEY))
        setResult(Activity.RESULT_CANCELED, resultIntent)
        finish()
    }
}
