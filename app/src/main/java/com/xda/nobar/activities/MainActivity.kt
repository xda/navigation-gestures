package com.xda.nobar.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.widget.Switch
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.util.Utils

class MainActivity : AppCompatActivity(), App.ActivationListener {
    private lateinit var switch: Switch
    private lateinit var handler: App
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IntroActivity.needsToRun(this)) {
            val intent = Intent(this, IntroActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        if (!Utils.canRunHiddenCommands(this)) {
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        setUpActionBar()

        switch = findViewById(R.id.activate)
        handler = application as App
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        handler.refreshPremium()
        handler.addActivationListener(this)

        switch.isChecked = handler.isActivated()
        switch.setOnCheckedChangeListener { _, isChecked ->
            handler.toggle(!isChecked)
        }
    }

    override fun onChange(activated: Boolean) {
        switch.isChecked = activated
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            handler.removeActivationListener(this)
        } catch (e: Exception) {}
    }

    private fun setUpActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val gear = LayoutInflater.from(this).inflate(R.layout.settings_button, toolbar, false)
        gear.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }

        val about = LayoutInflater.from(this).inflate(R.layout.help_button, toolbar, false)
        about.setOnClickListener { startActivity(Intent(this, HelpAboutActivity::class.java)) }

        toolbar.addView(gear)
        toolbar.addView(about)
    }
}
