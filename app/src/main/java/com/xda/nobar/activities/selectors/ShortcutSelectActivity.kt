package com.xda.nobar.activities.selectors

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.widget.Toast
import com.xda.nobar.R
import com.xda.nobar.adapters.ShortcutSelectAdapter
import com.xda.nobar.adapters.info.ShortcutInfo
import com.xda.nobar.interfaces.OnShortcutSelectedListener
import com.xda.nobar.util.prefManager

class ShortcutSelectActivity : BaseAppSelectActivity<ActivityInfo, ShortcutInfo>() {
    companion object {
        const val CHECKED_INFO = "checked_info"

        private const val CODE_CONFIG = 1000
    }

    private var selectedInfo: ShortcutInfo? = null

    override val adapter = ShortcutSelectAdapter(OnShortcutSelectedListener {
        selectedInfo = it

        val configIntent = Intent(Intent.ACTION_CREATE_SHORTCUT)
        configIntent.`package` = it.packageName
        configIntent.component = ComponentName(it.packageName, it.clazz)

        try {
            startActivityForResult(configIntent, CODE_CONFIG)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.unable_to_configure_shortcut, Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    })

    override fun canRun() = intent != null && key != null

    override fun loadAppList(): ArrayList<ActivityInfo> {
        return ArrayList(
                packageManager.queryIntentActivities(
                        Intent(Intent.ACTION_CREATE_SHORTCUT),
                        PackageManager.GET_RESOLVED_FILTER
                ).map { it.activityInfo }
        )
    }

    override fun loadAppInfo(info: ActivityInfo): ShortcutInfo? {
        return ShortcutInfo(
                info.name,
                info.packageName,
                info.iconResource,
                info.loadLabel(packageManager).toString(),
                info.name == prefManager.getShortcut(key)?.clazz
        )
    }

    override fun filter(query: String): ArrayList<ShortcutInfo> {
        val lowercase = query.toLowerCase()
        val filteredList = ArrayList<ShortcutInfo>()

        ArrayList(origAppSet).forEach {
            val title = it.label.toLowerCase()
            val summary = it.packageName + "/" + it.clazz

            if (title.contains(lowercase) || summary.contains(lowercase)) {
                filteredList.add(it)
            }
        }

        return filteredList
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            CODE_CONFIG -> {
                if (resultCode == Activity.RESULT_OK) {
                    val intent = data?.getParcelableExtra<Intent>(Intent.EXTRA_SHORTCUT_INTENT)

                    if (intent != null) {
                        val name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME)

                        if (name != null) selectedInfo?.label = name
                        selectedInfo?.intent = intent

                        prefManager.putShortcut(key!!, selectedInfo)

                        val result = Intent()
                        result.putExtras(intent)
                        result.putExtras(this.intent)
                        result.putExtra(CHECKED_INFO, selectedInfo)

                        setResult(Activity.RESULT_OK, result)
                    }
                }
            }
        }

        finish()
    }
}
