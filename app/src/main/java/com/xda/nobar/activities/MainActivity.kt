package com.xda.nobar.activities

import android.animation.LayoutTransition
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.xda.nobar.R
import com.xda.nobar.activities.ui.IntroActivity
import kotlinx.android.synthetic.main.activity_main.*

/**
 * The main app activity
 */
class MainActivity : AppCompatActivity() {
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

    private val navController: NavController
        get() = findNavController(R.id.main_host)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        IntroActivity.needsToRunAsync(this) {
            if (it) IntroActivity.start(this)
        }

        setContentView(R.layout.activity_main)

        root.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        setUpActionBar()
    }

    /**
     * Add buttons to the action bar
     */
    private fun setUpActionBar() {
        setSupportActionBar(toolbar)

        val gear = LayoutInflater.from(this).inflate(R.layout.settings_button, toolbar, false)
        gear.setOnClickListener { navController.navigate(R.id.action_homeFragment_to_settingsActivity2) }

        val about = LayoutInflater.from(this).inflate(R.layout.help_button, toolbar, false)
        about.setOnClickListener { navController.navigate(R.id.action_homeFragment_to_helpAboutActivity) }

        toolbar.addView(gear)
        toolbar.addView(about)
    }
}
