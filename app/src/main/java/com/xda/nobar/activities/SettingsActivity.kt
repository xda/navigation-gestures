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
                    "other" -> OtherFragment()
                    else -> null
                }

                if (whichFrag != null) fragmentManager?.beginTransaction()?.replace(R.id.content, whichFrag, it.key)?.addToBackStack(it.key)?.commit()
                true
            }

            findPreference("gestures").onPreferenceClickListener = listener
            findPreference("appearance").onPreferenceClickListener = listener
            findPreference("behavior").onPreferenceClickListener = listener
            findPreference("other").onPreferenceClickListener = listener
        }
    }

    /**
     * Gesture preferences
     */
    class GestureFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            addPreferencesFromResource(R.xml.prefs_gestures)

//            addOHMIfAvail()

            preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onResume() {
            super.onResume()

            activity.title = resources.getText(R.string.gestures)

            addNougatActionsIfAvail()
            addPremiumActionsIfAvail()
            addRootActionsIfAvail()

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
            val pillCornerRadius = findPreference("pill_corner_radius") as SliderPreferenceEmbedded
            val posX = findPreference("custom_x") as SliderPreferenceEmbedded

            val resetW = findPreference("reset_width")
            val resetH = findPreference("reset_height")
            val resetY = findPreference("reset_pos_y")
            val resetPill = findPreference("reset_pill_bg")
            val resetPillBorder = findPreference("reset_pill_fg")
            val resetCorners = findPreference("reset_pill_corner_radius")
            val resetX = findPreference("reset_pos_x")
            val defaults = findPreference("defaults")

            width.seekBar.min = Utils.dpAsPx(activity, 20)
            height.seekBar.min = Utils.dpAsPx(activity, 10)
            posY.seekBar.min = 0
            posX.seekBar.min = -(Utils.getRealScreenSize(activity).x.toFloat() / 2f - Utils.getCustomWidth(activity).toFloat() / 2f).toInt()

            width.seekBar.max = screenSize.x
            height.seekBar.max = Utils.dpAsPx(activity, 50)
            posY.seekBar.max = Utils.dpAsPx(activity, 70)
            posX.seekBar.max = -posX.seekBar.min

            width.seekBar.progress = Utils.getCustomWidth(activity)
            height.seekBar.progress = Utils.getCustomHeight(activity)
            posY.seekBar.progress = Utils.getHomeY(activity)
            posX.seekBar.progress = Utils.getHomeX(activity)
            pillCornerRadius.seekBar.progress = Utils.getPillCornerRadiusInDp(activity)

            pillColor.saveValue(Utils.getPillBGColor(activity))
            pillBorderColor.saveValue(Utils.getPillFGColor(activity))
            
            val resetListener = Preference.OnPreferenceClickListener {
                preferenceManager.sharedPreferences.edit().apply {
                    if (it.key == "reset_width" || it.key == "defaults") remove("custom_width")
                    if (it.key == "reset_height" || it.key == "defaults") remove("custom_height")
                    if (it.key == "reset_pos_y" || it.key == "defaults") remove("custom_y")
                    if (it.key == "reset_pos_x" || it.key == "defaults") remove("custom_x")
                    if (it.key == "reset_pill_bg" || it.key == "defaults") remove("pill_bg")
                    if (it.key == "reset_pill_fg" || it.key == "defaults") remove("pill_fg")
                    if (it.key == "reset_pill_corner_radius" || it.key == "defaults") remove("pill_corner_radius")
                    apply()
                }
                width.seekBar.progress = Utils.getCustomWidth(activity)
                height.seekBar.progress = Utils.getCustomHeight(activity)
                posY.seekBar.progress = Utils.getHomeY(activity)
                posX.seekBar.progress = Utils.getHomeX(activity)
                pillCornerRadius.seekBar.progress = Utils.getPillCornerRadiusInDp(activity)
                pillColor.saveValue(Utils.getPillBGColor(activity))
                pillBorderColor.saveValue(Utils.getPillFGColor(activity))
                true
            }

            resetW.onPreferenceClickListener = resetListener
            resetH.onPreferenceClickListener = resetListener
            resetY.onPreferenceClickListener = resetListener
            resetX.onPreferenceClickListener = resetListener
            resetPill.onPreferenceClickListener = resetListener
            resetPillBorder.onPreferenceClickListener = resetListener
            resetCorners.onPreferenceClickListener = resetListener
            defaults.onPreferenceClickListener = resetListener
        }
    }

    class BehaviorFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            addPreferencesFromResource(R.xml.prefs_behavior)
        }

        override fun onResume() {
            super.onResume()

            activity.title = resources.getText(R.string.behavior)

            setListeners()
        }

        private fun setListeners() {
            val hold = findPreference("hold_time") as SliderPreferenceEmbedded
            val vib = findPreference("vibration_duration") as SliderPreferenceEmbedded
            val anim = findPreference("anim_duration") as SliderPreferenceEmbedded

            val resetHold = findPreference("reset_hold_time")
            val resetVib = findPreference("reset_vibration_duration")
            val resetAnim = findPreference("reset_anim_duration")
            val resetAll = findPreference("reset_all")

            val listener = Preference.OnPreferenceClickListener {
                preferenceManager.sharedPreferences.edit().apply {
                    if (it.key == "reset_hold_time" || it.key == "reset_all") remove("hold_time")
                    if (it.key == "reset_vibration_duration" || it.key == "reset_all") remove("vibration_duration")
                    if (it.key == "reset_anim_duration" || it.key == "reset_all") remove("anim_duration")

                    hold.seekBar.resetProgress()
                    vib.seekBar.resetProgress()
                    anim.seekBar.resetProgress()
                    apply()
                }
                true
            }

            resetHold.onPreferenceClickListener = listener
            resetVib.onPreferenceClickListener = listener
            resetAnim.onPreferenceClickListener = listener
            resetAll.onPreferenceClickListener = listener
        }
    }

    class OtherFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            addPreferencesFromResource(R.xml.prefs_other)
        }

        override fun onResume() {
            super.onResume()

            activity.title = resources.getText(R.string.other)

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
