package com.xda.nobar.activities

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceCategory
import android.preference.PreferenceFragment
import android.preference.SwitchPreference
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.jaredrummler.android.colorpicker.ColorPreference
import com.pavelsikun.seekbarpreference.SeekBarPreference
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.prefs.SectionableListPreference
import com.xda.nobar.util.Utils
import java.util.*

/**
 * The configuration activity
 */
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        fragmentManager?.beginTransaction()?.replace(R.id.content, MainFragment())?.addToBackStack("main")?.commit()
    }

    override fun onResume() {
        super.onResume()

        (application as App).refreshPremium()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                handleBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        handleBackPressed()
    }

    private fun handleBackPressed() {
        if (fragmentManager != null) {
            if (fragmentManager.backStackEntryCount > 1) {
                fragmentManager.popBackStack()
            } else {
                finish()
            }
        } else {
            finish()
        }
    }

    /**
     * Main settings page
     */
    class MainFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            addPreferencesFromResource(R.xml.prefs_main)
        }

        override fun onResume() {
            super.onResume()

            activity.title = resources.getText(R.string.settings)

            setListeners()
        }

        private fun setListeners() {
            val listener = Preference.OnPreferenceClickListener {
                val whichFrag = when (it.key) {
                    "gestures" -> GestureFragment()
                    "appearance" -> AppearanceFragment()
                    "behavior" -> BehaviorFragment()
                    "compatibility" -> CompatibilityFragment()
                    else -> null
                }

                if (whichFrag != null) fragmentManager?.beginTransaction()?.replace(R.id.content, whichFrag, it.key)?.addToBackStack(it.key)?.commit()
                true
            }

            findPreference("gestures").onPreferenceClickListener = listener
            findPreference("appearance").onPreferenceClickListener = listener
            findPreference("behavior").onPreferenceClickListener = listener
            findPreference("compatibility").onPreferenceClickListener = listener
        }
    }

    /**
     * Gesture preferences
     */
    class GestureFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {
        private val listPrefs = ArrayList<SectionableListPreference>()

        private lateinit var app: App

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            app = activity.application as App

            addPreferencesFromResource(R.xml.prefs_gestures)

            listPrefs.addAll(getAllListPrefs())

            removeNougatActionsIfNeeded()
            removeRootActionsIfNeeded()
            setListeners()

            preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onResume() {
            super.onResume()

            activity.title = resources.getText(R.string.gestures)

            updateSummaries()
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            val map = HashMap<String, Int>()
            Utils.getActionList(activity, map)

            if (map.keys.contains(key)) updateSummaries()
        }

        override fun onDestroy() {
            super.onDestroy()
            preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            if (requestCode == 10) {
                val key = data?.getStringExtra(AppLaunchSelectActivity.EXTRA_KEY)
                val appName = data?.getStringExtra(AppLaunchSelectActivity.EXTRA_RESULT_DISPLAY_NAME)

                when (resultCode) {
                    Activity.RESULT_OK -> {
                        updateAppLaunchSummary(key ?: return, appName ?: return)
                    }

                    Activity.RESULT_CANCELED -> {
                        listPrefs.forEach {
                            if (it.key == key) {
                                val pack = preferenceManager.sharedPreferences.getString("${key}_package", null)
                                if (pack == null) {
                                    it.saveValue(app.typeNoAction.toString())
                                } else {
                                    it.saveValueWithoutListener(app.premTypeLaunchApp.toString())
                                }
                            }
                        }
                    }
                }
            }
        }

        private fun updateAppLaunchSummary(key: String, appName: String) {
            listPrefs.forEach {
                if (key == it.key) {
                    it.summary = String.format(Locale.getDefault(), it.summary.toString(), appName)
                    return@forEach
                }
            }
        }

        private fun updateSummaries() {
            listPrefs.forEach {
                it.updateSummary(it.getSavedValue())

                if (it.getSavedValue() == app.premTypeLaunchApp.toString()) {
                    val packageInfo = preferenceManager.sharedPreferences.getString("${it.key}_package", null) ?: return

                    it.summary = String.format(Locale.getDefault(),
                            resources.getString(R.string.prem_launch_app),
                            activity.packageManager.getApplicationLabel(
                                            activity.packageManager.getApplicationInfo(packageInfo.split("/")[0], 0)))
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
            if (!Utils.shouldUseRootCommands(activity)) {
                listPrefs.forEach {
                    val actions = resources.getStringArray(R.array.root_action_values)
                    it.removeItemsByValue(actions)
                }
            }
        }

        private fun setListeners() {
            listPrefs.forEach {
                it.setOnPreferenceChangeListener { _, newValue ->
                    if (newValue?.toString() == app.premTypeLaunchApp.toString()) {
                        val intent = Intent(activity, AppLaunchSelectActivity::class.java)
                        intent.putExtra(AppLaunchSelectActivity.EXTRA_KEY, it.key)

                        startActivityForResult(intent, 10)
                    }
                    true
                }
            }
        }

        private fun getAllListPrefs(): ArrayList<SectionableListPreference> {
            val ret = ArrayList<SectionableListPreference>()

            for (i in 0 until preferenceScreen.preferenceCount) {
                val pref = preferenceScreen.getPreference(i)

                if (pref is PreferenceCategory) {
                    for (j in 0 until pref.preferenceCount) {
                        val child = pref.getPreference(j)

                        if (child is SectionableListPreference) ret.add(child)
                    }
                }
            }

            return ret
        }
    }

    /**
     * Appearance settings
     */
    class AppearanceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            addPreferencesFromResource(R.xml.prefs_appearance)
        }

        override fun onResume() {
            super.onResume()

            activity.title = resources.getText(R.string.appearance)
            setListeners()
        }

        private fun setListeners() {
            val height = findPreference("custom_height") as SeekBarPreference
            val posY = findPreference("custom_y") as SeekBarPreference
            val pillColor = findPreference("pill_bg") as ColorPreference
            val pillBorderColor = findPreference("pill_fg") as ColorPreference

            height.minValue = Utils.dpAsPx(activity, 5)
            posY.minValue = 0

            height.maxValue = Utils.dpAsPx(activity, 50)
            posY.maxValue = Utils.dpAsPx(activity, 70)

            height.setDefaultValue(resources.getDimensionPixelSize(R.dimen.pill_height))
            posY.setDefaultValue(Utils.getDefaultY(activity))

            pillColor.setDefaultValue(Utils.getDefaultPillBGColor())
            pillBorderColor.setDefaultValue(Utils.getDefaultPillFGColor())

            pillColor.saveValue(Utils.getPillBGColor(activity))
            pillBorderColor.saveValue(Utils.getPillFGColor(activity))
        }
    }

    /**
     * Behavior settings
     */
    class BehaviorFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            addPreferencesFromResource(R.xml.prefs_behavior)
        }

        override fun onResume() {
            super.onResume()

            activity.title = resources.getText(R.string.behavior)
        }
    }

    /**
     * Compatibility settings
     */
    class CompatibilityFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            addPreferencesFromResource(R.xml.prefs_compatibility)
        }

        override fun onResume() {
            super.onResume()

            activity.title = resources.getText(R.string.compatibility)

            setUpListeners()
        }
        
        private fun setUpListeners() {
            val rot270Fix = findPreference("rot270_fix") as SwitchPreference
            val tabletMode = findPreference("tablet_mode") as SwitchPreference

            if (rot270Fix.isChecked) {
                tabletMode.isChecked = false
                tabletMode.isEnabled = false
            }

            if (tabletMode.isChecked) {
                rot270Fix.isChecked = false
                rot270Fix.isEnabled = false
            }

            rot270Fix.setOnPreferenceChangeListener { _, newValue -> 
                val enabled = newValue.toString().toBoolean()
                
                tabletMode.isEnabled = !enabled
                tabletMode.isChecked = if (enabled) false else tabletMode.isChecked
                
                true
            }

            tabletMode.setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue.toString().toBoolean()

                rot270Fix.isEnabled = !enabled
                rot270Fix.isChecked = if (enabled) false else rot270Fix.isChecked

                true
            }
        }
    }
}
