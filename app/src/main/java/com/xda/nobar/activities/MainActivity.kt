package com.xda.nobar.activities

import android.animation.LayoutTransition
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.opengl.Visibility
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.xda.nobar.R
import com.xda.nobar.activities.ui.HelpAboutActivity
import com.xda.nobar.activities.ui.IntroActivity
import com.xda.nobar.activities.ui.SettingsActivity
import com.xda.nobar.activities.ui.TroubleshootingActivity
import com.xda.nobar.interfaces.OnGestureStateChangeListener
import com.xda.nobar.interfaces.OnLicenseCheckResultListener
import com.xda.nobar.interfaces.OnNavBarHideStateChangeListener
import com.xda.nobar.util.*
import com.xda.nobar.views.BarView
import kotlinx.android.synthetic.main.activity_main.*

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

    private val navListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        app.toggleNavState(!isChecked)
        if (!hasWss) onNavStateChange(!isChecked)
        if (hasWss) fix_immersive_wrapper.visibility = if (isChecked) View.GONE else View.VISIBLE
    }

    private var currentPremReason: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IntroActivity.needsToRun(this)) {
            IntroActivity.start(this)
        }

        allowHiddenMethods()

        setContentView(R.layout.activity_main)
        setUpActionBar()

        root.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        app.addLicenseCheckListener(this)
        app.addGestureActivationListener(this)
        app.addNavBarHideListener(this)

        activate.isChecked = prefManager.isActive
        activate.onCheckedChangeListener = CompoundButton.OnCheckedChangeListener { button, isChecked ->
            if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this))
                    && isAccessibilityEnabled) {
                if (isChecked) app.addBar() else app.removeBar()
                app.setGestureState(isChecked)
            } else {
                button.isChecked = false
                IntroActivity.start(this)
            }
        }

        hide_nav.isChecked = prefManager.shouldUseOverscanMethod
        hide_nav.onCheckedChangeListener = navListener

        refresh_prem.setOnClickListener {
            refresh()
        }

        prem_stat_clicker.setOnClickListener {
            AlertDialog.Builder(this)
                    .setMessage(currentPremReason)
                    .show()
        }

        fix_immersive_wrapper.visibility = if (hasWss && !hide_nav.isChecked) View.VISIBLE else View.GONE
        fix_immersive.setOnClickListener {
            runSecureSettingsAction {
                Settings.Global.putString(contentResolver, POLICY_CONTROL, null)
            }
        }

        troubleshoot.setOnClickListener {
            startActivity(Intent(this, TroubleshootingActivity::class.java))
        }

        refresh()
    }

    override fun onGestureStateChange(barView: BarView?, activated: Boolean) {
        activate.isChecked = activated
    }

    override fun onNavStateChange(hidden: Boolean) {
        hide_nav.onCheckedChangeListener = null
        hide_nav.isChecked = hidden
        hide_nav.onCheckedChangeListener = navListener
    }

    override fun onResult(valid: Boolean, reason: String?) {
        currentPremReason = reason
        runOnUiThread {
            prem_stat.setTextColor(if (valid) Color.GREEN else Color.RED)
            prem_stat.text = resources.getText(if (valid) R.string.installed else R.string.not_found)
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
        prem_stat.setTextColor(Color.YELLOW)
        prem_stat.text = resources.getText(R.string.checking)

        app.refreshPremium()
    }

    /**
     * Add buttons to the action bar
     */
    private fun setUpActionBar() {
        setSupportActionBar(toolbar)

        val gear = LayoutInflater.from(this).inflate(R.layout.settings_button, toolbar, false)
        gear.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }

        val about = LayoutInflater.from(this).inflate(R.layout.help_button, toolbar, false)
        about.setOnClickListener { startActivity(Intent(this, HelpAboutActivity::class.java)) }

        toolbar.addView(gear)
        toolbar.addView(about)
    }
}
