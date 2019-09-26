package com.xda.nobar.activities.ui

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ResolveInfo
import com.xda.nobar.activities.selectors.BaseAppSelectActivity
import com.xda.nobar.adapters.AppSelectAdapter
import com.xda.nobar.adapters.info.AppInfo
import com.xda.nobar.interfaces.OnAppSelectedListener
import java.util.*
import kotlin.collections.ArrayList

class AppDrawerActivity : BaseAppSelectActivity<ResolveInfo, AppInfo>(), OnAppSelectedListener {
    private val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

    override val adapter = AppSelectAdapter(isSingleSelect = false, showSummary = true, checkListener = this, showCheck = false)

    override fun onAppSelected(info: AppInfo, isChecked: Boolean) {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.component = ComponentName(info.packageName, info.activity)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        startActivity(intent)
        finish()
    }

    override fun loadAppList(): ArrayList<ResolveInfo> {
        val list = packageManager.queryIntentActivities(launcherIntent, 0)

        return ArrayList(list)
    }

    override fun loadAppInfo(info: ResolveInfo): AppInfo? {
        return AppInfo(info.activityInfo.packageName,
                info.activityInfo.name,
                info.loadLabel(packageManager).toString(),
                info.iconResource,
                false
        )
    }

    override fun filter(query: String): List<AppInfo> {
        val ret = ArrayList<AppInfo>()

        ArrayList(origAppSet).forEach {
            val lowerQuery = query.toLowerCase(Locale.getDefault())
            val lowerAppName = it.displayName.toLowerCase(Locale.getDefault())
            val lowerPackageName = it.packageName.toLowerCase(Locale.getDefault())

            if (lowerAppName.contains(lowerQuery) || lowerPackageName.contains(lowerQuery))
                ret.add(it)
        }

        return ret
    }
}