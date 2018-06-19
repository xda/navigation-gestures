package com.xda.nobar.activities

import android.app.Activity
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import com.xda.nobar.R
import com.xda.nobar.util.AppInfo
import com.xda.nobar.util.AppSelectAdapter
import com.xda.nobar.util.Utils
import java.util.*
import kotlin.collections.ArrayList

class BlacklistSelectorActivity : BaseAppSelectActivity() {
    companion object {
        const val EXTRA_WHICH = "which"

        const val FOR_BAR = "bar"
        const val FOR_NAV = "nav"
        const val FOR_IMM = "imm"
    }

    private val currentlyBlacklisted = ArrayList<String>()

    override val adapter = AppSelectAdapter(false, true) { info ->
        if (info.isChecked) currentlyBlacklisted.add(info.packageName)
        else currentlyBlacklisted.removeAll(Collections.singleton(info.packageName))
    }

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

    override fun loadAppInfo(info: Any): AppInfo {
        info as ApplicationInfo
        return AppInfo(info.packageName,
                "",
                info.loadLabel(packageManager).toString(),
                info.loadIcon(packageManager), currentlyBlacklisted.contains(info.packageName))
    }

    override fun onDestroy() {
        super.onDestroy()

        when (which) {
            FOR_NAV -> Utils.saveBlacklistedNavPackageList(this, currentlyBlacklisted)
            FOR_BAR -> Utils.saveBlacklistedBarPackages(this, currentlyBlacklisted)
            FOR_IMM -> Utils.saveBlacklistedImmPackages(this, currentlyBlacklisted)
        }
    }
}
