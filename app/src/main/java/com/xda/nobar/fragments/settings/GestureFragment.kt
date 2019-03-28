package com.xda.nobar.fragments.settings

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.SwitchPreference
import com.xda.nobar.R
import com.xda.nobar.activities.selectors.AppLaunchSelectActivity
import com.xda.nobar.activities.selectors.BaseAppSelectActivity
import com.xda.nobar.activities.selectors.IntentSelectorActivity
import com.xda.nobar.activities.selectors.ShortcutSelectActivity
import com.xda.nobar.adapters.info.ShortcutInfo
import com.xda.nobar.prefs.SectionableListPreference
import com.xda.nobar.util.PrefManager
import com.xda.nobar.util.actionHolder
import com.xda.nobar.util.dpAsPx
import com.xda.nobar.util.minPillHeightPx
import java.util.*

/**
 * Gesture preferences
 */
@SuppressLint("RestrictedApi")
class GestureFragment : BasePrefFragment(), SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        const val SECTION_GESTURES = "section_gestures"

        const val REQ_APP = 10
        const val REQ_INTENT = 11
        const val REQ_SHORTCUT = 12
    }

    override val resId = R.xml.prefs_gestures

    private val listPrefs = ArrayList<SectionableListPreference>()
    private val actionHolder by lazy { activity!!.actionHolder }

    private val sectionedCategory by lazy { findPreference<PreferenceCategory>(SECTION_GESTURES)!! }
    private val swipeUp by lazy { findPreference<Preference>(actionHolder.actionUp)!! }
    private val swipeUpHold by lazy { findPreference<Preference>(actionHolder.actionUpHold)!! }
    private val sectionedPill by lazy { findPreference<SwitchPreference>(PrefManager.SECTIONED_PILL)!! }

    private val sectionedPillListener = Preference.OnPreferenceChangeListener { _, newValue ->
        val new = newValue.toString().toBoolean()

        updateSplitPill(new)

        if (new) {
            refreshListPrefs()

            updateSummaries()

            AlertDialog.Builder(activity)
                    .setTitle(R.string.use_recommended_settings)
                    .setView(R.layout.use_recommended_settings_dialog_message_view)
                    .setPositiveButton(android.R.string.yes) { _, _ -> setSectionedSettings() }
                    .setNegativeButton(R.string.no, null)
                    .show()
        } else {
            AlertDialog.Builder(activity)
                    .setTitle(R.string.back_to_default)
                    .setMessage(R.string.back_to_default_desc)
                    .setPositiveButton(android.R.string.yes) { _, _ -> resetSectionedSettings() }
                    .setNegativeButton(R.string.no, null)
                    .show()

            refreshListPrefs()
        }

        true
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        refreshListPrefs()

        removeNougatActionsIfNeeded()
        removeRootActionsIfNeeded()
    }

    override fun onResume() {
        super.onResume()

        activity?.title = resources.getText(R.string.gestures)

        sectionedPill.onPreferenceChangeListener = sectionedPillListener

        sectionedCategory.isVisible = sectionedPill.isChecked
        swipeUp.isVisible = !sectionedPill.isChecked
        swipeUpHold.isVisible = !sectionedPill.isChecked

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

                    if (res < 1) {
                        saveValue(actionHolder.typeNoAction.toString())
                    } else {
                        saveValueWithoutListener(actionHolder.premTypeIntent.toString())
                        updateIntentSummary(key, res)
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

    private fun refreshListPrefs() {
        listPrefs.clear()
        listPrefs.addAll(getAllListPrefs())

        listPrefs.forEach {
            it.setOnPreferenceChangeListener { _, newValue ->
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

                    startActivityForResult(intent, REQ_APP)
                } else if (newValue == actionHolder.premTypeIntent.toString()) {
                    val intent = Intent(activity, IntentSelectorActivity::class.java)

                    intent.putExtra(BaseAppSelectActivity.EXTRA_KEY, it.key)

                    startActivityForResult(intent, REQ_INTENT)
                } else if (newValue == actionHolder.premTypeLaunchShortcut.toString()) {
                    val intent = Intent(activity, ShortcutSelectActivity::class.java)

                    intent.putExtra(BaseAppSelectActivity.EXTRA_KEY, it.key)

                    startActivityForResult(intent, REQ_SHORTCUT)
                }

                true
            }
        }
    }

    private fun setSectionedSettings() {
        preferenceManager.sharedPreferences.edit().apply {
            putBoolean(PrefManager.USE_PIXELS_WIDTH, false)
            putBoolean(PrefManager.USE_PIXELS_HEIGHT, true)
            putBoolean(PrefManager.USE_PIXELS_Y, false)
            putBoolean(PrefManager.LARGER_HITBOX, false)
            putBoolean(PrefManager.HIDE_IN_FULLSCREEN, false)
            putBoolean(PrefManager.STATIC_PILL, true)
            putBoolean(PrefManager.HIDE_PILL_ON_KEYBOARD, false)
            putBoolean(PrefManager.AUTO_HIDE_PILL, false)

            putInt(PrefManager.CUSTOM_WIDTH_PERCENT, 1000)
            putInt(PrefManager.CUSTOM_HEIGHT, activity!!.minPillHeightPx + activity!!.dpAsPx(7))
            putInt(PrefManager.CUSTOM_Y_PERCENT, 0)
            putInt(PrefManager.PILL_CORNER_RADIUS, 0)
            putInt(PrefManager.PILL_BG, Color.TRANSPARENT)
            putInt(PrefManager.PILL_FG, Color.TRANSPARENT)
            putInt(PrefManager.ANIM_DURATION, 0)
            putInt(PrefManager.HOLD_TIME, 500)
        }.apply()

        updateSummaries()
    }

    private fun resetSectionedSettings() {
        preferenceManager.sharedPreferences.edit().apply {
            remove(PrefManager.USE_PIXELS_WIDTH)
            remove(PrefManager.USE_PIXELS_HEIGHT)
            remove(PrefManager.USE_PIXELS_Y)
            remove(PrefManager.LARGER_HITBOX)
            remove(PrefManager.HIDE_IN_FULLSCREEN)
            remove(PrefManager.STATIC_PILL)
            remove(PrefManager.HIDE_PILL_ON_KEYBOARD)
            remove(PrefManager.AUTO_HIDE_PILL)

            remove(PrefManager.CUSTOM_WIDTH_PERCENT)
            remove(PrefManager.CUSTOM_HEIGHT)
            remove(PrefManager.CUSTOM_Y_PERCENT)
            remove(PrefManager.PILL_CORNER_RADIUS)
            remove(PrefManager.PILL_BG)
            remove(PrefManager.PILL_FG)
            remove(PrefManager.ANIM_DURATION)
            remove(PrefManager.HOLD_TIME)
        }.apply()

        updateSummaries()
    }

    private fun updateActivityLaunchSummary(key: String, activityName: String) {
        findPreference<Preference?>(key)?.apply {
            summary = String.format(
                    Locale.getDefault(),
                    resources.getString(R.string.prem_launch_activity),
                    activityName
            )
        }
    }

    private fun updateAppLaunchSummary(key: String, appName: String) {
        findPreference<Preference?>(key)?.apply {
            summary = String.format(
                    Locale.getDefault(),
                    resources.getString(R.string.prem_launch_app),
                    appName
            )
        }
    }

    private fun updateIntentSummary(key: String, res: Int) {
        if (res < 1) return

        findPreference<Preference?>(key)?.apply {
            summary = try {
                String.format(
                        Locale.getDefault(),
                        resources.getString(R.string.prem_intent),
                        resources.getString(res)
                )
            } catch (e: Exception) {
                resources.getString(R.string.prem_intent_no_format)
            }
        }
    }

    private fun updateShortcutSummary(key: String, shortcut: ShortcutInfo) {
        findPreference<Preference?>(key)?.apply {
            summary = String.format(
                    Locale.getDefault(),
                    resources.getString(R.string.prem_launch_shortcut),
                    shortcut.label
            )
        }
    }

    private fun updateSummaries() {
        listPrefs.forEach {
            it.updateSummary(it.getSavedValue())

            if (it.getSavedValue() == actionHolder.premTypeLaunchApp.toString() || it.getSavedValue() == actionHolder.premTypeLaunchActivity.toString()) {
                val forActivity = it.getSavedValue() == actionHolder.premTypeLaunchActivity.toString()
                val packageInfo = (if (forActivity) prefManager.getActivity(it.key) else prefManager.getPackage(it.key)) ?: return@forEach
                val disp = prefManager.getDisplayName(it.key) ?: packageInfo.split("/")[0]

                if (forActivity) {
                    updateActivityLaunchSummary(it.key, disp)
                } else {
                    updateAppLaunchSummary(it.key, disp)
                }
            } else if (it.getSavedValue() == actionHolder.premTypeIntent.toString()) {
                val res = prefManager.getIntentKey(it.key)
                updateIntentSummary(it.key, res)
            } else if (it.getSavedValue() == actionHolder.premTypeLaunchShortcut.toString()) {
                val shortcut = prefManager.getShortcut(it.key) ?: return
                updateShortcutSummary(it.key, shortcut)
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
        if (!prefManager.useRoot) {
            listPrefs.forEach {
                val actions = resources.getStringArray(R.array.root_action_values)
                it.removeItemsByValue(actions)
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

    private fun updateSplitPill(enabled: Boolean) {
        sectionedCategory.isVisible = enabled
        swipeUp.isVisible = !enabled
        swipeUpHold.isVisible = !enabled
    }
}