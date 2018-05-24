package com.xda.nobar.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.xda.nobar.App

/**
 * Simple activity that can be called from anywhere to toggle NoBar
 */
class ToggleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!IntroActivity.needsToRun(this)) {
            (application as App).toggle()
        }

        finish()
    }
}
