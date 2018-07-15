package com.xda.nobar.activities

import android.content.*
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceFragment
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.widget.Toast
import com.xda.nobar.BuildConfig
import com.xda.nobar.R

/**
 * Information about the app
 */
class HelpAboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        fragmentManager?.beginTransaction()?.replace(R.id.content, HelpFragment())?.commit()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Main fragment for the activity
     */
    class HelpFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            addPreferencesFromResource(R.xml.prefs_about)

            fillInVersion()
            fillInOverscan()
            addTutorialListener()
            addEmailListener()
            addThreadListener()
            addOtherAppsListener()
            addPremiumListener()
            addLibListener()
        }

        private fun fillInVersion() {
            val pref = findPreference("version")

            pref.summary = BuildConfig.VERSION_NAME
            pref.setOnPreferenceClickListener {
                val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val data = ClipData.newPlainText("Navigation Gestures version", it.summary)
                cm.primaryClip = data

                true
            }
        }

        private fun fillInOverscan() {
            val pref = findPreference("current_overscan")

            pref.summary = Rect().apply { activity.display.getOverscanInsets(this) }.toShortString()
        }

        private fun addTutorialListener() {
            val pref = findPreference("tutorial_video")

            pref.setOnPreferenceClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://youtu.be/H_kT-YoPjAU")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                true
            }
        }

        private fun addEmailListener() {
            val pref = findPreference("email_us")

            pref.setOnPreferenceClickListener {
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.data = Uri.parse("mailto:")
                intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("navigationgestures@xda-developers.com"))
                intent.putExtra(Intent.EXTRA_SUBJECT, resources.getString(R.string.app_name))
                intent.putExtra(Intent.EXTRA_TEXT, "Version: ${BuildConfig.VERSION_NAME}")
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    val clipboardManager = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboardManager.primaryClip = ClipData(ClipDescription("email", arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN)), ClipData.Item("navigationgestures@xda-developers.com"))
                    Toast.makeText(activity, resources.getText(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                }
                true
            }
        }

        private fun addThreadListener() {
            val pref = findPreference("xda_thread")
            val intent = Intent(Intent.ACTION_VIEW)

            intent.data = Uri.parse("https://forum.xda-developers.com/android/apps-games/official-xda-navigation-gestures-iphone-t3792361")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            pref.setOnPreferenceClickListener {
                startActivity(intent)
                true
            }
        }

        private fun addOtherAppsListener() {
            val pref = findPreference("other_apps")
            val intent = Intent(Intent.ACTION_VIEW)

            intent.data = Uri.parse("https://play.google.com/store/apps/developer?id=XDA")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            pref.setOnPreferenceClickListener {
                startActivity(intent)
                true
            }
        }

        private fun addPremiumListener() {
            val pref = findPreference("buy_premium")
            val intent = Intent(Intent.ACTION_VIEW)

            intent.data = Uri.parse("https://play.google.com/store/apps/details?id=com.xda.nobar.premium")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            pref.setOnPreferenceClickListener {
                startActivity(intent)
                true
            }
        }

        private fun addLibListener() {
            val pref = findPreference("libraries")
            val intent = Intent(activity, LibraryActivity::class.java)

            pref.setOnPreferenceClickListener {
                startActivity(intent)
                true
            }
        }
    }
}
