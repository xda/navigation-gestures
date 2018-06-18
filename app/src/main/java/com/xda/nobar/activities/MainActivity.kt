package com.xda.nobar.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.widget.CompoundButton
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.interfaces.OnGestureStateChangeListener
import com.xda.nobar.interfaces.OnNavBarHideStateChangeListener
import com.xda.nobar.util.Utils
import com.xda.nobar.views.TextSwitch

/**
 * The main app activity
 */
class MainActivity : AppCompatActivity(), OnGestureStateChangeListener, OnNavBarHideStateChangeListener {
    private lateinit var gestureSwitch: TextSwitch
    private lateinit var hideNavSwitch: TextSwitch
    private lateinit var handler: App
    private lateinit var prefs: SharedPreferences

    private val navListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        handler.toggleNavState(!isChecked)
    }

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

        gestureSwitch = findViewById(R.id.activate)
        hideNavSwitch = findViewById(R.id.hide_nav)
        handler = application as App
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        handler.refreshPremium()
        handler.addGestureActivationListener(this)
        handler.addNavBarHideListener(this)

        gestureSwitch.isChecked = handler.areGesturesActivated()
        gestureSwitch.onCheckedChangeListener = CompoundButton.OnCheckedChangeListener { button, isChecked ->
            if (!IntroActivity.needsToRun(this)) {
                if (isChecked) handler.addBar() else handler.removeBar()
                handler.setGestureState(isChecked)
            } else {
                button.isChecked = false
                startActivity(Intent(this, IntroActivity::class.java))
            }
        }

        hideNavSwitch.isChecked = Utils.shouldUseOverscanMethod(this) && handler.isNavBarHidden()
        hideNavSwitch.onCheckedChangeListener = navListener
    }

    override fun onResume() {
        super.onResume()

        handler.refreshPremium()
    }

    override fun onGestureStateChange(activated: Boolean) {
        gestureSwitch.isChecked = activated
    }

    override fun onNavBarHideStateChange(hidden: Boolean) {
        hideNavSwitch.onCheckedChangeListener = null
        hideNavSwitch.isChecked = hidden
        hideNavSwitch.onCheckedChangeListener = navListener
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            handler.removeGestureActivationListener(this)
        } catch (e: Exception) {}
    }

    /**
     * Add buttons to the action bar
     */
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
