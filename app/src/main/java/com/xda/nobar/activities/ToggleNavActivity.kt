package com.xda.nobar.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.xda.nobar.App

class ToggleNavActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val handler = application as App
        handler.toggleNavState()

        finish()
    }
}
