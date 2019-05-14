package com.xda.nobar.activities.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.xda.nobar.R
import com.xda.nobar.receivers.StartupReceiver
import com.xda.nobar.util.relaunch
import kotlinx.android.synthetic.main.activity_crash.*

class CrashActivity : AppCompatActivity() {
    private var isForCrashlytics = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crash)

        enable_c_id.setOnClickListener {
            isForCrashlytics = true
            finish()
        }

        relaunch.setOnClickListener {
            val relaunchIntent = Intent(this, StartupReceiver::class.java)
            relaunchIntent.action = StartupReceiver.ACTION_RELAUNCH

            sendBroadcast(relaunchIntent)

            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        relaunch(isForCrashlytics)
    }
}
