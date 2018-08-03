package com.xda.nobar.activities

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.interfaces.OnGestureStateChangeListener
import com.xda.nobar.interfaces.OnLicenseCheckResultListener
import com.xda.nobar.interfaces.OnNavBarHideStateChangeListener
import com.xda.nobar.util.Utils
import com.xda.nobar.views.BarView
import com.xda.nobar.views.TextSwitch

/**
 * The main app activity
 */
class MainActivity : AppCompatActivity(), OnGestureStateChangeListener, OnNavBarHideStateChangeListener, OnLicenseCheckResultListener {
    companion object {
        fun start(context: Context) {
            context.startActivity(makeIntent(context))
        }

        fun makeIntent(context: Context): Intent {
            val launch = Intent(context, MainActivity::class.java)
            launch.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK)
            return launch
        }
    }

    private val app by lazy { application as App }

    private val gestureSwitch by lazy { findViewById<TextSwitch>(R.id.activate) }
    private val hideNavSwitch by lazy { findViewById<TextSwitch>(R.id.hide_nav) }
    private val premStatus by lazy { findViewById<TextView>(R.id.prem_stat) }
    private val refresh by lazy { findViewById<ImageView>(R.id.refresh_prem) }

    private val navListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        app.toggleNavState(!isChecked)
        if (!IntroActivity.hasWss(this)) onNavStateChange(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IntroActivity.needsToRun(this)) {
            IntroActivity.start(this)
        }

        if (!Utils.canRunHiddenCommands(this)) {
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        setUpActionBar()

        app.addLicenseCheckListener(this)
        app.addGestureActivationListener(this)
        app.addNavBarHideListener(this)

        gestureSwitch.isChecked = app.areGesturesActivated()
        gestureSwitch.onCheckedChangeListener = CompoundButton.OnCheckedChangeListener { button, isChecked ->
            if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this))
                    && Utils.isAccessibilityEnabled(this)) {
                if (isChecked) app.addBar() else app.removeBar()
                app.setGestureState(isChecked)
            } else {
                button.isChecked = false
                IntroActivity.start(this)
            }
        }

        hideNavSwitch.isChecked = Utils.shouldUseOverscanMethod(this)
        hideNavSwitch.onCheckedChangeListener = navListener

        refresh.setOnClickListener {
            refresh()
        }

        refresh()
    }

    override fun onGestureStateChange(barView: BarView?, activated: Boolean) {
        gestureSwitch.isChecked = activated
    }

    override fun onNavStateChange(hidden: Boolean) {
        hideNavSwitch.onCheckedChangeListener = null
        hideNavSwitch.isChecked = hidden
        hideNavSwitch.onCheckedChangeListener = navListener
    }

    override fun onResult(valid: Boolean, reason: String?) {
        runOnUiThread {
            premStatus.setTextColor(if (valid) Color.GREEN else Color.RED)
            premStatus.text = resources.getText(if (valid) R.string.installed else R.string.not_found)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        app.removeLicenseCheckListener(this)
        app.removeGestureActivationListener(this)
        app.removeNavbarHideListener(this)

        try {
            app.removeGestureActivationListener(this)
        } catch (e: Exception) {}
    }

    private fun refresh() {
        premStatus.setTextColor(Color.YELLOW)
        premStatus.text = resources.getText(R.string.checking)

        app.refreshPremium()
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
