package com.xda.nobar.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import androidx.appcompat.view.ContextThemeWrapper
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceManager
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
        val gestureScreen = preferenceManager.inflateFromResource(this@SettingsIndex, R.xml.prefs_gestures, null)
        val appearanceScreen = preferenceManager.inflateFromResource(this@SettingsIndex, R.xml.prefs_appearance, null)
        val behaviorScreen = preferenceManager.inflateFromResource(this@SettingsIndex, R.xml.prefs_behavior, null)
        val compatibilityScreen = preferenceManager.inflateFromResource(this@SettingsIndex, R.xml.prefs_compatibility, null)
        val experimentalScreen = preferenceManager.inflateFromResource(this@SettingsIndex, R.xml.prefs_experimental, null)
        val backupRestoreScreen = preferenceManager.inflateFromResource(this@SettingsIndex, R.xml.prefs_backup_restore, null)

        recurseThroughGroup(gestureScreen, R.id.action_searchFragment_to_gesturesFragment)
        recurseThroughGroup(appearanceScreen, R.id.action_searchFragment_to_appearanceFragment)
        recurseThroughGroup(behaviorScreen, R.id.action_searchFragment_to_behaviorFragment)
        recurseThroughGroup(compatibilityScreen, R.id.action_searchFragment_to_compatibilityFragment)
        recurseThroughGroup(experimentalScreen, R.id.action_searchFragment_to_experimentalFragment)
        recurseThroughGroup(backupRestoreScreen, R.id.action_searchFragment_to_backupAndRestoreFragment)
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