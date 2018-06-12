package com.xda.nobar.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.xda.nobar.App

class ToggleNavActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val handler = application as App

        val hidden = handler.isNavBarHidden()
        if (!IntroActivity.needsToRun(this)) {
            handler.prefs.edit().putBoolean("hide_nav", !hidden).apply()
            if (!handler.isNavBarHidden()) handler.hideNav() else handler.showNav()
        }
    }
}
