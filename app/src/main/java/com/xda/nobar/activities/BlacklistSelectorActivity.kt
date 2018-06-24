package com.xda.nobar.activities

import android.Manifest
import android.app.Activity
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import com.xda.nobar.R
import com.xda.nobar.interfaces.OnAppSelectedListener
import com.xda.nobar.util.AppInfo
import com.xda.nobar.util.AppSelectAdapter
import com.xda.nobar.util.Utils
import java.util.*
import kotlin.collections.ArrayList

/**
 * Activity to manage which apps should be blacklisted for a certain function
 * Use the constants in the companion object to specify the function
 */
class BlacklistSelectorActivity : BaseAppSelectActivity() {
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
                Utils.loadBlacklistedBarPackages(this, currentlyBlacklisted)
            }
            FOR_NAV -> {
                title = resources.getText(R.string.nav_blacklist)
                Utils.loadBlacklistedNavPackages(this, currentlyBlacklisted)
            }
            FOR_IMM -> {
                title = resources.getText(R.string.imm_blacklist)
                Utils.loadBlacklistedImmPackages(this, currentlyBlacklisted)
            }
            FOR_WIN -> {
                title = resources.getText(R.string.fix_for_other_windows)
                Utils.loadOtherWindowApps(this, currentlyBlacklisted)
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

    override fun loadAppInfo(info: Any): AppInfo? {
        info as ApplicationInfo

        val appInfo = AppInfo(info.packageName,
                "",
                info.loadLabel(packageManager).toString(),
                info.loadIcon(packageManager), currentlyBlacklisted.contains(info.packageName))

        return if (which == FOR_WIN) {
            val perms = packageManager.getPackageInfo(info.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions
            if (perms != null && perms.contains(Manifest.permission.SYSTEM_ALERT_WINDOW)) appInfo
            else null
        } else appInfo
    }

    override fun onDestroy() {
        super.onDestroy()

        when (which) {
            FOR_NAV -> Utils.saveBlacklistedNavPackageList(this, currentlyBlacklisted)
            FOR_BAR -> Utils.saveBlacklistedBarPackages(this, currentlyBlacklisted)
            FOR_IMM -> Utils.saveBlacklistedImmPackages(this, currentlyBlacklisted)
            FOR_WIN -> Utils.saveOtherWindowApps(this, currentlyBlacklisted)
        }
    }
}
