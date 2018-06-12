package com.xda.nobar.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.xda.nobar.App

class ToggleGesturesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!IntroActivity.needsToRun(this)) (application as App).toggleGestureBar()

        finish()
    }
}
