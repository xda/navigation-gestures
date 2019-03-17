package com.xda.nobar.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.xda.nobar.util.app

/**
 * Simple activity to toggle NoBar's navbar hiding
 * This is exported and can be called from anywhere
 */
class ToggleNavActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        app.toggleNavState()

        finish()
    }
}
