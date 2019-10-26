package com.xda.nobar.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import androidx.appcompat.view.ContextThemeWrapper
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import com.xda.nobar.R
import java.util.*
import kotlin.collections.ArrayList

class SettingsIndex private constructor(context: Context) : ContextWrapper(context) {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: SettingsIndex? = null

        @Synchronized
        fun getInstance(context: Context): SettingsIndex {
            return instance ?: run {
                instance = SettingsIndex(ContextThemeWrapper(context.applicationContext, R.style.AppTheme))
                instance!!
            }
        }
    }

    private val settings = ArrayList<SettingsItem>()
    private val preferenceManager = PreferenceManager(this)

    init {
        parsePreferences()
    }

    fun search(text: CharSequence): List<SettingsItem> {
        val lowercase = text.toString().toLowerCase(Locale.getDefault())
        if (lowercase.isBlank()) return ArrayList()

        return settings.filter {
            it.key?.toLowerCase(Locale.getDefault())?.contains(lowercase) == true
                    || it.title?.toString()?.toLowerCase(Locale.getDefault())?.contains(lowercase) == true
                    || it.summary?.toString()?.toLowerCase(Locale.getDefault())?.contains(lowercase) == true
        }
    }

    private fun parsePreferences() {
        val gestureScreen = inflate(R.xml.prefs_gestures)
        val pillGestureScreen = inflate(R.xml.prefs_pill_gestures)
        val sideGestureScreen = inflate(R.xml.prefs_side_gestures)
        val appearanceScreen = inflate(R.xml.prefs_appearance)
        val pillAppearanceScreen = inflate(R.xml.prefs_pill_appearance)
        val sideAppearanceScreen = inflate(R.xml.prefs_side_appearance)
        val behaviorScreen = inflate(R.xml.prefs_behavior)
        val compatibilityScreen = inflate(R.xml.prefs_compatibility)
        val experimentalScreen = inflate(R.xml.prefs_experimental)
        val backupRestoreScreen = inflate(R.xml.prefs_backup_restore)

        recurseThroughGroup(gestureScreen, R.id.action_searchFragment_to_gesturesFragment)
        recurseThroughGroup(pillGestureScreen, R.id.action_searchFragment_to_pillGestureFragment2)
        recurseThroughGroup(sideGestureScreen, R.id.action_searchFragment_to_sideGestureFragment2)
        recurseThroughGroup(appearanceScreen, R.id.action_searchFragment_to_appearanceFragment)
        recurseThroughGroup(pillAppearanceScreen, R.id.action_searchFragment_to_pillAppearanceFragment2)
        recurseThroughGroup(sideAppearanceScreen, R.id.action_searchFragment_to_sideAppearanceFragment2)
        recurseThroughGroup(behaviorScreen, R.id.action_searchFragment_to_behaviorFragment)
        recurseThroughGroup(compatibilityScreen, R.id.action_searchFragment_to_compatibilityFragment)
        recurseThroughGroup(experimentalScreen, R.id.action_searchFragment_to_experimentalFragment)
        recurseThroughGroup(backupRestoreScreen, R.id.action_searchFragment_to_backupAndRestoreFragment)
    }

    private fun inflate(res: Int): PreferenceScreen {
        return preferenceManager.inflateFromResource(this@SettingsIndex, res, null)
    }

    private fun recurseThroughGroup(parent: PreferenceGroup, pageAction: Int) {
        for (i in 0 until parent.preferenceCount) {
            val child = parent.getPreference(i)

            if (child is PreferenceGroup) recurseThroughGroup(child, pageAction)
            else settings.add(SettingsItem(child, pageAction))
        }
    }

    class SettingsItem(val preference: Preference, val pageAction: Int) {
        val title: CharSequence?
            get() = preference.title
        val summary: CharSequence?
            get() = preference.summary
        val key: String?
            get() = preference.key
    }
}