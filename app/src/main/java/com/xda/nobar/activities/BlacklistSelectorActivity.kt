package com.xda.nobar.activities

import android.Manifest
import android.app.Activity
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import com.xda.nobar.R
import com.xda.nobar.adapters.AppSelectAdapter
import com.xda.nobar.interfaces.OnAppSelectedListener
import com.xda.nobar.util.AppInfo
import java.util.*
import kotlin.collections.ArrayList

/**
 * Activity to manage which apps should be blacklisted for a certain function
 * Use the constants in the companion object to specify the function
 */
class BlacklistSelectorActivity : BaseAppSelectActivity<ApplicationInfo, AppInfo>() {
    companion object {
        const val EXTRA_WHICH = "which"

        const val FOR_BAR = "bar"
        const val FOR_NAV = "nav"
        const val FOR_WIN = "win"
        const val FOR_IMM = "imm"
    }

    private val currentlyBlacklisted = ArrayList<String>()

    override val adapter = AppSelectAdapter(false, true, OnAppSelectedListener { info ->
        if (info.isChecked) currentlyBlacklisted.add(info.packageName)
        else currentlyBlacklisted.removeAll(Collections.singleton(info.packageName))
    })

    private var which: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        which = intent.getStringExtra(EXTRA_WHICH)

        super.onCreate(savedInstanceState)

        when (which) {
            FOR_BAR -> {
                title = resources.getText(R.string.bar_blacklist)
                prefManager.loadBlacklistedBarPackages(currentlyBlacklisted)
            }
            FOR_NAV -> {
                title = resources.getText(R.string.nav_blacklist)
                prefManager.loadBlacklistedNavPackages(currentlyBlacklisted)
            }
            FOR_IMM -> {
                title = resources.getText(R.string.imm_blacklist)
                prefManager.loadBlacklistedImmPackages(currentlyBlacklisted)
            }
            FOR_WIN -> {
                title = resources.getText(R.string.fix_for_other_windows)
                prefManager.loadOtherWindowApps(currentlyBlacklisted)
            }
            else -> {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
    }

    override fun canRun(): Boolean {
        return intent != null && intent.hasExtra(EXTRA_WHICH)
    }

    override fun loadAppList(): ArrayList<ApplicationInfo> {
        val list = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        Collections.sort(list, ApplicationInfo.DisplayNameComparator(packageManager))

        return ArrayList(list)
    }

    override fun loadAppInfo(info: ApplicationInfo): AppInfo? {
        val appInfo = AppInfo(info.packageName,
                "",
                info.loadLabel(packageManager).toString(),
                info.icon, currentlyBlacklisted.contains(info.packageName))

        return if (which == FOR_WIN) {
            val perms = packageManager.getPackageInfo(info.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions
            if (perms != null
                    && (perms.contains(Manifest.permission.SYSTEM_ALERT_WINDOW )
                            || perms.contains(Manifest.permission.INTERNAL_SYSTEM_WINDOW))) appInfo
            else null
        } else appInfo
    }

    override fun onDestroy() {
        super.onDestroy()

        when (which) {
            FOR_NAV -> prefManager.saveBlacklistedNavPackageList(currentlyBlacklisted)
            FOR_BAR -> prefManager.saveBlacklistedBarPackages(currentlyBlacklisted)
            FOR_IMM -> prefManager.saveBlacklistedImmPackages(currentlyBlacklisted)
            FOR_WIN -> prefManager.saveOtherWindowApps(currentlyBlacklisted)
        }
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
