package com.xda.nobar.activities.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.xda.nobar.R
import com.xda.nobar.fragments.settings.MainFragment
import com.xda.nobar.util.beginAnimatedTransaction

/**
 * The configuration activity
 */
class SettingsActivity : AppCompatActivity() {
    companion object {
        const val REQ_APP = 10
        const val REQ_INTENT = 11
        const val REQ_SHORTCUT = 12
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        supportFragmentManager
                .beginAnimatedTransaction()
                .replace(R.id.content, MainFragment())
                .addToBackStack("main")
                .commit()
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
        if (supportFragmentManager.backStackEntryCount > 1) {
            supportFragmentManager
                    .popBackStack()
        } else {
            finish()
        }
    }
}
