package com.xda.nobar.fragments.settings.gestures

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import com.xda.nobar.R
import com.xda.nobar.activities.selectors.AppLaunchSelectActivity
import com.xda.nobar.activities.selectors.BaseAppSelectActivity
import com.xda.nobar.activities.selectors.IntentSelectorActivity
import com.xda.nobar.activities.selectors.ShortcutSelectActivity
import com.xda.nobar.adapters.info.ShortcutInfo
import com.xda.nobar.fragments.settings.BasePrefFragment
import com.xda.nobar.prefs.SectionableListPreference
import com.xda.nobar.util.actionHolder
import com.xda.nobar.util.isSu
import java.util.*

/**
 * Gesture preferences
 */
@SuppressLint("RestrictedApi")
open class GestureFragment : BasePrefFragment(), SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        const val REQ_APP = 10
        const val REQ_INTENT = 11
        const val REQ_SHORTCUT = 12
    }

    override val resId = R.xml.prefs_gestures
    override val activityTitle by lazy { resources.getText(R.string.gestures) }

    internal val listPrefs = ArrayList<SectionableListPreference>()
    internal val actionHolder by lazy { requireActivity().actionHolder }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        refreshListPrefs()

        removeNougatActionsIfNeeded()
        removeRootActionsIfNeeded()
    }

    override fun onResume() {
        super.onResume()

        refreshListPrefs()
        updateSummaries()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        val map = HashMap<String, Int>()
        prefManager.getActionsList(map)

        if (map.keys.contains(key)) updateSummaries()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQ_APP -> {
                val key = data?.getStringExtra(BaseAppSelectActivity.EXTRA_KEY) ?: return
                val appName = data.getStringExtra(AppLaunchSelectActivity.EXTRA_RESULT_DISPLAY_NAME)
                val forActivity = data.getBooleanExtra(AppLaunchSelectActivity.FOR_ACTIVITY_SELECT, false)

                (findPreference<SectionableListPreference?>(key))?.apply {
                    val pack = if (forActivity) prefManager.getActivity(key) else prefManager.getPackage(key)
                    if (pack == null) {
                        saveValue(actionHolder.typeNoAction.toString())
                    } else {
                        saveValueWithoutListener((if (forActivity) actionHolder.premTypeLaunchActivity else actionHolder.premTypeLaunchApp).toString())

                        if (forActivity) {
                            updateActivityLaunchSummary(key, appName ?: return)
                        } else {
                            updateAppLaunchSummary(key, appName ?: return)
                        }
                    }
                }
            }
            REQ_INTENT -> {
                val key = data?.getStringExtra(BaseAppSelectActivity.EXTRA_KEY) ?: return

                (findPreference<SectionableListPreference?>(key))?.apply {
                    val res = prefManager.getIntentKey(key)

                    if (res < 0) {
                        saveValue(actionHolder.typeNoAction.toString())
                    } else {
                        saveValueWithoutListener(actionHolder.premTypeIntent.toString())
                        updateIntentSummary(key, IntentSelectorActivity.INTENTS[res]!!.res)
                    }
                }
            }
            REQ_SHORTCUT -> {
                val key = data?.getStringExtra(BaseAppSelectActivity.EXTRA_KEY) ?: return

                (findPreference<SectionableListPreference?>(key))?.apply {
                    val shortcut = prefManager.getShortcut(key)

                    if (shortcut == null) {
                        saveValue(actionHolder.typeNoAction.toString())
                    } else {
                        saveValueWithoutListener(actionHolder.premTypeLaunchShortcut.toString())
                        updateShortcutSummary(key, shortcut)
                    }
                }
            }
        }
    }

    internal fun refreshListPrefs() {
        listPrefs.clear()
        listPrefs.addAll(getAllListPrefs())

        listPrefs.forEach {
            it.setOnPreferenceChangeListener { _, newValue ->
                if (activity?.isDestroyed == false) {
                    if (newValue == actionHolder.premTypeLaunchApp.toString() || newValue == actionHolder.premTypeLaunchActivity.toString()) {
                        val forActivity = newValue == actionHolder.premTypeLaunchActivity.toString()
                        val intent = Intent(activity, AppLaunchSelectActivity::class.java)

                        var pack = if (forActivity) prefManager.getActivity(it.key) else prefManager.getPackage(it.key)
                        var activity: String? = null
                        if (pack != null) {
                            activity = pack.split("/")[1]
                            pack = pack.split("/")[0]
                        }

                        intent.putExtra(BaseAppSelectActivity.EXTRA_KEY, it.key)
                        intent.putExtra(AppLaunchSelectActivity.CHECKED_PACKAGE, pack)
                        intent.putExtra(AppLaunchSelectActivity.CHECKED_ACTIVITY, activity)
                        intent.putExtra(AppLaunchSelectActivity.FOR_ACTIVITY_SELECT, forActivity)

                        startActivityForResult(intent,
                            REQ_APP
                        )
                    } else if (newValue == actionHolder.premTypeIntent.toString()) {
                        val intent = Intent(activity, IntentSelectorActivity::class.java)

                        intent.putExtra(BaseAppSelectActivity.EXTRA_KEY, it.key)

                        startActivityForResult(intent,
                            REQ_INTENT
                        )
                    } else if (newValue == actionHolder.premTypeLaunchShortcut.toString()) {
                        val intent = Intent(activity, ShortcutSelectActivity::class.java)

                        intent.putExtra(BaseAppSelectActivity.EXTRA_KEY, it.key)

                        startActivityForResult(intent,
                            REQ_SHORTCUT
                        )
                    }

                    true
                } else {
                    false
                }
            }
        }
    }

    private fun updateActivityLaunchSummary(key: String, activityName: String) {
        findPreference<Preference?>(key)?.apply {
            summary = resources.getString(R.string.prem_launch_activity, activityName)
        }
    }

    private fun updateAppLaunchSummary(key: String, appName: String) {
        findPreference<Preference?>(key)?.apply {
            summary = resources.getString(R.string.prem_launch_app, appName)
        }
    }

    private fun updateIntentSummary(key: String, res: Int?) {
        if (res == null || res < 1) return

        findPreference<Preference?>(key)?.apply {
            summary = try {
                resources.getString(R.string.prem_intent, resources.getString(res))
            } catch (e: Exception) {
                resources.getString(R.string.prem_intent_no_format)
            }
        }
    }

    private fun updateShortcutSummary(key: String, shortcut: ShortcutInfo) {
        findPreference<Preference?>(key)?.apply {
            summary = resources.getString(R.string.prem_launch_shortcut, shortcut.label)
        }
    }

    private fun updateKeycodeSummary(key: String, code: Int) {
        findPreference<Preference?>(key)?.apply {
            summary = resources.getString(R.string.root_send_keycode, code.toString())
        }
    }

    private fun updateDoubleKeycodeSummary(key: String, code: Int) {
        findPreference<Preference?>(key)?.apply {
            summary = resources.getString(R.string.root_send_double_keycode, code.toString())
        }
    }

    private fun updateLongKeycodeSummary(key: String, code: Int) {
        findPreference<Preference?>(key)?.apply {
            summary = resources.getString(R.string.root_send_long_keycode, code.toString())
        }
    }

    internal fun updateSummaries() {
        listPrefs.forEach {
            it.updateSummary(it.getSavedValue())

            when (it.getSavedValue()) {
                actionHolder.premTypeLaunchApp.toString(),
                actionHolder.premTypeLaunchActivity.toString() -> {
                    val forActivity = it.getSavedValue() == actionHolder.premTypeLaunchActivity.toString()
                    val packageInfo = (if (forActivity) prefManager.getActivity(it.key) else prefManager.getPackage(it.key)) ?: return@forEach
                    val disp = prefManager.getDisplayName(it.key) ?: packageInfo.split("/")[0]

                    if (forActivity) {
                        updateActivityLaunchSummary(it.key, disp)
                    } else {
                        updateAppLaunchSummary(it.key, disp)
                    }
                }
                actionHolder.premTypeIntent.toString() -> {
                    val res = prefManager.getIntentKey(it.key)
                    updateIntentSummary(it.key, IntentSelectorActivity.INTENTS[res]?.res)
                }
                actionHolder.premTypeLaunchShortcut.toString() -> {
                    val shortcut = prefManager.getShortcut(it.key) ?: return
                    updateShortcutSummary(it.key, shortcut)
                }
                actionHolder.typeRootKeycode.toString() -> {
                    val keycode = prefManager.getKeycode(it.key)
                    updateKeycodeSummary(it.key, keycode)
                }
                actionHolder.typeRootDoubleKeycode.toString() -> {
                    val keycode = prefManager.getKeycode(it.key)
                    updateDoubleKeycodeSummary(it.key, keycode)
                }
                actionHolder.typeRootLongKeycode.toString() -> {
                    val keycode = prefManager.getKeycode(it.key)
                    updateLongKeycodeSummary(it.key, keycode)
                }
            }
        }
    }

    private fun removeNougatActionsIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            listPrefs.forEach {
                val actions = resources.getStringArray(R.array.nougat_action_values)
                it.removeItemsByValue(actions)
            }
        }
    }

    private fun removeRootActionsIfNeeded() {
        if (!isSu) {
//            listPrefs.forEach {
//                val actions = resources.getStringArray(R.array.root_action_values)
//                it.removeItemsByValue(actions)
//            }

            listPrefs.forEach {
                it.removeSection(resources.getString(R.string.root).toLowerCase(Locale.getDefault()))
            }
        }
    }

    private fun getAllListPrefs(): ArrayList<SectionableListPreference> {
        val ret = ArrayList<SectionableListPreference>()

        for (i in 0 until preferenceScreen.preferenceCount) {
            val pref = preferenceScreen.getPreference(i)

            if (pref is PreferenceGroup) {
                for (j in 0 until pref.preferenceCount) {
                    val child = pref.getPreference(j)

                    if (child is SectionableListPreference) ret.add(child)
                    else if (child is PreferenceGroup) {
                        for (k in 0 until child.preferenceCount) {
                            val c = child.getPreference(k)

                            if (c is SectionableListPreference) ret.add(c)
                        }
                    }
                }
            }
        }

        return ret
    }
}