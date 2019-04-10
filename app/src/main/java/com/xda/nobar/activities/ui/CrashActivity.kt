package com.xda.nobar.activities.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.appcompat.app.AppCompatActivity
import com.xda.nobar.R
import com.xda.nobar.receivers.StartupReceiver
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
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        val startup = if (isForCrashlytics)
            PendingIntent.getActivity(
                    this,
                    11,
                    Intent(this, HelpAboutActivity::class.java),
                    PendingIntent.FLAG_ONE_SHOT
            )
        else
            PendingIntent.getBroadcast(
                this,
                10,
                Intent(this, StartupReceiver::class.java).apply { action = StartupReceiver.ACTION_RELAUNCH },
                PendingIntent.FLAG_ONE_SHOT
        )

        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 100, startup)

        Process.killProcess(Process.myPid())
    }
}
