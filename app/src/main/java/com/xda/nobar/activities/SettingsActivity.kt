package com.xda.nobar.activities

import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.*
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.jaredrummler.android.colorpicker.ColorPreference
import com.xda.nobar.R
import com.xda.nobar.util.Utils
import com.zacharee1.sliderpreferenceembedded.SliderPreferenceEmbedded

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
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            addPreferencesFromResource(R.xml.prefs_gestures)

            addNougatActionsIfAvail()
            addPremiumActionsIfAvail()
            addRootActionsIfAvail()
//            addOHMIfAvail()

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

        private fun updateSummaries() {
            for (i in 0 until preferenceScreen.preferenceCount) {
                val pref = preferenceScreen.getPreference(i)

                if (pref is PreferenceCategory) {
                    for (j in 0 until pref.preferenceCount) {
                        val child = pref.getPreference(j)

                        if (child is ListPreference) {
                            child.summary = child.entry
                        }
                    }
                }
            }
        }

        private fun addNougatActionsIfAvail() {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                for (i in 0 until preferenceScreen.preferenceCount) {
                    val pref = preferenceScreen.getPreference(i)

                    if (pref is PreferenceCategory) {
                        for (j in 0 until pref.preferenceCount) {
                            val child = pref.getPreference(j)

                            if (child is ListPreference) {
                                val currentEntries = child.entries.clone()
                                val currentValues = child.entryValues.clone()

                                val nougatEntries = resources.getStringArray(R.array.nougat_action_names)
                                val nougatValues = resources.getStringArray(R.array.nougat_action_values)

                                child.entries = currentEntries.plus(nougatEntries)
                                child.entryValues = currentValues.plus(nougatValues)
                            }
                        }
                    }
                }
            }
        }

        private fun addPremiumActionsIfAvail() {
            for (i in 0 until preferenceScreen.preferenceCount) {
                val pref = preferenceScreen.getPreference(i)

                if (pref is PreferenceCategory) {
                    for (j in 0 until pref.preferenceCount) {
                        val child = pref.getPreference(j)

                        if (child is ListPreference) {
                            val currentEntries = child.entries.clone()
                            val currentValues = child.entryValues.clone()

                            val premiumEntries = resources.getStringArray(R.array.premium_action_names)
                            val premiumValues = resources.getStringArray(R.array.premium_action_values)

                            child.entries = currentEntries.plus(premiumEntries)
                            child.entryValues = currentValues.plus(premiumValues)
                        }
                    }
                }
            }
        }

        private fun addOHMIfAvail() {
            try {
                activity.packageManager.getPackageInfo("com.xda.onehandedmode", PackageManager.GET_META_DATA)

                for (i in 0 until preferenceScreen.preferenceCount) {
                    val pref = preferenceScreen.getPreference(i)

                    if (pref is PreferenceCategory) {
                        for (j in 0 until pref.preferenceCount) {
                            val child = pref.getPreference(j)

                            if (child is ListPreference) {
                                val currentEntries = child.entries.clone()
                                val currentValues = child.entryValues.clone()

                                val ohmEntries = resources.getStringArray(R.array.ohm_action_names)
                                val ohmValues = resources.getStringArray(R.array.ohm_action_values)

                                child.entries = currentEntries.plus(ohmEntries)
                                child.entryValues = currentValues.plus(ohmValues)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun addRootActionsIfAvail() {
            if (Utils.shouldUseRootCommands(activity)) {
                for (i in 0 until preferenceScreen.preferenceCount) {
                    val pref = preferenceScreen.getPreference(i)

                    if (pref is PreferenceCategory) {
                        for (j in 0 until pref.preferenceCount) {
                            val child = pref.getPreference(j)

                            if (child is ListPreference) {
                                val currentEntries = child.entries.clone()
                                val currentValues = child.entryValues.clone()

                                val premiumEntries = resources.getStringArray(R.array.root_action_names)
                                val premiumValues = resources.getStringArray(R.array.root_action_values)

                                child.entries = currentEntries.plus(premiumEntries)
                                child.entryValues = currentValues.plus(premiumValues)
                            }
                        }
                    }
                }
            }
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
            val screenSize = Utils.getRealScreenSize(activity)

            val width = findPreference("custom_width") as SliderPreferenceEmbedded
            val height = findPreference("custom_height") as SliderPreferenceEmbedded
            val posY = findPreference("custom_y") as SliderPreferenceEmbedded
            val pillColor = findPreference("pill_bg") as ColorPreference
            val pillBorderColor = findPreference("pill_fg") as ColorPreference
            val posX = findPreference("custom_x") as SliderPreferenceEmbedded

            width.seekBar.min = Utils.dpAsPx(activity, 10)
            height.seekBar.min = Utils.dpAsPx(activity, 5)
            posY.seekBar.min = 0
            posX.seekBar.min = -(Utils.getRealScreenSize(activity).x.toFloat() / 2f - Utils.getCustomWidth(activity).toFloat() / 2f).toInt()

            width.setDefaultValue(resources.getDimensionPixelSize(R.dimen.pill_width))
            height.setDefaultValue(resources.getDimensionPixelSize(R.dimen.pill_height))
            posY.setDefaultValue(Utils.getDefaultY(activity))
            pillColor.setDefaultValue(Utils.getDefaultPillBGColor(activity))
            pillBorderColor.setDefaultValue(Utils.getDefaultPillFGColor(activity))

            pillColor.saveValue(Utils.getPillBGColor(activity))
            pillBorderColor.saveValue(Utils.getPillFGColor(activity))

            width.seekBar.max = screenSize.x
            height.seekBar.max = Utils.dpAsPx(activity, 50)
            posY.seekBar.max = Utils.dpAsPx(activity, 70)
            posX.seekBar.max = -posX.seekBar.min
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
