package com.xda.nobar.fragments.settings

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.crashlytics.android.Crashlytics
import com.xda.nobar.BuildConfig
import com.xda.nobar.R
import com.xda.nobar.activities.ui.LibraryActivity
import com.xda.nobar.util.PrefManager
import com.xda.nobar.util.launchUrl
import com.xda.nobar.util.prefManager

/**
 * Main fragment for the HelpAboutActivity
 */
class HelpFragment : PreferenceFragmentCompat() {
    companion object {
        const val VERSION = "version"
        const val TUTORIAL_VIDEO = "tutorial_video"
        const val FEEDBACK = "feedback"
        const val XDA_THREAD = "xda_thread"
        const val OTHER_APPS = "other_apps"
        const val BUY_PREMIUM = "buy_premium"
        const val LIBRARIES = "libraries"
        const val CRASHLYTICS_ID = "crashlytics_id"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.prefs_about)

        fillInVersion()
        fillInOverscan()
        addTutorialListener()
        addFeedbackListener()
        addThreadListener()
        addOtherAppsListener()
        addPremiumListener()
        addLibListener()
        crashlyticsStuff()
    }

    private fun fillInVersion() {
        val pref = findPreference<Preference>(VERSION)

        pref.summary = BuildConfig.VERSION_NAME
        pref.setOnPreferenceClickListener {
            val cm = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val data = ClipData.newPlainText("Navigation Gestures version", it.summary)
            cm.primaryClip = data

            true
        }
    }

    private fun fillInOverscan() {
//            val pref = findPreference("current_overscan")

//            pref.summary = Rect().apply { activity.display.getOverscanInsets(this) }.toShortString()

//            preferenceScreen.removePreference(pref)
    }

    private fun addTutorialListener() {
        val pref = findPreference<Preference>(TUTORIAL_VIDEO)

        pref.setOnPreferenceClickListener {
            context?.launchUrl("https://youtu.be/H_kT-YoPjAU")
            true
        }
    }

    private fun addFeedbackListener() {
        val pref = findPreference<Preference>(FEEDBACK)

        pref.setOnPreferenceClickListener {
            context?.launchUrl("https://github.com/zacharee/nobar-issues")
            true
        }
    }

    private fun addThreadListener() {
        val pref = findPreference<Preference>(XDA_THREAD)

        pref.setOnPreferenceClickListener {
            context?.launchUrl("https://forum.xda-developers.com/android/apps-games/official-xda-navigation-gestures-iphone-t3792361")
            true
        }
    }

    private fun addOtherAppsListener() {
        val pref = findPreference<Preference>(OTHER_APPS)

        pref.setOnPreferenceClickListener {
            context?.launchUrl("https://play.google.com/store/apps/developer?id=XDA")
            true
        }
    }

    private fun addPremiumListener() {
        val pref = findPreference<Preference>(BUY_PREMIUM)

        pref.setOnPreferenceClickListener {
            context?.launchUrl("https://play.google.com/store/apps/details?id=com.xda.nobar.premium")
            true
        }
    }

    private fun addLibListener() {
        val pref = findPreference<Preference>(LIBRARIES)
        val intent = Intent(activity, LibraryActivity::class.java)

        pref.setOnPreferenceClickListener {
            startActivity(intent)
            true
        }
    }

    private fun crashlyticsStuff() {
        val switch = findPreference<SwitchPreference>(PrefManager.ENABLE_CRASHLYTICS_ID)
        val id = findPreference<Preference>(CRASHLYTICS_ID)

        switch.setOnPreferenceChangeListener { _, newValue ->
            updateCrashlyticsId(newValue.toString().toBoolean())

            true
        }

        updateCrashlyticsId(switch.isChecked)

        id.setOnPreferenceClickListener {
            if (!it.summary.isNullOrBlank()) {
                val cm = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val data = ClipData.newPlainText(id.title, id.summary)
                cm.primaryClip = data

                Toast.makeText(activity, resources.getText(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
            }

            true
        }
    }

    private fun updateCrashlyticsId(enabled: Boolean) {
        val id = findPreference<Preference>(CRASHLYTICS_ID)

        id.summary = if (enabled) activity!!.prefManager.crashlyticsId else ""
        Crashlytics.setUserIdentifier(id.summary.toString())
    }
}