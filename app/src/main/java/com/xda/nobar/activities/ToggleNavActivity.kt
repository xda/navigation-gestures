package com.xda.nobar.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.xda.nobar.App

/**
 * Simple activity to toggle NoBar's navbar hiding
 * This is exported and can be called from anywhere
 */
class ToggleNavActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val handler = application as App
        handler.toggleNavState()

        finish()
    }
}
